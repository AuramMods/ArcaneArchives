package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;

public final class RadiantCraftingTableBlock extends BaseEntityBlock {
    public RadiantCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadiantCraftingTableBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && placer instanceof Player && !(placer instanceof FakePlayer)
                && level.getBlockEntity(pos) instanceof RadiantCraftingTableBlockEntity table) {
            table.bindTo(placer.getUUID());
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.RADIANT_CRAFTING_USE)) {
            return InteractionResult.FAIL;
        }
        if (level.getBlockEntity(pos) instanceof RadiantCraftingTableBlockEntity table) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(table);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston) {
        if (!replacement.is(this) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof RadiantCraftingTableBlockEntity table) {
            Containers.dropContents(level, pos, table.grid());
            table.removeFromNetwork();
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }
}
