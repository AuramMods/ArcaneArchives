package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public final class RadiantTroveBlockEntity extends PortableStorageBlockEntity {
    public static final String STORED_ITEMS = "handler_item";
    private final RadiantTroveInventory inventory = new RadiantTroveInventory(this::contentsChanged,
            () -> upgrades().multiplier(), () -> upgrades().hasOption(ArcaneArchives.RADIANT_KEY.get()),
            () -> upgrades().hasOption(ArcaneArchives.DEVOURING_CHARM.get()));
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private UUID lastDepositor;
    private long lastDeposit = -16;
    private long lastExtraction = -3;

    public RadiantTroveBlockEntity(BlockPos position, BlockState state) {
        super(ArcaneArchives.RADIANT_TROVE_ENTITY.get(), position, state, STORED_ITEMS, false);
    }

    public RadiantTroveInventory inventory() {
        return inventory;
    }

    @Override
    int storedAmount() {
        return inventory.count();
    }

    @Override
    int capacityWithMultiplier(int multiplier) {
        return RadiantTroveInventory.capacity(inventory.reference(), multiplier);
    }

    @Override
    void applyUpgrades() {
        inventory.clearUnusedReference();
    }

    void deposit(Player player) {
        if (level == null || level.isClientSide) {
            return;
        }
        long time = level.getGameTime();
        boolean repeat = player.getUUID().equals(lastDepositor) && time - lastDeposit <= 16;
        lastDepositor = player.getUUID();
        lastDeposit = time;

        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) {
            ItemStack remaining = inventory.insertItem(0, held, false);
            if (remaining.getCount() != held.getCount()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, remaining);
            }
        }
        if (!repeat || inventory.count() == 0) {
            return;
        }
        Inventory playerItems = player.getInventory();
        for (int slot = 0; slot < playerItems.items.size(); slot++) {
            ItemStack stack = playerItems.getItem(slot);
            if (inventory.isItemValid(0, stack)) {
                playerItems.setItem(slot, inventory.insertItem(0, stack, false));
            }
        }
    }

    void dispense(Player player) {
        if (level == null || level.isClientSide || level.getGameTime() - lastExtraction < 3) {
            return;
        }
        lastExtraction = level.getGameTime();
        boolean fullStack = player.isShiftKeyDown() == ArchiveConfig.TROVES_DISPENSE_STACK.get();
        ItemStack extracted = inventory.extractItem(0, fullStack ? Integer.MAX_VALUE : 1, false);
        if (!extracted.isEmpty()) {
            player.getInventory().placeItemBackInInventory(extracted);
        }
    }

    @Override
    CompoundTag saveContents() {
        return inventory.save();
    }

    @Override
    void loadContents(CompoundTag tag) {
        inventory.load(tag);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> inventory);
    }
}
