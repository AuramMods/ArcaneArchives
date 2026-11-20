package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArsenalGems {
    private ArsenalGems() {}

    public record Source(ItemStack stack, Runnable changed) {
        public boolean is(GemType type) {
            return stack.getItem() instanceof ArcaneGemItem gem && gem.type() == type;
        }
    }

    public static List<Source> available(Player player) {
        if (!ArchiveConfig.ARSENAL_ENABLED.get() || player.isSpectator()) {
            return List.of();
        }
        var found = new ArrayList<Source>();
        if (player.getMainHandItem().getItem() instanceof ArcaneGemItem) {
            found.add(held(player, player.getMainHandItem()));
        }
        if (player.getOffhandItem().getItem() instanceof ArcaneGemItem) {
            found.add(held(player, player.getOffhandItem()));
        }
        GemSocketMenu menu = player.containerMenu instanceof GemSocketMenu open && open.stillValid(player) ? open : null;
        if (menu != null && !menu.source().stack().isEmpty()) {
            found.add(menu.source());
        }
        for (var ref : GemSocketItem.equipped(player)) {
            ItemStack socket = ref.stack(player);
            if (menu != null && menu.edits(socket)) {
                continue;
            }
            ItemStack gem = GemSocketItem.gem(socket);
            if (!gem.isEmpty()) {
                found.add(new Source(gem, () -> GemSocketItem.store(socket, gem)));
            }
        }
        return found;
    }

    static Source held(Player player, ItemStack gem) {
        return new Source(gem, player.getInventory()::setChanged);
    }

    public static void toggleHeld(Player player) {
        if (ArchiveConfig.ARSENAL_ENABLED.get() && !player.isSpectator()) {
            ItemStack stack = player.getMainHandItem();
            GemCharge.toggle(stack, !GemCharge.toggled(stack));
        }
    }

    static void spend(Player player, Source gem, int amount) {
        GemCharge.consume(gem.stack(), amount);
        gem.changed().run();
        if (!GemCharge.available(gem.stack(), 1)) {
            GemRecharge.recharge(player, gem);
        }
    }

    static void restore(Source gem, int amount) {
        GemCharge.restore(gem.stack(), amount);
        gem.changed().run();
    }

    static void toggle(Source gem, boolean enabled) {
        GemCharge.toggle(gem.stack(), enabled);
        gem.changed().run();
    }
}
