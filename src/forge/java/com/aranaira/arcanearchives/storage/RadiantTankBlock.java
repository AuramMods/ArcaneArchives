package com.aranaira.arcanearchives.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fluids.FluidUtil;

public final class RadiantTankBlock extends PortableStorageBlock {
    private static final VoxelShape SHAPE = Block.box(1.6, 0, 1.6, 14.4, 15.68, 14.4);

    public RadiantTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiantTankBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return FluidUtil.getFluidHandler(player.getItemInHand(hand)).isPresent()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RadiantTankBlockEntity tank && !tank.tank().isEmpty()) {
            return Math.min(15, 1 + (int) (14.0 * tank.tank().getFluidAmount() / tank.tank().getCapacity()));
        }
        return 0;
    }
}
