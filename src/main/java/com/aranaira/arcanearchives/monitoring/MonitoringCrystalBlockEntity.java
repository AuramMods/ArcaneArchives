package com.aranaira.arcanearchives.monitoring;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import java.util.Optional;

public final class MonitoringCrystalBlockEntity extends ArchiveDeviceBlockEntity {
    public MonitoringCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.MONITORING_CRYSTAL_ENTITY.get(), pos, state);
    }

    public Direction targetDirection() {
        return getBlockState().getValue(MonitoringCrystalBlock.FACING).getOpposite();
    }

    public BlockPos targetPos() {
        return worldPosition.relative(targetDirection());
    }

    public Optional<IItemHandler> targetInventory() {
        BlockPos pos = targetPos();
        if (level == null || !level.hasChunkAt(pos)
                || (level instanceof ServerLevel server && !ArchiveStages.allowsOwner(server, owner(), ArchiveFeature.MONITORING_READ.id()))) {
            return Optional.empty();
        }
        BlockEntity target = level.getBlockEntity(pos);
        if (target == null || target instanceof ArchiveDeviceBlockEntity) {
            return Optional.empty();
        }
        Optional<IItemHandler> inventory = target.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (inventory.isEmpty()) {
            inventory = target.getCapability(ForgeCapabilities.ITEM_HANDLER, targetDirection().getOpposite()).resolve();
        }
        return inventory.isPresent() ? inventory
                : target instanceof Container container ? Optional.of(new InvWrapper(container)) : Optional.empty();
    }
}
