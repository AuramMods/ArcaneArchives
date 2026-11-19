package com.aranaira.arcanearchives.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.function.IntSupplier;

public final class RadiantTroveInventory implements IItemHandler {
    public static final int BASE_CAPACITY = 64 * 512;

    private final Runnable onChange;
    private final IntSupplier multiplier;
    private ItemStack reference = ItemStack.EMPTY;
    private int count;

    RadiantTroveInventory(Runnable onChange, IntSupplier multiplier) {
        this.onChange = onChange;
        this.multiplier = multiplier;
    }

    public int count() {
        return count;
    }

    public ItemStack reference() {
        return reference.copy();
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 && count > 0 ? reference.copyWithCount(count) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) {
            return stack;
        }
        int capacity = capacity(reference.isEmpty() ? stack : reference, multiplier.getAsInt());
        int accepted = Math.min(stack.getCount(), Math.max(0, capacity - count));
        if (accepted > 0 && !simulate) {
            if (count == 0) {
                reference = stack.copyWithCount(1);
            }
            count += accepted;
            onChange.run();
        }
        return stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= getSlots() || amount <= 0 || count <= 0) {
            return ItemStack.EMPTY;
        }
        int extracted = Math.min(count, Math.min(amount, reference.getMaxStackSize()));
        ItemStack result = reference.copyWithCount(extracted);
        if (!simulate) {
            count -= extracted;
            if (count == 0) {
                reference = ItemStack.EMPTY;
            }
            onChange.run();
        }
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            return 0;
        }
        return capacity(reference, multiplier.getAsInt());
    }

    static int capacity(ItemStack reference, int multiplier) {
        long base = reference.isEmpty() ? BASE_CAPACITY : Math.max(64L, reference.getMaxStackSize()) * 512;
        return (int) Math.min(Integer.MAX_VALUE, base * multiplier);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot >= 0 && slot < getSlots() && !stack.isEmpty()
                && (reference.isEmpty() || ItemStack.isSameItemSameTags(reference, stack));
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("COUNT", count);
        if (!reference.isEmpty()) {
            tag.put("REFERENCE", reference.save(new CompoundTag()));
        }
        return tag;
    }

    void load(CompoundTag tag) {
        count = Math.max(0, tag.getInt("COUNT"));
        reference = ItemStack.of(tag.getCompound("REFERENCE")).copyWithCount(1);
        if (count == 0 || reference.isEmpty()) {
            count = 0;
            reference = ItemStack.EMPTY;
        }
    }
}
