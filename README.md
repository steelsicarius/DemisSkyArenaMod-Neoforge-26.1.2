# Sky Arena (NeoForge 26.1.2)

*Acepta el desafío.* Arena de batalla progresiva con oleadas, jefes y trofeos.
Port de **Sky Arena** de JrDemiurge (Forge 1.20.1) a **NeoForge 26.1.2 / Minecraft 26.1.2**.

- **Mod ID:** `skyarena`
- **Versión:** `1.3.6-26.1.2`
- **Autor original:** JrDemiurge — licencia **MIT** (identidad conservada del original)
- **Fuente original:** https://github.com/JrDemiurg/DemisSkyArenaMod

## Qué contiene

- **Altar de Batalla** (`altar_battle` + `altar_battle_top`): bloca el altar, haz clic para iniciar oleadas progresivas de mobs con dificultad configurable; al terminar, recompensas con llaves por bioma (bosque, desierto, hielo, crimson, end).
- **Trofeos** (roble, piedra, hierro, oro, diamante, netherita): recompensas por avance, con efectos configurables.
- **Ojo de invocación** (`crimson_eye`, `ice_eye`) y **analizador de mobs** (`mob_analyzer`).
- Estructuras generadas `sky_arena` e `ice_arena` con tags de biomas.
- Configuración JSON (`config/sky_arena.json`) con oleadas predefinidas, grupos de mobs y rangos de dificultad.
- Avances (advancements) propios y triggers de criterio.

## Requisitos

| | |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.114 o superior |
| Java | 25 (compilación y ejecución) |

## Instalación

1. Instala NeoForge 26.1.2 para Minecraft 26.1.2.
2. Copia `SkyArena-1.3.6-26.1.2.jar` en la carpeta `mods/`.
3. Opcional: edita `config/sky_arena.json` tras el primer arranque.

## Compilar desde fuente

```bash
# JDK 25 en el PATH (o ajusta org.gradle.java.installations.paths)
./gradlew build
# resultado: build/libs/SkyArena-1.3.6-26.1.2.jar
```

> Nota del sandbox de desarrollo: `gradle.properties` fija `-Xmx1G` y `org.gradle.java.installations.paths` a una ruta local de JDK 25; ajusta ambos a tu entorno.

## Port a 26.1.2

Este JAR es un **port completo del código, datos y assets** desde Forge 1.20.1 (v1.3.6). Lo más relevante:

- `Block#use()` ya no existe: la interacción del altar usa el hook vanilla `useWithoutItem(...)`; `PlayerInteractEvent.RightClickBlock` solo cancela clicks en cofres cerrados.
- NBT de BlockEntities migrado a `ValueInput`/`ValueOutput` (`loadAdditional`/`saveAdditional`).
- Tickers vía `TickEvent.ServerTickEvent.Post`; hora del mundo con `ServerClockManager`; clima con `Level.getWeatherData()`.
- Registros con `DeferredRegister.Blocks/Items` (`DeferredBlock`/`DeferredItem`) y `registerSimpleBlockItem`.
- `ResourceLocation` → `Identifier`; datos con carpetas singulares (`advancement/`, `loot_table/`, `recipe/`), estructuras en `data/skyarena/structure/<id>.nbt`, `pack.mcmeta` con `min_format/max_format: 101`.
- Mixins de terceros apartados a `skyarena-thirdparty.mixins.json` (no cargado hasta que sus mods existan en 26.1.2).

### Correcciones de la iteración 2 (bugs reportados al probar en juego)

1. **No aparecían mobs al activar el altar** y **se podían romper bloques junto al altar** — misma causa raíz: los mixins se declaraban como `mixins = [{...}]` dentro de `[[mods]]` en `neoforge.mods.toml`, y FML 11 solo lee la clave `mixins` de **nivel superior**. El config nunca se cargó: el cast a `MobLootTableAccessor` lanzaba `ClassCastException` (abortaba la oleada justo tras música/noche) y `LevelMixin` no bloqueaba `setBlock`/`destroyBlock`. Corregido con la forma canónica `[[mixins]]` y un cast defensivo `instanceof`.
2. **Las arenas no se colocaban** (`/place template` → "There is no template...") — los templates estaban en `structure/<id>/structures/<id>.nbt`; en 26.1.2 viven en `data/<ns>/structure/<id>.nbt` (verificado contra el jar vanilla). Corregido junto con las referencias de los `template_pool`.
3. **Verificado**: al alejarse del altar más de `battleLossDistance`, los mobs invocados desaparecen.

Pruebas automatizadas incluidas (`gradlew runGameTestServer`): spawn de la oleada completa, zona de protección y despawn por distancia sobre una estructura de test propia.

### Correcciones de la iteración 3 (trofeos sin traducir y mobs de otros mods)

1. **Trofeos sin nombre** (`item.skyarena.oak_trophy`): en 26.1.2 el nombre visible de un item resuelve la clave `item.skyarena.<nombre>` (el texto flotante del bloque ya no existe); los 6 idiomas solo traían `block.skyarena.<nombre>`. Añadidas las claves `item.skyarena.*_trophy` a los 6 idiomas (en_us, es_es, de_de, fr_fr, ja_jp, ru_ru).
2. **Solo aparecían mobs vanilla**: la pool de mobs sale de `mobGroups` en `config/sky_arena.json`, que en el original trae listas fijas curadas (~190 mobs de mods concretos: Born in Chaos, Cataclysm, Alex's Mobs, Goety…). Si no tienes esos mods, tras el filtro de registro solo quedan los vanilla. Ahora cada grupo admite **expansión dinámica** de la pool:

   | Campo (por grupo) | Default | Efecto |
   |---|---|---|
   | `dynamic` | `true` | añade a la pool todos los mobs hostiles registrados que no estén ya en una lista estática |
   | `onlyModded` | `true` | solo mobs con namespace distinto de `minecraft` (la lista vanilla del autor queda intacta; con `false` entran también hostiles vanilla no listados) |
   | `healthCostDivisor` | `3.0` | coste automático = vida máxima / divisor, recortado a [1, 250] (default revisado en la iteración 4, antes `4.0`) |
   | `defaultCost` | `10` | coste si el mob no tiene atributos de vida |
   | `excludeMobs` | lista propia | mobs nunca añadidos dinámicamente (ender dragon, wither, giant, iron golem, snow golem) |

   Cada mob entra una sola vez (el primer grupo dinámico gana) y el grupo `bosses` se distribuye con `"dynamic": false` para no desequilibrar; ponlo a `true` si quieres jefes de mods en la arena. **La config ya generada en tu instancia no necesita retoques**: los campos ausentes usan estos mismos valores por defecto, así que los hostiles de tus mods (p. ej. Bosses of Mass Destruction, Legendary Monsters) entran solos en los grupos `common`/`elite`/`minibosses`.

Pruebas: gametest `dynamic_mob_pool` (pool intacta con `onlyModded=true`; con `false` se expande con los hostiles registrados, sin duplicados, sin mobs no hostiles y con coste automático) + `altar_battle_flow` (spawn, protección y despawn intactos).

### Correcciones de la iteración 4 (mobs de mods escalados por dificultad)

1. **Mobs demasiado fuertes en niveles bajos** (reportado con el Paladín Poseído de Legendary Monsters — 400 HP, 15 de daño, 12 de armadura, dos fases — apareciendo en nivel 1): la expansión dinámica de la iteración 3 añadía todos los hostiles de mods a TODOS los grupos de la arena, incluido `common` (niveles 1-10). Ahora cada grupo tiene un **tope de coste** `dynamicMaxCost`: un mob dinámico cuyo coste supere el tope no entra en ese grupo y queda reservado para los tiers superiores, que solo se usan a partir de ciertos rangos de dificultad. El resultado es que la pool dinámica escala con la dificultad:

   | Campo (por grupo) | Default | Efecto |
   |---|---|---|
   | `dynamicMaxCost` | el coste estático más alto del grupo (`0` si el grupo está vacío) | los mobs dinámicos con coste superior quedan fuera del grupo. Un grupo vacío (p. ej. `additional_unused_mobs`) no expande nada salvo que definas un cap explícito — así se respetan los aparcaderos del autor |
   | `attackCostMultiplier` | `4.0` | peso del daño de ataque en el coste automático |
   | `excludeMods` | vacía | excluye un mod entero por namespace, p. ej. `["legendary_monsters"]` |
   | `healthCostDivisor` | `3.0` | peso de la vida en el coste automático (antes `4.0`) |

   Fórmula del coste automático: `vida/healthCostDivisor + daño×attackCostMultiplier + armadura×2`, recortado a [1, 250]. Ejemplos vanilla: zombie/creeper 15, blaze 31, ravager 81, warden 250. El Paladín Poseído (400/15/12) cuesta 217, así que con la config distribuida (caps 60/130/270 en `common`/`elite`/`minibosses`) no puede aparecer hasta el tier `minibosses` (nivel 21+), mientras que un mob de mod tipo esqueleto (coste ~19) sigue entrando en `common` desde el nivel 1.

   - **Modo estricto "solo los mobs de mi config"**: pon `"dynamic": false` en cada grupo.
   - **Excluir un mob concreto**: `"excludeMobs": ["legendary_monsters:posessed_paladin"]` (el id exacto se ve con el F3+Jade o en el log al invocarlo).
   - **Excluir un mod entero**: `"excludeMods": ["legendary_monsters"]`.
   - **Afinar el tier de un grupo**: baja o sube `dynamicMaxCost` (p. ej. `common` con `30` para que en niveles bajos solo entren mobs débiles).
   - Tu `config/sky_arena.json` actual sigue funcionando sin tocar nada: los campos ausentes usan estos defaults, así que el escalado por dificultad se aplica solo. Para obtener la config con los caps ya escritos, borra el archivo y se regenerará al siguiente arranque.

2. **Duplicados entre grupos**: un mismo mob dinámico ya no puede entrar dos veces cuando un rango de dificultad usa varios grupos (common + elite a la vez, por ejemplo); gana el primero que lo admita.

Pruebas: gametest `dynamic_mob_pool` ampliado a 6 casos (cap por defecto y cap explícito, exclusiones por id y por mod entero, grupos vacíos con y sin cap) + `altar_battle_flow` intacto. `All 3 required tests passed`.

Detalle completo (fases, errores, decisiones, verificación): ver **PORTING.md**.

## Licencia

MIT — © JrDemiurge. Port mantenido con la misma licencia.
