package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class GemRecharge {
    private GemRecharge() {}

    static boolean recharge(Player player, ArsenalGems.Source source) {
        ItemStack gem = source.stack();
        int charge = GemCharge.remaining(gem);
        int missing = GemCharge.capacity(gem) - charge;
        if (!(gem.getItem() instanceof ArcaneGemItem item) || GemCharge.infinite(gem) || missing <= 0) {
            return false;
        }
        var level = player.level();
        var rules = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.INVENTORY_RECHARGE_TYPE.get()).stream()
                .sorted(InventoryRechargeRule.ORDER).toList();
        for (var rule : rules) {
            if ((rule.emptyOnly() && charge != 0) || !rule.gems().matches(level, item.type())) {
                continue;
            }
            ItemStack input = findIngredient(player, item.type(), rule);
            if (!input.isEmpty()) {
                return ArchiveStages.allowsGemRecharge(level, player, item.type(), ArchiveStages.INVENTORY_RECHARGE, rule.id())
                        && use(player, source, input, rule, missing);
            }
        }
        return false;
    }

    private static ItemStack findIngredient(Player player, GemType gem, InventoryRechargeRule rule) {
        var items = player.getInventory().items;
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(rule.preferLastSlot() ? items.size() - 1 - i : i);
            if (rule.matches(player.level(), gem, stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean use(Player player, ArsenalGems.Source source, ItemStack stack, InventoryRechargeRule rule, int missing) {
        int count = Math.min(rule.maximum(), stack.getCount());
        Component ingredient = stack.getHoverName();
        stack.shrink(count);
        ItemStack remainder = rule.remainder();
        if (!remainder.isEmpty()) {
            int amount = remainder.getCount() * count;
            while (amount > 0) {
                int batch = Math.min(remainder.getMaxStackSize(), amount);
                player.getInventory().placeItemBackInInventory(remainder.copyWithCount(batch));
                amount -= batch;
            }
        }
        int restored = rule.chargePerItem() == -1 ? missing : (int) Math.min(missing, (long) rule.chargePerItem() * count);
        ArsenalGems.restore(source, restored);
        player.displayClientMessage(Component.translatable("message.arcanearchives.gem.recharged", count, ingredient,
                source.stack().getHoverName()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, 1);
        return true;
    }
}
