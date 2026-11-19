package com.aranaira.arcanearchives.crafting;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;


final class CraftingGrid extends SimpleContainer implements CraftingContainer {
    static final int WIDTH = 3;
    static final int SIZE = WIDTH * WIDTH;

    CraftingGrid() {
        super(SIZE);
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return WIDTH;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int slot = 0; slot < SIZE; slot++) {
            items.set(slot, getItem(slot));
        }
        return items;
    }

    CompoundTag save() {
        CompoundTag contents = new CompoundTag();
        contents.putInt("Size", SIZE);
        return ContainerHelper.saveAllItems(contents, getItems());
    }

    void load(CompoundTag contents) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(contents, items);
        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, items.get(slot));
        }
    }
}
