package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Optional;

public final class RadiantCraftingTableScreen extends AbstractContainerScreen<RadiantCraftingTableMenu> {
    private static final ResourceLocation BACKGROUND = ArcaneArchives.id("textures/gui/radiantcraftingtable.png");
    private static final ResourceLocation SIMPLE_BACKGROUND = ArcaneArchives.id("textures/gui/simple/radiantcraftingtable.png");
    private static final String BOOKMARK = "gui.arcanearchives.radiant_crafting.bookmark.";

    public RadiantCraftingTableScreen(RadiantCraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 203;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int bookmark = hoveredBookmark(mouseX, mouseY);
        if (bookmark < 0) {
            renderTooltip(graphics, mouseX, mouseY);
            return;
        }
        ItemStack saved = menu.bookmarkPreview(bookmark);
        var lines = new ArrayList<Component>();
        if (saved.isEmpty()) {
            lines.add(Component.translatable(BOOKMARK + "save"));
            lines.add(Component.translatable(BOOKMARK + "save_last").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(saved.getHoverName());
            lines.add(Component.translatable(BOOKMARK + (menu.canUseBookmark(bookmark) ? "craft" : "missing"))
                    .withStyle(menu.canUseBookmark(bookmark) ? ChatFormatting.GRAY : ChatFormatting.RED));
            lines.add(Component.translatable(BOOKMARK + "clear").withStyle(ChatFormatting.GRAY));
        }
        graphics.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(ArchiveConfig.PRETTY_GUIS.get() ? BACKGROUND : SIMPLE_BACKGROUND,
                leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int index = hoveredBookmark(mouseX, mouseY);
        if (index >= 0 && menu.bookmarkPreview(index).isEmpty()) {
            ItemStack preview = menu.previewToSave(hasShiftDown());
            if (!preview.isEmpty()) {
                int x = leftPos + RadiantCraftingTableMenu.BOOKMARK_X;
                int y = topPos + bookmarkY(index);
                graphics.renderItem(preview, x, y);
                graphics.renderItemDecorations(font, preview, x, y);
                graphics.fill(x, y, x + 16, y + 16, 200, 0x804C4364);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 16, 10, 0x35293F, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = hoveredBookmark(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                    RadiantCraftingTableMenu.bookmarkButton(index, hasShiftDown()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int hoveredBookmark(double mouseX, double mouseY) {
        for (int index = 0; index < RadiantCraftingTableMenu.BOOKMARK_COUNT; index++) {
            if (isHovering(RadiantCraftingTableMenu.BOOKMARK_X, bookmarkY(index), 16, 16, mouseX, mouseY)) {
                return index;
            }
        }
        return -1;
    }

    private static int bookmarkY(int index) {
        return RadiantCraftingTableMenu.FIRST_BOOKMARK_Y + index * RadiantCraftingTableMenu.BOOKMARK_SPACING;
    }
}
