package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.BrazierSettingsMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class BrazierSettingsScreen extends AbstractContainerScreen<BrazierSettingsMenu> {
    private static final ResourceLocation TEXTURE = ArcaneArchives.id("textures/gui/brazier_hoarding.png");
    private static final String TEXT = "gui.arcanearchives.brazier.";
    private EditBox radius;
    private boolean syncing;
    private int lastRadius;

    public BrazierSettingsScreen(BrazierSettingsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 98;
        imageHeight = 41;
    }

    @Override
    protected void init() {
        super.init();
        radius = new EditBox(font, leftPos + 45, topPos + 9, 31, 12, Component.translatable(TEXT + "radius"));
        radius.setBordered(false);
        radius.setMaxLength(3);
        radius.setFilter(input -> input.chars().allMatch(character -> character >= '0' && character <= '9'));
        setRadiusField(menu.radius());
        radius.setResponder(input -> {
            if (!syncing && !input.isEmpty()) {
                ArchiveNetwork.setBrazierRadius(menu.containerId, Integer.parseInt(input));
            }
        });
        addRenderableWidget(radius);
    }

    @Override
    protected void containerTick() {
        radius.tick();
        int current = menu.radius();
        if (current != lastRadius) {
            lastRadius = current;
            if (!radius.isFocused()) {
                setRadiusField(current);
            }
        }
    }

    private void setRadiusField(int value) {
        syncing = true;
        radius.setValue(Integer.toString(value));
        syncing = false;
    }

    private GlobalPos location() {
        return GlobalPos.of(minecraft.level.dimension(), menu.position());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        String control = controlAt(mouseX, mouseY);
        if (control != null) {
            graphics.renderTooltip(font, Component.translatable(TEXT + control), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (ArchiveConfig.PRETTY_GUIS.get()) {
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        } else {
            graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF484848);
            graphics.drawString(font, "−", leftPos + 30, topPos + 8, 0xFFFFFF, true);
            graphics.drawString(font, "+", leftPos + 85, topPos + 8, 0xFFFFFF, true);
            graphics.renderOutline(leftPos + 68, topPos + 28, 12, 12, 0xFFCCCCCC);
        }
        graphics.blit(TEXTURE, leftPos + 7, topPos + 8, BrazierRangePreview.showing(location()) ? 222 : 210,
                0, 12, 10);
        if (menu.personalOnly()) {
            graphics.blit(TEXTURE, leftPos + 70, topPos + 30, 234, 0, 6, 6);
            graphics.blit(TEXTURE, leftPos + 82, topPos + 25, 240, 0, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String control = controlAt(mouseX, mouseY);
        if (button == 0 && control != null) {
            radius.setFocused(false);
            switch (control) {
                case "shrink" -> clickSettings(BrazierSettingsMenu.SHRINK);
                case "expand" -> clickSettings(BrazierSettingsMenu.EXPAND);
                case "personal", "network" -> clickSettings(BrazierSettingsMenu.TOGGLE_NETWORK);
                case "show", "hide" -> BrazierRangePreview.toggle(location());
                default -> throw new IllegalStateException(control);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (radius.isFocused() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            radius.setFocused(false);
            setRadiusField(menu.radius());
            return true;
        }
        if (key != GLFW.GLFW_KEY_ESCAPE && radius.isFocused()
                && (radius.keyPressed(key, scanCode, modifiers) || radius.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void clickSettings(int action) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    private String controlAt(double mouseX, double mouseY) {
        if (isHovering(29, 2, 8, 16, mouseX, mouseY)) {
            return "shrink";
        }
        if (isHovering(85, 2, 8, 16, mouseX, mouseY)) {
            return "expand";
        }
        if (isHovering(67, 27, 12, 12, mouseX, mouseY)) {
            return menu.personalOnly() ? "personal" : "network";
        }
        if (isHovering(6, 6, 14, 14, mouseX, mouseY)) {
            return BrazierRangePreview.showing(location()) ? "hide" : "show";
        }
        return null;
    }
}
