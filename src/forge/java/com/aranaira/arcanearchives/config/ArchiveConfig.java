package com.aranaira.arcanearchives.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArchiveConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue RADIANT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue TROVES_DISPENSE_STACK;
    public static final ForgeConfigSpec.BooleanValue CHESTS_EXISTING_ITEMS_ONLY;
    public static final ForgeConfigSpec.BooleanValue BRAZIER_SOUND;

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
        SERVER_SPEC = builder.build();
    }

    private ArchiveConfig() {}
}
