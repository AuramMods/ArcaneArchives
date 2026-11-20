# Arcane Archives

Arcane Archives adds magical storage, automation, and progression powered by Radiant Quartz.

## Minecraft 1.20.1

The port runs on Forge 47.x and Java 17. Install the same Arcane Archives jar in the client and server `mods` directories. The Arsenal is disabled by default. Set `enableArsenal = true` in the world's `serverconfig/arcanearchives-server.toml` to enable gem recipes and effects.

Build the production jar with `./gradlew build`. The reobfuscated jar is written to `build/libs`. Run `./gradlew runGameTestServer` for the headless integration suite.

See the [port notes](docs/forge-port.md) for gameplay coverage, optional integrations, and save compatibility. Use a fresh 1.20.1 world. A direct upgrade from 1.12.2 loses Arcane Archives devices and their contents.

[Published 1.12.2 release](https://www.curseforge.com/minecraft/mc-mods/arcane-archives/files/3057332) · [Original development roadmap](https://docs.google.com/spreadsheets/d/1Q08AQy5pMY0nrt2vuPV9bQ9y3ZgJ5ayHfISNp_g4ivo/edit#gid=286281250) · [Discord](https://discord.gg/U5Emdp4)
