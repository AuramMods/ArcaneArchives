package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.violetmoon.quark.api.IQuarkButtonAllowed;

final class QuarkChestScreen extends RadiantChestScreen implements IQuarkButtonAllowed {
    private QuarkChestScreen(RadiantChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected int titleWidth() {
        return super.titleWidth() - 24;
    }

    static void register() {
        MenuScreens.register(ArcaneArchives.RADIANT_CHEST_MENU.get(), QuarkChestScreen::new);
    }
}
