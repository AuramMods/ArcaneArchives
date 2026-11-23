package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArcaneGemItem;
import com.aranaira.arcanearchives.arsenal.GemCharge;
import com.aranaira.arcanearchives.arsenal.GemType;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

public final class DevouringCharmMenu extends AbstractContainerMenu {
    public static final int FLIP = 0;
    public static final int FLUID_SLOT = 36;
    public static final int FIRST_DISCARD_SLOT = FLUID_SLOT + 1;
    public static final int FIRST_FILTER_SLOT = FIRST_DISCARD_SLOT + DevouringCharmItem.FILTER_COUNT;

    private final int heldSlot;
    private final ItemStack charm;
    private final SimpleContainer fluid = new SimpleContainer(1);
    private final SimpleContainer discard = new SimpleContainer(DevouringCharmItem.FILTER_COUNT);
    private final SimpleContainer filters = new SimpleContainer(DevouringCharmItem.FILTER_COUNT);
    private final DataSlot flipped = DataSlot.standalone();

    public DevouringCharmMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readVarInt());
    }

    DevouringCharmMenu(int id, Inventory inventory, int heldSlot) {
        super(ArcaneArchives.DEVOURING_CHARM_MENU.get(), id);
        this.heldSlot = heldSlot;
        charm = inventory.getItem(heldSlot);
        addDataSlot(flipped);
        var saved = DevouringCharmItem.readFilters(charm);
        for (int slot = 0; slot < saved.size(); slot++) {
            filters.setItem(slot, saved.get(slot));
        }

        for (int slot = 0; slot < 27; slot++) {
            addSlot(new PlayerSlot(inventory, slot + 9, 10 + slot % 9 * 18, 107 + slot / 9 * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new PlayerSlot(inventory, slot, 10 + slot * 18, 165));
        }
        addSlot(new FluidSlot());
        for (int slot = 0; slot < DevouringCharmItem.FILTER_COUNT; slot++) {
            addSlot(new FrontSlot(discard, slot, 64 + slot % 3 * 18, 55 + slot / 3 * 18));
        }
        for (int slot = 0; slot < DevouringCharmItem.FILTER_COUNT; slot++) {
            addSlot(new FilterSlot(slot));
        }
    }

    public boolean flipped() {
        return flipped.get() != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.CHARM_CONFIGURE)) {
            return false;
        }
        ItemStack current = player.getInventory().getItem(heldSlot);
        return current.is(ArcaneArchives.DEVOURING_CHARM.get()) && (player.level().isClientSide || current == charm);
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (button != FLIP || player.isSpectator() || !stillValid(player)) {
            return false;
        }
        flipped.set(flipped() ? 0 : 1);
        broadcastChanges();
        return true;
    }

    @Override
    public void clicked(int index, int button, ClickType action, Player player) {
        if (action == ClickType.SWAP && button == heldSlot) {
            return;
        }
        if (index >= FIRST_FILTER_SLOT && index < slots.size()) {
            if (flipped() && action == ClickType.PICKUP && (button == 0 || button == 1)) {
                int sampleSlot = index - FIRST_FILTER_SLOT;
                ItemStack sample = button == 0 ? getCarried().copyWithCount(1) : ItemStack.EMPTY;
                filters.setItem(sampleSlot, sample);
                if (!player.level().isClientSide && stillValid(player)) {
                    var saved = DevouringCharmItem.readFilters(charm);
                    saved.set(sampleSlot, sample);
                    DevouringCharmItem.writeFilters(charm, saved);
                }
            }
            return;
        }
        if (!flipped() || index < FLUID_SLOT) {
            super.clicked(index, button, action, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (flipped() || index < 0 || index >= FIRST_FILTER_SLOT || !stillValid(player)) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved;
        if (index >= FLUID_SLOT) {
            moved = moveItemStackTo(stack, 0, FLUID_SLOT, true);
        } else if (getSlot(FLUID_SLOT).mayPlace(stack)) {
            moved = moveItemStackTo(stack, FLUID_SLOT, FIRST_DISCARD_SLOT, false);
        } else {
            moved = moveItemStackTo(stack, FIRST_DISCARD_SLOT, FIRST_FILTER_SLOT, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            clearContainer(player, fluid);
            discard.clearContent();
        }
    }

    private static ItemStack drain(ItemStack stack) {
        if (isParchtear(stack)) {
            GemCharge.restore(stack, -1);
            return stack;
        }
        return FluidUtil.getFluidHandler(stack).map(handler -> {
            for (int i = 0; i < handler.getTanks(); i++) {
                var stored = handler.getFluidInTank(i);
                if (!stored.isEmpty()) {
                    handler.drain(stored.copy(), FluidAction.EXECUTE);
                }
            }
            return handler.getContainer();
        }).orElse(stack);
    }

    private static boolean isParchtear(ItemStack stack) {
        return ArchiveConfig.ARSENAL_ENABLED.get() && stack.getItem() instanceof ArcaneGemItem gem && gem.type() == GemType.PARCHTEAR;
    }

    private final class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int slot, int x, int y) {
            super(inventory, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return getContainerSlot() != heldSlot;
        }

        @Override
        public boolean mayPickup(Player player) {
            return getContainerSlot() != heldSlot;
        }
    }

    private class FrontSlot extends Slot {
        FrontSlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean isActive() {
            return !flipped();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive();
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive();
        }
    }

    private final class FluidSlot extends FrontSlot {
        FluidSlot() {
            super(fluid, 0, 82, 13);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return super.mayPlace(stack) && !stack.is(ArcaneArchives.RADIANT_AMPHORA.get())
                    && (isParchtear(stack) || FluidUtil.getFluidHandler(stack).isPresent());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            super.setByPlayer(drain(stack));
        }
    }

    private final class FilterSlot extends Slot {
        FilterSlot(int slot) {
            super(filters, slot, 58 + slot % 3 * 24, 41 + slot / 3 * 24);
        }

        @Override
        public boolean isActive() {
            return flipped();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
