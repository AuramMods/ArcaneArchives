package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public final class RadiantChestScreen extends AbstractContainerScreen<RadiantChestMenu> {
    private static final ResourceLocation BACKGROUND = ArcaneArchives.id("textures/gui/radiantchest.png");
    private static final ResourceLocation SIMPLE_BACKGROUND = ArcaneArchives.id("textures/gui/simple/radiantchest.png");
    private static final int LABEL_COLOR = 0xFFE7CC;
    private EditBox chestName;
    private boolean syncingName;

    public RadiantChestScreen(RadiantChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 253;
        titleLabelX = 16;
        titleLabelY = 5;
        inventoryLabelX = 16;
        inventoryLabelY = 132;
    }

    @Override
    protected void init() {
        super.init();
        chestName = new EditBox(font, leftPos + 53, topPos + 238, 88, 10,
                Component.translatable("gui.arcanearchives.radiant_chest.name"));
        chestName.setBordered(false);
        chestName.setMaxLength(RadiantChestMenu.NAME_LIMIT);
        chestName.setTextColor(ArchiveConfig.PRETTY_GUIS.get() ? 0xFFFFFF : 0x404040);
        chestName.setHint(Component.translatable("arcanearchives.text.radiantchest.unnamed_chest"));
        chestName.setValue(menu.customNameText());
        chestName.setResponder(name -> {
            if (!syncingName) {
                ArchiveNetwork.renameChest(menu.containerId, name);
            }
        });
        addRenderableWidget(chestName);
    }

    @Override
    protected void containerTick() {
        chestName.tick();
        String name = menu.customNameText();
        if (!chestName.isFocused() && !name.equals(chestName.getValue())) {
            syncingName = true;
            chestName.setValue(name);
            syncingName = false;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (overRouting(mouseX, mouseY)) {
            String key = menu.existingItemsOnly() ? "nonewitems" : "any";
            String tooltip = "arcanearchives.tooltip.radiantchest.routingmode.";
            var lines = new ArrayList<Component>();
            lines.add(Component.translatable(tooltip + key + "1"));
            lines.add(Component.translatable(tooltip + key + "2"));
            if (!menu.routingAllowed()) {
                lines.add(Component.translatable("gui.arcanearchives.radiant_chest.routing_denied"));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int color = ArchiveConfig.PRETTY_GUIS.get() ? LABEL_COLOR : 0x404040;
        String title = font.plainSubstrByWidth(menu.chestTitle().getString(), imageWidth - 2 * titleLabelX);
        graphics.drawString(font, title, titleLabelX, titleLabelY, color, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, color, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation background = ArchiveConfig.PRETTY_GUIS.get() ? BACKGROUND : SIMPLE_BACKGROUND;
        graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.existingItemsOnly()) {
            graphics.blit(background, leftPos + 164, topPos + 239, 234, 0, 6, 6);
            graphics.blit(background, leftPos + 176, topPos + 234, 240, 0, 16, 16);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (chestName.isMouseOver(mouseX, mouseY)) {
            if (button == 1) {
                chestName.setValue("");
                return true;
            }
        } else {
            chestName.setFocused(false);
        }
        if (button == 0 && overRouting(mouseX, mouseY)) {
            if (menu.routingAllowed()) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RadiantChestMenu.TOGGLE_ROUTING);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (chestName.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            chestName.setFocused(false);
            return true;
        }
        if (key != GLFW.GLFW_KEY_ESCAPE && chestName.isFocused()
                && (chestName.keyPressed(key, scanCode, modifiers) || chestName.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private boolean overRouting(double mouseX, double mouseY) {
        return isHovering(159, 234, 33, 16, mouseX, mouseY);
    }
}
