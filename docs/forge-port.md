# Forge 1.20.1 port

The gameplay baseline is the shipped [0.2.0.25-mixins8 source](https://github.com/AuramMods/ArcaneArchives/tree/bb99accf48ed583e29b0efae56e28c963407b8df). The modern build uses Java 17, Forge 47.4.28, and Mojang mappings. During the port, the active sources are in src/forge and the prior sources remain available in src/main for comparison.

## Shipped behavior to verify

| Area | Baseline to carry forward | Port status |
| --- | --- | --- |
| Radiant Chest | 54 slots, configurable oversized stacks, automation, comparator, inventory transfers, conversion, naming, displayed item, routing mode and both construction recipes | Core chest and original art in place. Conversion, display tools, routing and survival acquisition remain. |
| Trove, Tank and Amphora | Single-item Trove with a 32,768-item base capacity in the shipped source, 16-bucket Tank, upgrades, portable contents, capabilities and remote fill and drain | Both storage devices have automation and portable contents. The Tank supports buckets and has a fluid level renderer. Upgrades, Trove item display and Amphora remain. |
| Networks and Manifest | Personal networks and shared hives, invitation, resignation and expulsion items, permissions, item and fluid search, location tracking, filters, Manifest Lectern and listener updates | Device ownership and hive membership persist. Player-facing letters, Manifest and routing remain. |
| Brazier of Hoarding | Brazier and fire model, dropped item and fluid routing, configurable range, sorting and priority rules, visual feedback and access controls | Dropped, held and automated routing works with Chest, Trove and Tank, including range and hive scope. Configuration screen, range display and crafting table destination remain. |
| Quartz progression | Radiant Resonator, Raw Quartz Cluster, Sliver of Light, raw and shaped quartz, raw and shaped storage blocks, Radiant Lantern, crafting components, sounds and Tome acquisition | Pending |
| Crafting | Gem Cutter’s Table multiblock and ingredient matching, custom recipes and JEI categories, recipe reuse, Radiant Crafting Table with persistent matrix and network access | Pending |
| Tools and upgrades | Monitoring Crystal, Radiant Key, Scepters of Manipulation and Revelation, Devouring Charm, Debug Orb and administrative commands | Pending |
| Arsenal | Gem Socket and its inventory, all 15 effects, charges, recharging powders, cooldowns, unlocks, visuals and configurable availability | Pending |
| Guide and integrations | Tome of Arcana, guides, translations, advancements, config controls, JEI, scriptable Gem Cutter recipes, block inspection, crafting controls, equipment slot and supported cross-mod interactions | Pending |

The Arsenal baseline includes Slaughtergleam, Murdergleam, Agegleam, Cleansegleam, Switchgleam, Salvegleam, Munchstone, Orderstone, Mindspindle, Elixirspindle, Mountaintear, Rivertear, Parchtear, Phoenixway and Stormway. Both Chromatic Powders are part of that baseline. The old registry includes a conditional guidebook marker for the Arsenal. Check behavior with the feature enabled and disabled.

## Local verification

Run ./gradlew build with Java 17 to produce the reobfuscated jar in build/libs. Run ./gradlew runGameTestServer for the headless tests. The server configurations use disposable directories beneath run and automatically accept the server EULA. Keep client runs muted and use an isolated display.

Headless GameTests exercise chest hopper insertion, capability simulation, transfers and slot packets, hive membership and device ownership, Trove interactions and portable loot, Tank buckets, fluid automation and portable loot, and Brazier item priority, hive scope, range and fluid routing. Legacy extended-count deserialization has a test fixture. Whole-world upgrade support remains unverified.
