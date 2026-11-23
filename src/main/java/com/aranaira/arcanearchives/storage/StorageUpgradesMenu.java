package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class StorageUpgradesMenu extends AbstractContainerMenu {
    public static final int PLAYER_TOP = 67;
    public static final int FIRST_UPGRADE_X = 57;
    public static final int UPGRADE_SPACING = 25;
    private static final int PLAYER_SLOTS = 36;

    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final boolean tank;
    private final StorageUpgrades upgrades;
    @Nullable
    private final PortableStorageBlockEntity storage;

    public StorageUpgradesMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, data.readBlockPos(), data.readBoolean(), null);
    }

    StorageUpgradesMenu(int id, Inventory inventory, PortableStorageBlockEntity storage) {
        this(id, inventory, storage.getBlockPos(), storage instanceof RadiantTankBlockEntity, storage);
    }

    private StorageUpgradesMenu(int id, Inventory inventory, BlockPos pos, boolean tank,
                                @Nullable PortableStorageBlockEntity storage) {
        super(ArcaneArchives.STORAGE_UPGRADES_MENU.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        this.tank = tank;
        this.storage = storage;
        this.upgrades = storage == null ? new StorageUpgrades(tank, () -> {}) : storage.upgrades();

        for (int slot = 0; slot < 27; slot++) {
            addSlot(new Slot(inventory, slot + 9, 10 + slot % 9 * 18, PLAYER_TOP + 13 + slot / 9 * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 10 + slot * 18, PLAYER_TOP + 71));
        }
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            addSlot(new UpgradeSlot(slot));
        }
    }

    public Item expectedUpgrade(int slot) {
        return upgrades.upgrade(slot);
    }

    public int tierMultiplier(int slot) {
        return StorageUpgrades.multiplierAfter(slot + 1);
    }

    public boolean installed(int slot) {
        return !upgrades.getStackInSlot(slot).isEmpty();
    }

    public boolean acceptsLockUpgrade() {
        return !tank;
    }

    @Override
    public boolean stillValid(Player player) {
        var block = tank ? ArcaneArchives.RADIANT_TANK.get() : ArcaneArchives.RADIANT_TROVE.get();
        return ArchiveStages.allowsFeature(player, ArchiveFeature.STORAGE_UPGRADES) && stillValid(access, player, block)
                && (storage == null || !storage.isRemoved() && player.level().getBlockEntity(pos) == storage);
    }

    @Override
    public void clicked(int index, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (stillValid(player)) {
            super.clicked(index, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index < PLAYER_SLOTS ? moveItemStackTo(stack, PLAYER_SLOTS, slots.size(), false)
                : moveItemStackTo(stack, 0, PLAYER_SLOTS, true);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return before;
    }

    private final class UpgradeSlot extends SlotItemHandler {
        UpgradeSlot(int slot) {
            super(upgrades, slot, FIRST_UPGRADE_X + slot % StorageUpgrades.SIZE_SLOTS * UPGRADE_SPACING,
                    slot < StorageUpgrades.SIZE_SLOTS ? 8 : 44);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean mayPickup(Player player) {
            int slot = getSlotIndex();
            if (slot >= StorageUpgrades.SIZE_SLOTS) {
                return true;
            }
            if (!upgrades.canRemove(slot)) {
                return false;
            }
            var entity = player.level().getBlockEntity(pos);
            return !(entity instanceof PortableStorageBlockEntity viewed)
                    || viewed.storedAmount() <= viewed.capacityWithMultiplier(StorageUpgrades.multiplierAfter(slot));
        }
    }
}
