package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public final class GemCuttersTableBlockEntity extends ArchiveDeviceBlockEntity implements MenuProvider {
    static final int DRAWER_SIZE = 18;
    private final ItemStackHandler drawer = new ItemStackHandler(DRAWER_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private LazyOptional<IItemHandler> items = LazyOptional.of(() -> drawer);
    private ResourceLocation selectedRecipe = ArcaneArchives.id("radiant_dust");

    public GemCuttersTableBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.GEMCUTTERS_TABLE_ENTITY.get(), pos, state);
    }

    public ItemStackHandler drawer() {
        return drawer;
    }

    ResourceLocation selectedRecipe() {
        return selectedRecipe;
    }

    void selectRecipe(ResourceLocation recipe) {
        if (!selectedRecipe.equals(recipe)) {
            selectedRecipe = recipe;
            setChanged();
        }
    }

    public ItemStack acceptExisting(ItemStack incoming, boolean simulate) {
        ItemStack remaining = incoming;
        for (int slot = 0; slot < drawer.getSlots() && !remaining.isEmpty(); slot++) {
            if (ItemStack.isSameItemSameTags(drawer.getStackInSlot(slot), remaining)) {
                remaining = drawer.insertItem(slot, remaining, simulate);
            }
        }
        return remaining;
    }

    @Override
    public Component getDisplayName() {
        return ArcaneArchives.GEMCUTTERS_TABLE.get().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GemCuttersTableMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", drawer.serializeNBT());
        tag.putString("SelectedRecipe", selectedRecipe.toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag contents = tag.getCompound("inventory").copy();
        contents.putInt("Size", DRAWER_SIZE);
        drawer.deserializeNBT(contents);
        ResourceLocation recipe = tag.contains("SelectedRecipe") ? ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
        selectedRecipe = recipe == null ? ArcaneArchives.id("radiant_dust") : recipe;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return items.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        items.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        items = LazyOptional.of(() -> drawer);
    }
}
