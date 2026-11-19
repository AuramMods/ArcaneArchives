package com.aranaira.arcanearchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class ArchiveDeviceBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    @Nullable
    private UUID deviceId;

    protected ArchiveDeviceBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    @Nullable
    public final UUID owner() {
        return owner;
    }

    @Nullable
    public final UUID deviceId() {
        return deviceId;
    }

    public final void bindTo(UUID player) {
        if (owner == null) {
            owner = player;
            setChanged();
            registerDevice();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerDevice();
    }

    private void registerDevice() {
        if (owner != null && level instanceof ServerLevel serverLevel) {
            if (deviceId == null) {
                deviceId = UUID.randomUUID();
                setChanged();
            }
            GlobalPos location = GlobalPos.of(serverLevel.dimension(), worldPosition);
            ArchiveNetworks.get(serverLevel.getServer()).registerDevice(location, deviceId, owner,
                    BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()));
        }
    }

    public final void removeFromNetwork() {
        if (deviceId != null && level instanceof ServerLevel serverLevel) {
            GlobalPos location = GlobalPos.of(serverLevel.dimension(), worldPosition);
            ArchiveNetworks.get(serverLevel.getServer()).removeDevice(location, deviceId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = readIdentity(tag, "ArchiveOwner", "playerId");
        deviceId = readIdentity(tag, "ArchiveDevice", "tileId");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("ArchiveOwner", owner);
        }
        if (deviceId != null) {
            tag.putUUID("ArchiveDevice", deviceId);
        }
    }

    @Nullable
    private static UUID readIdentity(CompoundTag tag, String key, String legacyKey) {
        if (tag.hasUUID(key)) {
            return tag.getUUID(key);
        }
        if (tag.contains(legacyKey, Tag.TAG_STRING)) {
            try {
                return UUID.fromString(tag.getString(legacyKey));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }
}
