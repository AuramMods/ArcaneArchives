package com.aranaira.arcanearchives.config;

import net.minecraftforge.common.ForgeConfigSpec;

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
    public static final ForgeConfigSpec.IntValue SLIVER_SINGLE_CHANCE;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_CHANCE;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_MIN;
    public static final ForgeConfigSpec.IntValue SLIVER_CLUSTER_MAX;
    public static final ForgeConfigSpec.BooleanValue RESONATOR_LOOP_ENABLED;
    public static final ForgeConfigSpec.DoubleValue RESONATOR_LOOP_VOLUME;

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
        SLIVER_SINGLE_CHANCE = builder.defineInRange("sliverSingleChance", 40, 0, 100);
        SLIVER_CLUSTER_CHANCE = builder.defineInRange("sliverClusterChance", 20, 0, 100);
        SLIVER_CLUSTER_MIN = builder.defineInRange("sliverClusterMinimum", 8, 1, 4096);
        SLIVER_CLUSTER_MAX = builder.defineInRange("sliverClusterMaximum", 24, 1, 4096);
        SERVER_SPEC = builder.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        RESONATOR_LOOP_ENABLED = client.comment("Play the ongoing sound while a Radiant Resonator grows quartz.")
                .define("resonatorTicking", true);
        RESONATOR_LOOP_VOLUME = client.comment("Volume of the Radiant Resonator's loop.")
                .defineInRange("resonatorVolume", 0.15, 0.0, 1.0);
        CLIENT_SPEC = client.build();
    }

    private ArchiveConfig() {}
}
