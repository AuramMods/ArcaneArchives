package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

final class StorageUpgrades extends ItemStackHandler {
    static final String SIZE_TAG = "size_upgrades";
    static final int SIZE_SLOTS = 3;
    private static final int[] MULTIPLIERS = {1, 3, 6, 10};
    private final boolean tank;
    private final Runnable changed;

    StorageUpgrades(boolean tank, Runnable changed) {
        super(SIZE_SLOTS);
        this.tank = tank;
        this.changed = changed;
    }

    Item upgrade(int slot) {
        return switch (slot) {
            case 0 -> ArcaneArchives.MATRIX_BRACE.get();
            case 1 -> tank ? ArcaneArchives.CONTAINMENT_FIELD.get() : ArcaneArchives.MATERIAL_INTERFACE.get();
            case 2 -> ArcaneArchives.STORAGE_SHAPED_QUARTZ_ITEM.get();
            default -> throw new IndexOutOfBoundsException(slot);
        };
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(upgrade(slot)) && (slot == 0 || !getStackInSlot(slot - 1).isEmpty());
    }

    @Override
    protected void onContentsChanged(int slot) {
        changed.run();
    }

    boolean canRemove(int slot) {
        return slot == SIZE_SLOTS - 1 || getStackInSlot(slot + 1).isEmpty();
    }

    int multiplier() {
        int count = 0;
        while (count < SIZE_SLOTS && !getStackInSlot(count).isEmpty()) {
            count++;
        }
        return multiplierAfter(count);
    }

    static int multiplierAfter(int installed) {
        return MULTIPLIERS[installed];
    }

    static int portableMultiplier(CompoundTag tag) {
        if (tag == null) {
            return multiplierAfter(0);
        }
        CompoundTag upgrades = tag.getCompound(SIZE_TAG);
        int count = 0;
        while (count < SIZE_SLOTS && installed(upgrades, Integer.toString(count))) {
            count++;
        }
        return multiplierAfter(count);
    }

    private static boolean installed(CompoundTag upgrades, String slot) {
        return upgrades.contains(slot, Tag.TAG_COMPOUND) || upgrades.getBoolean(slot);
    }

    void saveTo(CompoundTag tag) {
        CompoundTag upgrades = new CompoundTag();
        for (int i = 0; i < SIZE_SLOTS; i++) {
            ItemStack stack = getStackInSlot(i);
            if (!stack.isEmpty()) {
                upgrades.put(Integer.toString(i), stack.save(new CompoundTag()));
            }
        }
        if (!upgrades.isEmpty()) {
            tag.put(SIZE_TAG, upgrades);
        }
    }

    void loadFrom(CompoundTag tag) {
        CompoundTag upgrades = tag.getCompound(SIZE_TAG);
        for (int i = 0; i < SIZE_SLOTS; i++) {
            String slot = Integer.toString(i);
            ItemStack stack = upgrades.getBoolean(slot) ? new ItemStack(upgrade(i)) : ItemStack.of(upgrades.getCompound(slot));
            stacks.set(i, stack.is(upgrade(i)) ? stack.copyWithCount(1) : ItemStack.EMPTY);
        }
    }
}
