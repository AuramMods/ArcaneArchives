package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.UUID;

public final class RadiantTroveBlockEntity extends ArchiveDeviceBlockEntity {
    public static final String STORED_ITEMS = "handler_item";
    private final RadiantTroveInventory inventory = new RadiantTroveInventory(this::contentsChanged);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    private UUID lastDepositor;
    private long lastDeposit = -16;
    private long lastExtraction = -3;

    public RadiantTroveBlockEntity(BlockPos position, BlockState state) {
        super(ArcaneArchives.RADIANT_TROVE_ENTITY.get(), position, state);
    }

    public RadiantTroveInventory inventory() {
        return inventory;
    }

    void restorePortableContents(CompoundTag tag) {
        inventory.load(tag);
        contentsChanged();
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
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.load(tag.getCompound(STORED_ITEMS));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(STORED_ITEMS, inventory.save());
    }

    private void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(STORED_ITEMS, inventory.save());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        inventory.load(tag.getCompound(STORED_ITEMS));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
