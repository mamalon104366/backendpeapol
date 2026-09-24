"""
BlendEmotes - exportador por lotes para el rig `emote_creator.blend` (rig 2.0).

Exporta TODAS las acciones (animaciones) del .blend a archivos .json que el mod
BlendEmotes carga directamente. Usa las mismas funciones de exportación que trae
el rig dentro del .blend (`collect_animation_data.py` y `set_up_bedrock.py`), así
que el resultado es el mismo que al pulsar "Export" en el panel de la acción, con tres
correcciones del exportador del rig:

  * los manejadores Bézier se multiplican por el signo del eje (el rig solo lo hace con
    los valores, así que en algunos ejes la curva salía con la forma invertida);
  * los manejadores del canal `bend` se escriben en grados (el rig los dejaba en radianes);
  * los tiempos no se redondean a 3 decimales (con 24 fps los fotogramas caían entre
    milisegundos).

Además añade la marca `"blendemotes": {"rig": "emote_creator", "exactHandles": true,
"fps": ...}` para que el mod no aplique sus correcciones automáticas a estos archivos.

Uso (Blender 5.2+ recomendado, igual que el rig):

    blender -b emote_creator.blend -P batch_export.py -- --out ./emotes
    blender -b emote_creator.blend -P batch_export.py -- --out ./emotes --actions cartwheel,inchworm
    blender -b emote_creator.blend -P batch_export.py -- --out ./emotes --no-icon

Opciones:
    --out DIR         Carpeta de salida (por defecto: carpeta del .blend / "emotes").
    --actions a,b,c   Solo exporta estas acciones (por defecto: todas las que tengan
                      rango de fotogramas mayor que cero).
    --no-icon         No renderiza el icono (más rápido; el mod usa un icono genérico).
    --icon-engine E   Motor para el icono (por defecto el de la escena, EEVEE). En
                      servidores sin GPU usa CYCLES.
    --author NOMBRE   Autor por defecto si la acción no tiene uno configurado.

Luego copia los .json a `.minecraft/blendemotes/emotes/` (o usa /blendemotes reload).
"""

import base64
import json
import math
import os
import sys
import tempfile

import bpy

EXPORTER_VERSION = "1.0"
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


def parse_args():
    argv = sys.argv
    argv = argv[argv.index("--") + 1:] if "--" in argv else []
    opts = {"out": None, "actions": None, "icon": True, "author": None, "icon_engine": None}
    i = 0
    while i < len(argv):
        arg = argv[i]
        if arg == "--out":
            opts["out"] = argv[i + 1]
            i += 1
        elif arg == "--actions":
            opts["actions"] = [a.strip() for a in argv[i + 1].split(",") if a.strip()]
            i += 1
        elif arg == "--no-icon":
            opts["icon"] = False
        elif arg == "--icon-engine":
            opts["icon_engine"] = argv[i + 1].upper()
            i += 1
        elif arg == "--author":
            opts["author"] = argv[i + 1]
            i += 1
        i += 1
    return opts


def ensure_action_panel_registered():
    """El rig guarda los metadatos del emote en `action.emote`, que solo existe
    cuando el script del panel está registrado."""
    if hasattr(bpy.types.Action, "emote"):
        return
    text = bpy.data.texts.get("action_settings_panel.py")
    if text is None:
        raise RuntimeError("Este .blend no contiene 'action_settings_panel.py'. ¿Es el rig emote_creator 2.0?")
    text.as_module().register()


def find_rig():
    rig = bpy.data.objects.get(RIG_NAME)
    if rig is None or rig.type != 'ARMATURE':
        raise RuntimeError(f"No se encontró el armature '{RIG_NAME}' en el .blend")
    return rig


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


def frame_rate(scene):
    return scene.render.fps / scene.render.fps_base


def patch_exporter(bedrock):
    """Corrige, sobre el módulo `set_up_bedrock.py` del rig, los fallos del exportador."""
    signs = {}  # puntero del keyframe -> signo del eje con el que se escribe su valor

    original_write_mode = bedrock.write_mode

    def write_mode(bone_name, mode, animation_data, rig_object, default_bones, export_bones):
        signs.clear()
        if mode == 'bend':
            key = f"{bone_name}_bend"
            fcurves = animation_data[key]["rotation"] if key in default_bones and key in animation_data else None
        else:
            fcurves = animation_data.get(bone_name, {}).get(mode)
        if fcurves and any(fc is not None and len(fc.keyframe_points) for fc in fcurves):
            difference = bedrock.get_bone_axis_difference(rig_object, bone_name, mode)
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

    bedrock.write_mode = write_mode
    bedrock.get_bezier_args = get_bezier_args
    bedrock.fcurves_to_mode_dict = fcurves_to_mode_dict


def load_exporter():
    collect = bpy.data.texts['collect_animation_data.py'].as_module()
    bedrock = bpy.data.texts['set_up_bedrock.py'].as_module()
    patch_exporter(bedrock)
    return collect.collect_animation_data, bedrock.create_emote


def export_action(rig, action, out_dir, with_icon, default_author, icon_engine=None):
    scene = bpy.context.scene
    collect_animation_data, create_emote = load_exporter()

    assign_action(rig, action)
    bpy.context.view_layer.objects.active = rig

    export_bones = list(DEFAULT_EXPORT_BONES)
    for pivot_bone in action.emote.pivot_bones:
        if pivot_bone.name:
            export_bones.append(pivot_bone.name)

    preview_frame = scene.frame_current
    scene.frame_set(0)
    animation_data, work_action = collect_animation_data(rig, export_bones)
    emote = create_emote(rig, export_bones, animation_data)
    bpy.data.actions.remove(work_action)

    anim = emote["animations"][action.name]
    fps = frame_rate(scene)
    if "loopTick" in anim:
        # el rig lo redondea a 3 decimales
        start = int(action.frame_start) if action.use_frame_range else 0
        anim["loopTick"] = round((scene.frame_start - start) / fps, TIME_DECIMALS)
    anim["blendemotes"] = {
        "rig": "emote_creator",
        "exactHandles": True,
        "fps": round(fps, 6),
        "exporter": EXPORTER_VERSION,
    }
    meta = anim["player_animation_library"]
    # Valores por defecto del panel -> algo útil.
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


def main():
    opts = parse_args()
    ensure_action_panel_registered()
    rig = find_rig()
    if bpy.context.object and bpy.context.object.mode != 'OBJECT':
        bpy.ops.object.mode_set(mode='OBJECT')

    out_dir = opts["out"] or os.path.join(os.path.dirname(bpy.data.filepath) or os.getcwd(), "emotes")
    original_action = rig.animation_data.action if rig.animation_data else None
    original_slot = getattr(rig.animation_data, "action_slot", None) if rig.animation_data else None

    if opts["actions"]:
        actions = []
        for name in opts["actions"]:
            act = bpy.data.actions.get(name)
            if act is None:
                raise RuntimeError(f"La acción '{name}' no existe en el .blend")
            actions.append(act)
    else:
        actions = [a for a in bpy.data.actions
                   if not a.name.endswith("_pal_export_tmp") and (a.frame_range[1] - a.frame_range[0]) > 0]

    exported = []
    try:
        for action in actions:
            print(f"Exportando '{action.name}'...")
            exported.append(export_action(rig, action, out_dir, opts["icon"], opts["author"], opts["icon_engine"]))
    finally:
        if original_action is not None:
            rig.animation_data.action = original_action
            if original_slot is not None:
                rig.animation_data.action_slot = original_slot

    print(f"Listo: {len(exported)} emote(s) exportados a {out_dir}")
    for path in exported:
        print("  " + path)


if __name__ == "__main__":
    main()
