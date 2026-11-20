package com.aranaira.arcanearchives.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public final class GemCuttersTableBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACCESSOR = BooleanProperty.create("accessor");

    public GemCuttersTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.WEST).setValue(ACCESSOR, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACCESSOR);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getCounterClockWise();
        BlockState main = defaultBlockState().setValue(FACING, facing);
        BlockPos other = partner(context.getClickedPos(), main);
        Level level = context.getLevel();
        BlockPlaceContext otherContext = BlockPlaceContext.at(context, other, context.getClickedFace());
        if (!level.isInWorldBounds(other) || !level.getWorldBorder().isWithinBounds(other)
                || !level.getBlockState(other).canBeReplaced(otherContext)
                || !level.isUnobstructed(accessor(main), other, CollisionContext.empty())) {
            return null;
        }
        return main;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && !state.getValue(ACCESSOR)) {
            level.setBlock(partner(pos, state), accessor(state), Block.UPDATE_ALL);
            if (placer instanceof Player && !(placer instanceof FakePlayer)
                    && level.getBlockEntity(pos) instanceof GemCuttersTableBlockEntity table) {
                table.bindTo(placer.getUUID());
            }
        }
    }

    private static BlockPos partner(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(FACING).getOpposite());
    }

    public static BlockPos inventoryPosition(BlockPos pos, BlockState state) {
        return state.getValue(ACCESSOR) ? partner(pos, state) : pos;
    }

    private static BlockState accessor(BlockState main) {
        return main.setValue(ACCESSOR, true).setValue(FACING, main.getValue(FACING).getOpposite());
    }

    private boolean hasPartner(Level level, BlockPos pos, BlockState state) {
        BlockState other = level.getBlockState(pos);
        return other.is(this) && other.getValue(ACCESSOR) != state.getValue(ACCESSOR)
                && other.getValue(FACING) == state.getValue(FACING).getOpposite();
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(ACCESSOR)) {
            BlockPos other = partner(pos, state);
            if (hasPartner(level, other, state)) {
                level.destroyBlock(other, false, player);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!replacement.is(this) && !level.isClientSide) {
            boolean accessor = state.getValue(ACCESSOR);
            if (!accessor && level.getBlockEntity(pos) instanceof GemCuttersTableBlockEntity table) {
                for (int slot = 0; slot < table.drawer().getSlots(); slot++) {
                    ItemStack stack = table.drawer().getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                    }
                }
                table.removeFromNetwork();
            }
            BlockPos other = partner(pos, state);
            if (hasPartner(level, other, state)) {
                level.destroyBlock(other, accessor);
            }
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        BlockPos main = inventoryPosition(pos, state);
        if (level.getBlockEntity(main) instanceof GemCuttersTableBlockEntity table) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, table, main);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(ACCESSOR) ? null : new GemCuttersTableBlockEntity(pos, state);
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
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
