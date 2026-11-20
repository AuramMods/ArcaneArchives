package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.StorageUpgradesMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

final class StorageUpgradesScreen extends AbstractContainerScreen<StorageUpgradesMenu> {
    private static final ResourceLocation UPGRADES = ArcaneArchives.id("textures/gui/radiant_upgrades.png");
    private static final ResourceLocation INVENTORY = ArcaneArchives.id("textures/gui/player_inv.png");
    private static final ResourceLocation SIMPLE_UPGRADES = ArcaneArchives.id("textures/gui/simple/radiant_upgrades.png");
    private static final ResourceLocation SIMPLE_INVENTORY = ArcaneArchives.id("textures/gui/simple/player_inv.png");

    StorageUpgradesScreen(StorageUpgradesMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 181;
        imageHeight = StorageUpgradesMenu.PLAYER_TOP + 101;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        for (int i = 0; i < 3; i++) {
            int x = StorageUpgradesMenu.FIRST_UPGRADE_X + i * StorageUpgradesMenu.UPGRADE_SPACING;
            if (!menu.installed(i) && isHovering(x, 8, 16, 16, mouseX, mouseY)) {
                graphics.renderComponentTooltip(font, List.of(menu.expectedUpgrade(i).getDescription(),
                        Component.translatable("gui.arcanearchives.storage_upgrades.capacity", menu.tierMultiplier(i))
                                .withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                break;
            }
            if (!menu.installed(i + 3) && isHovering(x, 44, 16, 16, mouseX, mouseY)) {
                graphics.renderTooltip(font, Component.translatable("gui.arcanearchives.storage_upgrades."
                        + (menu.acceptsLockUpgrade() ? "trove_options" : "tank_options")), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean pretty = ArchiveConfig.PRETTY_GUIS.get();
        graphics.blit(pretty ? UPGRADES : SIMPLE_UPGRADES, leftPos + 49, topPos, 0, 0, 82, 32);
        graphics.blit(pretty ? UPGRADES : SIMPLE_UPGRADES, leftPos + 49, topPos + 36, 0, 32, 82, 32);
        graphics.blit(pretty ? INVENTORY : SIMPLE_INVENTORY, leftPos, topPos + StorageUpgradesMenu.PLAYER_TOP, 0, 0, 181, 101);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
