package com.aranaira.arcanearchives.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

final class ExtendedStacks {
    private static final String EXTENDED_COUNT = "ExtendedCount";

    private ExtendedStacks() {}

    static CompoundTag save(ItemStack stack) {
        CompoundTag tag = stack.copyWithCount(1).save(new CompoundTag());
        tag.putInt(EXTENDED_COUNT, stack.getCount());
        return tag;
    }

    static ItemStack load(CompoundTag tag) {
        if (!tag.contains(EXTENDED_COUNT, Tag.TAG_ANY_NUMERIC)) {
            return ItemStack.of(tag);
        }
        int count = tag.getInt(EXTENDED_COUNT);
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        CompoundTag prototype = tag.copy();
        prototype.putByte("Count", (byte) 1);
        return ItemStack.of(prototype).copyWithCount(count);
    }

    static void write(FriendlyByteBuf buffer, ItemStack stack) {
        buffer.writeVarInt(stack.getCount());
        if (!stack.isEmpty()) {
            buffer.writeItem(stack.copyWithCount(1));
        }
    }

    static ItemStack read(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        return count > 0 ? buffer.readItem().copyWithCount(count) : ItemStack.EMPTY;
    }
}
