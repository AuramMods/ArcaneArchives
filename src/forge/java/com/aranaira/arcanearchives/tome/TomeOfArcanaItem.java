package com.aranaira.arcanearchives.tome;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.client.tome.TomeOfArcanaScreen;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public final class TomeOfArcanaItem extends Item {
    public static final ResourceLocation BOOK = ArcaneArchives.id("xml/tome.xml");
    private static final String RECEIVED_TOME = "arcanearchives:received_tome";

    public TomeOfArcanaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> TomeOfArcanaScreen::open);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.tome_arcana").withStyle(ChatFormatting.GOLD));
    }

    public static ItemStack tome() {
        var result = new ItemStack(ArcaneArchives.TOME_OF_ARCANA.get());
        result.getOrCreateTag().putString("Book", BOOK.toString());
        return result;
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (ArchiveConfig.TOME_FROM_BOOKSHELF.get() && event.getState().is(Blocks.BOOKSHELF)
                && event.getPlayer() instanceof ServerPlayer player) {
            offerTome(player, "bookshelf");
        }
    }

    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getCrafting().is(ArcaneArchives.TOME_OF_ARCANA.get())) {
                markReceived(player);
            } else if (ArchiveConfig.TOME_FROM_RESONATOR.get() && event.getCrafting().is(ArcaneArchives.RADIANT_RESONATOR_ITEM.get())) {
                offerTome(player, "resonator");
            }
        }
    }

    private static boolean hasReceived(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(RECEIVED_TOME);
    }

    private static void markReceived(Player player) {
        CompoundTag persistent = player.getPersistentData();
        CompoundTag record = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        record.putBoolean(RECEIVED_TOME, true);
        persistent.put(Player.PERSISTED_NBT_TAG, record);
    }

    private static void offerTome(ServerPlayer player, String source) {
        if (hasReceived(player)) {
            return;
        }
        var level = player.serverLevel();
        var entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), tome());
        entity.setNoPickUpDelay();
        entity.setTarget(player.getUUID());
        if (level.addFreshEntity(entity)) {
            markReceived(player);
            player.sendSystemMessage(Component.translatable("arcanearchives.message.book_received." + source)
                    .withStyle(ChatFormatting.GOLD));
            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1, 1);
        }
    }
}
