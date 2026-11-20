package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.DevouringCharmMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class DevouringCharmScreen extends AbstractContainerScreen<DevouringCharmMenu> {
    private static final ResourceLocation CHARM = ArcaneArchives.id("textures/gui/devouring_charm.png");
    private static final ResourceLocation SIMPLE_CHARM = ArcaneArchives.id("textures/gui/simple/devouring_charm.png");
    private static final ResourceLocation INVENTORY = ArcaneArchives.id("textures/gui/player_inv.png");
    private static final ResourceLocation SIMPLE_INVENTORY = ArcaneArchives.id("textures/gui/simple/player_inv.png");
    private static final String TEXT = "gui.arcanearchives.devouring_charm.";

    DevouringCharmScreen(DevouringCharmMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 181;
        imageHeight = 194;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (overFlip(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable(TEXT + (menu.flipped() ? "front" : "back")), mouseX, mouseY);
        } else if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot.index >= DevouringCharmMenu.FIRST_FILTER_SLOT) {
                graphics.renderTooltip(font, Component.translatable(TEXT + "sample"), mouseX, mouseY);
            } else if (hoveredSlot.index >= DevouringCharmMenu.FIRST_DISCARD_SLOT) {
                graphics.renderTooltip(font, Component.translatable(TEXT + "discard"), mouseX, mouseY);
            } else if (hoveredSlot.index == DevouringCharmMenu.FLUID_SLOT) {
                graphics.renderTooltip(font, Component.translatable(TEXT + "fluid"), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean pretty = ArchiveConfig.PRETTY_GUIS.get();
        ResourceLocation charm = pretty ? CHARM : SIMPLE_CHARM;
        int u = menu.flipped() ? 126 : 0;
        int v = menu.flipped() ? 127 : 0;
        graphics.blit(charm, leftPos + 25, topPos, u, v, 130, 129);
        graphics.blit(charm, leftPos, topPos + 2, 231, 0, 25, 15);
        graphics.blit(pretty ? INVENTORY : SIMPLE_INVENTORY, leftPos, topPos + 93, 0, 0, 181, 101);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overFlip(mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, DevouringCharmMenu.FLIP);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overFlip(double x, double y) {
        return isHovering(0, 2, 25, 15, x, y);
    }
}
