# BlendEmotes

Mod de emotes para Minecraft en el que **todo se anima en Blender** con el rig
`emote_creator.blend` (el mismo rig 2.0 de Emotecraft) y el juego lo reproduce sobre un
**rig de huesos real** montado en el personaje de Minecraft: codos, rodillas y cintura se
doblan igual que en Blender, sin huecos, y el cuerpo entero, la cabeza, la capa, la armadura y
los objetos en la mano siguen la animación.

> Estado de las versiones y de los loaders: ver la tabla [Versiones](#versiones).

---

## Cómo funciona (resumen)

```
Blender (emote_creator.blend)  ──export──►  emote.json  ──►  .minecraft/blendemotes/emotes/
                                                                   │
                         mod BlendEmotes (Fabric / Forge / NeoForge, 1.8.9 → actual)
                                                                   │
               rig: body → body_control → waist → torso / cabeza / brazos / piernas
                    torso_bend, arm_bend, leg_bend (bends), cape, right/left_item
```

* **Mismo resultado que en Blender.** El núcleo del mod reproduce la jerarquía de huesos
  del rig, los ejes exactos de cada hueso (incluida la ligera inclinación de brazos y piernas
  del rig), las curvas Bézier de Blender (corrección de manejadores y resolución de la curva
  igual que el Graph Editor) y el *skinning* de la malla del jugador del `.blend` (modificador
  Armature con **Preserve Volume**, es decir dual quaternion, y los pesos medidos en la malla).
  Los tests automáticos comparan el mod contra Blender:
  * huesos: error máximo **0.005 píxeles** en `cartwheel` e `inchworm`;
  * malla doblada (codos, rodillas, torso): error máximo **0.04 píxeles**.
* **Corrige los fallos del exportador del rig.** El script que trae el `.blend` escribe mal los
  manejadores Bézier de algunos ejes (no aplica el signo del eje), deja los del canal `bend` en
  radianes y redondea los tiempos a 3 decimales. El mod lo detecta y lo corrige al cargar el
  archivo, y el exportador por lotes de este repositorio ya exporta bien.
* **Compatible** con los emotes de Emotecraft / PlayerAnimationLibrary (formato Bedrock con
  `player_animation_library`) y con el formato clásico `emote.json` de Emotecraft.

## Crear emotes en Blender

1. Abre `emote_creator.blend` (Blender 5.2+, igual que el rig).
2. Crea una acción nueva para el armature `export_armature` y anímala (IK, *bends* y todo lo
   que ofrece el rig).
3. En el panel de la acción rellena nombre, autor, descripción e insignias.
4. Exporta:
   * **Recomendado – por lotes, sin fallos del exportador:**

     ```bash
     blender -b emote_creator.blend -P tools/blender/batch_export.py -- --out ./emotes
     ```

     Opciones: `--actions a,b` (solo esas acciones), `--no-icon`, `--icon-engine CYCLES`
     (para servidores sin GPU), `--author "Nombre"`.
   * O el botón **Export** del propio rig (el mod corrige sus fallos al cargar).
5. Copia los `.json` a `.minecraft/blendemotes/emotes/` (se crea al arrancar el juego) y pulsa
   **Recargar** en el menú de emotes. También puedes usar subcarpetas.

El icono que se ve en la rueda es el render que hace el exportador desde la cámara de la escena.

## En el juego

| Acción | Tecla por defecto |
|---|---|
| Rueda de emotes (mantener, apuntar, soltar) | **B** |
| Menú de emotes (asignar emotes a la rueda, reproducir, recargar, abrir carpeta) | **N** |
| Detener el emote | sin asignar |

* En la rueda: clic derecho o **TAB** abre el menú de edición.
* En el menú: elige un emote y luego una casilla de la rueda; clic derecho en una casilla la
  vacía; doble clic en un emote lo reproduce.
* Moverse, saltar, agacharse o atacar detiene el emote (configurable).
* Al empezar un emote la cámara pasa a tercera persona y vuelve sola al terminar (como Lunar).

### Configuración

`config/blendemotes.json`:

| Clave | Por defecto | Significado |
|---|---|---|
| `fadeIn` / `fadeOut` | 0.15 / 0.25 | segundos de transición desde/hacia la pose normal |
| `stopOnMove`, `stopOnJump`, `stopOnSneak`, `stopOnAttack`, `stopOnHurt` | true, true, true, true, false | qué detiene el emote |
| `autoThirdPerson` | true | cambiar a tercera persona al emotear |
| `bends` | true | doblar codos, rodillas y cintura |
| `showOtherPlayers` | true | ver los emotes de otros jugadores |
| `shareEmotes` | true | enviar tus emotes al servidor para que otros los vean |
| `emotesFolder` | `blendemotes/emotes` | carpeta de emotes |
| `wheel` | 8 casillas | emotes de la rueda (id o nombre) |

## Multijugador

Los demás jugadores ven tus emotes cuando el **servidor** tiene instalado BlendEmotes
(Fabric / Forge / NeoForge; en un solo jugador y en LAN funciona siempre). Los emotes se
identifican por su contenido: si otro jugador no tiene tu archivo, el servidor le envía una
versión comprimida (unos pocos KB) una sola vez. El mod no es obligatorio: clientes y
servidores sin él pueden conectarse igualmente.

Canal de red: `blendemotes:main` (el mismo en todas las versiones, 1.8.9 incluida).

## Versiones

| Minecraft | Loaders | Estado |
|---|---|---|
| 1.8.9 | Forge, Legacy Fabric | compila en CI, prueba en juego en CI |
| 1.20.1 | Fabric, Forge | en desarrollo |

## Compilar

Requisitos: JDK 21 (y JDK 8/17 para las versiones antiguas, Gradle los encuentra solos).

```bash
./gradlew build                       # todo
./gradlew -PcoreOnly :core:check      # solo el núcleo y sus tests (sin descargar Minecraft)
./gradlew build -PmcVersions=1.8.9    # solo algunas versiones
```

Los jars quedan en `versions/<versión>/build/libs/` (uno por loader).

La integración continua (`.github/workflows/blendemotes.yml`) compila todo y además arranca
Minecraft de verdad en una pantalla virtual, crea un mundo plano, reproduce los emotes
incluidos y guarda capturas.

## Estructura

```
core/              núcleo sin Minecraft (Java 8): formato, curvas, rig, bends, red, config
  src/test/        tests, incluida la comparación contra Blender (tools/blender/*.py)
versions/<mc>/     código de cada versión de Minecraft (main = común, fabric/forge = loader)
gradle/preprocess.gradle   preprocesador para compartir código entre versiones
tools/blender/     exportador por lotes y generador de datos de referencia de Blender
```
