package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.GemSocketMenu;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class GemSocketScreen extends AbstractContainerScreen<GemSocketMenu> {
    private static final ResourceLocation FABRIAL = ArcaneArchives.id("textures/gui/fabrial.png");
    private static final ResourceLocation INVENTORY = ArcaneArchives.id("textures/gui/player_inv.png");
    private static final ResourceLocation SIMPLE_INVENTORY = ArcaneArchives.id("textures/gui/simple/player_inv.png");
    private static final ResourceLocation SINGLE = ArcaneArchives.id("textures/gui/simple/single_slot.png");

    GemSocketScreen(GemSocketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 181;
        imageHeight = 145;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (overRecharge(mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.translatable("gui.arcanearchives.socket.recharge"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean pretty = ArchiveConfig.PRETTY_GUIS.get();
        var inventory = pretty ? INVENTORY : SIMPLE_INVENTORY;
        if (pretty) {
            graphics.blit(inventory, leftPos + 42, topPos + 11, 0, 101, 94, 37);
            graphics.blit(inventory, leftPos - 32, topPos + 106, 207, 0, 49, 33);
            graphics.blit(FABRIAL, leftPos + 78, topPos, 102, 0, 22, 22);
        } else {
            graphics.blit(SINGLE, leftPos - 29, topPos + 109, 32, 0, 28, 28);
            graphics.blit(SINGLE, leftPos + 73, topPos - 5, 0, 0, 32, 32);
        }
        graphics.blit(inventory, leftPos, topPos + 44, 0, 0, 181, 101);
        graphics.blit(INVENTORY, leftPos + 153, topPos + 2, 238, 33, 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && overRecharge(mouseX, mouseY)) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, GemSocketMenu.RECHARGE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overRecharge(double x, double y) {
        return isHovering(153, 2, 18, 18, x, y);
    }
}
