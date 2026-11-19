package com.aranaira.arcanearchives.monitoring;

import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.util.List;

public final class MonitoringCrystalBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape BELOW = box(5.6, -1.6, 5.6, 10.4, 0.8, 10.4);
    private static final VoxelShape ABOVE = box(5.6, 15.2, 5.6, 10.4, 17.6, 10.4);
    private static final VoxelShape NORTH_SIDE = box(5.6, 5.6, -1.6, 10.4, 10.4, 0.8);
    private static final VoxelShape SOUTH_SIDE = box(5.6, 5.6, 15.2, 10.4, 10.4, 17.6);
    private static final VoxelShape WEST_SIDE = box(-1.6, 5.6, 5.6, 0.8, 10.4, 10.4);
    private static final VoxelShape EAST_SIDE = box(15.2, 5.6, 5.6, 17.6, 10.4, 10.4);

    public MonitoringCrystalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> BELOW;
            case DOWN -> ABOVE;
            case SOUTH -> NORTH_SIDE;
            case NORTH -> SOUTH_SIDE;
            case EAST -> WEST_SIDE;
            case WEST -> EAST_SIDE;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonitoringCrystalBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player && !(placer instanceof FakePlayer)
                && level.getBlockEntity(pos) instanceof MonitoringCrystalBlockEntity crystal) {
            crystal.bindTo(placer.getUUID());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!replacement.is(this) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MonitoringCrystalBlockEntity crystal) {
            crystal.removeFromNetwork();
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }

    public static final class CrystalItem extends BlockItem {
        public CrystalItem(MonitoringCrystalBlock block, Properties properties) {
            super(block, properties);
        }

        @Override
        public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            Level level = context.getLevel();
            if (level.getBlockEntity(context.getClickedPos()) instanceof ArchiveDeviceBlockEntity) {
                if (!level.isClientSide && context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("arcanearchives.message.invalid_crystal_target").withStyle(ChatFormatting.GOLD), true);
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("arcanearchives.tooltip.device.monitoring_crystal").withStyle(ChatFormatting.GOLD));
        }
    }
}
