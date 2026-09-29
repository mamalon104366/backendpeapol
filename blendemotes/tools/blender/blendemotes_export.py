"""
BlendEmotes - exportador del rig `emote_creator.blend` (rig 2.0 + BlendEmotes).

Este módulo es el exportador de verdad. Lo usan:
  * el botón **Export** del panel de la acción (el .blend lo lleva dentro como el texto
    `blendemotes_export.py`; ver tools/blender/upgrade_rig.py), y
  * `tools/blender/batch_export.py` (exportación por lotes desde la línea de comandos).

Usa las funciones de exportación que trae el rig (`collect_animation_data.py` y
`set_up_bedrock.py`) y añade:

  * **Codos y rodillas hacia los lados.** Si el hueso `*_bend` gira en Z (o en Y), el canal
    `bend` se escribe como `"vector": [x, y, z]` (grados, la rotación del hueso de doblez sobre
    sus propios ejes). Si solo se dobla hacia delante/atrás se escribe `"value"` como siempre
    (compatible con Emotecraft).
  * **Palma y pie.** Los huesos `right_hand` / `left_hand` (palma) y `right_foot` / `left_foot`
    (pie) se escriben como el canal `tip` de su brazo/pierna: `"tip": {"vector": [x, y, z]}`
    (grados, la rotación del hueso sobre sus propios ejes, iguales a los del hueso de doblez).
  * **Modelos.** Los objetos de malla de la colección de modelos de la acción, emparentados a
    un hueso del rig, viajan con el emote (micrófono, guitarra, caballo...). Se escriben en
    `"blendemotes": {"models": [...]}` con su textura.
  * Las correcciones del exportador del rig: manejadores Bézier con el signo del eje y en
    grados en el canal `bend`, y tiempos sin redondear a 3 decimales.
"""

import base64
import json
import math
import os
import struct
import tempfile
import zlib

import bpy

EXPORTER_VERSION = "1.2"
TIME_DECIMALS = 6
RIG_NAME = "export_armature"
DEFAULT_EXPORT_BONES = [
    "body", "body_control",
    "head",
    "left_arm", "right_arm",
    "left_leg", "right_leg",
    "waist", "torso", "cape",
    "left_item", "right_item",
]
BENDABLE = ["left_arm", "right_arm", "left_leg", "right_leg", "torso", "cape"]
# Palma y pie de cada extremidad (rig de BlendEmotes 1.2+).
TIPS = {"right_arm": "right_hand", "left_arm": "left_hand", "right_leg": "right_foot", "left_leg": "left_foot"}
# Bones the mod knows by name (they need no pivot in the file).
DEFAULT_BONES = ["body", "head", "left_arm", "left_leg", "right_arm", "right_leg", "torso", "left_arm_bend",
                 "left_leg_bend", "right_arm_bend", "right_leg_bend", "torso_bend", "right_item", "left_item",
                 "cape", "cape_bend"] + list(TIPS.values())
# A bend axis whose keys all stay below this (degrees) counts as not animated.
BEND_EPSILON = 1e-3


# ---------------------------------------------------------------------------- utilities

def frame_rate(scene):
    return scene.render.fps / scene.render.fps_base


def find_rig():
    rig = bpy.data.objects.get(RIG_NAME)
    if rig is None or rig.type != 'ARMATURE':
        raise RuntimeError(f"No se encontró el armature '{RIG_NAME}' en el .blend")
    return rig


def ensure_action_panel_registered():
    """El rig guarda los metadatos del emote en `action.emote`, que solo existe cuando el
    script del panel está registrado."""
    if hasattr(bpy.types.Action, "emote"):
        return
    text = bpy.data.texts.get("action_settings_panel.py")
    if text is None:
        raise RuntimeError("Este .blend no contiene 'action_settings_panel.py'. ¿Es el rig emote_creator 2.0?")
    text.as_module().register()


def assign_action(rig, action):
    rig.animation_data.action = action
    # Blender 4.4+ usa "slots" de acción; asignamos el primero compatible.
    if hasattr(rig.animation_data, "action_slot"):
        if rig.animation_data.action_slot is None:
            suitable = list(getattr(rig.animation_data, "action_suitable_slots", []))
            if suitable:
                rig.animation_data.action_slot = suitable[0]
            elif len(action.slots) > 0:
                rig.animation_data.action_slot = action.slots[0]


def render_icon(scene, frame, engine_override=None):
    scene.frame_set(frame)
    engines = [engine_override] if engine_override else [scene.render.engine]
    original = scene.render.engine
    try:
        for engine in engines:
            try:
                scene.render.engine = engine
            except TypeError:
                continue
            try:
                bpy.ops.render.render()
            except Exception as ex:  # motor no disponible
                print(f"  icono: el motor {engine} falló ({ex})")
                continue
            image = bpy.data.images.get("Render Result")
            if image is None:
                continue
            with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
                path = tmp.name
            try:
                image.save_render(path)
                with open(path, "rb") as f:
                    return base64.b64encode(f.read()).decode("ascii")
            finally:
                os.remove(path)
    finally:
        scene.render.engine = original
    return None


# ---------------------------------------------------------------------------- the rig's exporter, fixed

def patch_exporter(bedrock):
    """Corrige, sobre el módulo `set_up_bedrock.py` del rig, los fallos del exportador y le
    enseña a escribir el doblez en las tres direcciones."""
    signs = {}  # puntero del keyframe -> signo del eje con el que se escribe su valor

    original_write_mode = bedrock.write_mode
    original_axis_difference = bedrock.get_bone_axis_difference

    def get_bone_axis_difference(rig_object, bone_name, mode):
        # canal de doblez en 3D: los valores son la rotación del hueso de doblez sobre sus propios
        # ejes, tal cual (X, Y, Z)
        if bone_name.endswith("_bendvec"):
            return [(0, 1.0), (1, 1.0), (2, 1.0)]
        return original_axis_difference(rig_object, bone_name, mode)

    def write_mode(bone_name, mode, animation_data, rig_object, default_bones, export_bones):
        signs.clear()
        if mode == 'bend':
            key = f"{bone_name}_bend"
            fcurves = animation_data[key]["rotation"] if key in default_bones and key in animation_data else None
        else:
            fcurves = animation_data.get(bone_name, {}).get(mode)
        if fcurves and any(fc is not None and len(fc.keyframe_points) for fc in fcurves):
            difference = get_bone_axis_difference(rig_object, bone_name, mode)
            for index, fcurve in enumerate(fcurves):
                if fcurve is None or index > 2:
                    continue
                sign = difference[index][1]
                for point in fcurve.keyframe_points:
                    signs[point.as_pointer()] = sign
        return original_write_mode(bone_name, mode, animation_data, rig_object, default_bones, export_bones)

    def get_bezier_args(keyframe, mode, multiplier):
        fps = frame_rate(bpy.context.scene)
        sign = signs.get(keyframe.as_pointer(), multiplier)
        left_y = (keyframe.handle_left.y - keyframe.co.y) * sign
        left_x = (keyframe.handle_left.x - keyframe.co.x) / fps
        right_y = (keyframe.handle_right.y - keyframe.co.y) * sign
        right_x = (keyframe.handle_right.x - keyframe.co.x) / fps
        if mode == "position":
            left_y *= 4
            right_y *= 4
        if mode in ("rotation", "bend"):
            left_y = math.degrees(left_y)
            right_y = math.degrees(right_y)
        return [round(x, 6) for x in (left_y, left_x, right_y, right_x)]

    def fcurves_to_mode_dict(fcurves, is_bend=False):
        fps = frame_rate(bpy.context.scene)
        maps = []
        for fcurve in fcurves:
            maps.append({} if fcurve is None else {round(k.co.x, 6): k for k in fcurve.keyframe_points})
        frames = set()
        for m in maps:
            frames |= set(m.keys())
        result = {}
        for frame in sorted(frames):
            time = round(frame / fps, TIME_DECIMALS)
            if is_bend:
                vector = [maps[0].get(frame, "pal.disabled"), "pal.disabled", "pal.disabled"]
            else:
                vector = [m.get(frame, "pal.disabled") for m in maps[:3]]
            result[time] = {"vector": vector}
        return result

    bedrock.get_bone_axis_difference = get_bone_axis_difference
    bedrock.write_mode = write_mode
    bedrock.get_bezier_args = get_bezier_args
    bedrock.fcurves_to_mode_dict = fcurves_to_mode_dict


MECHANISM_SUFFIXES = ("_vanilla", "_vanilla_crutch")


def reset_mechanism(rig):
    """Los huesos auxiliares del modo vanilla (`*_vanilla`, `*_vanilla_crutch`) no se animan: los
    mueven sus restricciones. El horneado de Blender les deja la pose visual como pose propia y,
    como sus restricciones se aplican encima, cada exportación los desplazaba un poco más (el
    rig original exportaba distinto cada vez). Se dejan en reposo antes de hornear."""
    for pb in rig.pose.bones:
        if pb.name.endswith(MECHANISM_SUFFIXES):
            pb.location = (0.0, 0.0, 0.0)
            pb.rotation_quaternion = (1.0, 0.0, 0.0, 0.0)
            pb.rotation_euler = (0.0, 0.0, 0.0)
            pb.rotation_axis_angle = (0.0, 0.0, 1.0, 0.0)
            pb.scale = (1.0, 1.0, 1.0)


def reset_pose(rig):
    """Toda la pose en reposo. Los canales sin claves conservan lo que dejó la última acción que se
    vio en Blender; así lo que se exporta (y lo que se muestrea para las pruebas) depende solo de
    la acción."""
    for pb in rig.pose.bones:
        pb.location = (0.0, 0.0, 0.0)
        pb.rotation_quaternion = (1.0, 0.0, 0.0, 0.0)
        pb.rotation_euler = (0.0, 0.0, 0.0)
        pb.rotation_axis_angle = (0.0, 0.0, 1.0, 0.0)
        pb.scale = (1.0, 1.0, 1.0)


def snapshot_pose(rig):
    return {pb.name: (pb.location.copy(), pb.rotation_quaternion.copy(), pb.rotation_euler.copy(),
                      tuple(pb.rotation_axis_angle), pb.scale.copy()) for pb in rig.pose.bones}


def restore_pose(rig, snapshot):
    for pb in rig.pose.bones:
        loc, quat, euler, axis_angle, scale = snapshot[pb.name]
        pb.location = loc
        pb.rotation_quaternion = quat
        pb.rotation_euler = euler
        pb.rotation_axis_angle = axis_angle
        pb.scale = scale


LIMBS = ["left_arm", "right_arm", "left_leg", "right_leg"]


def baked_bones(rig, export_bones):
    """Huesos que hornea `collect_animation_data` (añade los de doblez y los del modo vanilla)."""
    names = list(export_bones)
    names += [b + "_bend" for b in BENDABLE] + [b + "_vanilla" for b in LIMBS]
    return [n for n in dict.fromkeys(names) if n in rig.pose.bones]


class keyed_source:
    """Acción de la que exporta el rig: una copia de la acción con una clave (el valor que ya tiene la
    pose, así que no cambia nada) en cada canal de los huesos exportados que no tenía ninguna.

    El exportador del rig solo mezcla lo horneado en canales que ya tienen curva: un hueso movido
    solo por el IK o por una restricción (la pierna cuando solo se mueve el cubo del pie) salía
    congelado. Después de hornear, `prune` quita esas claves donde el horneado no añadió
    movimiento, para que un canal sin animar siga sin animar (en el juego sigue la pose normal del
    jugador). La acción original no se toca: la copia ocupa su nombre mientras dura la exportación.
    """

    def __init__(self, rig, action, bones):
        self.rig = rig
        self.action = action
        self.bones = bones
        self.name = action.name
        self.copy = None
        self.created = []

    def __enter__(self):
        from bpy_extras import anim_utils
        rig, action = self.rig, self.action
        frame = int(action.frame_start) if action.use_frame_range else 0
        self.copy = action.copy()
        action.name = self.name + ".blendemotes_original"
        self.copy.name = self.name
        rig.animation_data.action = self.copy
        slot = rig.animation_data.action_slot
        if slot is None:
            suitable = list(getattr(rig.animation_data, "action_suitable_slots", []))
            slot = suitable[0] if suitable else (self.copy.slots[0] if len(self.copy.slots) else None)
            rig.animation_data.action_slot = slot
        bag = anim_utils.action_get_channelbag_for_slot(self.copy, slot)
        for name in self.bones:
            pb = self.rig.pose.bones[name]
            for prop in ("location", "rotation_euler", "scale"):
                path = f'pose.bones["{name}"].{prop}'
                values = getattr(pb, prop)
                for index in range(3):
                    if bag.fcurves.find(path, index=index) is None:
                        fc = bag.fcurves.new(path, index=index, group_name=name)
                        fc.keyframe_points.insert(frame, values[index])
                        self.created.append((name, prop, index))
        return self

    def prune(self, animation_data):
        """Quita de los datos recogidos los canales añadidos en los que no se mezcló nada horneado."""
        modes = {"location": "position", "rotation_euler": "rotation", "scale": "scale"}
        for name, prop, index in self.created:
            curves = animation_data.get(name, {}).get(modes[prop])
            if curves and index < len(curves) and curves[index] is not None and len(curves[index].keyframe_points) <= 1:
                curves[index] = None

    def __exit__(self, *exc):
        self.rig.animation_data.action = self.action
        if self.copy is not None:
            bpy.data.actions.remove(self.copy)
        self.action.name = self.name
        assign_action(self.rig, self.action)
        return False


def load_exporter():
    collect = bpy.data.texts['collect_animation_data.py'].as_module()
    bedrock = bpy.data.texts['set_up_bedrock.py'].as_module()
    patch_exporter(bedrock)
    return collect.collect_animation_data, bedrock


# ---------------------------------------------------------------------------- bends in 3D

def _moves(fcurve):
    if fcurve is None:
        return False
    return any(abs(math.degrees(k.co.y)) > BEND_EPSILON for k in fcurve.keyframe_points)


def write_bend_vectors(anim, animation_data, rig, bedrock, export_bones):
    """Los huesos que se doblan de lado (o giran) llevan `bend` como vector [x, y, z]."""
    for bone in BENDABLE:
        key = f"{bone}_bend"
        data = animation_data.get(key)
        if not data:
            continue
        curves = data["rotation"]
        if not (_moves(curves[1]) or _moves(curves[2])):
            continue  # solo hacia delante/atrás: se queda el "value" de siempre
        fake = {f"{bone}_bendvec": {"rotation": curves}}
        vec = bedrock.write_mode(f"{bone}_bendvec", "rotation", fake, rig, DEFAULT_BONES, export_bones)
        if vec:
            anim["bones"].setdefault(bone, {})["bend"] = vec


def write_tip_vectors(anim, animation_data, rig, bedrock, export_bones):
    """La palma y el pie van en el canal `tip` de su brazo/pierna, como vector [x, y, z]."""
    for part, tip in TIPS.items():
        data = animation_data.get(tip)
        if not data or "rotation" not in data:
            continue
        curves = data["rotation"]
        if not any(_moves(c) for c in curves[:3]):
            continue  # recta todo el emote: no se escribe
        fake = {f"{tip}_bendvec": {"rotation": curves}}
        vec = bedrock.write_mode(f"{tip}_bendvec", "rotation", fake, rig, DEFAULT_BONES, export_bones)
        if vec:
            anim["bones"].setdefault(part, {})["tip"] = vec


def strip_tip_bones(anim):
    """El exportador del rig escribe la palma y el pie como huesos sueltos: se quitan (van en `tip`)."""
    tips = set(TIPS.values())
    for tip in tips:
        anim.get("bones", {}).pop(tip, None)
        anim.get("model", {}).pop(tip, None)
    parents = anim.get("parents", {})
    for bone in [b for b, p in parents.items() if b in tips or p in tips]:
        del parents[bone]


# ---------------------------------------------------------------------------- models

def _to_model_space(v):
    """Blender (armature space, 1 unit = 4 px) -> Minecraft model space (px, Y down, origin at the neck)."""
    return (4.0 * v[0], 24.0 - 4.0 * v[2], 4.0 * v[1])


def _to_model_dir(v):
    return (v[0], -v[2], v[1])


def _png(width, height, rgba_rows):
    """PNG (RGBA 8 bit) from rows of bytes, top row first."""
    raw = b"".join(b"\x00" + row for row in rgba_rows)

    def chunk(kind, data):
        c = kind + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xffffffff)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def _linear_to_srgb(c):
    c = max(0.0, min(1.0, c))
    return 12.92 * c if c <= 0.0031308 else 1.055 * (c ** (1 / 2.4)) - 0.055


def _image_png(image):
    """PNG bytes of a Blender image, read from its pixels (the image itself is not touched)."""
    width, height = image.size
    if width == 0 or height == 0:
        return None
    pixels = list(image.pixels[:])
    channels = image.channels
    rows = []
    for y in range(height - 1, -1, -1):  # Blender guarda las filas de abajo arriba
        row = bytearray()
        base = y * width * channels
        for x in range(width):
            i = base + x * channels
            r = pixels[i]
            g = pixels[i + 1] if channels > 1 else r
            b = pixels[i + 2] if channels > 2 else r
            a = pixels[i + 3] if channels > 3 else 1.0
            if image.is_float:
                r, g, b = _linear_to_srgb(r), _linear_to_srgb(g), _linear_to_srgb(b)
            row += bytes((int(round(max(0, min(1, r)) * 255)), int(round(max(0, min(1, g)) * 255)),
                          int(round(max(0, min(1, b)) * 255)), int(round(max(0, min(1, a)) * 255))))
        rows.append(bytes(row))
    return _png(width, height, rows)


def _material_texture(material):
    """(PNG bytes, doble cara) del material: la imagen conectada a Base Color, o su color."""
    if material is None:
        return _png(1, 1, [bytes((255, 255, 255, 255))]), True
    double_sided = not getattr(material, "use_backface_culling", False)
    color = (1.0, 1.0, 1.0, 1.0)
    if material.use_nodes and material.node_tree:
        for node in material.node_tree.nodes:
            if node.type != 'BSDF_PRINCIPLED':
                continue
            socket = node.inputs.get("Base Color")
            if socket is None:
                continue
            if socket.is_linked:
                source = socket.links[0].from_node
                if source.type == 'TEX_IMAGE' and source.image is not None:
                    png = _image_png(source.image)
                    if png:
                        return png, double_sided
            else:
                color = tuple(socket.default_value)
            break
    else:
        color = tuple(material.diffuse_color)
    pixel = bytes((int(round(_linear_to_srgb(color[0]) * 255)), int(round(_linear_to_srgb(color[1]) * 255)),
                   int(round(_linear_to_srgb(color[2]) * 255)), int(round(max(0, min(1, color[3])) * 255))))
    return _png(1, 1, [pixel]), double_sided


def _model_bone(ob, rig):
    """Hueso del rig del que cuelga el objeto (directamente o a través de sus padres)."""
    node = ob
    while node is not None:
        if node.parent == rig and node.parent_type == 'BONE' and node.parent_bone:
            return node.parent_bone
        node = node.parent
    return None


def model_objects(action, rig):
    collection = getattr(action.emote, "models_collection", None) if hasattr(action, "emote") else None
    if collection is None:
        return []
    return [ob for ob in collection.all_objects
            if ob.type == 'MESH' and not ob.hide_render and _model_bone(ob, rig) is not None]


def collect_models(action, rig):
    """Geometría (en la pose de reposo del rig) y textura de cada modelo de la acción."""
    objects = model_objects(action, rig)
    if not objects:
        return []
    models = []
    previous = rig.data.pose_position
    rig.data.pose_position = 'REST'
    bpy.context.view_layer.update()
    try:
        to_rig = rig.matrix_world.inverted()
        depsgraph = bpy.context.evaluated_depsgraph_get()
        for ob in objects:
            bone = _model_bone(ob, rig)
            evaluated = ob.evaluated_get(depsgraph)
            mesh = evaluated.to_mesh()
            try:
                mesh.calc_loop_triangles()
                matrix = to_rig @ ob.matrix_world
                normal_matrix = matrix.to_3x3().inverted().transposed()
                uv_layer = mesh.uv_layers.active
                corner_normals = getattr(mesh, "corner_normals", None)
                per_material = {}
                for tri in mesh.loop_triangles:
                    pos, uvs, nrm = per_material.setdefault(tri.material_index, ([], [], []))
                    for corner, loop in enumerate(tri.loops):
                        co = matrix @ mesh.vertices[tri.vertices[corner]].co
                        if corner_normals is not None:
                            n = corner_normals[loop].vector
                        else:
                            n = tri.split_normals[corner]
                        n = (normal_matrix @ n).normalized()
                        pos.extend(round(c, 5) for c in _to_model_space(co))
                        nrm.extend(round(c, 5) for c in _to_model_dir(n))
                        if uv_layer is not None:
                            uv = uv_layer.data[loop].uv
                            uvs.extend((round(uv.x, 6), round(1.0 - uv.y, 6)))
                        else:
                            uvs.extend((0.0, 0.0))
                for index, (pos, uvs, nrm) in per_material.items():
                    material = ob.material_slots[index].material if index < len(ob.material_slots) else None
                    texture, double_sided = _material_texture(material)
                    name = ob.name if len(per_material) == 1 else f"{ob.name}.{material.name if material else index}"
                    models.append({
                        "name": name,
                        "bone": bone,
                        "doubleSided": double_sided,
                        "texture": base64.b64encode(texture).decode("ascii"),
                        "positions": pos,
                        "uvs": uvs,
                        "normals": nrm,
                    })
            finally:
                evaluated.to_mesh_clear()
    finally:
        rig.data.pose_position = previous
        bpy.context.view_layer.update()
    return models


# ---------------------------------------------------------------------------- export

def export_action(rig, action, out_dir, with_icon=True, default_author=None, icon_engine=None):
    """Exporta una acción del rig a `<out_dir>/<acción>.json` y devuelve la ruta."""
    ensure_action_panel_registered()
    scene = bpy.context.scene
    collect_animation_data, bedrock = load_exporter()

    assign_action(rig, action)
    bpy.context.view_layer.objects.active = rig
    if bpy.context.object and bpy.context.object.mode != 'OBJECT':
        bpy.ops.object.mode_set(mode='OBJECT')

    export_bones = list(DEFAULT_EXPORT_BONES)
    for pivot_bone in action.emote.pivot_bones:
        if pivot_bone.name and pivot_bone.name not in export_bones:
            export_bones.append(pivot_bone.name)
    # los huesos propios de los que cuelgan modelos se exportan solos
    for ob in model_objects(action, rig):
        bone = _model_bone(ob, rig)
        base = bone[:-5] if bone.endswith("_bend") else bone
        if base not in export_bones and bone not in DEFAULT_BONES:
            export_bones.append(bone)

    # la palma y el pie se hornean con el resto y luego se escriben en el canal `tip`
    for tip in TIPS.values():
        if tip in rig.data.bones and tip not in export_bones:
            export_bones.append(tip)

    action_name = action.name
    preview_frame = scene.frame_current
    pose_before = snapshot_pose(rig)
    reset_pose(rig)
    scene.frame_set(0)
    with keyed_source(rig, action, baked_bones(rig, export_bones)) as source:
        animation_data, work_action = collect_animation_data(rig, export_bones)
        source.prune(animation_data)
        restore_pose(rig, pose_before)
        reset_mechanism(rig)
        emote = bedrock.create_emote(rig, export_bones, animation_data)
        anim = emote["animations"][action_name]
        strip_tip_bones(anim)
        if not rig.pose.bones["settings"]["vanilla"]:
            write_bend_vectors(anim, animation_data, rig, bedrock, export_bones)
            write_tip_vectors(anim, animation_data, rig, bedrock, export_bones)
        bpy.data.actions.remove(work_action)

    # padres de los huesos propios que cuelgan de un hueso normal del rig (el exportador del rig
    # solo apunta los hijos de los huesos propios)
    parents = anim.setdefault("parents", {})
    for bone_name in export_bones:
        if bone_name in DEFAULT_BONES or "_vanilla" in bone_name or bone_name not in rig.data.bones:
            continue
        parent = rig.data.bones[bone_name].parent
        if parent is not None and bone_name not in parents:
            parents[bone_name] = parent.name

    fps = frame_rate(scene)
    if "loopTick" in anim:
        start = int(action.frame_start) if action.use_frame_range else 0
        anim["loopTick"] = round((scene.frame_start - start) / fps, TIME_DECIMALS)
    own = {
        "rig": "emote_creator",
        "exactHandles": True,
        "fps": round(fps, 6),
        "exporter": EXPORTER_VERSION,
    }
    models = collect_models(action, rig)
    if models:
        own["models"] = models
    anim["blendemotes"] = own

    meta = anim["player_animation_library"]
    if not meta.get("name") or meta.get("name") == "Name":
        meta["name"] = action.name
    if meta.get("description") == "Description":
        meta["description"] = ""
    if default_author and (not meta.get("author") or meta.get("author") == "Author"):
        meta["author"] = default_author

    if with_icon:
        icon = render_icon(scene, preview_frame, icon_engine)
        if icon:
            meta["iconData"] = icon
        else:
            print("  icono: no se pudo renderizar, se exporta sin icono")
    scene.frame_set(preview_frame)

    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, action.name + ".json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(emote, f, ensure_ascii=False, indent=4)
    return path
