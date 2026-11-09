# PORTING.md — Sky Arena: Forge 1.20.1 → NeoForge 26.1.2

Registro vivo del port. Origen: https://github.com/JrDemiurg/DemisSkyArenaMod (v1.3.6, Forge 1.20.1, Java 17, 65 clases).
Destino: **NeoForge 26.1.2.114 / Minecraft 26.1.2 / Java 25 / ModDevGradle 2.0.148**.

Identidad conservada del original: `mod_id=skyarena`, paquete `net.jrdemiurge.skyarena`, licencia MIT, autor JrDemiurge.
Versión: **1.3.6-26.1.2** → JAR `SkyArena-1.3.6-26.1.2.jar`.

Metodología por fase: ANALIZAR → PORTAR → COMPILAR → CORREGIR (nunca silenciar sin anotar) → COMMIT (solo si compila) → registrar aquí.

---

## Estado (resumen)

| Fase | Estado | Commit |
|---|---|---|
| 1. Esqueleto del proyecto (MDG 2.0.148, mods.toml, pipeline de artefactos MC) | ✅ | `4fff071`, `f4cffe3`, `ea27ef7` |
| 2. Registros (bloques, items, tabs, triggers, BE) | ✅ | `ba61840` |
| 3. Lógica (altar, oleadas, NBT, reloj, clima, scheduler, mixins) | ✅ | `ba61840` |
| 4. Datos y assets (structures, advancement, loot_table, recipe, items/, lang, tags) | ✅ | `ba61840` |
| 5. Pruebas de servidor (runServer sin excepciones) | ✅ | `adc99e4` |
| 6. Cliente (runClient en xvfb) y verificación visual | ⚠️ parcial | — |
| 7. Entrega (ZIP + JAR) | ✅ (iteración 1) | `4270e7c` |
| 8. **Bugs funcionales en juego** (mobs, protección, arenas) | ✅ | `4270e7c`, `7587fc9`, gametest |
| 9. **Iteración 3** — trofeos sin traducir + pool dinámica de mobs de mods | ✅ | ver «Verificación / Iteración 3» |
| 10. **Iteración 4** — pool dinámica escalada por dificultad (tiering) | ✅ | ver «Verificación / Iteración 4» |

**Iteración 2 (09/11)**: el usuario probó el JAR en su instancia real (client NeoForge 26.1.2.122, 100+ mods) y reportó 3 bugs; los 3 están corregidos y verificados (ver «Fase 8 — Bugs funcionales» y «Verificación»). Los mobs ya aparecen y la protección funciona.

**Iteración 3 (09/11)**: dos incidencias nuevas: (a) los trofeos se mostraban como `item.skyarena.oak_trophy` — en 26.1.2 el nombre visible del item usa la clave `item.*` y los lang solo traían `block.*`; añadidas las 6 claves a los 6 idiomas. (b) solo aparecían mobs vanilla: la config del original trae listas fijas de mods concretos y el pack del usuario no los tiene; añadida **expansión dinámica** de la pool por grupo (`dynamic`, `onlyModded`, `healthCostDivisor`, `defaultCost`, `excludeMobs` en `MobGroup`, todos con default seguro si el campo no existe en la config del usuario) + gametest `dynamic_mob_pool` que la cubre.

**Iteración 4 (09/11)**: el usuario confirmó los trofeos pero reportó que el **Paladín Poseído** (Legendary Monsters, 400 HP / 15 daño / 12 armadura) aparecía en nivel 1 sin estar en su config. Causa: la expansión dinámica de la iteración 3 añadía todos los hostiles de mods a TODOS los grupos, incluido `common` (niveles 1-10); su `config/sky_arena.json` en disco es anterior a los flags y usa los defaults. **Fix — tiering**: nuevo campo `dynamicMaxCost` por grupo (default: el coste estático más alto del grupo; grupo vacío → 0 = sin expansión, se respetan los aparcaderos del autor como `additional_unused_mobs`), coste automático revisado a `vida/3 + daño×4 + armadura×2` (`healthCostDivisor` 3.0, nuevo `attackCostMultiplier` 4.0), exclusión por mod entero (`excludeMods`) y deduplicación entre grupos del mismo rango de dificultad. Config distribuida con caps explícitos 60/130/270; el Paladín (217) queda reservado a `minibosses` (nivel 21+). Gametest ampliado a 6 casos.

---

## Cambios de API (antes → después)

| Área | Forge 1.20.1 (antes) | NeoForge 26.1.2 (después) |
|---|---|---|
| Interacción con bloque | `Block#use(...)` + `useWithoutItem` | `use()` **ya no existe**. Hook vanilla `useWithoutItem(state, level, pos, player, hit)` (override `protected`, corre en ambos lados). La lógica vive en `AltarBlock#activate()`; `AltarBlockTop` redirige al bloque inferior. |
| Click derecho externo | `PlayerInteractEvent.RightClickBlock` para todo | Solo se conserva para **cancelar** el click en cofres con llave (`RightClickBlockHandler`). La interacción del altar va por `useWithoutItem`. |
| `updateShape` | `(state, dir, facing, level, pos, facingPos)` | `(state, LevelReader, ScheduledTickAccess, pos, facing, facingPos, facingState, RandomSource)` |
| Registro de bloques | `DeferredRegister.create(ForgeRegistries.BLOCKS,…)`, `RegistryObject` | `DeferredRegister.createBlocks(MOD_ID)` → `DeferredBlock<T>` |
| Registro de items | `DeferredRegister.create(ForgeRegistries.ITEMS,…)`, `RegistryObject` | `DeferredRegister.createItems(MOD_ID)` → `DeferredItem<T>`; items de bloques con `Function<Block, Item>` + `Supplier<BlockBehaviour.Properties>` y `ITEMS.registerSimpleBlockItem(nombre, bloque)` para los simples |
| Tabs creativos / triggers | `CreativeModeTabEvent`, `CriteriaTriggers.register` manual | `DeferredRegister` para ambos (`ModCreativeTabs`, `ModTriggers`) |
| NBT de BlockEntity | `load(CompoundTag)` / `save(CompoundTag)` | `loadAdditional(ValueInput)` / `saveAdditional(ValueOutput)`; listas con `childrenList(...)` + `addChild()` |
| Hora del mundo | `level.getDayTime()`, `/time set` | `ServerClockManager.setTotalTicks(...)` (26.x) |
| Clima | `level.isRaining()`, `level.setRainLevel(...)` | `level.getWeatherData().setRaining(...)` |
| Ticker de BlockEntity | `EntityBlock#getTicker` | `TickEvent.ServerTickEvent.Post` (`net.neoforged.neoforge.event.tick`) — `AltarTicker`, `Scheduler` |
| Tipo básico | `ResourceLocation` | `Identifier` (mojmap 26.x) |
| Estructuras | `data/<ns>/structures/<name>.nbt` | `data/<ns>/structure/<name>.nbt` (ver **E-9** abajo) |
| Carpetas de datos | `advancements/`, `loot_tables/`, `recipes/` | singulares: `advancement/`, `loot_table/`, `recipe/` |
| `pack.mcmeta` | `pack_format: 15` | `min_format: 101`, `max_format: 101` |
| **Ruta de templates de estructura** | `data/<ns>/structures/<id>.nbt` | `data/<ns>/structure/<id>.nbt` — **E-9**: en la iteración 1 se usó `structure/<id>/structures/<id>.nbt` (inválida; los pools apuntaban a `<ns>:<id>/structures/<id>`), por lo que ni el worldgen jigsaw ni `/place template` encontraban la arena. Verificado contra el layout del jar vanilla (`ancient_city/city_center/...`). |
| Item models | `models/item/*.json` directos | definiciones `assets/<ns>/items/*.json` + `models/item/*.json` |
| Config | `ForgeConfigSpec` | JSON propia (Gson) `config/sky_arena.json`, cargada en `ServerAboutToStartEvent` (`ConfigHandler` → `SkyArenaConfig.loadConfig()`) |
| Registro de mixins | `mixins` clave del `mods.toml` de Forge (jar manifest) | Tabla **`[[mixins]]` de nivel superior** en `neoforge.mods.toml` con `config="..."` — **E-8**: anidada dentro de `[[mods]]` FML 11 la ignora silenciosamente. |
| Gametests | (sin equivalente) | `DeferredRegister` sobre `BuiltInRegistries.TEST_FUNCTION` + JSON `data/<ns>/test_instance/<test>.json` (datapack registry) + estructura en `data/<ns>/structure/`; ejecución con `gradlew runGameTestServer`. |

---

## Fase 8 — Bugs funcionales reportados en juego (iteración 2)

El usuario probó el JAR de la iteración 1 en su instancia real (NeoForge 26.1.2.122, ~100 mods) y reportó:

1. **Click al altar: música y noche, pero ningún mob** — el `latest.log` mostraba `ClassCastException: Skeleton cannot be cast to MobLootTableAccessor` en `AltarBlock.spawnArenaMob` (línea 346). Causa raíz: `skyarena.mixins.json` **nunca se cargó** porque en `neoforge.mods.toml` los mixins estaban declarados como `mixins = [{...}]` **dentro** de `[[mods]]`; `ModFileParser` de FML 11 solo lee la clave `mixins` de nivel superior (verificado por bytecode + docs MDK). Sin mixin, el cast explotaba y `activate()` abortaba justo cuando iba a spawnear la oleada (música/noche ya se habían aplicado). 
2. **Se podían romper bloques junto al altar** — misma causa raíz: `LevelMixin` (inyectado en `Level.setBlock`/`destroyBlock`) tampoco estaba aplicado, así que `FullProtectionZones` nunca se consultaba.
3. **Verificar que al alejarse los mobs desaparecen** (`battleLossDistance`).

Además se descubrió y corrigió un **tercer bug latente**: los templates de estructura estaban en `data/skyarena/structure/<id>/structures/<id>.nbt` y los pools referenciaban `skyarena:<id>/structures/<id>`, pero en 26.1.2 los templates viven en `data/<ns>/structure/<id>.nbt` — ni el worldgen jigsaw ni `/place template` encontraban la arena ("There is no template with id skyarena:sky_arena"). Movidos a la ruta correcta y actualizadas las referencias de `start_pool.json`.

**Correcciones** (commit `4270e7c`, `7587fc9` y siguientes):
- `neoforge.mods.toml`: `[[mixins]]` top-level canónico.
- `AltarBlock.spawnArenaMob`: cast defensivo `instanceof MobLootTableAccessor` (degrada con elegancia en vez de abortar la oleada si el mixin no está).
- Targets de los 4 mixins verificados contra el jar real `minecraft-patched-26.1.2.114.jar` (javap): `Level.setBlock(BlockPos,BlockState,int,int)`, `Level.destroyBlock(BlockPos,boolean,Entity,int)`, `Mob.lootTable : Optional<ResourceKey<LootTable>>`, `ServerBossEvent.addPlayer(ServerPlayer)`, `WitherBoss.dropCustomDeathLoot(ServerLevel,DamageSource,boolean)` — todos existen con la firma exacta.
- Templates movidos a `data/skyarena/structure/{sky_arena,ice_arena}.nbt` + referencias de pools corregidas.
- Trazas `[SkyArena]` añadidas a los retornos silenciosos de `activate()` (peaceful, cooldown) para diagnóstico.

---

## Decisiones (DEC)

- **DEC-1 — Pipeline binario de MC**: el sandbox mata procesos >~3 GB RSS y la decompilación de Vineflower con `--include-classpath` necesita >3 GB. Se usa el pipeline **binario** de ModDevGradle (jar binpatched de NeoForm, sin recompilar Minecraft) y se inyectaron los artefactos previos en la caché de NFRT. Sin impacto en el resultado: el juego es el binario oficial parcheado.
- **DEC-2 — IO pesado fuera de /tmp**: `/tmp` es FUSE y se cuelga en D-state con IO intensivo. Todo el trabajo de build intermedio vive en `workspace/build-tmp` (home).
- **DEC-3 — Identidad y versionado**: se copia identidad del original (`skyarena`, MIT, JrDemiurge) y se versiona `1.3.6-26.1.2` para que el JAR sea distinguible del de Forge.
- **DEC-4 — Interacción del altar**: se descartó el enfoque inicial (todo por `PlayerInteractEvent.RightClickBlock`) porque duplicaba lógica y mensajes; el altar usa el hook vanilla `useWithoutItem` (equivalente directo del antiguo `use()`), y el handler de evento queda solo para bloquear cofres cerrados (`RewardKeyItem.keyedChests`).
- **DEC-5 — Mixins de terceros**: los mixins que parcheaban mobs de otros mods (Catwalks & Co., etc.) no tienen objetivo en 26.1.2; se conservan compilados en `src/thirdparty/java` con su config aparte **no registrada** en `neoforge.mods.toml`, documentado como pendiente hasta que esos mods existan.
- **DEC-6 — Trazas de depuración**: `AltarBlock` lleva LOGGER.info (`[SkyArena] ...`) que resultaron decisivas para diagnosticar los bugs de la iteración 2 desde el log del usuario. Se mantienen en esta entrega (nivel info, una línea por activación); para el release final pueden bajarse a debug.

---

## Errores encontrados y resolución

| # | Error | Resolución |
|---|---|---|
| E-1 | OOM del sandbox decompilando MC con classpath (Vineflower carga ~65 jars, >3 GB) | DEC-1: pipeline binario + caché NFRT inyectada (HIT verificado en `decompile`, `patch`) |
| E-2 | `/tmp` (FUSE) en D-state con IO pesado | DEC-2: trabajar en `workspace/build-tmp` |
| E-3 | `processResources` con `expand` corrompía `[[mods]]` del `neoforge.mods.toml` | `mods.toml` literal sin placeholders (commit `f4cffe3`); verificado byte a byte en el JAR final |
| E-4 | JSON de datos 26.x estricto: comentarios y claves obsoletas rechazados | Regenerados limpios; triggers registrados vía `DeferredRegister` |
| E-5 | 4 modelos de trofeo (iron/oak/stone/netherite) con caras de área cero referenciando `#missing` (export de Blockbench) → 8 warnings "Missing texture references" | Remapeados `#missing` → textura del trofeo (`#4`/`#5`); cero impacto visual |
| E-6 | OpenAL falla en el cliente headless (sin dispositivo de audio) | Esperado en xvfb; el juego continúa sin sonido |
| E-7 | `updateShape` nueva firma: sin `Level` accesible al devolver `air` cuando desaparece la contraparte | Adaptado a la firma 26.x con `ScheduledTickAccess`; documentado en `AltarBlockTop` (quitar la tapa rompe la base) |
| E-8 | **Mixins nunca cargados** (bugs 1 y 2 del usuario): `mixins = [{...}]` dentro de `[[mods]]` es sintaxis inválida para FML 11; sin refmap ni error, el config se ignora → `ClassCastException` en `spawnArenaMob` (sin mobs) y `LevelMixin` sin aplicar (sin protección) | `[[mixins]]` top-level en `neoforge.mods.toml` + cast defensivo `instanceof`; targets de los 4 mixins verificados con javap contra el jar MC real |
| E-9 | **Templates de arena no encontrados**: ruta `structure/<id>/structures/<id>.nbt` inválida en 26.1.2 (layout verificado contra el jar vanilla: `structure/<id-path>.nbt`) → `/place template` fallaba y el worldgen jigsaw no generaba arenas | Movidos a `data/skyarena/structure/{sky_arena,ice_arena}.nbt` + `location` de los `start_pool.json` corregidos |
| E-10 | Gametest: el mundo del GameTestServer arranca con `gameTime < 30`, el cooldown anti-doble-activación del altar (`getGameTime() - battleEndTime < 30`) bloqueaba la activación de forma silenciosa | El test marca `battleEndTime = gameTime - 100` (estado legítimo de un altar real); añadidas trazas al retorno silencioso |
| E-11 | `GameTestAssertException(String)` ya no existe (26.1: constructor `(Component, int)`); `LevelData.setDifficulty` tampoco (ahora `MinecraftServer.setDifficulty(Difficulty, boolean)`) | Adaptado el test a la API 26.1 |

---

## Verificación

### Iteración 1 (08/11)

- **runServer**: arranca limpio en 26.1.2, mundo generado, sin excepciones del mod (commit `adc99e4`).
- **runClient (xvfb)**: mod descubierto (`Sky Arena 1.3.6-26.1.2`), recursos `mod/skyarena` cargados, jugador entra en el mundo y renderiza. Warnings E-5 resueltos; fallo de audio esperado (E-6).

### Iteración 2 (09/11) — bugs del usuario corregidos

**Track A — servidor dedicado + RCON** (`evidencia/track-a-server.log`, `track-a-results.txt`): **10/10 PASS**
- `/place template skyarena:sky_arena 0 120 0` → "Loaded template" (E-9 corregido).
- `data get block <altar>` → `ArenaType=sky_arena`, `FullProtectionRadius=35`, `MobSpawnRadius=20` (config cargada vía loadAdditional → loadArenaConfig).
- `setblock` **dentro** de la zona (×3, antes y después de summon/kill) → "Could not set the block" (LevelMixin aplicado → **bug 2 corregido**).
- `setblock` **fuera** (60 bloques) → "Changed the block" (control); `summon`/`kill` de zombie → OK (el mixin no interfiere con spawns normales).

**Track B — GameTestServer** (`evidencia/track-b-gametest.log`): **"All 2 required tests passed :)"**
- Gametest `skyarena:altar_battle_flow` sobre la estructura `skyarena:arena_test_platform` (15×8×15, altar con `ArenaType=gametest_arena`, config inyectada por el propio test: 60 puntos, zombies de 20).
- Protección: `setBlock` dentro de la zona cancelado ✓ (bug 2).
- Activación con jugador simulado (`makeMockPlayer`) → **3 zombies spawneados exactos**, `battlePhaseActive=true`, sin `ClassCastException` (bug 1 corregido; `disableMobItemDrop=true` ejercita el accessor mixin).
- Jugador teletransportado a `battleLossDistance + 8` → **los mobs desaparecen** vía `AltarTicker → tick → checkPlayerLeftArena → onDefeat` (bug 3 verificado y además prueba que `TickEvent.ServerTickEvent.Post` dispara).

**JAR final**: `SkyArena-1.3.6-26.1.2.jar` — TOML verificado con parser (mixins top-level presentes), templates en la ruta correcta, gametest incluido. Reconstruido y Track A re-ejecutado 10/10 con el código final.

**Pendiente para el release final**: verificación visual en cliente con los mods del usuario, y quitar/bajar a debug las trazas `[SkyArena]`.

### Iteración 3 (09/11) — trofeos sin traducir + pool dinámica de mobs de mods

**Incidencia (a) — trofeos sin nombre**: el tooltip mostraba `item.skyarena.oak_trophy`. En 26.1.2 el nombre visible de un item resuelve la clave `item.<ns>.<path>` (el hover de bloque desapareció); los trofeos se registran como `TrophyBlockItem` vía `registerItem`, así que su clave es `item.skyarena.*`, pero los 6 lang solo traían `block.skyarena.*` (la clave del 1.20.1 original, donde el BlockItem heredaba el descriptionId del bloque). **Fix**: añadidas las claves `item.skyarena.{oak,stone,iron,gold,diamond,netherite}_trophy` a en_us, es_es, de_de, fr_fr, ja_jp y ru_ru, copiando el texto del `block.*` de cada idioma. Cobertura verificada por script: los 16 nombres registrados (bloques + items) tienen clave en todos los idiomas.

**Incidencia (b) — solo mobs vanilla**: no era un fallo del port sino del diseño del original: `getAvailableMobs()` solo ofrece los mobs listados en `mobGroups` (config), que trae ~190 IDs de mods concretos; si el pack no tiene esos mods, el filtro de registro los descarta y quedan los vanilla. **Mejora del port** (no rompe configs existentes): cada `MobGroup` admite ahora `dynamic` (default true), `onlyModded` (default true), `healthCostDivisor` (4.0), `defaultCost` (10) y `excludeMobs` (ender_dragon, wither, giant, iron_golem, snow_golem). `AltarBlockEntity.getAvailableMobs()` expande cada grupo dinámico con todos los entity types hostiles registrados (`MobCategory.MONSTER` o categoría custom no-friendly no-MISC) no presentes en ninguna lista estática, con coste automático `maxHealth / healthCostDivisor` vía `DefaultAttributes` (fallback `defaultCost`), sin duplicados y una sola vez por pool. El JSON por defecto lleva los flags explícitos; `bosses` sale con `dynamic: false`.

**Track B — GameTestServer**: **"All 3 required tests passed :)"**
- `altar_battle_flow`: protección OK, spawn exacto de 3 zombies, despawn por distancia OK (regresión intacta).
- `dynamic_mob_pool` (nuevo): con `onlyModded=true` la pool en vanilla queda en 1 entrada (zombie, coste 20 sin cambios); con `onlyModded=false` se expande a 41 hostiles vanilla, sin duplicados, costes ≥ 1 (creeper = 5 = 20 HP / 4), sin iron_golem/villager y con el zombie estático preservado.

**JAR final**: `SkyArena-1.3.6-26.1.2.jar` (build 09/11 12:29 UTC). Track A re-ejecutado sobre este build para evidencia limpia (ver ZIP).

### Iteración 4 (09/11) — pool dinámica escalada por dificultad

**Incidencia — mobs de mods demasiado fuertes en niveles bajos**: el Paladín Poseído de Legendary Monsters (2.2.0, presente en el pack: 400 HP, 15 daño, 12 armadura, 2 fases) aparecía en dificultad 1. No era un residuo de ninguna configuración anterior: la expansión dinámica de la iteración 3 añadía cada hostil de mod a **todos** los grupos (`common`, `elite`, `minibosses`), y el altar de nivel 1 solo usa `common`, así que entraba directamente. La `config/sky_arena.json` del usuario en disco (`config/sky_arena.json`, extraída por una partida anterior) no tiene los nuevos campos, así que se aplicaban los defaults.

**Fix — tiering por coste** (todos los campos con default seguro para configs existentes):

- `MobGroup.dynamicMaxCost` (Integer, nullable): coste máximo admitido para mobs dinámicos en el grupo. Default = el mayor coste estático ya presente en el grupo (la curaduría del autor define el techo del tier); grupo vacío → 0 → sin expansión (preserva `additional_unused_mobs` y cualquier grupo-aparcadero).
- Coste automático revisado: `vida/healthCostDivisor + daño×attackCostMultiplier + armadura×2`, recortado a [1, 250]. `healthCostDivisor` pasa de 4.0 a **3.0**; nuevo `attackCostMultiplier` (default 4.0); la armadura pesa ×2. Justificación con datos reales del Paladín: 400/3 + 15×4 + 12×2 = **217** > cap de `elite` (130) → solo entra en `minibosses` (nivel 21+). Vanilla: zombie/creeper 15, blaze 31, ravager 81, warden 250.
- `MobGroup.excludeMods` (List, nullable): exclusión por namespace completo, p. ej. `["legendary_monsters"]`.
- Deduplicación: los ids añadidos dinámicamente se marcan en `usedMobIds`, así un mob admitido por `common` no se duplica en `elite` cuando un mismo rango usa ambos grupos.
- `appendDynamicMobs()` aplica el cap antes de añadir y añade el mob al set de usados solo si entra.
- Config distribuida (`assets/skyarena/config/sky_arena.json`): caps explícitos `common=60`, `elite=130`, `minibosses=270` (sky_arena, ice_arena y ejemplos con grupos), `additional_unused_mobs` con `dynamic:false` en todos sus grupos.

**Track B — GameTestServer**: **"All 3 required tests passed :)"** (build 09/11 14:00 UTC)
- `altar_battle_flow`: intacto (spawn 3 zombies, protección, despawn).
- `dynamic_mob_pool` ampliado a 6 casos: (1) `onlyModded=true` → pool vanilla sin cambios; (2) `onlyModded=false` → 23 hostiles con cap por defecto 20, creeper cost=15, sin duplicados ni pacíficos; (3) cap explícito 20 → warden (≥167) y ravager (≥33) fuera; (4) `excludeMobs` → creeper fuera, blaze (31≤60) dentro (38 mobs); (5) `excludeMods=[minecraft]` → pool reducida al estático; (6) grupo vacío sin cap → 0 entradas / con cap 60 → opt-in de dinámicos.

**JAR final**: `SkyArena-1.3.6-26.1.2.jar` (build 09/11 14:00 UTC). El build también vuelve a incluir los NBT anidados `structure/<arena>/structures/<arena>.nbt` que faltaban en el JAR de la iteración 3 (referenciados por el layout original; los template_pool usan los de primer nivel, presentes en ambos).
