package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

final class NaturalRecharge {
    private NaturalRecharge() {}

    static boolean apply(Level level, @Nullable Player player, ArsenalGems.Source gem, NaturalRechargeRule.Trigger trigger,
                         double eventAmount) {
        var rule = find(level, player, gem.stack(), trigger);
        if (rule == null) {
            return false;
        }
        restore(gem, rule.settings(), eventAmount);
        return true;
    }

    static void tick(Player player, ArsenalGems.Source gem) {
        if (GemCharge.remaining(gem.stack()) >= GemCharge.capacity(gem.stack())) {
            return;
        }
        var rule = find(player.level(), player, gem.stack(), NaturalRechargeRule.Trigger.TIMER);
        if (rule == null) {
            return;
        }
        var tag = gem.stack().getOrCreateTag();
        int period = rule.settings().ticks();
        if (!tag.contains("recharge", Tag.TAG_ANY_NUMERIC)) {
            tag.putInt("recharge", period);
            gem.changed().run();
            return;
        }
        int remaining = Math.min(period, tag.getInt("recharge"));
        if (remaining <= 1) {
            restore(gem, rule.settings(), 0);
            tag.putInt("recharge", period);
        } else {
            tag.putInt("recharge", remaining - 1);
        }
        gem.changed().run();
    }

    @Nullable
    private static NaturalRechargeRule find(Level level, @Nullable Player actor, ItemStack stack, NaturalRechargeRule.Trigger trigger) {
        if (!(stack.getItem() instanceof ArcaneGemItem item) || GemCharge.infinite(stack)) {
            return null;
        }
        var rule = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.NATURAL_RECHARGE_TYPE.get()).stream()
                .filter(candidate -> candidate.settings().trigger() == trigger && candidate.settings().gems().matches(level, item.type()))
                .min(NaturalRechargeRule.ORDER).orElse(null);
        return rule != null && ArchiveStages.allowsGemRecharge(level, actor, item.type(), ArchiveStages.NATURAL_RECHARGE, rule.id())
                ? rule : null;
    }

    private static void restore(ArsenalGems.Source gem, NaturalRechargeRule.Settings rule, double eventAmount) {
        ArsenalGems.restore(gem, rule.restoredCharge(eventAmount));
        if (rule.toggleWhenFull() && GemCharge.remaining(gem.stack()) == GemCharge.capacity(gem.stack())) {
            ArsenalGems.toggle(gem, true);
        }
    }
}
