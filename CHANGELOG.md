# Changelog

All notable changes to NTM IQ Expansion are listed here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses [Semantic Versioning](https://semver.org/).
The feature codes (F01 to F29) refer to the IQ Edition feature inventory the port works through.

## 0.4.0 - 2026-10-16

### Added
- **Contagion and genetics (F02):** diseases of five kinds with mutating genomes and symptoms, running in the
  bloodstream. They spread by air, touch, hits, proximity, contaminated items and injection, with a config switch for
  every path and a cap on infected bodies around a carrier (`ntmiq-contagion.cfg`).
- Genome vials, floppy disks, the Pharmaceutical Computing Unit and the **Dial-A-Dose medicine synthesizer**, which
  brews a matched antiserum, or grows the disease when loaded with blood.
- Rubber and Desh-lined contagion gloves.
- Wild disease syringes in dungeon and mineshaft loot.
- Genome-sample recipes in the Magnetic Separator.
- **Husks and cloning (F03):** five human parts with stats and traits, dropped by zombies that players kill.
- The **Cloner**, which grows a husk of the blood sample's owner.
- The **Neuralyser** body swap and the **Dead Man's Neuralyser** death swap. Both were rewritten to be multiplayer-safe
  and can be turned off in `ntmiq-husks.cfg`.
- **DIA-1000** haemodialysis machine, which cleans The Nest's bloods (`hbmHaemodialysis.json` recipes).
- **Artery Pump:** taps Satan's Blood bedrock arteries for an endless supply.
- Fluid attacks for the nether glyphids: the behemoth spits burning bloatmusk and leaves a cloud when it dies, the
  blaster lobs **bloat bombs**, and Brenda dies into a pheromone mist that hatches grunts.

### Fixed
- A crash at startup in large packs: IQ's fluids read the flammable trait of CE's reformed kerosene before CE had
  attached it. They now fall back to a default heat value.
- A crash the first time some machines rendered, with "Cannot load new models after uploadModels()" (seen on the Mass
  Centrifuge). Every machine's OBJ models now load during client pre-init.

## 0.3.0 - 2026-10-16

### Added
- **Mining drones (F09):** the Quarry Drone with Quarry Markers, the Spelunking Drone, and four Demolisher Drones
  (normal, express, chunkloading, chunkloading express) that run on CE's drone system.
- **Conveyor Combinator and Conveyor Spraypainter (F16)** for CE conveyor belts.
- **94 IQ fluids (F24)**, registered as CE fluids, with IQ's chemical plant, mixer and electrolyser recipes where the
  inputs exist.
- **Growth Chamber (F13):** budding salt and budding certus quartz (AE2) with buds, and salt geodes in world
  generation.
- **Magnetic Separator (F11)** with iron, nickel, vanadium and neodymium discs, and survival recipes for them.
- **Mass Centrifuge (F12)**, a sludge processor.
- **Magnetic crates (F10):** the Magnetic Strip, Magnetic Crafter and Magnetic Stocker, with key bindings B, M and H.
- **Construction foam and Quikrete (F19).**
- **Detail decals (F20)**, 16 types.
- **Bloodstream and pharma (F01):** a per-entity bloodstream, drug fluids, syringes (fluid, combat, medical, Notch),
  the Auto Drip and Blood Monitor armor mods, and drug and symptom potions.
- **The Nest (F05):** a flesh biome for the nether, added through Nether-API or as nether regions, without replacing
  the nether. It brings flesh plants, cysts, bloatsprout trees, bloatfire, bedrock arteries, nether hives, nine nether
  glyphids and netherglyphid food (`ntmiq-nest.cfg`).

## 0.2.0 - 2026-10-12

Released together with 0.3.0 in a single commit; it was not published on its own.

### Added
- **Railgun turret "Louis" (F08)** with six sabot types. It aims with a ballistic solver and is controlled from its GUI
  or over Redstone-over-Radio.
- **Industrial Pipe Anchor and Pipe Anchor Pressurizer (F17).**
- **Cassiterite, rutile and cobaltite** resource stones and the stone-layer world generation for them, plus anthracite
  and flaming anthracite (`ntmiq-worldgen.cfg`).
- **Soldering alloy** ingot (F22).

## 0.1.0 - 2026-10-03

First release of the port.

### Added
- Tin, bronze and vanadium (ingots, nuggets, powders).
- 24 raw ores and raw ore blocks. HBM ores drop raw ore, with smelting and ore dictionary entries.
- 34 crystal storage blocks.
- Sheetmetal in 16 colours.
- The plastic crate.
- Anthracite and flaming anthracite.
- Hemp coke and haemacoke.
- The **Industrial Pipette** helmet mod.
