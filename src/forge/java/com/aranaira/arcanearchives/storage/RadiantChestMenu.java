package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.SlotItemHandler;

public final class RadiantChestMenu extends AbstractContainerMenu {
    public static final int SLOT_COUNT = RadiantChestInventory.SIZE + 36;

    private final Player owner;
    private final ContainerLevelAccess access;
    private final RadiantChestInventory chest;

    public RadiantChestMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, buffer.readBlockPos(), new RadiantChestInventory(() -> {}));
    }

    public RadiantChestMenu(int id, Inventory playerInventory, RadiantChestBlockEntity chest) {
        this(id, playerInventory, chest.getBlockPos(), chest.inventory());
    }

    private RadiantChestMenu(int id, Inventory playerInventory, BlockPos pos, RadiantChestInventory chest) {
        super(ArcaneArchives.RADIANT_CHEST_MENU.get(), id);
        owner = playerInventory.player;
        access = ContainerLevelAccess.create(owner.level(), pos);
        this.chest = chest;

        for (int index = 0; index < RadiantChestInventory.SIZE; index++) {
            addSlot(new ChestSlot(chest, index, 16 + (index % 9) * 18, 16 + (index / 9) * 18));
        }
        for (int index = 0; index < 27; index++) {
            addSlot(new Slot(playerInventory, index + 9, 16 + (index % 9) * 18, 142 + (index / 9) * 18));
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(playerInventory, index, 16 + index * 18, 200));
        }
    }

    @Override
    public void setSynchronizer(ContainerSynchronizer synchronizer) {
        // The vanilla content packets encode item counts as a signed byte.
        super.setSynchronizer(owner instanceof ServerPlayer serverPlayer
                ? ArchiveNetwork.chestSynchronizer(serverPlayer, synchronizer) : synchronizer);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ArcaneArchives.RADIANT_CHEST.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = stack.copy();
        if (index < RadiantChestInventory.SIZE) {
            if (!moveItemStackTo(stack, RadiantChestInventory.SIZE, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else {
            ItemStack remaining = insertIntoChest(stack);
            stack.setCount(remaining.getCount());
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    private ItemStack insertIntoChest(ItemStack stack) {
        if (stack.isStackable()) {
            return ItemHandlerHelper.insertItemStacked(chest, stack.copy(), false);
        }
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < chest.getSlots() && !remaining.isEmpty(); slot++) {
            if (ItemStack.isSameItemSameTags(remaining, chest.getStackInSlot(slot))) {
                remaining = chest.insertItem(slot, remaining, false);
            }
        }
        return ItemHandlerHelper.insertItem(chest, remaining, false);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < RadiantChestInventory.SIZE) {
            Slot slot = slots.get(slotId);
            ItemStack stack = slot.getItem();
            if (stack.getCount() > stack.getMaxStackSize()) {
                switch (type) {
                    case PICKUP -> {
                        if (button != 0 && button != 1) {
                            return;
                        }
                        if (getCarried().isEmpty()) {
                            int capacity = stack.getMaxStackSize();
                            int amount = button == 1 ? (capacity + 1) / 2 : capacity;
                            setCarried(slot.safeTake(amount, amount, player));
                            return;
                        }
                        if (!ItemStack.isSameItemSameTags(stack, getCarried())) {
                            return;
                        }
                    }
                    case SWAP -> {
                        if (button >= 0 && (button < Inventory.getSelectionSize() || button == 40)) {
                            swapLargeStack(slot, button, player);
                        }
                        return;
                    }
                    case THROW -> {
                        if (button == 1 && getCarried().isEmpty() && slot.mayPickup(player)) {
                            int max = stack.getMaxStackSize();
                            while (slot.hasItem()) {
                                ItemStack extracted = slot.safeTake(max, max, player);
                                if (extracted.isEmpty()) {
                                    break;
                                }
                                player.drop(extracted, true);
                            }
                            return;
                        }
                    }
                    default -> {}
                }
            }
        }
        super.clicked(slotId, button, type, player);
    }

    private static void swapLargeStack(Slot slot, int inventorySlot, Player player) {
        if (!slot.mayPickup(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        ItemStack incoming = inventory.getItem(inventorySlot);
        ItemStack stored = slot.getItem();
        int max = stored.getMaxStackSize();
        if (incoming.isEmpty()) {
            inventory.setItem(inventorySlot, slot.safeTake(max, max, player));
            return;
        }
        if (slot.mayPlace(incoming) && incoming.getCount() <= slot.getMaxStackSize(incoming)) {
            slot.setByPlayer(incoming);
            inventory.setItem(inventorySlot, stored.split(max));
            while (!stored.isEmpty()) {
                inventory.placeItemBackInInventory(stored.split(max));
            }
        }
    }

    private static final class ChestSlot extends SlotItemHandler {
        private final RadiantChestInventory chest;

        private ChestSlot(RadiantChestInventory chest, int index, int x, int y) {
            super(chest, index, x, y);
            this.chest = chest;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return chest.getStackLimit(getSlotIndex(), stack);
        }

        @Override
        public void setChanged() {
            chest.onContentsChanged(getSlotIndex());
        }
    }
}
