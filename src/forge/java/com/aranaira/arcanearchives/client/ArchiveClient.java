package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.RadiantAmphoraItem;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ArcaneArchives.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArchiveClient {
    private ArchiveClient() {}

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ManifestClient.KEY);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ArcaneArchives.RADIANT_CHEST_MENU.get(), RadiantChestScreen::new);
            MenuScreens.register(ArcaneArchives.GEMCUTTERS_TABLE_MENU.get(), GemCuttersTableScreen::new);
            MenuScreens.register(ArcaneArchives.RADIANT_CRAFTING_TABLE_MENU.get(), RadiantCraftingTableScreen::new);
            ItemProperties.register(ArcaneArchives.RADIANT_AMPHORA.get(), ArcaneArchives.id("amphora_mode"),
                    (stack, level, entity, seed) -> RadiantAmphoraItem.linked(stack)
                            ? (RadiantAmphoraItem.drainMode(stack) ? 2.0F : 1.0F) : 0.0F);
            MenuScreens.register(ArcaneArchives.MANIFEST_MENU.get(), ManifestScreen::new);
            ManifestClient.registerListeners();
            MenuScreens.register(ArcaneArchives.BRAZIER_SETTINGS_MENU.get(), BrazierSettingsScreen::new);
            MenuScreens.register(ArcaneArchives.STORAGE_UPGRADES_MENU.get(), StorageUpgradesScreen::new);
            MinecraftForge.EVENT_BUS.addListener(BrazierRangePreview::render);
            MinecraftForge.EVENT_BUS.addListener(BrazierRangePreview::logout);
            MinecraftForge.EVENT_BUS.addListener(ArchiveClient::onLeftClickEmpty);
        });
    }

    private static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (Minecraft.getInstance().getConnection() != null && event.getEntity().getMainHandItem().is(ArcaneArchives.RADIANT_AMPHORA.get())) {
            ArchiveNetwork.sendAmphoraToggle();
        }
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
