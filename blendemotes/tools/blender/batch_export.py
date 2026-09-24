"""
BlendEmotes - exportador por lotes para el rig `emote_creator.blend` (rig 2.0).

Exporta TODAS las acciones (animaciones) del .blend a archivos .json que el mod
BlendEmotes carga directamente. Usa las mismas funciones de exportación que trae
el rig dentro del .blend (`collect_animation_data.py` y `set_up_bedrock.py`), así
que el resultado es idéntico a pulsar "Export" en el panel de la acción.

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
import os
import sys
import tempfile

import bpy

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


def export_action(rig, action, out_dir, with_icon, default_author, icon_engine=None):
    scene = bpy.context.scene
    collect_animation_data = bpy.data.texts['collect_animation_data.py'].as_module().collect_animation_data
    create_emote = bpy.data.texts['set_up_bedrock.py'].as_module().create_emote

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
