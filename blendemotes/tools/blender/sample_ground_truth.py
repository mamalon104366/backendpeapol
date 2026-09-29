"""
Herramienta de desarrollo: genera la "verdad de Blender" para los tests del mod.

Para cada acción del rig `emote_creator.blend` guarda las matrices (espacio del
armature) de los huesos que el mod reproduce, muestreadas en fotogramas enteros y
fraccionarios. Los tests de `core` comparan su evaluación del .json exportado
contra estas matrices.

Modos:
  "export": la acción de trabajo que usa el exportador (curvas ya horneadas y
            fusionadas) con las restricciones silenciadas -> es exactamente lo que
            representa el .json. Error esperado ~ precisión de redondeo.
  "rig":    el rig original con IK/restricciones -> error total del pipeline.

Uso:
    blender -b emote_creator.blend -P sample_ground_truth.py -- --out truth.json
    ... -- --out cantar_truth.json --actions cantar --mesh-frames 0,6.5,12 --models

Con --models también guarda dónde quedan los vértices de los modelos de la acción (en el
mismo orden que los escribe blendemotes_export.py).
"""

import json
import os
import sys

import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blendemotes_export as exporter  # noqa: E402

BONES = [
    "body", "body_control", "waist",
    "torso", "torso_bend", "head", "cape", "cape_bend",
    "left_arm", "left_arm_bend", "left_item",
    "right_arm", "right_arm_bend", "right_item",
    "left_leg", "left_leg_bend",
    "right_leg", "right_leg_bend",
]
# palma y pie (rig de BlendEmotes 1.2+)
TIP_BONES = ["right_hand", "left_hand", "right_foot", "left_foot"]
EXPORT_BONES = [
    "body", "body_control", "head", "left_arm", "right_arm", "left_leg", "right_leg",
    "waist", "torso", "cape", "left_item", "right_item",
]
BEND_BONES = ["left_arm_bend", "right_arm_bend", "left_leg_bend", "right_leg_bend", "torso_bend", "cape_bend"]
SUBFRAMES = (0.0, 0.25, 0.5, 0.75)


def bones_of(rig):
    return BONES + [b for b in TIP_BONES if b in rig.pose.bones]


def export_list(rig):
    return list(EXPORT_BONES) + [b for b in TIP_BONES if b in rig.pose.bones]


def mat(m):
    return [[round(m[r][c], 6) for c in range(4)] for r in range(4)]


def args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = "truth.json"
    actions = None
    mesh_frames = []
    models = "--models" in argv
    for i, a in enumerate(argv):
        if a == "--out":
            out = argv[i + 1]
        if a == "--actions":
            actions = argv[i + 1].split(",")
        if a == "--mesh-frames":
            mesh_frames = [float(x) for x in argv[i + 1].split(",")]
    return out, actions, mesh_frames, models


MESH_GROUPS = {
    "left_arm": ("left_arm", "left_arm_bend", "left_hand"), "right_arm": ("right_arm", "right_arm_bend", "right_hand"),
    "left_leg": ("left_leg", "left_leg_bend", "left_foot"), "right_leg": ("right_leg", "right_leg_bend", "right_foot"),
    "torso": ("torso", "torso_bend"),
}


def sample_mesh(scene, frames):
    """Rest and deformed positions of the player mesh vertices, per part."""
    ob = bpy.data.objects["player_mesh"]
    names = {g.index: g.name for g in ob.vertex_groups}
    part_of = {}
    for v in ob.data.vertices:
        gs = {names[g.group]: g.weight for g in v.groups}
        for part, groups in MESH_GROUPS.items():
            if sum(gs.get(g, 0) for g in groups) > 0.5 and gs.get("head", 0) < 0.5:
                part_of[v.index] = part
    rest = {i: tuple(round(c, 6) for c in ob.data.vertices[i].co) for i in part_of}
    out = []
    for f in frames:
        scene.frame_set(int(f), subframe=f - int(f))
        dg = bpy.context.evaluated_depsgraph_get()
        me = ob.evaluated_get(dg).to_mesh()
        verts = [[part_of[i], rest[i], tuple(round(c, 6) for c in me.vertices[i].co)] for i in sorted(part_of)]
        ob.evaluated_get(dg).to_mesh_clear()
        out.append({"frame": f, "vertices": verts})
    return out


def sample_models(action, rig, scene, frames):
    """Armature-space positions of the model vertices, per exported triangle corner."""
    objects = exporter.model_objects(action, rig)
    out = []
    to_rig = rig.matrix_world.inverted()
    for f in frames:
        scene.frame_set(int(f), subframe=f - int(f))
        dg = bpy.context.evaluated_depsgraph_get()
        models = []
        for ob in objects:
            ev = ob.evaluated_get(dg)
            me = ev.to_mesh()
            me.calc_loop_triangles()
            m = to_rig @ ob.matrix_world
            groups = {}
            for tri in me.loop_triangles:
                pts = groups.setdefault(tri.material_index, [])
                for v in tri.vertices:
                    pts.append([round(c, 6) for c in (m @ me.vertices[v].co)])
            ev.to_mesh_clear()
            for pts in groups.values():
                models.append({"object": ob.name, "vertices": pts})
        out.append({"frame": f, "models": models})
    return out


def sample(rig, scene, start, end):
    frames = []
    f = start
    while f <= end:
        for sub in SUBFRAMES:
            if f == end and sub > 0:
                break
            scene.frame_set(f, subframe=sub)
            bpy.context.view_layer.update()
            frames.append({
                "frame": f + sub,
                "bones": {b: mat(rig.pose.bones[b].matrix) for b in bones_of(rig)},
            })
        f += 1
    return frames


def set_constraints_muted(rig, muted):
    state = {}
    for pb in rig.pose.bones:
        for c in pb.constraints:
            state[(pb.name, c.name)] = c.mute
            c.mute = muted
    return state


def restore_constraints(rig, state):
    for pb in rig.pose.bones:
        for c in pb.constraints:
            c.mute = state[(pb.name, c.name)]


def main():
    out, only, mesh_frames, with_models = args()
    text = bpy.data.texts.get("action_settings_panel.py")
    if not hasattr(bpy.types.Action, "emote"):
        text.as_module().register()
    collect_animation_data = bpy.data.texts['collect_animation_data.py'].as_module().collect_animation_data

    scene = bpy.context.scene
    rig = bpy.data.objects["export_armature"]
    bpy.context.view_layer.objects.active = rig
    fps = scene.render.fps / scene.render.fps_base

    result = {
        "fps": fps,
        "rest": {b.name: mat(b.matrix_local) for b in rig.data.bones},
        "rotation_mode": {pb.name: pb.rotation_mode for pb in rig.pose.bones},
        "parents": {b.name: (b.parent.name if b.parent else None) for b in rig.data.bones},
        "actions": {},
    }

    actions = [a for a in bpy.data.actions if (a.frame_range[1] - a.frame_range[0]) > 0
               and not a.name.endswith("_pal_export_tmp")]
    if only:
        actions = [a for a in actions if a.name in only]

    for action in actions:
        rig.animation_data.action = action
        if rig.animation_data.action_slot is None and len(action.slots) > 0:
            rig.animation_data.action_slot = action.slots[0]
        start = 0
        end = int(scene.frame_end)
        if action.use_frame_range:
            start, end = int(action.frame_start), int(action.frame_end)

        # 1) rig completo, desde la pose de reposo (los canales sin claves no heredan la acción anterior)
        exporter.reset_pose(rig)
        scene.frame_set(0)
        vanilla = bool(rig.pose.bones["settings"]["vanilla"])
        rig_frames = sample(rig, scene, start, end)

        # 2) lo que representa el json exportado
        scene.frame_set(0)
        exporter.reset_pose(rig)
        # como el exportador: los huesos movidos solo por IK/restricciones también se hornean
        with exporter.keyed_source(rig, action, exporter.baked_bones(rig, export_list(rig))):
            _, work_action = collect_animation_data(rig, export_list(rig))
        original_slot = rig.animation_data.action_slot
        rig.animation_data.action = work_action
        for slot in work_action.slots:
            rig.animation_data.action_slot = slot
            break
        state = set_constraints_muted(rig, True)
        muted_curves = []
        if vanilla:
            # en modo vanilla el json no lleva "bend": los antebrazos quedan rectos
            from bpy_extras import anim_utils
            cb = anim_utils.action_get_channelbag_for_slot(work_action, rig.animation_data.action_slot)
            for fc in cb.fcurves:
                if any(f'pose.bones["{b}"]' in fc.data_path for b in BEND_BONES):
                    fc.mute = True
                    muted_curves.append(fc)
            for b in BEND_BONES:
                pb = rig.pose.bones[b]
                pb.location = (0, 0, 0)
                pb.rotation_euler = (0, 0, 0)
                pb.scale = (1, 1, 1)
        export_frames = sample(rig, scene, start, end)
        mesh = sample_mesh(scene, mesh_frames) if mesh_frames else []
        models = sample_models(action, rig, scene, mesh_frames or [start]) if with_models else []
        restore_constraints(rig, state)
        rig.animation_data.action = action
        rig.animation_data.action_slot = original_slot
        bpy.data.actions.remove(work_action)

        result["actions"][action.name] = {
            "start": start, "end": end, "vanilla": vanilla,
            "export": export_frames, "rig": rig_frames, "mesh": mesh, "models": models,
        }
        print(f"{action.name}: {len(export_frames)} muestras (vanilla={vanilla})")

    with open(out, "w", encoding="utf-8") as f:
        json.dump(result, f)
    print("Guardado en", out)


if __name__ == "__main__":
    main()
