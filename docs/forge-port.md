# Forge 1.20.1 port

The gameplay baseline is the shipped [0.2.0.25-mixins8 source](https://github.com/AuramMods/ArcaneArchives/tree/bb99accf48ed583e29b0efae56e28c963407b8df). The modern build uses Java 17, Forge 47.4.28, and Mojang mappings. The Forge 1.20.1 source and resources use the standard Gradle layout under `src/main`. The published 1.12.2 sources remain on their release branch.

The release inventory covers the 16 block IDs, 50 item IDs, and 3 sound IDs recorded in the original Forge save. The legacy `arcanearchives:weight` entity only rendered temporary `/brazier` diagnostics. In 1.20.1 those markers travel in the diagnostic packet and render on the requesting player's client.

## Shipped behavior to verify

| Area | Baseline to carry forward | Port status |
| --- | --- | --- |
| Radiant Chest | 54 slots, configurable oversized stacks, automation, comparator, inventory transfers, conversion, naming, displayed item, routing mode and both construction recipes | Storage, both survival recipes, in-world conversion, naming and routing controls are in place. A Scepter and a representative item set the display on a horizontal face. The owner and hive members control Brazier routing. |
| Trove, Tank and Amphora | Single-item Trove with a 32,768-item base capacity in the shipped source, 16-bucket Tank, upgrades, portable contents, capabilities and remote fill and drain | Both devices keep their contents and upgrades when moved and expose capabilities when placed or carried. The Tank renders its level in the world and as an item. The Radiant Key locks the Trove's item type. The Devouring Charm voids matching overflow. The Amphora links to a permitted Tank and supports modes, fluid capability and dispensers. |
| Networks and Manifest | Personal networks and shared hives, invitation, resignation and expulsion items, permissions, item and fluid search, location tracking, filters, Manifest Lectern and listener updates | Signed letters use the live hive roles. The Manifest lists loaded inventories and Tank fluids by personal or hive scope, filters and sorts the results, and tracks locations from the item, hotkey or two-block Lectern. Monitoring Crystals supply attached inventory contents. The Manifest can share search text with JEI and track JEI items. |
| Brazier of Hoarding | Brazier and fire model, dropped item and fluid routing, configurable range, sorting and priority rules, visual feedback and access controls | Dropped, held and automated routing works with Chest, Trove, Tank and the Gem Cutter’s drawer. The Scepter opens the Brazier’s radius and network controls for the owner and members of the hive. The range preview follows server changes. |
| Quartz progression | Radiant Resonator, Raw Quartz Cluster, Sliver of Light, raw and shaped quartz, raw and shaped storage blocks, Radiant Lantern, crafting components, sounds and Tome acquisition | Resonator growth, harvesting, its completion and loop sounds, Sliver of Light, quartz packing recipes, the Lantern and the materials are in place. The Tome can be crafted or offered once after breaking a bookshelf or crafting a Resonator, as configured. |
| Crafting | Gem Cutter’s Table multiblock and ingredient matching, custom recipes and JEI categories, and a Radiant Crafting Table with a persistent grid, three recipe shortcuts and Manifest tracking | The Gem Cutter has the original model and screen, an automated drawer, persistent selection and data-driven recipes. CraftTweaker can add, replace or remove recipes. The Radiant Crafting Table saves its grid and shortcuts, accepts JEI recipe transfers, and supports Crafting Tweaks. Both tables supply their contents to the Manifest. |
| Tools and upgrades | Monitoring Crystal, Radiant Key, Scepters of Manipulation and Revelation, Devouring Charm, Debug Orb and administrative commands | The Scepters inspect devices, set Chest labels and open Brazier or storage settings. The Monitoring Crystal reports attached inventories. The Devouring Charm disposes of items and fluid, has saved pickup filters, and recharges Parchtear when it voids liquid. The Debug Orb reports device data and can fill or clear a Trove in Creative mode. The released diagnostic commands are available. |
| Arsenal | Gem Socket and its inventory, all 15 effects, charges, recharging powders, cooldowns, visuals and configurable availability | The Gem Cutter supplies all 15 gems and the socket when enabled. Both powders and natural recharge sources work. The socket has its own inventory and supports the optional Curios body slot. The original gem art, charge HUD, colour-blind icons, and illustrated Tome pages are available. |
| Guide and integrations | Tome of Arcana, guides, translations, advancements, config controls, JEI, scriptable Gem Cutter recipes, block inspection, crafting controls, equipment slot and supported cross-mod interactions | The native Tome reads the released illustrated pages and displays recipes from the active recipe manager. The 24 advancements and Brazilian Portuguese translations are restored. Curios supplies the optional Gem Socket equipment slot. Jade and The One Probe show device details. Rivertear can fill Botania's Petal Apothecary. |

The Arsenal baseline includes Slaughtergleam, Murdergleam, Agegleam, Cleansegleam, Switchgleam, Salvegleam, Munchstone, Orderstone, Mindspindle, Elixirspindle, Mountaintear, Rivertear, Parchtear, Phoenixway and Stormway. Both Chromatic Powders are part of that baseline. The release registers these items and the Gem Socket with the Arsenal disabled by default. The setting controls recipes, the Arsenal keybinds and the guidebook marker. Parchtear also uses the runtime-only `fake_air` block to hold back water temporarily. Check behavior with the feature enabled and disabled.

## Local verification

Run ./gradlew build with Java 17 to produce the reobfuscated jar in build/libs. Run ./gradlew runGameTestServer for the headless tests. The server configurations use disposable directories beneath run and automatically accept the server EULA. Keep client runs muted and use an isolated display.

Headless GameTests exercise chest hopper insertion, capability simulation, transfers and slot packets, hive membership and device ownership, Trove interactions and portable loot, Tank buckets, fluid automation and portable loot, Brazier routing, Amphora binding, replacement and dispensing, Resonator growth, harvesting and placement limits, double-chest conversion, and Sliver of Light yields. The Gem Cutter tests cover two-block placement and drops, drawer and recipe persistence, crafting from both inventories, overlapping ingredients, shift crafting and Brazier routing. Legacy extended-count deserialization has a test fixture.

The packaged build at 137553ed ran on a dedicated Forge server and a muted client on a virtual display. The opened Chest displayed 256 items initially and 177 after a live server update. The restarted server still reported 177. The later materials checkpoint passed all 18 headless GameTests and built from a clean source snapshot.

The packaged build at 0af9f886 was also tested on a dedicated server with a muted Xvfb client. A survival player placed both halves of the Gem Cutter, opened the menu from the accessor, paged through recipes and crafted Shaped Radiant Quartz from one raw quartz in each inventory. The selected recipe and crafted item in the drawer survived a server restart. This checkpoint changed the mod network protocol. Update the client and server together when upgrading from an earlier port build.

The Radiant Lantern checkpoint passed all 24 headless GameTests, including the released batch of four Lanterns and placement states for all six faces with full light and no collision. In a packaged client and server run, a survival player placed a wall Lantern and a Resonator. The Resonator paused when blocked at 75 growth ticks and resumed to 116 after the obstruction was removed. The headless suite verifies that the client loop's state follows owner activity and the space above the Resonator. The client was muted on Xvfb with OpenAL's No Output driver.

The Radiant Crafting Table checkpoint passed 27 headless GameTests. They cover a shared matrix with two viewers, stale output rejection, saving and clearing shortcuts, crafting cake from both inventories with all three buckets returned, and dropping the grid when the table breaks. Saved recipes use namespaced IDs in 1.20.1. Existing 1.12.2 worlds may have numeric recipe IDs, which require a verified mapping to migrate their saved shortcuts.

The packaged build at 9e399789 was tested on a dedicated server with a muted Xvfb client. A survival player placed the Radiant Crafting Table, crafted sticks through the grid and a saved shortcut, and saved and cleared the last recipe. The restarted server retained the saved shortcut and the six quartz left in the grid.

The monitoring checkpoint passed 29 headless GameTests. A placed Monitoring Crystal found a wooden chest, stopped reporting when the chest was replaced by a Radiant Chest, and read a barrel after being reoriented. Inspection tests check a named Radiant Chest's free slots and a loaded Radiant Tank's fluid and capacity. In the packaged build at 4741a8aa, a muted client attached a Crystal to a chest and the Scepter reported its direction and a Tank's 16,000 mB capacity. The same run opened the Radiant Crafting Table using the plain inventory theme.

The hive-letter checkpoint passed 31 headless GameTests and built from an exported clean source snapshot at 8bc4d09f. The tests cover signed crafting, joining, resigning, anvil naming against the offline member's stored name, and refusal to use another player's writ or resignation. The recipe packet now includes hive requirements and signature behavior. Use matching client and server jars for this checkpoint.

Two muted Forge clients verified that a signed invitation joined the recipient to the owner's hive. After restarting the same server, the member could no longer craft invitations and the failed attempt left the ingredients intact. The member crafted and used a signed resignation, and the owner received the disband notice. Both clients ran on isolated Xvfb displays with zero master volume and OpenAL's No Output driver.

The Manifest checkpoint passed 34 headless GameTests. The new tests check oversized counts, tagged variants, Tank millibuckets, personal and hive visibility, membership revocation, table inventories, double-chest deduplication and the Lectern's two-block placement and single drop. Manifest snapshots reflect loaded inventories without forcing chunks to load. Existing alpha clients and servers need matching jars for the new Manifest packets.

The Brazier settings checkpoint passed 35 headless GameTests. The settings test covers the radius limit and ten-block steps, personal scope, permission checks while the menu is open and save and reload. The Scepter recipe uses a Scepter of Revelation and Shaped Radiant Quartz in the Gem Cutter.

In the packaged Manifest and Brazier runs, a muted Xvfb client found 321 cobblestone, watched the count drop to 276 with the screen open and displayed 9,000 mB of water. A server restart retained the inventory changes. The client opened the Manifest from a keybind and from the upper half of a survival-placed Lectern. The Scepter expanded the Brazier from 150 to 160 blocks, set it to 12 blocks, and selected personal routing. The dedicated server retained both settings after restart.

The capacity upgrade checkpoint passed all 37 headless GameTests. Matrix Brace, the storage-specific component and a Shaped Radiant Quartz block raise capacity to three, six and ten times base. The tests cover tier order, shift-clicks, exact downgrade limits, preserving component tags and contents across save and reload, and using the upgraded portable item capabilities. The stock Trove and Tank keep the released base capacities. Use matching jars for the new upgrade menu.

The exported `bca81e8c` source built successfully. In a packaged run, the Manifest drew a ray to a selected chest and Shift plus the Manifest key cleared it. The Brazier drew the orange boundary at its configured radius. The muted client also installed the three Trove capacity components and the dedicated server rejected a third-tier component offered before the lower tiers.

The Chest controls checkpoint passed all 39 headless GameTests. They cover naming and clearing, routing permission changes with an open menu, invalidation when the chest is replaced, and Scepter labels surviving save and reload. Chest renderer update packets carry only the settings. The inventory retains the separate oversized-stack synchronization. Use matching client and server jars for the new naming packet.

The exported `300f741f` source built successfully. A muted packaged client named a Chest Quartz Vault, rendered and cleared a diamond label and restricted routing to existing items. The dedicated server retained the new name, routing and restored diamond label after restarting. The same run installed the Tank's three capacity components and checked that the upgraded Trove and Tank blocked downgrades with 229,376 items and 120,000 mB stored. Both contents and upgrade tiers survived the restart.

The Devouring Charm and optional upgrades checkpoint passed all 43 headless GameTests. These add the Charm's disposal slots, fluid draining, exact pickup filters and pickup ownership checks. The tests also exercise a locked empty Trove, its portable capability, matching overflow voiding, and removing an upgrade without losing stored items or fluid. Use matching client and server jars for the new Charm and upgrade menus.

The exported `8db54a0a` source built successfully. In the packaged run, the muted client drained a water bucket, recovered the empty bucket, discarded gravel and saved a cobblestone filter without consuming the sample. A matching ground pickup vanished without entering the inventory. The client installed the lock and void upgrades on the Trove and the void upgrade on the Tank. A full Tank accepted and voided a matching water bucket. The optional upgrades and stored contents survived a server restart.

The portable Tank renderer passed the 43 headless tests and ran in the packaged muted client. Empty, 8,000 of 16,000 mB, and 40,000 of 48,000 mB Tank items showed distinct fluid levels in the original model.

The Tome and advancements checkpoint passed all 45 headless GameTests. The server loaded all 24 released advancements. Tests verify the book and gold nugget recipe, the original book resource ID, and one-time acquisition across bookshelf breaks, Resonator crafting and direct Tome crafting. The native reader uses the original text, art and internal links with recipes from the active datapacks.

The exported `30f10f18` source built successfully. In the packaged run, a survival player broke two bookshelves, received one Tome and earned its advancement. The muted client opened the illustrated home page, followed links through Blocks to the Radiant Resonator, rendered the live crafting recipe, and moved to the lore page and back.

The diagnostics checkpoint passed all 48 headless GameTests. `/brazier` traces the same priority order the Brazier uses and simulates transfers without changing contents. `/tiles` filters and marks network devices. `/rebuild_network` refreshes loaded registrations while preserving unloaded devices, and `/hive` reports membership. `/clipboard` and the Debug Orb use Minecraft's click-to-copy chat support. The original Orb's external paste service has been replaced with an in-game copyable report. Use matching client and server jars for diagnostic markers.

The Arsenal checkpoint passed all 54 headless GameTests. The new fixtures cover config-gated recipes and effects, socket transfers and persistence, matching powder, looting with the final charge, critical hits, passive healing, experience and potions, block transformation, and Parchtear's temporary water barrier. Use matching client and server jars for the socket menu and controls.

The exported `816005cb` source built successfully. A muted client installed a Salvegleam in a handheld socket and recharged it from 2 to 30 with pink powder. It displayed the charge HUD, toggled Murdergleam, transformed gravel with Orderstone and lit a fire with Phoenixway. A follow-up build added a short Switchgleam cooldown to prevent a second activation from undoing the first swap. The packaged run confirmed one swap for one charge, opened a Curios-equipped socket by hotkey and healed four points by spending four of its installed gem's charges.

The crafting integrations checkpoint passed all 54 headless GameTests. A muted packaged client displayed both JEI recipe categories, transferred a Brush recipe to the Radiant Crafting Table, and returned the ingredients with Crafting Tweaks. The Manifest copied the JEI filter, mirrored a new search and restored the previous filter when closed. CraftTweaker loaded the Gem Cutter's add, replace and remove operations on dedicated server startup and resource reload.

A packaged client with Jade 11.13.2 and The One Probe 10.0.5 displayed a custom Chest name, a Trove holding 12,500 Raw Iron, and a Resonator at 57% growth with its owner offline. With Botania 1.20.1-456 installed, a Rivertear in the hotbar filled an empty Petal Apothecary with water while retaining all ten charges. The same muted client tracked a Brush by hovering it in JEI and pressing the Manifest key. The block highlight and tracking line appeared over its Chest.

Quark 4.0-462 supplies the Radiant Chest's filter, bulk transfers, and sorting. Quark sorts the regular slots around oversized stacks, which retain their position and count.

The dedicated server kept a 256-item Chest stack and its owner, 12,500 Raw Iron in a Trove, 9,000 mB in a Tank, and a Brazier's 34-block personal routing setting through an actual chunk unload, reload, and server restart.

The 0.2.1 build passed all 54 headless GameTests. The production jar loaded on a Forge 47.4.28 dedicated server and a muted Xvfb client with Arcane Archives as the only installed mod. A survival player placed a Chest, saw 256 items displayed in one slot, and shift-transferred them as four stacks of 64. The Tome home page rendered, and the Manifest located a new 177-item stack in the Chest.

The legacy Astral Sorcery liquefaction recipes, Thaumcraft aspects and Crucible interaction, and Potion Core buffs depend on those mods' older APIs. Their official release lists omit Forge 1.20.1. [Astral Sorcery](https://www.curseforge.com/minecraft/mc-mods/astral-sorcery/files/all), [Thaumcraft](https://www.curseforge.com/minecraft/mc-mods/thaumcraft/files/all), [Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core/files/all)

### Gem Cutter scripts

With CraftTweaker 14.0.61 on Forge 1.20.1, the `mods.arcanearchives.GCT` commands accept current ZenScript item and tag brackets. Multiply an ingredient by its required count. Replacement keeps the recipe's sort order and gameplay conditions.

```zenscript
import mods.arcanearchives.GCT;

GCT.removeRecipe(<item:arcanearchives:radiant_dust>);
GCT.addRecipe("radiant_dust", <item:arcanearchives:radiant_dust> * 2,
    [<item:minecraft:flint>, <item:arcanearchives:raw_quartz>]);
GCT.replaceRecipe("shaped_quartz", <item:arcanearchives:shaped_quartz>,
    [<item:arcanearchives:raw_quartz> * 10]);
```

## Save compatibility

The modern port preserves the shipped registry IDs where possible. The native loaders accept legacy device UUID strings, extended Chest stack counts, and several legacy storage tags when those data reach a matching 1.20.1 block entity. Fresh 1.20.1 worlds and saves created by the port are supported.

A direct Forge upgrade of a populated 1.12.2 world lost the sampled Arcane Archives devices. The test created a world under Forge 14.23.5.2860 with the published Arcane Archives 0.2.0.25-mixins8 jar. It verified a named Chest with a 256-item stack and legacy spruce logs, a Tank with 9,000 mB of water, a Trove, a Resonator, and a Shaped Radiant Quartz block before copying the world. Forge 47.4.28 `--forceUpgrade` kept the adjacent vanilla stone. The Chest, Resonator, and quartz block positions became air. The Chest, Tank, and Trove positions had no block entities after upgrade. [Published Arcane Archives 1.12.2 release](https://www.curseforge.com/minecraft/mc-mods/arcane-archives/files/3057332)
