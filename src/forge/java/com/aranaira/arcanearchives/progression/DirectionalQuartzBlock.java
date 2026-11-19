package com.aranaira.arcanearchives.progression;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DirectionalQuartzBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private final VoxelShape xShape;
    private final VoxelShape yShape;
    private final VoxelShape zShape;

    public DirectionalQuartzBlock(Properties properties) {
        this(properties, 3.2);
    }

    public DirectionalQuartzBlock(Properties properties, double inset) {
        super(properties);
        double edge = 16 - inset;
        xShape = box(0, inset, inset, 16, edge, edge);
        yShape = box(inset, 0, inset, edge, 16, edge);
        zShape = box(inset, inset, 0, edge, edge, 16);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> xShape;
            case Y -> yShape;
            case Z -> zShape;
        };
    }
}
