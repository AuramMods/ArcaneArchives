package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.client.ResonatorLoopSound;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public final class RadiantResonatorBlockEntity extends ArchiveDeviceBlockEntity {
    private int growth;
    private boolean online;

    public RadiantResonatorBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.RADIANT_RESONATOR_ENTITY.get(), pos, state);
    }

    static void tick(Level level, BlockPos pos, BlockState state, RadiantResonatorBlockEntity resonator) {
        if (level instanceof ServerLevel serverLevel) {
            boolean ownerOnline = resonator.owner() != null && serverLevel.getServer().getPlayerList().getPlayer(resonator.owner()) != null;
            resonator.advance(serverLevel, ownerOnline);
        } else if (level.isClientSide && resonator.isResonating()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ResonatorLoopSound.startIfNeeded(resonator));
        }
    }

    void advance(ServerLevel level, boolean ownerOnline) {
        if (online != ownerOnline) {
            online = ownerOnline;
            BlockState state = getBlockState();
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        BlockPos above = worldPosition.above();
        if (!online || !level.isEmptyBlock(above)) {
            return;
        }

        if (++growth >= ArchiveConfig.RESONATOR_TICKS.get()
                && level.setBlock(above, ArcaneArchives.RAW_QUARTZ_CLUSTER.get().defaultBlockState(), Block.UPDATE_ALL)) {
            growth = 0;
            if (ArchiveConfig.MACHINE_SOUNDS.get() && ArchiveConfig.RESONATOR_COMPLETION_SOUND.get()) {
                level.playSound(null, worldPosition, ArcaneArchives.RESONATOR_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            for (Ocelot ocelot : level.getEntitiesOfClass(Ocelot.class, new AABB(above).inflate(0.4))) {
                ocelot.push((level.random.nextDouble() - 0.5) * 3.0, level.random.nextDouble() * 5.0,
                        (level.random.nextDouble() - 0.5) * 3.0);
            }
        }
        setChanged();
    }

    public boolean isResonating() {
        return online && !isRemoved() && level != null && level.isEmptyBlock(worldPosition.above());
    }

    public int percentGrown() {
        return (int) Math.min(100, 100.0 * growth / ArchiveConfig.RESONATOR_TICKS.get());
    }

    public GrowthStatus growthStatus() {
        if (level != null) {
            BlockState above = level.getBlockState(worldPosition.above());
            if (above.is(ArcaneArchives.RAW_QUARTZ_CLUSTER.get())) {
                return GrowthStatus.HARVESTABLE;
            }
            if (!above.isAir()) {
                return GrowthStatus.OBSTRUCTED;
            }
        }
        return online ? GrowthStatus.RESONATING : GrowthStatus.OFFLINE;
    }

    int comparatorSignal() {
        if (level == null) {
            return 0;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        if (above.is(ArcaneArchives.RAW_QUARTZ_CLUSTER.get())) {
            return 15;
        }
        if (!online || !above.isAir()) {
            return 0;
        }
        return 1 + Math.min(13, (int) (14.0 * growth / ArchiveConfig.RESONATOR_TICKS.get()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        growth = Math.max(0, tag.getInt("current_tick"));
        online = tag.getBoolean("can_tick");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("current_tick", growth);
        tag.putBoolean("can_tick", online);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("current_tick", growth);
    }

    public enum GrowthStatus {
        HARVESTABLE("harvestable", ChatFormatting.GOLD), OBSTRUCTED("obstruction", ChatFormatting.RED),
        RESONATING("resonating", ChatFormatting.GREEN), OFFLINE("offline", ChatFormatting.DARK_RED);

        private final String key;
        private final ChatFormatting color;

        GrowthStatus(String key, ChatFormatting color) {
            this.key = key;
            this.color = color;
        }

        public Component label() {
            return Component.translatable("arcanearchives.data.tooltip.resonator_status." + key).withStyle(color);
        }
    }
}
