package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

public final class ManifestLecternBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACCESSOR = BooleanProperty.create("accessor");

    public ManifestLecternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST).setValue(ACCESSOR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACCESSOR);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos above = context.getClickedPos().above();
        Level level = context.getLevel();
        BlockState lower = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getCounterClockWise());
        BlockPlaceContext upperContext = BlockPlaceContext.at(context, above, Direction.UP);
        if (!level.isInWorldBounds(above) || !level.getWorldBorder().isWithinBounds(above)
                || !level.getBlockState(above).canBeReplaced(upperContext)
                || !level.isUnobstructed(lower.setValue(ACCESSOR, true), above, CollisionContext.empty())) {
            return null;
        }
        return lower;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && !state.getValue(ACCESSOR)) {
            level.setBlock(pos.above(), state.setValue(ACCESSOR, true), UPDATE_ALL);
        }
    }

    static boolean isComplete(Level level, BlockPos base) {
        BlockState bottom = level.getBlockState(base);
        BlockState top = level.getBlockState(base.above());
        return bottom.is(ArcaneArchives.MANIFEST_LECTERN.get()) && !bottom.getValue(ACCESSOR)
                && top == bottom.setValue(ACCESSOR, true);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        BlockPos base = state.getValue(ACCESSOR) ? pos.below() : pos;
        if (!isComplete(level, base)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ManifestItem.open(serverPlayer, ContainerLevelAccess.create(level, base));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(ACCESSOR) && isComplete(level, pos.below())) {
            level.destroyBlock(pos.below(), false, player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!level.isClientSide && !replacement.is(this)) {
            boolean upper = state.getValue(ACCESSOR);
            BlockPos other = upper ? pos.below() : pos.above();
            if (level.getBlockState(other) == state.setValue(ACCESSOR, !upper)) {
                level.destroyBlock(other, upper);
            }
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(ACCESSOR) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }
}
