"""
Herramienta de desarrollo: añade a una COPIA del rig de BlendEmotes las acciones con las que se
generan los datos de prueba del mod (no es para el rig que se usa para animar).

    blender -b emote_creator_blendemotes.blend -P test_fixtures.py -- --out rig_pruebas.blend

  * `manos_pies`: palmas y pies girando en los tres ejes, codos y rodillas doblados hacia delante
    y hacia los lados, una pierna con IK.
  * `cantar` (con --cantar): micrófono en la mano derecha (datos de la prueba de modelos).

Luego:
    blender -b rig_pruebas.blend -P batch_export.py -- --out exact --no-icon --actions manos_pies
    blender -b rig_pruebas.blend -P sample_ground_truth.py -- --out manos_pies_truth.json \
        --actions manos_pies --mesh-frames 0,7.5,18
"""

import math
import sys

import bmesh
import bpy
from mathutils import Matrix


def args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = None
    for i, a in enumerate(argv):
        if a == "--out":
            out = argv[i + 1]
    return out, "--cantar" in argv


def new_action(rig, name, frames):
    if name in bpy.data.actions:
        bpy.data.actions.remove(bpy.data.actions[name])
    action = bpy.data.actions.new(name)
    action.use_fake_user = True
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
    action.use_frame_range = True
    action.frame_start = frames[0]
    action.frame_end = frames[-1]
    action.use_cyclic = True
    return action


def key(pb, path, frame, value, index=None):
    if index is None:
        setattr(pb, path, value)
        pb.keyframe_insert(path, frame=frame)
    else:
        arr = list(getattr(pb, path))
        arr[index] = value
        setattr(pb, path, arr)
        pb.keyframe_insert(path, index=index, frame=frame)


def ik(rig, frames, **values):
    settings = rig.pose.bones["settings"]
    for f in frames:
        for prop, value in values.items():
            settings[prop] = value
            settings.keyframe_insert(f'["{prop}"]', frame=f)


def hands_and_feet(rig):
    frames = [0, 12, 24]
    action = new_action(rig, "manos_pies", frames)
    ik(rig, frames, **{"rightArm IK": 0.0, "leftArm IK": 0.0, "rightLeg IK": 0.0, "leftLeg IK": 1.0})
    pb = rig.pose.bones
    r = math.radians
    for i, f in enumerate(frames):
        s = 1 if i % 2 == 0 else -1
        # right arm forward, elbow forward, palm turning on every axis
        key(pb["right_arm"], "rotation_euler", f, r(-60), 0)
        key(pb["right_arm_bend"], "rotation_euler", f, r(-45 - 10 * s), 0)
        key(pb["right_hand"], "rotation_euler", f, r(40 * s), 0)
        key(pb["right_hand"], "rotation_euler", f, r(15), 1)
        key(pb["right_hand"], "rotation_euler", f, r(25 * s), 2)
        # left arm out to the side, elbow sideways, palm sideways and forward
        key(pb["left_arm"], "rotation_euler", f, r(-70), 2)
        key(pb["left_arm_bend"], "rotation_euler", f, r(30 * s), 2)
        key(pb["left_arm_bend"], "rotation_euler", f, r(-20), 0)
        key(pb["left_hand"], "rotation_euler", f, r(-35), 2)
        key(pb["left_hand"], "rotation_euler", f, r(-20 * s), 0)
        # right leg forward, knee bent, foot up and sideways
        key(pb["right_leg"], "rotation_euler", f, r(-35), 0)
        key(pb["right_leg_bend"], "rotation_euler", f, r(50), 0)
        key(pb["right_leg_bend"], "rotation_euler", f, r(-15 * s), 2)
        key(pb["right_foot"], "rotation_euler", f, r(-30 * s), 0)
        key(pb["right_foot"], "rotation_euler", f, r(20), 2)
        # left leg with IK: the goal moves, the foot turns
        key(pb["left_leg_ik_goal"], "location", f, 0.4 * s, 1)
        key(pb["left_leg_ik_goal"], "location", f, 0.6, 2)
        key(pb["left_foot"], "rotation_euler", f, r(25 * s), 0)
        key(pb["left_foot"], "rotation_euler", f, r(-10), 1)
    meta = action.emote
    meta.name = "Manos y pies"
    meta.author = "BlendEmotes"
    meta.description = "Prueba: palmas y pies"
    return action


# ---------------------------------------------------------------------------- microphone (models test)

def microphone(rig):
    image = bpy.data.images.new("microfono_textura", 16, 16, alpha=True)
    pixels = []
    for y in range(16):
        for x in range(16):
            if y >= 8:
                c = (0.55, 0.57, 0.6, 1.0) if (x + y) % 3 == 0 else (0.18, 0.19, 0.21, 1.0)
            else:
                c = (0.8, 0.08, 0.08, 1.0) if y in (3, 4) else (0.05, 0.05, 0.06, 1.0)
            pixels.extend(c)
    image.pixels = pixels
    image.pack()
    mat = bpy.data.materials.new("microfono")
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = next(n for n in nodes if n.type == 'BSDF_PRINCIPLED')
    tex = nodes.new("ShaderNodeTexImage")
    tex.image = image
    tex.interpolation = 'Closest'
    mat.node_tree.links.new(tex.outputs["Color"], bsdf.inputs["Base Color"])
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, segments=10, radius1=0.12, radius2=0.15, depth=1.0,
                          matrix=Matrix.Translation((0, 0, -0.5)), calc_uvs=True)
    bmesh.ops.create_uvsphere(bm, u_segments=12, v_segments=8, radius=0.24,
                              matrix=Matrix.Translation((0, 0, -1.18)), calc_uvs=True)
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
    me.materials.append(mat)
    coll = bpy.data.collections.new("Modelos - cantar")
    bpy.context.scene.collection.children.link(coll)
    ob = bpy.data.objects.new("microfono", me)
    coll.objects.link(ob)
    rig.data.pose_position = 'REST'
    bpy.context.view_layer.update()
    world = rig.matrix_world @ Matrix.Translation((-1.25, 0.0, 2.85))
    ob.parent = rig
    ob.parent_type = 'BONE'
    ob.parent_bone = "right_item"
    ob.matrix_world = world
    rig.data.pose_position = 'POSE'
    bpy.context.view_layer.update()
    return coll


def sing(rig):
    frames = [0, 12, 24, 36, 48]
    action = new_action(rig, "cantar", frames)
    ik(rig, frames, **{"rightArm IK": 0.0, "leftArm IK": 0.0, "rightLeg IK": 0.0, "leftLeg IK": 0.0})
    pb = rig.pose.bones
    r = math.radians
    for f in frames:
        beat = 3 if (f // 12) % 2 == 0 else 0
        wave = 1 if (f // 12) % 2 == 0 else -1
        up = (f // 12) % 2 == 0
        key(pb["right_arm"], "rotation_euler", f, r(-32 - beat), 0)
        key(pb["right_arm"], "rotation_euler", f, r(24), 2)
        key(pb["right_arm_bend"], "rotation_euler", f, r(-128 + beat), 0)
        key(pb["left_arm"], "rotation_euler", f, r(-75), 2)
        key(pb["left_arm"], "rotation_euler", f, r(-10), 0)
        key(pb["left_arm_bend"], "rotation_euler", f, r(-55 - 25 * wave), 2)
        key(pb["left_arm_bend"], "rotation_euler", f, r(-15), 0)
        key(pb["right_leg"], "rotation_euler", f, r(-25 if up else -10), 0)
        key(pb["right_leg"], "rotation_euler", f, r(12), 2)
        key(pb["right_leg_bend"], "rotation_euler", f, r(35 if up else 15), 0)
        key(pb["right_leg_bend"], "rotation_euler", f, r(-30 if up else -12), 2)
        key(pb["head"], "rotation_euler", f, r(8 if (f // 12) % 2 == 0 else -4), 0)
    meta = action.emote
    meta.name = "Cantar"
    meta.author = "BlendEmotes"
    meta.description = "Micrófono en mano, codo y rodilla hacia los lados"
    meta.models_collection = microphone(rig)
    return action


def main():
    out, with_sing = args()
    text = bpy.data.texts.get("action_settings_panel.py")
    if not hasattr(bpy.types.Action, "emote"):
        text.as_module().register()
    rig = bpy.data.objects["export_armature"]
    if "right_hand" not in rig.pose.bones:
        raise RuntimeError("primero actualiza el rig con upgrade_rig.py")
    previous = rig.animation_data.action if rig.animation_data else None
    hands_and_feet(rig)
    if with_sing:
        sing(rig)
    if previous is not None:
        rig.animation_data.action = previous
    bpy.ops.wm.save_as_mainfile(filepath=out or bpy.data.filepath)
    print("Acciones de prueba guardadas en", out or bpy.data.filepath)


if __name__ == "__main__":
    main()
