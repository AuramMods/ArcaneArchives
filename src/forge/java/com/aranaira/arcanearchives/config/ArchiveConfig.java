package com.aranaira.arcanearchives.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ArchiveConfig {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue RADIANT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue TROVES_DISPENSE_STACK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        RADIANT_MULTIPLIER = builder.comment("Capacity of a Radiant Chest slot in multiples of 64 items.")
                .defineInRange("radiantMultiplier", 4, 1, 8);
        TROVES_DISPENSE_STACK = builder.comment("Dispense an entire stack while crouching instead of while standing.")
                .define("trovesDispenseStack", true);
        SERVER_SPEC = builder.build();
    }

    private ArchiveConfig() {}
}
