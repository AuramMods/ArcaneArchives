package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

public final class RadiantChestInventory extends ItemStackHandler {
    public static final int SIZE = 54;
    private final Runnable onChange;

    public RadiantChestInventory(Runnable onChange) {
        super(SIZE);
        this.onChange = onChange;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64 * ArchiveConfig.RADIANT_MULTIPLIER.get();
    }

    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        return getSlotLimit(slot);
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChange.run();
    }

    @Override
    public CompoundTag serializeNBT() {
        ListTag items = new ListTag();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = ExtendedStacks.save(stack);
                tag.putInt("Slot", slot);
                items.add(tag);
            }
        }
        CompoundTag result = new CompoundTag();
        result.putInt("Size", SIZE);
        result.put("Items", items);
        return result;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < items.size(); index++) {
            CompoundTag item = items.getCompound(index);
            int slot = item.getInt("Slot");
            if (slot >= 0 && slot < SIZE) {
                stacks.set(slot, ExtendedStacks.load(item));
            }
        }
    }

    int comparatorSignal() {
        double fullness = 0.0;
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = stacks.get(slot);
            if (!stack.isEmpty()) {
                fullness += Math.min(1.0, (double) stack.getCount() / getStackLimit(slot, stack));
            }
        }
        return fullness == 0.0 ? 0 : 1 + (int) (14.0 * fullness / SIZE);
    }
}
