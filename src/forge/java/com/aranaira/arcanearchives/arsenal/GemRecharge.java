package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class GemRecharge {
    private GemRecharge() {}

    static boolean recharge(Player player, ArsenalGems.Source source) {
        ItemStack gem = source.stack();
        if (!(gem.getItem() instanceof ArcaneGemItem item) || GemCharge.infinite(gem)
                || GemCharge.remaining(gem) >= GemCharge.capacity(gem)) {
            return false;
        }
        if (GemCharge.remaining(gem) == 0) {
            boolean used = switch (item.type()) {
                case SLAUGHTERGLEAM -> consume(player, source, Items.GOLD_NUGGET, 5, 12, false);
                case MUNCHSTONE -> consume(player, source, Items.BONE_MEAL, 5, 12, false);
                case CLEANSEGLEAM -> consume(player, source, Items.MILK_BUCKET, 1, -1, true);
                case MINDSPINDLE -> consume(player, source, Items.BOOK, 1, -1, false);
                case ELIXIRSPINDLE -> consume(player, source, Items.NETHER_WART, 5, 1, false);
                case PHOENIXWAY -> consume(player, source, Items.GUNPOWDER, 3, 25, false);
                default -> false;
            };
            if (used) {
                return true;
            }
        }

        ItemStack rainbow = ItemStack.EMPTY;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ArcaneArchives.CHROMATIC_POWDER.get()) && ChromaticPowderItem.color(stack) == item.type().color) {
                return use(player, source, stack, 1, -1, false);
            }
            if (stack.is(ArcaneArchives.FULL_SPECTRUM_POWDER.get())) {
                rainbow = stack;
            }
        }
        return !rainbow.isEmpty() && use(player, source, rainbow, 1, -1, false);
    }

    private static boolean consume(Player player, ArsenalGems.Source source, Item item, int maximum, int charge, boolean bucket) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return use(player, source, stack, Math.min(maximum, stack.getCount()), charge, bucket);
            }
        }
        return false;
    }

    private static boolean use(Player player, ArsenalGems.Source source, ItemStack stack, int count, int charge, boolean bucket) {
        Component ingredient = stack.getHoverName();
        stack.shrink(count);
        if (bucket) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.BUCKET, count));
        }
        ArsenalGems.restore(source, charge < 0 ? -1 : charge * count);
        player.displayClientMessage(Component.translatable("message.arcanearchives.gem.recharged", count, ingredient,
                source.stack().getHoverName()), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, 1);
        return true;
    }
}
