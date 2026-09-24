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
"""

import json
import sys

import bpy

BONES = [
    "body", "body_control", "waist",
    "torso", "torso_bend", "head", "cape", "cape_bend",
    "left_arm", "left_arm_bend", "left_item",
    "right_arm", "right_arm_bend", "right_item",
    "left_leg", "left_leg_bend",
    "right_leg", "right_leg_bend",
]
EXPORT_BONES = [
    "body", "body_control", "head", "left_arm", "right_arm", "left_leg", "right_leg",
    "waist", "torso", "cape", "left_item", "right_item",
]
BEND_BONES = ["left_arm_bend", "right_arm_bend", "left_leg_bend", "right_leg_bend", "torso_bend", "cape_bend"]
SUBFRAMES = (0.0, 0.25, 0.5, 0.75)


def mat(m):
    return [[round(m[r][c], 6) for c in range(4)] for r in range(4)]


def args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = "truth.json"
    actions = None
    for i, a in enumerate(argv):
        if a == "--out":
            out = argv[i + 1]
        if a == "--actions":
            actions = argv[i + 1].split(",")
    return out, actions


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
                "bones": {b: mat(rig.pose.bones[b].matrix) for b in BONES if b in rig.pose.bones},
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
    out, only = args()
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

        # 1) rig completo
        scene.frame_set(0)
        vanilla = bool(rig.pose.bones["settings"]["vanilla"])
        rig_frames = sample(rig, scene, start, end)

        # 2) lo que representa el json exportado
        scene.frame_set(0)
        _, work_action = collect_animation_data(rig, list(EXPORT_BONES))
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
        restore_constraints(rig, state)
        rig.animation_data.action = action
        rig.animation_data.action_slot = original_slot
        bpy.data.actions.remove(work_action)

        result["actions"][action.name] = {
            "start": start, "end": end, "vanilla": vanilla,
            "export": export_frames, "rig": rig_frames,
        }
        print(f"{action.name}: {len(export_frames)} muestras (vanilla={vanilla})")

    with open(out, "w", encoding="utf-8") as f:
        json.dump(result, f)
    print("Guardado en", out)


if __name__ == "__main__":
    main()
