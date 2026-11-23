package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

import javax.annotation.Nullable;
import java.util.List;

import static com.aranaira.arcanearchives.arsenal.NaturalRechargeRule.Trigger.*;

public final class ArcaneGemItem extends Item {
    private final GemType type;

    public ArcaneGemItem(GemType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public GemType type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!ArchiveConfig.ARSENAL_ENABLED.get() || player.isSpectator()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer server) {
            GemCasting.cast(server, type, ArsenalGems.held(player, stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!ArchiveConfig.ARSENAL_ENABLED.get() || context.getPlayer() == null || context.getPlayer().isSpectator()
                || (type != GemType.MUNCHSTONE && type != GemType.ORDERSTONE)) {
            return InteractionResult.PASS;
        }
        return GemCasting.onBlock(context, type);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return ArchiveConfig.ARSENAL_ENABLED.get() && switch (type) {
            case SLAUGHTERGLEAM, MURDERGLEAM, MUNCHSTONE, ORDERSTONE, STORMWAY -> false;
            default -> true;
        };
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide && ArchiveConfig.ARSENAL_ENABLED.get()
                && GemCharge.remaining(stack) < GemCharge.capacity(stack)) {
            var recharged = stack.copy();
            var source = new ArsenalGems.Source(recharged, () -> entity.setItem(recharged));
            var owner = entity.getOwner() instanceof Player player ? player : null;
            var level = entity.level();
            if ((entity.isInLava() && NaturalRecharge.apply(level, owner, source, DROPPED_IN_LAVA, 0))
                    || (entity.isInWater() && NaturalRecharge.apply(level, owner, source, DROPPED_IN_WATER, 0))
                    || (level.isRainingAt(entity.blockPosition()) && NaturalRecharge.apply(level, owner, source, DROPPED_IN_RAIN, 0))) {
                entity.level().playSound(null, entity.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, .5F);
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (type.toggle) {
            tooltip.add(Component.translatable("gui.arcanearchives.gem." + (GemCharge.toggled(stack) ? "on" : "off")));
        }
        tooltip.add(GemCharge.infinite(stack) ? Component.translatable("gui.arcanearchives.gem.unlimited")
                : Component.translatable("gui.arcanearchives.gem.charge", GemCharge.remaining(stack), GemCharge.capacity(stack)));
        tooltip.add(Component.translatable("arcanearchives.tooltip.gem." + type.id()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("arcanearchives.tooltip.gem.recharge." + type.id()).withStyle(ChatFormatting.GRAY));
    }
}
