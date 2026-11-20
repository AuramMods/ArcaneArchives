package com.aranaira.arcanearchives.arsenal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class GemCharge {
    private static final int POWER_UPGRADE = 2;

    private GemCharge() {}

    public static int capacity(ItemStack stack) {
        if (!(stack.getItem() instanceof ArcaneGemItem gem)) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.getByte("upgrades") & POWER_UPGRADE) != 0 ? gem.type().upgradedCapacity : gem.type().capacity;
    }

    public static int remaining(ItemStack stack) {
        int capacity = capacity(stack);
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("charge", Tag.TAG_ANY_NUMERIC) ? Mth.clamp(tag.getInt("charge"), 0, capacity) : capacity;
    }

    public static boolean infinite(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && (tag.getBoolean("infinite") || tag.getBoolean("infinity"));
    }

    public static boolean available(ItemStack stack, int cost) {
        return infinite(stack) || remaining(stack) >= cost;
    }

    public static void restore(ItemStack stack, int amount) {
        int capacity = capacity(stack);
        int before = remaining(stack);
        int charge = amount < 0 ? capacity : (int) Math.min(capacity, (long) before + amount);
        if (charge != before) {
            stack.getOrCreateTag().putInt("charge", charge);
        }
    }

    static void consume(ItemStack stack, int cost) {
        if (!infinite(stack) && cost > 0) {
            stack.getOrCreateTag().putInt("charge", Math.max(0, remaining(stack) - cost));
        }
    }

    public static boolean toggled(ItemStack stack) {
        return stack.getItem() instanceof ArcaneGemItem gem && gem.type().toggle && stack.getTag() != null
                && stack.getTag().getBoolean("toggle");
    }

    static void toggle(ItemStack stack, boolean value) {
        if (stack.getItem() instanceof ArcaneGemItem gem && gem.type().toggle && toggled(stack) != value) {
            stack.getOrCreateTag().putBoolean("toggle", value);
        }
    }

    static boolean cooldown(ItemStack stack, String key, long now, int ticks) {
        var tag = stack.getOrCreateTag();
        if (tag.contains(key) && now >= tag.getLong(key) && now - tag.getLong(key) < ticks) {
            return false;
        }
        tag.putLong(key, now);
        return true;
    }
}
