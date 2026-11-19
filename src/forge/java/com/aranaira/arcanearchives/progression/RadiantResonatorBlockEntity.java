package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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
        }
    }

    void advance(ServerLevel level, boolean ownerOnline) {
        if (online != ownerOnline) {
            online = ownerOnline;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        BlockPos above = worldPosition.above();
        if (!online || !level.isEmptyBlock(above)) {
            return;
        }

        if (++growth >= ArchiveConfig.RESONATOR_TICKS.get()
                && level.setBlock(above, ArcaneArchives.RAW_QUARTZ_CLUSTER.get().defaultBlockState(), Block.UPDATE_ALL)) {
            growth = 0;
            if (ArchiveConfig.RESONATOR_COMPLETION_SOUND.get()) {
                level.playSound(null, worldPosition, ArcaneArchives.RESONATOR_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            for (Ocelot ocelot : level.getEntitiesOfClass(Ocelot.class, new AABB(above).inflate(0.4))) {
                ocelot.push((level.random.nextDouble() - 0.5) * 3.0, level.random.nextDouble() * 5.0,
                        (level.random.nextDouble() - 0.5) * 3.0);
            }
        }
        setChanged();
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
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("current_tick", growth);
    }
}
