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
  * malla doblada (codos, rodillas, torso): error máximo **0.04 píxeles**;
  * codos y rodillas doblados hacia los lados y el micrófono del ejemplo `cantar`: error
    máximo **0.006 píxeles**.
* **Codos y rodillas en todas las direcciones.** El antebrazo y la parte baja de la pierna se
  doblan hacia delante y hacia atrás **y hacia los lados** (el hueso `*_bend` gira en X y en Z).
* **Modelos en los emotes.** Un emote puede llevar modelos 3D propios (un micrófono, una
  guitarra, un caballo...) enganchados a cualquier hueso del rig o a huesos nuevos que añadas;
  viajan dentro del `.json` (geometría y textura) y se ven en todas las versiones y en
  multijugador.
* **Corrige los fallos del exportador del rig.** El script que trae el `.blend` escribe mal los
  manejadores Bézier de algunos ejes (no aplica el signo del eje), deja los del canal `bend` en
  radianes y redondea los tiempos a 3 decimales. El mod lo detecta y lo corrige al cargar el
  archivo, y el exportador por lotes de este repositorio ya exporta bien.
* **Compatible** con los emotes de Emotecraft / PlayerAnimationLibrary (formato Bedrock con
  `player_animation_library`) y con el formato clásico `emote.json` de Emotecraft.

## Instalar

1. Instala el loader de tu versión: Fabric (con **Fabric API**; en 1.8.9 y 1.12.2, Legacy
   Fabric con Legacy Fabric API), Forge o NeoForge.
2. Copia en `.minecraft/mods/` el jar de tu versión y loader:
   `blendemotes-mc<versión>-<mod>-<loader>.jar`, por ejemplo
   `blendemotes-mc1.21.1-1.0.0-fabric.jar`. Los jars `-dev` y `-sources` no son para jugar.
3. Para que otros jugadores vean tus emotes, instálalo también en el servidor (ver
   [Multijugador](#multijugador)).

## Crear emotes en Blender

0. Una vez: actualiza el rig (se guarda un `.blend` nuevo, el original no se toca):

   ```bash
   blender -b emote_creator.blend -P tools/blender/upgrade_rig.py -- --out emote_creator_blendemotes.blend
   ```

   Desbloquea el eje Z de `left/right_arm_bend` y `left/right_leg_bend` (doblez lateral), pone
   el exportador nuevo en el botón **Export** del rig, añade el campo **Modelos** al panel de
   la acción y trae el ejemplo `cantar` (micrófono en la mano, codo y rodilla hacia los lados).
   `--no-demo` lo actualiza sin el ejemplo.
1. Abre `emote_creator_blendemotes.blend` (Blender 5.2+, igual que el rig).
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

### Doblar hacia los lados

Rota el hueso `*_bend` del antebrazo o de la pierna en **X** (delante/atrás, como siempre) y en
**Z** (hacia los lados). El exportador escribe los tres ejes (`"bend": {"vector": [x, y, z]}`)
solo cuando hace falta; los emotes que solo doblan hacia delante siguen escribiendo `"value"`,
así que los emotes viejos y los de Emotecraft funcionan igual.

### Modelos (micrófono, caballo, lo que quieras)

1. Modela el objeto (o impórtalo) y **emparéntalo a un hueso** del `export_armature`
   (*Parent → Bone*): `right_item` / `left_item` para lo que va en la mano, `body` para algo
   que se mueve con todo el cuerpo (un caballo), o un **hueso nuevo** que añadas al armature y
   animes como quieras (se exporta solo, con su jerarquía).
2. Mete los objetos en una colección y elígela en el campo **Modelos** del panel de la acción.
3. Exporta como siempre. Cada objeto viaja en el `.json` con su textura (la imagen del nodo
   *Image Texture* de su material, o su color base si no tiene imagen).

Límites: 20 000 triángulos por modelo, texturas de hasta 1 MB y 32 modelos por emote. Se
dibujan sin ocultar caras traseras (sirven planos y mallas abiertas) y con transparencia.

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

Un solo código fuente compartido (`shared/legacy` para 1.8.9–1.12.2 y `shared/modern` para
1.16.5 en adelante) se compila para cada versión con un preprocesador (`//#if MC >= ...`).

| Minecraft | Loaders | Java |
|---|---|---|
| 1.8.9 | Forge, Legacy Fabric | 8 |
| 1.12.2 | Forge, Legacy Fabric | 8 |
| 1.16.5 | Forge, Fabric | 8 |
| 1.18.2 | Forge, Fabric | 17 |
| 1.19.2 | Forge, Fabric | 17 |
| 1.20.1 | Forge, Fabric | 17 |
| 1.20.4 | Forge, Fabric, NeoForge | 17 |
| 1.21.1 | Forge, Fabric, NeoForge | 21 |
| 1.21.4 | Forge, Fabric, NeoForge | 21 |
| 1.21.8 | Forge, Fabric, NeoForge | 21 |
| 1.21.10 | Forge, Fabric, NeoForge | 21 |
| 1.21.11 | Forge, Fabric, NeoForge | 21 |
| 26.1.2 | Forge, Fabric, NeoForge | 25 |
| 26.3 | Forge, Fabric, NeoForge | 25 |

Cada jar es para su versión exacta de Minecraft. Añadir otra versión es crear
`versions/<versión>/build.gradle` (versiones de los loaders) y añadirla a `settings.gradle` y
`ci/targets.txt`. CI compila todas y arranca el juego de verdad con cada loader.

## Compilar

Requisitos: JDK 21 para Gradle, más JDK 8, 17 y 25 para las versiones que los usan (Gradle los
busca solo).

```bash
bash tools/unimined/build.sh              # una vez: plugin Unimined fijado (necesario para 26.x)
./gradlew build                           # todas las versiones
./gradlew -PcoreOnly :core:check          # solo el núcleo y sus tests (sin descargar Minecraft)
./gradlew build -PmcVersions=1.21.1       # solo algunas versiones (lista separada por comas)
./gradlew -PmcVersions=1.21.1 :mc-1.21.1:fabricRunClient   # probar en el juego
```

Los jars quedan en `versions/<versión>/build/libs/` (uno por loader).

La integración continua (`.github/workflows/blendemotes.yml`) compila todo y además arranca
Minecraft de verdad en una pantalla virtual para cada versión y loader, crea un mundo plano,
reproduce los emotes incluidos y guarda capturas (se imprimen como hoja de contacto en el log).
Lo hace dos veces: en el entorno de desarrollo y con los **jars finales** instalados en el
loader real (HeadlessMC instala Fabric/Forge/NeoForge como un launcher y el jar va en `mods/`).

## Estructura

```
core/              núcleo sin Minecraft (Java 8): formato, curvas, rig, bends, red, config
  src/test/        tests, incluida la comparación contra Blender (tools/blender/*.py)
shared/legacy/     código compartido 1.8.9–1.12.2 (nombres MCP)
shared/modern/     código compartido 1.16.5+ (nombres de Mojang): main, fabric, forge, neoforge
versions/<mc>/     un build.gradle por versión (versiones de los loaders)
gradle/            lógica de build común y el preprocesador (preprocess.gradle)
ci/                versiones de CI (targets.txt) y herramientas de consulta de API
tools/blender/     actualizador del rig, exportador (blendemotes_export.py, por lotes y botón
                   del rig) y generador de datos de referencia de Blender
```
