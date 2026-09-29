"""
BlendEmotes - convierte el rig `emote_creator.blend` (rig 2.0 de Emotecraft) en el rig de BlendEmotes.

    blender -b emote_creator.blend -P upgrade_rig.py -- --out emote_creator_blendemotes.blend

El .blend original no se toca: se guarda uno nuevo. Qué cambia:

  * **Sin espejo.** El rig original tenía activado *Pose > X-Axis Mirror*: al mover el brazo
    izquierdo se movía también el derecho. Se apaga.
  * **Palma y pie.** Huesos nuevos `right_hand` / `left_hand` (la palma: los 3 px del final del
    brazo giran en la muñeca) y `right_foot` / `left_foot` (el pie: los 3 px del final de la
    pierna giran en el tobillo), con sus pesos en la malla. Los objetos de la mano
    (`right_item` / `left_item`) cuelgan de la palma.
  * **Codos y rodillas hacia los lados.** Los huesos `*_bend` de brazos y piernas giran también
    en Z.
  * **Controles claros.** Todos los huesos en gris; colecciones ordenadas (Cuerpo, Brazos,
    Piernas, Manos y pies, IK, Objetos) y el mecanismo interno oculto. Con el IK de una
    extremidad encendido se ven su mano/pie de IK y su polo; apagado, sus huesos normales (así
    nunca hay un hueso a la vista que no haga nada).
  * **Panel "BlendEmotes"** en la barra lateral del visor 3D (tecla N): interruptores de IK de
    cada brazo y pierna.
  * **Exportador.** El botón *Export* usa `blendemotes_export.py` (doblez lateral, palma, pie,
    modelos y las correcciones del exportador original) y el panel de la acción tiene el campo
    *Modelos*.

Las acciones que ya tenga el archivo no cambian: la palma y el pie empiezan rectos.
"""

import os
import sys

import bpy
from mathutils import Matrix

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import blendemotes_export  # noqa: E402

RIG_NAME = "export_armature"
MESH_NAME = "player_mesh"
RIG_VERSION = "2.0 + BlendEmotes 1.2"
SIDEWAYS_BONES = ["left_arm_bend", "right_arm_bend", "left_leg_bend", "right_leg_bend"]

# Palma y pie: hueso nuevo, extremidad, pivote de la extremidad (px bajo el cuello) y la rampa de
# pesos (px bajo el pivote) en la que la punta pasa de no moverse a moverse entera. Deben coincidir
# con BendProfile.HAND / FOOT y RigDefinition del mod.
TIPS = {
    "right_hand": ("right_arm", 2.0, (6.4, 7.6)),
    "left_hand": ("left_arm", 2.0, (6.4, 7.6)),
    "right_foot": ("right_leg", 12.0, (8.4, 9.6)),
    "left_foot": ("left_leg", 12.0, (8.4, 9.6)),
}
ITEMS = {"right_item": "right_hand", "left_item": "left_hand"}
IK_SWITCHES = {
    "right_arm": "rightArm IK", "left_arm": "leftArm IK",
    "right_leg": "rightLeg IK", "left_leg": "leftLeg IK",
}
IK_CONTROLS = {
    "right_arm": ["right_arm_ik_goal", "rightArm_ik_pole"],
    "left_arm": ["left_arm_ik_goal", "leftArm_ik_pole"],
    "right_leg": ["right_leg_ik_goal", "rightLeg_ik_pole"],
    "left_leg": ["left_leg_ik_goal", "leftLeg_ik_pole"],
}
COLLECTIONS = [
    ("Cuerpo", ["body", "body_control", "waist", "torso", "torso_bend", "head", "cape", "cape_bend", "settings"], True),
    ("Brazos", ["right_arm", "right_arm_bend", "left_arm", "left_arm_bend"], True),
    ("Piernas", ["right_leg", "right_leg_bend", "left_leg", "left_leg_bend"], True),
    ("Manos y pies", list(TIPS), True),
    ("IK", ["right_arm_ik_goal", "rightArm_ik_pole", "left_arm_ik_goal", "leftArm_ik_pole",
            "right_leg_ik_goal", "rightLeg_ik_pole", "left_leg_ik_goal", "leftLeg_ik_pole", "head_goal"], True),
    ("Objetos", ["right_item", "left_item"], True),
]
MECHANISM = "Mecanismo"


def args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    out = None
    for i, a in enumerate(argv):
        if a == "--out":
            out = argv[i + 1]
    if out is None:
        base = os.path.splitext(bpy.data.filepath)[0]
        out = base + "_blendemotes.blend"
    return out


def set_mode(rig, mode):
    bpy.context.view_layer.objects.active = rig
    rig.select_set(True)
    if rig.mode != mode:
        bpy.ops.object.mode_set(mode=mode)


# ---------------------------------------------------------------------------- bones

def no_mirror(rig):
    rig.pose.use_mirror_x = False
    rig.data.use_mirror_x = False


def unlock_sideways(rig):
    for name in SIDEWAYS_BONES:
        pb = rig.pose.bones[name]
        lock = list(pb.lock_rotation)
        lock[2] = False
        pb.lock_rotation = lock
        for c in pb.constraints:
            if c.type == 'LIMIT_ROTATION':
                c.use_limit_z = False


def add_tip_bones(rig):
    """Palma y pie: mitad final del hueso de doblez, con sus mismos ejes (mismo roll)."""
    set_mode(rig, 'EDIT')
    edit = rig.data.edit_bones
    for tip, (part, _, _) in TIPS.items():
        bend = edit[part + "_bend"]
        bone = edit.get(tip) or edit.new(tip)
        bone.head = bend.head.lerp(bend.tail, 0.5)
        bone.tail = bend.tail.copy()
        bone.roll = bend.roll
        bone.parent = bend
        bone.use_connect = False
        bone.use_deform = True
        bone.use_inherit_rotation = True
        bone.inherit_scale = 'FULL'
    for item, tip in ITEMS.items():
        edit[item].parent = edit[tip]
        edit[item].use_connect = False
    set_mode(rig, 'POSE')
    for tip in TIPS:
        pb = rig.pose.bones[tip]
        pb.rotation_mode = 'XYZ'
        pb.lock_location = (True, True, True)
        pb.lock_scale = (True, True, True)
        pb.location = (0, 0, 0)
        pb.rotation_euler = (0, 0, 0)
        pb.scale = (1, 1, 1)
    set_mode(rig, 'OBJECT')


def paint_tip_weights(mesh_ob):
    """Reparte el peso del hueso de doblez entre él y la palma/pie según la rampa de cada uno."""
    if mesh_ob.matrix_world != Matrix.Identity(4):
        raise RuntimeError("player_mesh tiene transformación de objeto; se esperaba la identidad")
    groups = mesh_ob.vertex_groups
    for tip, (part, pivot_y, (d0, d1)) in TIPS.items():
        bend = groups[part + "_bend"]
        target = groups.get(tip) or groups.new(name=tip)
        for v in mesh_ob.data.vertices:
            bw = 0.0
            for g in v.groups:
                if g.group == bend.index:
                    bw = g.weight
            if bw <= 0.0:
                continue
            d = (24.0 - 4.0 * v.co.z) - pivot_y  # px bajo el pivote de la extremidad
            w = min(1.0, max(0.0, (d - d0) / (d1 - d0)))
            if w <= 0.0:
                continue
            target.add([v.index], bw * w, 'REPLACE')
            if w >= 1.0:
                bend.remove([v.index])
            else:
                bend.add([v.index], bw * (1.0 - w), 'REPLACE')


def _driver(owner, path, expression, variables):
    fc = owner.driver_add(path)
    drv = fc.driver
    drv.type = 'SCRIPTED'
    for v in list(drv.variables):
        drv.variables.remove(v)
    for name, target_id, data_path in variables:
        var = drv.variables.new()
        var.name = name
        var.type = 'SINGLE_PROP'
        var.targets[0].id_type = 'OBJECT'
        var.targets[0].id = target_id
        var.targets[0].data_path = data_path
    drv.expression = expression
    return fc


def add_drivers(rig):
    arm = rig.data
    # en modo "vanilla" (sin dobleces) la palma y el pie tampoco deforman
    for tip in TIPS:
        _driver(arm.bones[tip], "use_deform", "not vanilla",
                [("vanilla", rig, 'pose.bones["settings"]["vanilla"]')])
    # con el IK de una extremidad encendido se ven sus controles de IK; apagado, sus huesos normales
    for part, switch in IK_SWITCHES.items():
        path = f'pose.bones["settings"]["{switch}"]'
        for bone in (part, part + "_bend"):
            _driver(arm.bones[bone], "hide", "ik >= 0.5", [("ik", rig, path)])
        for bone in IK_CONTROLS[part]:
            _driver(arm.bones[bone], "hide", "ik < 0.5", [("ik", rig, path)])


def grey_bones(rig):
    for bone in rig.data.bones:
        bone.color.palette = 'DEFAULT'
    for pb in rig.pose.bones:
        pb.color.palette = 'DEFAULT'


def organise_collections(rig):
    arm = rig.data
    wanted = {}
    for name, bones, visible in COLLECTIONS:
        coll = arm.collections.get(name) or arm.collections.new(name)
        coll.is_visible = visible
        for b in bones:
            wanted[b] = coll
    mechanism = arm.collections.get(MECHANISM) or arm.collections.new(MECHANISM)
    mechanism.is_visible = False
    keep = {c for c in wanted.values()} | {mechanism}
    for bone in arm.bones:
        for coll in list(bone.collections):
            if coll not in keep:
                coll.unassign(bone)
        target = wanted.get(bone.name, mechanism)
        for coll in keep:
            if coll != target and bone.name in [b.name for b in coll.bones]:
                coll.unassign(bone)
        target.assign(bone)
    for coll in list(arm.collections_all):
        if coll not in keep:
            arm.collections.remove(coll)
    for bone in arm.bones:
        bone.hide = False


# ---------------------------------------------------------------------------- scripts

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

EXPORT_SCRIPT = '''# Botón "Export" del panel de la acción (rig de BlendEmotes).
# El trabajo lo hace blendemotes_export.py: doblez lateral de codos y rodillas, palma, pie, modelos
# y las correcciones del exportador original del rig.
import bpy

exporter = bpy.data.texts["blendemotes_export.py"].as_module()
rig = bpy.context.active_object
if rig is None or rig.type != 'ARMATURE':
    rig = exporter.find_rig()
action = rig.animation_data.action
path = exporter.export_action(rig, action, bpy.path.abspath(action.emote.emote_save_path), with_icon=True)
print("Emote exportado:", path)
'''

RIG_UI = '''# Panel "BlendEmotes" de la barra lateral del visor 3D (tecla N): interruptores de IK.
import bpy

SWITCHES = [
    ("rightArm IK", "Brazo derecho"),
    ("leftArm IK", "Brazo izquierdo"),
    ("rightLeg IK", "Pierna derecha"),
    ("leftLeg IK", "Pierna izquierda"),
]


class BLENDEMOTES_PT_rig(bpy.types.Panel):
    bl_label = "Rig BlendEmotes"
    bl_space_type = 'VIEW_3D'
    bl_region_type = 'UI'
    bl_category = "BlendEmotes"

    @classmethod
    def poll(cls, context):
        rig = bpy.data.objects.get("export_armature")
        return rig is not None and "settings" in rig.pose.bones

    def draw(self, context):
        settings = bpy.data.objects["export_armature"].pose.bones["settings"]
        col = self.layout.column(align=True)
        col.label(text="IK (0 = girar huesos, 1 = mover mano/pie):")
        for prop, label in SWITCHES:
            col.prop(settings, '["%s"]' % prop, text=label, slider=True)
        box = self.layout.box()
        box.label(text="Palma: right_hand / left_hand")
        box.label(text="Pie: right_foot / left_foot")
        box.label(text="Recuerda poner un keyframe al interruptor.")


def register():
    bpy.utils.register_class(BLENDEMOTES_PT_rig)


def unregister():
    bpy.utils.unregister_class(BLENDEMOTES_PT_rig)


if __name__ == "__main__":
    register()
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

    ui = bpy.data.texts.get("blendemotes_rig_ui.py") or bpy.data.texts.new("blendemotes_rig_ui.py")
    ui.from_string(RIG_UI)
    ui.use_module = True

    note = bpy.data.texts.get("Note")
    if note is not None:
        text = note.as_string()
        if "BlendEmotes:" in text:
            text = text[:text.index("BlendEmotes:")].rstrip()
        note.from_string(text.rstrip() + "\n\n"
                         "BlendEmotes:\n"
                         "- Palma (right_hand / left_hand) y pie (right_foot / left_foot): giran el final del brazo y de la pierna.\n"
                         "- Codos y rodillas se doblan también hacia los lados (eje Z de los huesos *_bend).\n"
                         "- Sin espejo X: cada lado se mueve por separado.\n"
                         "- Panel 'BlendEmotes' (tecla N): IK de cada brazo y pierna. Con IK se ven la mano/pie de IK y el polo;\n"
                         "  sin IK, los huesos del brazo/pierna.\n"
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


def remove_old_demo():
    """Quita el ejemplo del micrófono de versiones anteriores de este script."""
    action = bpy.data.actions.get("cantar")
    if action is not None:
        bpy.data.actions.remove(action)
    ob = bpy.data.objects.get("microfono")
    if ob is not None:
        mesh = ob.data
        bpy.data.objects.remove(ob)
        if mesh is not None and mesh.users == 0:
            bpy.data.meshes.remove(mesh)
    coll = bpy.data.collections.get("Modelos - cantar")
    if coll is not None:
        bpy.data.collections.remove(coll)
    mat = bpy.data.materials.get("microfono")
    if mat is not None and mat.users == 0:
        bpy.data.materials.remove(mat)
    img = bpy.data.images.get("microfono_textura")
    if img is not None and img.users == 0:
        bpy.data.images.remove(img)


def upgrade(rig):
    mesh_ob = bpy.data.objects[MESH_NAME]
    remove_old_demo()
    no_mirror(rig)
    blendemotes_export.reset_mechanism(rig)
    unlock_sideways(rig)
    first_time = "right_hand" not in rig.data.bones
    add_tip_bones(rig)
    if first_time:
        paint_tip_weights(mesh_ob)
    add_drivers(rig)
    grey_bones(rig)
    organise_collections(rig)
    upgrade_scripts()
    bpy.context.view_layer.update()


def main():
    out = args()
    rig = bpy.data.objects[RIG_NAME]
    upgrade(rig)
    bpy.ops.wm.save_as_mainfile(filepath=out, compress=True)
    print("Rig actualizado:", out)


if __name__ == "__main__":
    main()
