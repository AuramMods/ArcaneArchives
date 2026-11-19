package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public final class RadiantChestBlock extends BaseEntityBlock {
    public RadiantChestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiantChestBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof RadiantChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }
        if (configureDisplay(chest, player, hit.getDirection())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, chest, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static boolean configureDisplay(RadiantChestBlockEntity chest, @Nullable Player player, Direction face) {
        if (player == null || player.isSpectator()) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        var scepter = ArcaneArchives.SCEPTER_MANIPULATION.get();
        if (!main.is(scepter) && !off.is(scepter)) {
            return false;
        }
        ItemStack label = main.is(scepter) ? off : main;
        if (label.isEmpty() ? !player.isShiftKeyDown() : !face.getAxis().isHorizontal()) {
            return false;
        }
        if (!player.level().isClientSide) {
            chest.setDisplay(label, face);
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RadiantChestBlockEntity chest) {
            if (placer instanceof Player && !(placer instanceof FakePlayer)) {
                chest.bindTo(placer.getUUID());
                chest.setExistingItemsOnly(ArchiveConfig.CHESTS_EXISTING_ITEMS_ONLY.get());
            }
            if (stack.hasCustomHoverName()) {
                chest.setCustomName(stack.getHoverName());
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof RadiantChestBlockEntity chest) {
            RadiantChestInventory inventory = chest.inventory();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack.copy());
                }
            }
            chest.removeFromNetwork();
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RadiantChestBlockEntity chest ? chest.inventory().comparatorSignal() : 0;
    }
}
