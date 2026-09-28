"""
BlendEmotes - actualiza el rig `emote_creator.blend` (rig 2.0 de Emotecraft) para BlendEmotes.

    blender -b emote_creator.blend -P upgrade_rig.py -- --out emote_creator_blendemotes.blend

Qué cambia (el .blend original no se toca, se guarda uno nuevo):

  * **Codos y rodillas hacia los lados.** Los huesos `left/right_arm_bend` y `left/right_leg_bend`
    ya no tienen bloqueado el eje Z (ni en los candados de rotación ni en su restricción Limit
    Rotation): el antebrazo y la parte baja de la pierna se doblan hacia delante, hacia atrás y
    hacia los lados. El giro sobre sí mismos (Y) sigue bloqueado.
  * **Exportador nuevo.** El botón *Export* del panel de la acción usa `blendemotes_export.py`
    (el mismo que `batch_export.py`): exporta el doblez lateral y los modelos, y corrige los
    fallos del exportador original.
  * **Modelos.** El panel de la acción tiene un campo *Modelos*: la colección cuyos objetos
    (emparentados a un hueso del rig) viajan con el emote: un micrófono, una guitarra, un
    caballo...
  * **Ejemplo.** La acción `cantar` (micrófono en la mano derecha, brazo izquierdo saludando
    con el codo hacia un lado, rodilla hacia fuera) y la colección `Modelos - cantar`.
"""

import math
import os
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector

HERE = os.path.dirname(os.path.abspath(__file__))
RIG_NAME = "export_armature"
SIDEWAYS_BONES = ["left_arm_bend", "right_arm_bend", "left_leg_bend", "right_leg_bend"]
RIG_VERSION = "2.0 + BlendEmotes 1.1"


def args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = None
    demo = True
    for i, a in enumerate(argv):
        if a == "--out":
            out = argv[i + 1]
        if a == "--no-demo":
            demo = False
    if out is None:
        base = os.path.splitext(bpy.data.filepath)[0]
        out = base + "_blendemotes.blend"
    return out, demo


# ---------------------------------------------------------------------------- rig

def unlock_sideways(rig):
    for name in SIDEWAYS_BONES:
        pb = rig.pose.bones[name]
        lock = list(pb.lock_rotation)
        lock[2] = False
        pb.lock_rotation = lock
        for c in pb.constraints:
            if c.type == 'LIMIT_ROTATION':
                c.use_limit_z = False


PANEL_PROPERTY = '''
    models_collection: PointerProperty(
        name="Modelos",
        type=bpy.types.Collection,
        description="Colección con los modelos del emote (micrófono, guitarra, caballo...). "
                    "Cada objeto emparentado a un hueso del rig viaja con la animación"
    )
'''

PANEL_UI = '''    layout.prop(data, "models_collection", icon='OUTLINER_COLLECTION')
'''

EXPORT_SCRIPT = '''# Botón "Export" del panel de la acción (rig 2.0 + BlendEmotes).
# El trabajo lo hace blendemotes_export.py: doblez lateral de codos y rodillas, modelos y las
# correcciones del exportador original del rig.
import bpy

exporter = bpy.data.texts["blendemotes_export.py"].as_module()
rig = bpy.context.active_object
if rig is None or rig.type != 'ARMATURE':
    rig = exporter.find_rig()
action = rig.animation_data.action
path = exporter.export_action(rig, action, bpy.path.abspath(action.emote.emote_save_path), with_icon=True)
print("Emote exportado:", path)
'''


def upgrade_scripts():
    panel = bpy.data.texts["action_settings_panel.py"]
    code = panel.as_string()
    if "models_collection" not in code:
        anchor = "    emote_save_path: StringProperty("
        if anchor not in code:
            raise RuntimeError("action_settings_panel.py: no se encontró emote_save_path")
        code = code.replace(anchor, PANEL_PROPERTY.lstrip("\n") + "\n" + anchor, 1)
        ui_anchor = '    layout.prop(data, "emote_save_path")'
        code = code.replace(ui_anchor, PANEL_UI + ui_anchor, 1)
        panel.from_string(code)

    module = bpy.data.texts.get("blendemotes_export.py") or bpy.data.texts.new("blendemotes_export.py")
    with open(os.path.join(HERE, "blendemotes_export.py"), encoding="utf-8") as f:
        module.from_string(f.read())

    export = bpy.data.texts.get("export.py") or bpy.data.texts.new("export.py")
    export.from_string(EXPORT_SCRIPT)

    note = bpy.data.texts.get("Note")
    if note is not None and "BlendEmotes" not in note.as_string():
        note.from_string(note.as_string().rstrip() + "\n\n"
                         "BlendEmotes:\n"
                         "- Codos y rodillas se doblan también hacia los lados (eje Z de los huesos *_bend).\n"
                         "- Campo 'Modelos' en el panel de la acción: colección de modelos que viajan con el emote.\n"
                         "- El botón Export usa blendemotes_export.py.\n"
                         f"Versión del rig: {RIG_VERSION}\n")
    # re-register the panel with the new property
    if hasattr(bpy.types.Action, "emote"):
        try:
            bpy.data.texts["action_settings_panel.py"].as_module().unregister()
        except Exception:
            pass
    panel_module = bpy.data.texts["action_settings_panel.py"].as_module()
    try:
        panel_module.register()
    except ValueError:
        pass
    panel.use_module = True


# ---------------------------------------------------------------------------- demo

def microphone_image():
    name = "microfono_textura"
    image = bpy.data.images.get(name)
    if image is not None:
        return image
    size = 16
    image = bpy.data.images.new(name, size, size, alpha=True)
    pixels = []
    for y in range(size):
        for x in range(size):
            if y >= size // 2:
                # grille: dark metal with lighter holes
                light = (x + y) % 3 == 0
                c = (0.55, 0.57, 0.6, 1.0) if light else (0.18, 0.19, 0.21, 1.0)
            else:
                # handle: black with a red stripe
                c = (0.8, 0.08, 0.08, 1.0) if y in (3, 4) else (0.05, 0.05, 0.06, 1.0)
            pixels.extend(c)
    image.pixels = pixels
    image.pack()
    return image


def microphone_material():
    mat = bpy.data.materials.get("microfono")
    if mat is not None:
        return mat
    mat = bpy.data.materials.new("microfono")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = next(n for n in nodes if n.type == 'BSDF_PRINCIPLED')
    tex = nodes.new("ShaderNodeTexImage")
    tex.image = microphone_image()
    tex.interpolation = 'Closest'
    mat.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    return mat


def microphone_mesh():
    """Handle (cylinder) and grille (sphere) along -Z from the origin (held in the fist, it carries on
    past the hand: raising the hand to the chin points it at the mouth)."""
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=10, radius1=0.12, radius2=0.15, depth=1.0,
                          matrix=Matrix.Translation((0, 0, -0.5)), calc_uvs=True)
    bmesh.ops.create_uvsphere(bm, u_segments=12, v_segments=8, radius=0.24,
                              matrix=Matrix.Translation((0, 0, -1.18)), calc_uvs=True)
    # UVs: handle on the lower half of the picture, grille on the upper half
    uv = bm.loops.layers.uv.verify()
    for face in bm.faces:
        grille = sum((l.vert.co.z for l in face.loops)) / len(face.loops) < -0.98
        for loop in face.loops:
            u, v = loop[uv].uv
            loop[uv].uv = (u, 0.5 + v * 0.5) if grille else (u, v * 0.5)
    me = bpy.data.meshes.new("microfono")
    bm.to_mesh(me)
    bm.free()
    for poly in me.polygons:
        poly.use_smooth = True
    me.materials.append(microphone_material())
    return me


def demo_models(rig):
    coll = bpy.data.collections.get("Modelos - cantar")
    if coll is None:
        coll = bpy.data.collections.new("Modelos - cantar")
        bpy.context.scene.collection.children.link(coll)
    ob = bpy.data.objects.get("microfono")
    if ob is None:
        ob = bpy.data.objects.new("microfono", microphone_mesh())
        coll.objects.link(ob)
    rig.data.pose_position = 'REST'
    bpy.context.view_layer.update()
    # in the right fist, pointing down along the arm
    world = rig.matrix_world @ Matrix.Translation((-1.25, 0.0, 2.85))
    ob.parent = rig
    ob.parent_type = 'BONE'
    ob.parent_bone = "right_item"
    ob.matrix_world = world
    rig.data.pose_position = 'POSE'
    bpy.context.view_layer.update()
    return coll


def key(pb, path, frame, value, index=None):
    if index is None:
        setattr(pb, path, value)
        pb.keyframe_insert(path, frame=frame)
    else:
        arr = list(getattr(pb, path))
        arr[index] = value
        setattr(pb, path, arr)
        pb.keyframe_insert(path, index=index, frame=frame)


def demo_action(rig, models):
    if "cantar" in bpy.data.actions:
        bpy.data.actions.remove(bpy.data.actions["cantar"])
    action = bpy.data.actions.new("cantar")
    action.use_fake_user = True  # keep it in the file when another action is shown
    rig.animation_data_create()
    rig.animation_data.action = action
    if hasattr(rig.animation_data, "action_slot") and rig.animation_data.action_slot is None:
        slots = list(getattr(rig.animation_data, "action_suitable_slots", []))
        if slots:
            rig.animation_data.action_slot = slots[0]
    for pb in rig.pose.bones:
        pb.location = (0, 0, 0)
        pb.rotation_euler = (0, 0, 0)
        pb.scale = (1, 1, 1)
    settings = rig.pose.bones["settings"]
    frames = [0, 12, 24, 36, 48]
    for f in frames:
        # FK everywhere (the bends are keyed by hand)
        settings["rightArm IK"] = 0.0
        settings["leftArm IK"] = 0.0
        settings["rightLeg IK"] = 0.0
        settings["leftLeg IK"] = 0.0
        for prop in ("rightArm IK", "leftArm IK", "rightLeg IK", "leftLeg IK"):
            settings.keyframe_insert(f'["{prop}"]', frame=f)

    pb = rig.pose.bones
    # right hand under the chin, the microphone pointing at the mouth
    for f in frames:
        beat = 3 if (f // 12) % 2 == 0 else 0
        key(pb["right_arm"], "rotation_euler", f, math.radians(-32 - beat), 0)
        key(pb["right_arm"], "rotation_euler", f, math.radians(24), 2)
        key(pb["right_arm_bend"], "rotation_euler", f, math.radians(-128 + beat), 0)
    # left arm up and out, waving with the elbow bending sideways
    for f in frames:
        wave = 1 if (f // 12) % 2 == 0 else -1
        key(pb["left_arm"], "rotation_euler", f, math.radians(-75), 2)
        key(pb["left_arm"], "rotation_euler", f, math.radians(-10), 0)
        key(pb["left_arm_bend"], "rotation_euler", f, math.radians(-55 - 25 * wave), 2)
        key(pb["left_arm_bend"], "rotation_euler", f, math.radians(-15), 0)
    # right knee out to the side, bouncing; left leg straight
    for f in frames:
        up = (f // 12) % 2 == 0
        key(pb["right_leg"], "rotation_euler", f, math.radians(-25 if up else -10), 0)
        key(pb["right_leg"], "rotation_euler", f, math.radians(12), 2)
        key(pb["right_leg_bend"], "rotation_euler", f, math.radians(35 if up else 15), 0)
        key(pb["right_leg_bend"], "rotation_euler", f, math.radians(-30 if up else -12), 2)
    # head nodding to the beat
    for f in frames:
        key(pb["head"], "rotation_euler", f, math.radians(8 if (f // 12) % 2 == 0 else -4), 0)
    action.use_frame_range = True
    action.frame_start = 0
    action.frame_end = 48
    action.use_cyclic = True
    meta = action.emote
    meta.name = "Cantar"
    meta.author = "BlendEmotes"
    meta.description = "Micrófono en mano, codo y rodilla hacia los lados"
    meta.models_collection = models
    return action


def main():
    out, demo = args()
    rig = bpy.data.objects[RIG_NAME]
    unlock_sideways(rig)
    upgrade_scripts()
    if demo:
        previous = rig.animation_data.action if rig.animation_data else None
        models = demo_models(rig)
        demo_action(rig, models)
        if previous is not None:
            rig.animation_data.action = previous
    bpy.ops.wm.save_as_mainfile(filepath=out, compress=True)
    print("Rig actualizado:", out)


if __name__ == "__main__":
    main()
