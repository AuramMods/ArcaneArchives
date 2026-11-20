package com.aranaira.arcanearchives.storage;

import net.minecraft.world.entity.player.Inventory;
import org.violetmoon.quark.api.ISortingLockedSlots;

import java.util.stream.IntStream;

final class QuarkChestMenu extends RadiantChestMenu implements ISortingLockedSlots {
    private QuarkChestMenu(int id, Inventory inventory, RadiantChestBlockEntity chest) {
        super(id, inventory, chest);
    }

    static RadiantChestMenu create(int id, Inventory inventory, RadiantChestBlockEntity chest) {
        return new QuarkChestMenu(id, inventory, chest);
    }

    @Override
    public int[] getSortingLockedSlots(boolean playerInventory) {
        if (playerInventory) {
            return new int[0];
        }
        // Quark empties each unlocked slot in one normal item-handler extraction.
        return IntStream.range(0, RadiantChestInventory.SIZE).filter(slot -> {
            var item = getSlot(slot).getItem();
            return !item.isEmpty() && item.getCount() > item.getMaxStackSize();
        }).toArray();
    }
}
