"""
BlendEmotes - exportador por lotes para el rig `emote_creator.blend`.

Exporta TODAS las acciones (animaciones) del .blend a archivos .json que el mod BlendEmotes
carga directamente. El trabajo lo hace `blendemotes_export.py` (junto a este archivo), el mismo
exportador que usa el botón Export del rig actualizado: codos y rodillas hacia los lados,
modelos (micrófono, guitarra, caballo...) y las correcciones del exportador original del rig.

Uso (Blender 5.2+, igual que el rig):

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

import os
import sys

import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import blendemotes_export as exporter  # noqa: E402


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


def main():
    opts = parse_args()
    exporter.ensure_action_panel_registered()
    rig = exporter.find_rig()
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
            exported.append(exporter.export_action(rig, action, out_dir, opts["icon"], opts["author"], opts["icon_engine"]))
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
