package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class RadiantChestScreen extends AbstractContainerScreen<RadiantChestMenu> {
    private static final ResourceLocation BACKGROUND = ArcaneArchives.id("textures/gui/radiantchest.png");
    private static final int LABEL_COLOR = 0xFFE7CC;

    public RadiantChestScreen(RadiantChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 222;
        titleLabelX = 16;
        titleLabelY = 5;
        inventoryLabelX = 16;
        inventoryLabelY = 132;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
