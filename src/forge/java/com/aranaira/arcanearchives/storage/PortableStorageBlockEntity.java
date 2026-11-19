package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

abstract class PortableStorageBlockEntity extends ArchiveDeviceBlockEntity {
    private final String storageKey;

    PortableStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String storageKey) {
        super(type, pos, state);
        this.storageKey = storageKey;
    }

    abstract CompoundTag saveContents();

    abstract void loadContents(CompoundTag tag);

    final String storageKey() {
        return storageKey;
    }

    final void restorePortableContents(CompoundTag tag) {
        loadContents(tag);
        contentsChanged();
    }

    final void contentsChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadContents(tag.getCompound(storageKey));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(storageKey, saveContents());
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(storageKey, saveContents());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        loadContents(tag.getCompound(storageKey));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, blockEntity -> ((PortableStorageBlockEntity) blockEntity).getUpdateTag());
    }
}
