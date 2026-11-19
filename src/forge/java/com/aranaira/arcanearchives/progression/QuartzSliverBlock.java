package com.aranaira.arcanearchives.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class QuartzSliverBlock extends DirectionalQuartzBlock {
    private static final VoxelShape FLOOR = box(6.4, 0, 6.4, 9.6, 8, 9.6);
    private static final VoxelShape CEILING = box(6.4, 8, 6.4, 9.6, 16, 9.6);
    private static final VoxelShape NORTH = box(6.4, 6.4, 8, 9.6, 9.6, 16);
    private static final VoxelShape SOUTH = box(6.4, 6.4, 0, 9.6, 9.6, 8);
    private static final VoxelShape EAST = box(0, 6.4, 6.4, 8, 9.6, 9.6);
    private static final VoxelShape WEST = box(8, 6.4, 6.4, 16, 9.6, 9.6);

    public QuartzSliverBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> FLOOR;
            case DOWN -> CEILING;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
        };
    }
}
