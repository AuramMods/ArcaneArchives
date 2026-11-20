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
    private final StorageUpgrades upgrades;

    PortableStorageBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String storageKey, boolean tank) {
        super(type, pos, state);
        this.storageKey = storageKey;
        this.upgrades = new StorageUpgrades(tank, this::upgradesChanged);
    }

    abstract CompoundTag saveContents();

    abstract void loadContents(CompoundTag tag);

    abstract int storedAmount();

    abstract int capacityWithMultiplier(int multiplier);

    abstract void applyUpgrades();

    final StorageUpgrades upgrades() {
        return upgrades;
    }

    final void restorePortableContents(CompoundTag tag) {
        if (tag.contains(storageKey) || tag.contains(StorageUpgrades.SIZE_TAG) || tag.contains(StorageUpgrades.OPTIONAL_TAG)) {
            readStorage(tag);
            contentsChanged();
        }
    }

    private void upgradesChanged() {
        applyUpgrades();
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
        readStorage(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeStorage(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeStorage(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readStorage(tag);
    }

    private void readStorage(CompoundTag tag) {
        upgrades.loadFrom(tag);
        applyUpgrades();
        loadContents(tag.getCompound(storageKey));
    }

    private void writeStorage(CompoundTag tag) {
        tag.put(storageKey, saveContents());
        upgrades.saveTo(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, blockEntity -> ((PortableStorageBlockEntity) blockEntity).getUpdateTag());
    }
}
