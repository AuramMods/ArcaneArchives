package com.aranaira.arcanearchives.storage;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.List;

public final class RadiantTroveItem extends BlockItem {
    public RadiantTroveItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag tag) {
        return new TroveItemHandler(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (stack.getTagElement(RadiantTroveBlockEntity.STORED_ITEMS) == null && stack.getTagElement(StorageUpgrades.SIZE_TAG) == null) {
            return;
        }
        stack.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            ItemStack stored = handler.getStackInSlot(0);
            if (!stored.isEmpty()) {
                tooltip.add(Component.translatable("arcanearchives.tooltip.trove.items", stored.getHoverName()));
            }
            tooltip.add(Component.translatable("arcanearchives.tooltip.trove.contains", stored.getCount(), handler.getSlotLimit(0)));
        });
    }

    private static final class TroveItemHandler implements IItemHandler, ICapabilityProvider {
        private final ItemStack stack;
        private final RadiantTroveInventory inventory;
        private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> this);

        TroveItemHandler(ItemStack stack) {
            this.stack = stack;
            inventory = new RadiantTroveInventory(this::writeInventory, () -> StorageUpgrades.portableMultiplier(stack.getTag()));
        }

        private RadiantTroveInventory readInventory() {
            CompoundTag contents = stack.getTagElement(RadiantTroveBlockEntity.STORED_ITEMS);
            inventory.load(contents == null ? new CompoundTag() : contents);
            return inventory;
        }

        private void writeInventory() {
            stack.addTagElement(RadiantTroveBlockEntity.STORED_ITEMS, inventory.save());
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return readInventory().getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack offered, boolean simulate) {
            return stack.getCount() == 1 ? readInventory().insertItem(slot, offered, simulate) : offered;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return stack.getCount() == 1 ? readInventory().extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return readInventory().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack offered) {
            return stack.getCount() == 1 && readInventory().isItemValid(slot, offered);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.ITEM_HANDLER ? handler.cast() : LazyOptional.empty();
        }
    }
}
