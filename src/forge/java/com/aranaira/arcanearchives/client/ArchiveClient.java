package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ArcaneArchives.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArchiveClient {
    private ArchiveClient() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ArcaneArchives.RADIANT_CHEST_MENU.get(), RadiantChestScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ArcaneArchives.RADIANT_TANK_ENTITY.get(), RadiantTankRenderer::new);
    }

    public static void receiveChestContents(ArchiveNetwork.Contents packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RadiantChestMenu chest && chest.containerId == packet.menuId()) {
            chest.initializeContents(packet.stateId(), packet.stacks(), packet.carried());
        }
    }

    public static void receiveChestSlot(ArchiveNetwork.SlotChange packet) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RadiantChestMenu chest && chest.containerId == packet.menuId()
                && packet.slot() >= 0 && packet.slot() < chest.slots.size()) {
            chest.setItem(packet.slot(), packet.stateId(), packet.stack());
        }
    }
}
