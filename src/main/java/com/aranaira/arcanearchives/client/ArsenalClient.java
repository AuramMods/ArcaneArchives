package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArcaneGemItem;
import com.aranaira.arcanearchives.arsenal.ArsenalGems;
import com.aranaira.arcanearchives.arsenal.ChromaticPowderItem;
import com.aranaira.arcanearchives.arsenal.GemCharge;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

final class ArsenalClient {
    static final KeyMapping SOCKET = new KeyMapping("arcanearchives.gui.keybinds.socket", InputConstants.UNKNOWN.getValue(),
            "arcanearchives.gui.keygroup");
    private static final ResourceLocation HUD = ArcaneArchives.id("textures/gui/fabrial.png");

    private ArsenalClient() {}

    static void setup() {
        MenuScreens.register(ArcaneArchives.GEM_SOCKET_MENU.get(), GemSocketScreen::new);
        ArcaneArchives.GEMS.values().forEach(gem -> {
            ItemProperties.register(gem.get(), ArcaneArchives.id("colourblind"), (stack, level, entity, seed) ->
                    ArchiveConfig.ARSENAL_COLOURBLIND.get() ? 1 : 0);
            ItemProperties.register(gem.get(), ArcaneArchives.id("dun"), (stack, level, entity, seed) ->
                    GemCharge.available(stack, 1) ? 0 : 1);
        });
        ItemProperties.register(ArcaneArchives.CHROMATIC_POWDER.get(), ArcaneArchives.id("powder_color"),
                (stack, level, entity, seed) -> ChromaticPowderItem.color(stack).ordinal() / 10F);
        MinecraftForge.EVENT_BUS.addListener(ArsenalClient::tick);
        MinecraftForge.EVENT_BUS.addListener(ArsenalClient::renderHud);
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var client = Minecraft.getInstance();
        while (SOCKET.consumeClick()) {
            if (client.player != null && client.screen == null && ArchiveConfig.ARSENAL_ENABLED.get()) {
                ArchiveNetwork.sendArsenalControl(true);
            }
        }
    }

    private static void renderHud(RenderGuiEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui || client.screen != null) {
            return;
        }
        var graphics = event.getGuiGraphics();
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int equippedRow = 1;
        for (var gem : ArsenalGems.available(client.player)) {
            var stack = gem.stack();
            if (stack == client.player.getMainHandItem()) {
                drawBar(graphics, centerX, centerY, stack, false, 0);
            } else if (stack == client.player.getOffhandItem()) {
                drawBar(graphics, centerX, centerY, stack, true, 0);
            } else {
                drawBar(graphics, centerX, centerY, stack, false, equippedRow++);
            }
        }
    }

    private static void drawBar(GuiGraphics graphics, int centerX, int centerY, ItemStack stack, boolean left, int row) {
        if (!(stack.getItem() instanceof ArcaneGemItem gem)) {
            return;
        }
        boolean toggle = gem.type().toggle;
        int barX = left ? centerX - (toggle ? 40 : 33) : centerX + 8;
        int barY = centerY + 2 + 12 * row;
        int textureY = toggle ? left ? 144 : 134 : 154;
        graphics.blit(HUD, barX, barY, 0, textureY, 33, 9);
        int palette = 164 + (gem.type().color.ordinal() - 1) * 4;
        int fill = GemCharge.infinite(stack) ? 20 : (int) Math.ceil(GemCharge.remaining(stack) * 20.0 / GemCharge.capacity(stack));
        int fillX = left ? centerX - 30 + 20 - fill : barX + 3;
        if (fill > 0) {
            graphics.blit(HUD, fillX, barY + 3, 3, palette, fill, 3);
        }
        if (toggle && GemCharge.toggled(stack)) {
            graphics.blit(HUD, left ? barX + 3 : barX + 27, barY + 3, 3, palette, 3, 3);
        }
        int gemX = row == 0 ? centerX + (left ? -23 : 8) : barX + 27 + (toggle ? 7 : 0);
        int gemY = row == 0 ? centerY - 16 : barY - 7;
        graphics.blit(HUD, gemX, gemY, left ? 48 : 33, 133 + gem.type().cut.ordinal() * 15, 16, 16);
    }
}
