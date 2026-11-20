package com.aranaira.arcanearchives.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class ArchiveConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.IntValue RADIANT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue TROVES_DISPENSE_STACK;
    public static final ForgeConfigSpec.BooleanValue CHESTS_EXISTING_ITEMS_ONLY;
    public static final ForgeConfigSpec.BooleanValue BRAZIER_SOUND;
    public static final ForgeConfigSpec.IntValue RESONATOR_LIMIT;
    public static final ForgeConfigSpec.IntValue RESONATOR_TICKS;
    public static final ForgeConfigSpec.BooleanValue RESONATOR_COMPLETION_SOUND;
    public static final ForgeConfigSpec.BooleanValue CHEST_CONVERSION;
    public static final ForgeConfigSpec.BooleanValue TOME_FROM_BOOKSHELF;
    public static final ForgeConfigSpec.BooleanValue TOME_FROM_RESONATOR;
    public static final ForgeConfigSpec.BooleanValue ARSENAL_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ARSENAL_COLOURBLIND;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MUNCHSTONE_FOODS;
    public static final ForgeConfigSpec.IntValue SLIVER_SINGLE_CHANCE;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_CHANCE;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_MIN;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_MAX;
    public static final ForgeConfigSpec.BooleanValue RESONATOR_LOOP_ENABLED;
    public static final ForgeConfigSpec.DoubleValue RESONATOR_LOOP_VOLUME;
    public static final ForgeConfigSpec.BooleanValue PRETTY_GUIS;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_MANIFEST;
    public static final ForgeConfigSpec.BooleanValue MANIFEST_GRID;
    public static final ForgeConfigSpec.BooleanValue MANIFEST_KEEP_SEARCH;
    public static final ForgeConfigSpec.BooleanValue MANIFEST_JEI_SYNC;
    public static final ForgeConfigSpec.BooleanValue MANIFEST_SHIFT_KEEPS_OPEN;
    public static final ForgeConfigSpec.IntValue MANIFEST_RANGE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        RADIANT_MULTIPLIER = builder.comment("Capacity of a Radiant Chest slot in multiples of 64 items.")
                .defineInRange("radiantMultiplier", 4, 1, 8);
        TROVES_DISPENSE_STACK = builder.comment("Dispense an entire stack while crouching instead of while standing.")
                .define("trovesDispenseStack", true);
        CHESTS_EXISTING_ITEMS_ONLY = builder.comment("New Radiant Chests only accept existing item types from a Brazier.")
                .define("chestsExistingItemsOnly", false);
        BRAZIER_SOUND = builder.comment("Play the Brazier absorption sound for successful transfers.")
                .define("brazierSound", true);
        RESONATOR_LIMIT = builder.comment("Maximum number of placed Radiant Resonators per personal network.")
                .defineInRange("resonatorLimit", 3, 0, Integer.MAX_VALUE);
        RESONATOR_TICKS = builder.comment("Ticks to grow one Raw Quartz Cluster with an online owner.")
                .defineInRange("resonatorTicks", 6000, 1, Integer.MAX_VALUE);
        RESONATOR_COMPLETION_SOUND = builder.comment("Play a completion sound when a new quartz cluster grows.")
                .define("resonatorCompletionSound", true);
        CHEST_CONVERSION = builder.comment("Allow crouch-use with Raw Radiant Quartz to convert a wooden chest.")
                .define("inWorldChestConversion", true);
        TOME_FROM_BOOKSHELF = builder.comment("Drop a Tome of Arcana the first time a player breaks a bookshelf.")
                .define("tomeFromBookshelf", true);
        TOME_FROM_RESONATOR = builder.comment("Drop a Tome of Arcana when a player crafts their first Radiant Resonator.")
                .define("tomeFromResonator", true);
        ARSENAL_ENABLED = builder.comment("Enable Arcane Gem gameplay, recipes, and the Arsenal chapter in the Tome.")
                .define("enableArsenal", false);
        MUNCHSTONE_FOODS = builder.comment("Edible block IDs or #tags followed by a comma and a hunger value.")
                .defineListAllowEmpty("munchstoneFoods", List.of(
                        "#minecraft:logs, 4", "#minecraft:leaves, 2", "minecraft:hay_block, 15", "minecraft:melon, 15",
                        "minecraft:brown_mushroom_block, 8", "minecraft:brown_mushroom, 4", "minecraft:red_mushroom_block, 8",
                        "minecraft:red_mushroom, 4", "minecraft:nether_wart_block, 15", "minecraft:nether_wart, 4",
                        "minecraft:chorus_flower, 6", "minecraft:chorus_plant, 6", "minecraft:cactus, 6", "minecraft:cocoa, 4",
                        "minecraft:dead_bush, 2", "#minecraft:tall_flowers, 2", "#minecraft:small_flowers, 2", "minecraft:pumpkin, 15",
                        "minecraft:jack_o_lantern, 16", "minecraft:pumpkin_stem, 4", "minecraft:melon_stem, 4", "minecraft:sugar_cane, 4",
                        "#minecraft:saplings, 2", "minecraft:grass, 1", "minecraft:tall_grass, 2", "minecraft:vine, 2",
                        "minecraft:lily_pad, 4", "minecraft:wheat, 4", "minecraft:potatoes, 4", "minecraft:carrots, 4", "minecraft:beetroots, 4"),
                        ArchiveConfig::validFood);
        SLIVER_SINGLE_CHANCE = builder.defineInRange("sliverSingleChance", 40, 0, 100);
        SLIVER_CLUSTER_CHANCE = builder.defineInRange("sliverClusterChance", 20, 0, 100);
        SLIVER_CLUSTER_MIN = builder.defineInRange("sliverClusterMinimum", 8, 1, 4096);
        SLIVER_CLUSTER_MAX = builder.defineInRange("sliverClusterMaximum", 24, 1, 4096);
        REQUIRE_MANIFEST = builder.comment("Require a Manifest in the inventory to use its hotkey.")
                .define("requireManifestForHotkey", true);
        SERVER_SPEC = builder.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        RESONATOR_LOOP_ENABLED = client.comment("Play the ongoing sound while a Radiant Resonator grows quartz.")
                .define("resonatorTicking", true);
        RESONATOR_LOOP_VOLUME = client.comment("Volume of the Radiant Resonator's loop.")
                .defineInRange("resonatorVolume", 0.15, 0.0, 1.0);
        PRETTY_GUIS = client.comment("Use the decorated Arcane Archives inventory backgrounds.")
                .define("usePrettyGUIs", true);
        MANIFEST_GRID = client.comment("Draw inventory slots behind the Manifest search results.")
                .define("manifestGrid", false);
        MANIFEST_KEEP_SEARCH = client.comment("Restore the previous search when the Manifest is reopened.")
                .define("manifestSearchPersistence", false);
        MANIFEST_JEI_SYNC = client.comment("Mirror Manifest searches in Just Enough Items while the Manifest is open.")
                .define("manifestJeiSynchronise", false);
        MANIFEST_SHIFT_KEEPS_OPEN = client.comment("Hold Shift to keep the Manifest open when starting to track an entry.")
                .define("manifestShiftKeepsOpen", true);
        MANIFEST_RANGE = client.comment("Maximum distance at which the Manifest can track an inventory.")
                .defineInRange("manifestRange", 100, 1, 30000);
        ARSENAL_COLOURBLIND = client.comment("Use the Arsenal's color-accessible gem icons.").define("arsenalColourblind", false);
        CLIENT_SPEC = client.build();
    }

    private ArchiveConfig() {}

    private static boolean validFood(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }
        int comma = entry.indexOf(',');
        if (comma < 1) {
            return false;
        }
        String block = entry.substring(0, comma).strip();
        if (ResourceLocation.tryParse(block.startsWith("#") ? block.substring(1) : block) == null) {
            return false;
        }
        try {
            int hunger = Integer.parseInt(entry.substring(comma + 1).strip());
            return hunger > 0 && hunger <= 20;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
