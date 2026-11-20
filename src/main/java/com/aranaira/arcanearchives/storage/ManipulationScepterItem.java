package com.aranaira.arcanearchives.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public final class ManipulationScepterItem extends Item {
    public ManipulationScepterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var entity = level.getBlockEntity(pos);
        if (entity instanceof RadiantChestBlockEntity chest
                && RadiantChestBlock.configureDisplay(chest, context.getPlayer(), context.getClickedFace())) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (entity instanceof PortableStorageBlockEntity storage) {
            if (context.getPlayer() instanceof ServerPlayer player) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                        new StorageUpgradesMenu(id, inventory, storage), storage.getBlockState().getBlock().getName()),
                        buffer -> {
                            buffer.writeBlockPos(pos);
                            buffer.writeBoolean(storage instanceof RadiantTankBlockEntity);
                        });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(entity instanceof BrazierBlockEntity brazier)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() instanceof ServerPlayer player) {
            if (brazier.canConfigure(player)) {
                NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                        new BrazierSettingsMenu(id, inventory, brazier), brazier.getBlockState().getBlock().getName()), pos);
            } else {
                player.displayClientMessage(Component.translatable("message.arcanearchives.brazier.denied"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.scepter_manipulation").withStyle(ChatFormatting.GOLD));
    }
}
