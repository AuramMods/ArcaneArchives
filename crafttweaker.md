# Arcane Archives CraftTweaker guide

Use Minecraft 1.20.1, Forge 47.x, Java 17, and CraftTweaker for Forge. The Auram compatibility test stack uses Forge 47.4.10 and CraftTweaker 14.0.59. Arcane Archives also runs with its shipped rules on servers without CraftTweaker.

Install GameStages 15.0.2 and Bookshelf 20.2.15 to use stage requirements. RecipeStages 8.0.0.2 supplies a separate integration for standard crafting recipes. JEI 15.20.0.129 displays Gem Cutter and Resonator recipes.

## Loading and precedence

Put Arcane Archives scripts in the instance's `scripts` directory. The methods in this guide run in CraftTweaker's default loader. Use CraftTweaker's tags loader for changes to native Minecraft registry tags. Use a server `/reload` after editing, adding, or deleting scripts.

Rule IDs are namespaced. A short ID uses the `arcanearchives` namespace. Use your pack namespace for new rules. Each rule type supports adding an unused ID, replacing an existing ID, and removing an existing ID. Arcane Archives loads rules and custom tags from the active recipe manager, so deleting a script and reloading restores the recipe data from your datapacks and remaining scripts.

Processing rules with an order run in ascending order. The namespaced ID breaks ties. When a matching rule is gated, the gate denies that rule's operation. Ingredient selectors accept native CraftTweaker item ingredients, including tags and supported NBT predicates. Block selectors accept CraftTweaker block ingredients, such as `<block:minecraft:stone>`, exact states, and block tags. Cast and growth outputs use an exact block state, such as `<block:minecraft:oak_log>.defaultState.withProperty("axis", "z")`.

The server's `serverconfig/arcanearchives-server.toml` still controls the Arsenal master switch, Resonator placement limit, and Raw Quartz chest conversion switch. Shipped rules that refer to a server setting use its current value. Replacing one of those rules with explicit values gives that rule its own settings.

## Gem Cutter’s Table

Import `mods.arcanearchives.GCT` and, for advanced properties, `mods.arcanearchives.GemCutterHive`.

| Method | Arguments |
| --- | --- |
| `addRecipe` | `id as string, output as IItemStack, inputs as IIngredientWithAmount[]` |
| `replaceRecipe` | `id as string, output as IItemStack, inputs as IIngredientWithAmount[]` |
| Extended `addRecipe` and `replaceRecipe` | The same arguments followed by `order as int, hive as GemCutterHive, signed as bool, arsenal as bool` |
| `hasRecipe` and `getRecipe` | `id as string` |
| `removeRecipe` | `id as string` or `output as IItemStack` |

The legacy add overload uses the last order, `ANY` hive access, unsigned output and no Arsenal requirement. The legacy replace overload keeps the existing properties. Removing by output removes the first recipe with the same item and NBT. Use the ID overload to remove a specific recipe. `getRecipe` exposes `id`, `output`, `inputs`, `order`, `hive`, `signed`, and `requiresArsenal`.

Hive requirements are `ANY`, `CAN_INVITE`, `MEMBER`, and `OWNER`. Signed output includes the crafter's UUID and name. The table draws from its drawer and the player's inventory and returns crafting remainders. Flints and steel used as ingredients take durability damage.

```zenscript
import mods.arcanearchives.GCT;
import mods.arcanearchives.GemCutterHive;

GCT.addRecipe("auram:cut_emeralds", <item:minecraft:emerald> * 2,
    [<item:minecraft:flint>, <item:minecraft:stick> * 3],
    -10, GemCutterHive.ANY, true, false);
val added = GCT.getRecipe("auram:cut_emeralds");
println(added.id + " requires " + added.inputs.length + " distinct ingredient entries");

GCT.replaceRecipe("shaped_quartz", <item:arcanearchives:shaped_quartz>,
    [<item:arcanearchives:raw_quartz> * 10]);
GCT.removeRecipe("radiant_dust");
```

The shipped IDs are filenames under `data/arcanearchives/recipes` whose type is `arcanearchives:gem_cutting`. The examples above use `arcanearchives:shaped_quartz` and `arcanearchives:radiant_dust`.

## Radiant Crafting Table

Use CraftTweaker's standard crafting manager to customize this table. It crafts the same recipes as a standard grid and resolves saved shortcuts by recipe ID. The acting player must meet the recipes' Arcane Archives gates and native RecipeStages requirements when taking results or using a saved shortcut.

```zenscript
import mods.recipestages.Recipes;

Recipes.setRecipeStage("arcane_sticks", <resource:minecraft:stick>);
```

## Radiant Resonator

Import `mods.arcanearchives.Resonator`. Both `add` and `replace` use this argument list.

```text
id as string, output as BlockState, ticks as int, order as int, onlineOwner as bool,
foundation as CTBlockIngredient?, dimension as ResourceLocation?, minimumLight as int
```

`has(id)` reports whether a rule exists. `remove(id)` removes it. A null foundation matches any block below the Resonator. A null dimension allows any dimension. The minimum light is checked at the output location and ranges from 0 through 15. Zero ticks uses the configured duration.

The built-in `arcanearchives:resonator/raw_quartz_cluster` rule has order 0 and requires its owner online. Every rule needs space above the machine and a state that can survive there. The server's placement limit applies to the machine. Growth tracks the selected rule, and JEI shows the rule's duration and output. Stage rules use the online device owner.

```zenscript
import mods.arcanearchives.Resonator;

Resonator.add("auram:resonant_oak", <block:minecraft:oak_log>.defaultState.withProperty("axis", "z"),
    20, -10, false, <block:minecraft:diamond_block>, <resource:minecraft:overworld>, 0);
```

## Quartz interactions

Import `mods.arcanearchives.Quartz` and `mods.arcanearchives.QuartzDrop`.

| Methods | Arguments |
| --- | --- |
| `addChestConversion`, `replaceChestConversion` | `id as string, input as IIngredientWithAmount, target as CTBlockIngredient?, output as BlockState, order as int` |
| `addBashing`, `replaceBashing` | `id as string, input as IIngredientWithAmount, target as CTBlockIngredient?, primary as QuartzDrop, secondary as QuartzDrop?, order as int` |
| `hasChestConversion`, `removeChestConversion`, `hasBashing`, `removeBashing` | `id as string` |
| `new QuartzDrop` | `minimum as IItemStack, maximum as int, chance as int, consume as bool` |

The default `arcanearchives:quartz/chest_conversion` uses one Raw Quartz when sneaking and using a chest. Both halves of a double chest are checked before moving the name and contents. Radiant Chest outputs belong to the acting player. Vanilla-style chest outputs must be single chests and use ordinary vanilla ownership behavior. Contents that exceed the destination capacity drop at the new chest. Creative conversion is free.

The default `arcanearchives:quartz/bashing` uses the server's Sliver of Light chances and yield range. Custom bashing rules can give two exclusive outcomes whose chances total at most 100. The amount in the output stack is the minimum drop count. A consuming outcome spends the counted input, including in creative mode.

```zenscript
import mods.arcanearchives.Quartz;
import mods.arcanearchives.QuartzDrop;

Quartz.addChestConversion("auram:trapped_chest", <item:minecraft:emerald> * 2,
    <block:minecraft:trapped_chest>, <block:arcanearchives:radiant_chest>.defaultState, -10);
Quartz.replaceBashing("quartz/bashing", <item:minecraft:lapis_lazuli> * 2, <block:minecraft:stone>,
    new QuartzDrop(<item:minecraft:amethyst_shard> * 3, 3, 100, true), null, 0);
```

## Custom Arsenal tags

Import `mods.arcanearchives.Tags` and the types you use. `Gem` contains the Arsenal gem types, `GemCut` contains `ASSCHER`, `OVAL`, `PAMPEL`, `PENDELOQUE`, and `TRILLION`, and `ChromaticColor` contains `NONE`, `RED`, `ORANGE`, `YELLOW`, `GREEN`, `CYAN`, `BLUE`, `PURPLE`, `PINK`, `BLACK`, and `WHITE`.

`define(id, members)`, `add(id, members)`, and `remove(id, members)` take a typed array of any of these three types. To define an empty tag, cast the empty array to its type, such as `[] as Gem[]`. `getGems(id)`, `getCuts(id)`, and `getColors(id)` return the appropriate array. `clearGems(id)`, `clearCuts(id)`, and `clearColors(id)` keep the tag ID and empty its membership.

Use `new GemSelector(gems as Gem[])` to select specific gems. Use `new GemSelector(gems as Gem[], gemTag as string?, cutTag as string?, colorTag as string?)` to add tag conditions. All supplied conditions must match. An empty gem array accepts any gem. An undefined or empty required tag matches nothing. Rule execution reads the final tag membership after scripts have loaded. For inspection or mutation within scripts, define the tag first.

## Arsenal recharge

Set `enableArsenal = true` on the server before using Arsenal gameplay. Import `mods.arcanearchives.Arsenal`.

### Inventory recharge

`addRecharge(id, rule)` and `replaceRecharge(id, rule)` take an `InventoryRecharge`. `hasRecharge(id)` and `removeRecharge(id)` take a rule ID.

```text
new InventoryRecharge(gems as GemSelector, input as IIngredientWithAmount, maximum as int,
    chargePerItem as int, emptyOnly as bool, order as int)
new InventoryRecharge(gems as GemSelector, input as IIngredientWithAmount, maximum as int,
    chargePerItem as int, emptyOnly as bool, order as int, remainder as IItemStack?,
    matchGemColor as bool, powderColorTag as string?, preferLastSlot as bool)
```

The input count is the minimum in one slot. A rule consumes up to its maximum from that slot and returns the specified remainder per consumed item. A charge of -1 fills the gem. `matchGemColor` and `powderColorTag` apply to chromatic powder colors. The built-in inventory rules are under `arcanearchives:arsenal/recharge/` and use these suffixes.

| ID suffix | Input |
| --- | --- |
| `slaughtergleam` | Gold nuggets |
| `munchstone` | Bone meal |
| `cleansegleam` | Milk bucket with empty bucket remainder |
| `mindspindle` | Book |
| `elixirspindle` | Nether wart |
| `phoenixway` | Gunpowder |
| `chromatic_powder` | Same-color Chromatic Powder |
| `full_spectrum_powder` | Full Spectrum Chromatic Powder |

### Environmental and event recharge

`addNaturalRecharge` and `replaceNaturalRecharge` take `id as string, gems as GemSelector, trigger as RechargeTrigger, charge as int, scale as double, ticks as int, toggleWhenFull as bool, order as int`. Use `hasNaturalRecharge(id)` and `removeNaturalRecharge(id)` to inspect or remove a rule.

A charge of -1 fills the gem. For experience pickup, the scale multiplies the orb value. For an animal kill, it multiplies the animal's maximum health. Fractional charge rounds down. Timer rules take a positive interval in ticks. Other triggers take zero ticks. Held triggers also support sockets. Dropped triggers use a thrown item's player when available.

The defaults are under `arcanearchives:arsenal/natural_recharge/`. Their ID suffixes use the gem ID followed by the lowercase trigger joined with an underscore.

| Trigger | Shipped gem |
| --- | --- |
| `DROPPED_IN_LAVA` | Mountaintear |
| `DROPPED_IN_WATER` | Rivertear |
| `DROPPED_IN_RAIN`, `HELD_IN_RAIN` | Stormway |
| `ANIMAL_KILL` | Salvegleam |
| `NATURAL_CRITICAL_HIT` | Murdergleam |
| `TIMER` | Agegleam |
| `EXPERIENCE_PICKUP` | Orderstone |
| `ENDERMAN_TELEPORT` | Switchgleam |

```zenscript
import mods.arcanearchives.Arsenal;
import mods.arcanearchives.ChromaticColor;
import mods.arcanearchives.Gem;
import mods.arcanearchives.GemCut;
import mods.arcanearchives.GemSelector;
import mods.arcanearchives.InventoryRecharge;
import mods.arcanearchives.RechargeTrigger;
import mods.arcanearchives.Tags;

Tags.define("auram:wet", [Gem.PHOENIXWAY]);
Tags.add("auram:wet", [Gem.ORDERSTONE]);
Tags.remove("auram:wet", [Gem.PHOENIXWAY]);
Tags.define("auram:wet_cut", [GemCut.OVAL]);
Tags.define("auram:wet_color", [ChromaticColor.PINK]);
Arsenal.addNaturalRecharge("auram:wet_orderstone",
    new GemSelector([], "auram:wet", "auram:wet_cut", "auram:wet_color"),
    RechargeTrigger.DROPPED_IN_WATER, 11, 0.0, 0, false, -10);
Arsenal.addRecharge("auram:quartz_orderstone",
    new InventoryRecharge(new GemSelector([Gem.ORDERSTONE]), <item:minecraft:quartz> * 2,
        3, 11, false, -10, <item:minecraft:stick>, false, null, false));
```

With this script, a damaged Orderstone dropped in water recharges. Remove Orderstone from `auram:wet` and reload to stop that behavior.

## Arsenal block transforms and food

`addTransform(id as string, transform as BlockTransform)` and `replaceTransform(id, transform)` accept a `BlockTransform`. Use `hasTransform(id)` and `removeTransform(id)` to check or remove a rule.

```text
new BlockTransform(gem as Gem, blocks as CTBlockIngredient, output as BlockState, charge as int, order as int)
new BlockTransform(gem as Gem, target as TransformTarget, blocks as CTBlockIngredient?,
    fluids as CTFluidIngredient?, output as BlockState, charge as int, order as int,
    preserveFacing as bool, creativeFree as bool, coolDimensionsOnly as bool, dynamicFire as bool)
```

Transforms support `ORDERSTONE`, `RIVERTEAR`, `MOUNTAINTEAR`, `PARCHTEAR`, and `PHOENIXWAY`. Orderstone changes the clicked block. Rivertear, Mountaintear, and Phoenixway change the adjacent block on the hit face. Parchtear changes each matching block in a 3×3×3 area, or only the center when sneaking. Each successful change spends the rule's charge. Block entities are protected from transformation.

`TransformTarget` values are `ANY`, `REPLACEABLE`, `AIR`, and `LIQUID`. Any supplied block and fluid filters must also match. A fluid selector matches placed fluid types or native fluid tags. Placed fluids have no stack NBT or millibucket count. `Arsenal.fluidState(fluid as IFluidStack)` resolves a fluid to its placed block state. Rules can copy horizontal facing, waive charge in creative, exclude ultrawarm dimensions, or compute a fire state's shape from its surroundings.

`addFood` and `replaceFood` take `id as string, block as CTBlockIngredient, food as int, saturation as float, fixedCost as int, order as int`. Use `hasFood(id)` and `removeFood(id)` to check or remove a rule. Munchstone consumes a matching block if it grants food or saturation. A fixed cost of zero spends charge equal to the food gained plus the rounded-up saturation gained. The shipped `arcanearchives:arsenal/munchstone/server_config` rule uses the server's `munchstoneFoods` list.

```zenscript
import mods.arcanearchives.Arsenal;
import mods.arcanearchives.BlockTransform;
import mods.arcanearchives.Gem;
import mods.arcanearchives.TransformTarget;

Arsenal.replaceTransform("arsenal/orderstone/sand", new BlockTransform(Gem.ORDERSTONE,
    <block:minecraft:sand>, <block:minecraft:glass>.defaultState, 3, -10));
Arsenal.addTransform("auram:parch_water", new BlockTransform(Gem.PARCHTEAR, TransformTarget.LIQUID,
    null, <fluid:minecraft:water>, <block:minecraft:ice>.defaultState, 2, -10, false, false, false, false));
Arsenal.addFood("auram:eat_wool", <tag:blocks:minecraft:wool>, 4, 3.0f, 2, -10);
```

Orderstone default rule IDs use `arcanearchives:arsenal/orderstone/` followed by the source block's path. The source paths are `sand`, `coarse_dirt`, `dirt`, `mycelium`, `podzol`, `gravel`, `mossy_cobblestone`, `cobblestone`, `mossy_stone_bricks`, `cracked_stone_bricks`, `stone_bricks`, `damaged_anvil`, and `chipped_anvil`. The four other defaults use `arcanearchives:arsenal/transform/` followed by `rivertear`, `mountaintear`, `parchtear`, or `phoenixway`.

Botania's Petal Apothecary can still fill from an available Rivertear without spending charge. Its dedicated feature gate is `arsenal/rivertear/botania_apothecary`.

## Arsenal effect settings

Import `mods.arcanearchives.ArsenalEffect`. Use `Arsenal.setEffect(effect as ArsenalEffect, charge as int, ticks as int, amount as double)` to set or replace an override. `Arsenal.resetEffect(effect)` removes the override and restores the shipped values.

| Effect | Charge | Ticks | Amount |
| --- | --- | --- | --- |
| `AGEGLEAM` | Per affected entity, 1 | Age adjustment, 8000 | Radius, 3.5 |
| `CLEANSEGLEAM` | Per affected entity, 1 | 0 | Sneaking radius, 3.5 |
| `SWITCHGLEAM` | Per swap, 1 | Item cooldown, 10 | Target range, 40 |
| `SALVEGLEAM` | Per heal, 1 | Interval, 20 | Health per pulse, 1 |
| `PHOENIXWAY_FIRE_RESISTANCE` | Per trigger, 12 | Effect duration, 600 | 0 |

The `arsenal/cast` and per-gem cast gates also cover looting, critical hits, healing, XP amplification, potion preservation, Enderman teleport suppression, lightning and other triggered effects. A gem's natural recharge uses the separate recharge gates.

```zenscript
import mods.arcanearchives.Arsenal;
import mods.arcanearchives.ArsenalEffect;

Arsenal.setEffect(ArsenalEffect.AGEGLEAM, 2, 4000, 4.5);
```

## GameStages

Import `mods.arcanearchives.Stages` and `mods.arcanearchives.StageScope`. Each target has one rule per scope.

| Method | Arguments |
| --- | --- |
| `add` | `scope as StageScope, target as string, stage as string` |
| `add`, `replace` | `scope as StageScope, target as string, all as string[], any as string[]` |
| `has`, `remove` | `scope as StageScope, target as string` |
| `addFeatureGroup` | `features as string[], all as string[], any as string[]` |
| `features()`, `families()` | No arguments. Return namespaced IDs as `string[]` |

`FEATURE` targets an interaction or ability. `FAMILY` targets all rules of that processing family. `RECIPE` targets an individual contributing recipe or processing rule ID. A player must have every stage in `all` and at least one stage in a nonempty `any`. Applicable feature, family, and recipe gates all apply. Standard crafting at the Radiant Crafting Table also honors RecipeStages.

```zenscript
import mods.arcanearchives.StageScope;
import mods.arcanearchives.Stages;

Stages.add(StageScope.RECIPE, "arcanearchives:shaped_quartz", "arcane_basics");
Stages.add(StageScope.FEATURE, "manifest/track", "arcane_survey");
Stages.add(StageScope.FAMILY, "radiant_crafting", ["arcane_basics"], ["arcane_miner", "arcane_builder"]);
Stages.addFeatureGroup(["brazier/route", "amphora/transfer"], ["arcane_automation"], []);
```

Grant or revoke stages with GameStages during play, for example `/gamestage add PlayerName arcane_basics` and `/gamestage remove PlayerName arcane_basics`. The next use, transfer, cast, or craft sees the change. When GameStages is absent, configured gates deny access and ungated paths keep their normal behavior.

Player actions use the acting player and still check the original ownership, hive, and interaction permissions. Configured automation gates use the online owner of the Resonator, Brazier, Monitoring Crystal, or the tank linked to an Amphora. A dropped gem uses its online thrower in the same dimension. A configured gate denies an ownerless device, offline owner, unowned dropped gem, or Forge fake player. Without a configured gate, the usual requirements for that mechanic still apply.

### Processing family IDs

These IDs use the `arcanearchives` namespace. `gem_cutting`, `radiant_crafting`, `resonator_growth`, `chest_conversion`, `quartz_bashing`, `inventory_recharge`, `natural_recharge`, `block_transform`, and `munchstone_food`.

### Feature IDs

All feature IDs below use the `arcanearchives` namespace.

| Feature | Applies to |
| --- | --- |
| `place/<device>` | Placement of `radiant_chest`, `radiant_trove`, `radiant_tank`, `brazier_of_hoarding`, `gemcutters_table`, `radiant_crafting_table`, `radiant_resonator`, `manifest_lectern`, and `monitoring_crystal`. The Radiant Chest gate also applies to chest conversion. |
| `gem_cutter/use`, `radiant_crafting/use` | Table access and crafting |
| `storage/use` | Direct chest access, Trove deposit and withdrawal, handheld transfer at a Tank |
| `storage/upgrades` | Opening and changing the Tank and Trove upgrade menus |
| `brazier/route`, `brazier/configure` | Item and fluid routing, routing settings |
| `amphora/link`, `amphora/transfer`, `amphora/mode` | Linking, player and automated transfers, switching modes |
| `monitoring/read`, `monitoring/inspect` | Crystal inventory readings, Revelation Scepter and network inspection |
| `manifest/access`, `manifest/track` | Opening by item, lectern or hotkey, tracking targets |
| `hive/join`, `hive/leave`, `hive/expel`, `hive/status` | Using hive letters and reading hive status |
| `arsenal/cast`, `arsenal/recharge` | Arsenal-wide ability and recharge gates |
| `arsenal/<gem>/cast`, `arsenal/<gem>/recharge` | Gates for a single gem using the lowercase ID from `Gem` |
| `arsenal/socket/use` | Opening and changing a gem socket |
| `arsenal/rivertear/botania_apothecary` | Filling Botania's Petal Apothecary |
| `devouring_charm/configure`, `devouring_charm/consume` | Editing item filters and voiding matching pickups |
| `tome/read`, `tome/bookshelf`, `tome/resonator` | Reading the Tome and receiving the one-time grants from breaking a bookshelf or crafting a Resonator |

## Coverage map

| Gameplay | Customization |
| --- | --- |
| Gem cutting and signing | `GCT`, recipe gates and hive conditions |
| Normal crafting, quartz compression and unpacking, vanilla-grid versions of Arcane devices | CraftTweaker's vanilla recipe managers and optional RecipeStages |
| Radiant Crafting Table grid and shortcuts | The vanilla crafting manager, RecipeStages and Arcane recipe or feature gates |
| Quartz Cluster growth | `Resonator`, growth status, comparator and JEI use the active rule |
| Quartz harvesting and ordinary block drops | Minecraft loot tables and standard loot modifiers |
| Chest conversion and Sliver of Light bashing | `Quartz` selectors, outputs and bashing probabilities |
| Inventory, environmental and event gem recharge | `Arsenal` recharge rules and custom tags |
| Orderstone, Munchstone, Rivertear, Mountaintear, Parchtear and Phoenixway | `Arsenal` block and fluid selectors, states, costs and food rules |
| Other Arsenal effects and Botania | Typed settings for supported effects and feature gates |
| Item and fluid routing | Forge capabilities with dedicated Brazier and Amphora gates |
| Tome of Arcana grants and Devouring Charm voiding | Feature gates, server settings for Tome grants, and the charm's stack filters |
