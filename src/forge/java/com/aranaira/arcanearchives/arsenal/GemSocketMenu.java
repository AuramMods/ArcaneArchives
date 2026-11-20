package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class GemSocketMenu extends AbstractContainerMenu {
    public static final int RECHARGE = 0;
    private static final int OFFHAND = 36;
    private static final int GEM = 37;

    private final GemSocketItem.SlotRef origin;
    private final ItemStack socket;
    private final SimpleContainer gem;

    public GemSocketMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, GemSocketItem.SlotRef.read(data));
    }

    GemSocketMenu(int id, Inventory inventory, GemSocketItem.SlotRef origin) {
        super(ArcaneArchives.GEM_SOCKET_MENU.get(), id);
        this.origin = origin;
        this.socket = origin.stack(inventory.player);
        this.gem = new SimpleContainer(GemSocketItem.gem(socket)) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (!inventory.player.level().isClientSide && stillValid(inventory.player)) {
                    GemSocketItem.store(socket, getItem(0));
                    inventory.setChanged();
                }
            }
        };
        for (int slot = 0; slot < 27; slot++) {
            addSlot(new PlayerSlot(inventory, slot + 9, 10 + slot % 9 * 18, 57 + slot / 9 * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new PlayerSlot(inventory, slot, 10 + slot * 18, 115));
        }
        addSlot(new PlayerSlot(inventory, 40, -23, 115));
        addSlot(new Slot(gem, 0, 81, 3) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArcaneGemItem;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return ArchiveConfig.ARSENAL_ENABLED.get() && (player.level().isClientSide
                || socket.is(ArcaneArchives.GEM_SOCKET.get()) && origin.stack(player) == socket);
    }

    boolean edits(ItemStack socket) {
        return this.socket == socket;
    }

    ArsenalGems.Source source() {
        return new ArsenalGems.Source(gem.getItem(0), gem::setChanged);
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (action != RECHARGE || !stillValid(player) || player.isSpectator()) {
            return false;
        }
        for (var source : ArsenalGems.available(player)) {
            GemRecharge.recharge(player, source);
        }
        return true;
    }

    @Override
    public void clicked(int index, int button, ClickType action, Player player) {
        if (stillValid(player) && !(action == ClickType.SWAP && button == origin.inventorySlot())) {
            super.clicked(index, button, action, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        ItemStack stack = slot.getItem();
        if (!(stack.getItem() instanceof ArcaneGemItem) || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        boolean moved = switch (index) {
            case GEM -> moveItemStackTo(stack, OFFHAND, GEM, false) || moveItemStackTo(stack, 0, OFFHAND, true);
            case OFFHAND -> moveItemStackTo(stack, GEM, GEM + 1, false) || moveItemStackTo(stack, 0, OFFHAND, true);
            default -> moveItemStackTo(stack, GEM, GEM + 1, false) || moveItemStackTo(stack, OFFHAND, GEM, false);
        };
        if (!moved) {
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

    private final class PlayerSlot extends Slot {
        PlayerSlot(Inventory player, int slot, int x, int y) {
            super(player, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() != origin.inventorySlot();
        }

        @Override
        public boolean mayPickup(Player player) {
            return getContainerSlot() != origin.inventorySlot();
        }
    }
}
