package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlock;
import com.aranaira.arcanearchives.integration.jei.ArchiveJeiPlugin;
import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import com.aranaira.arcanearchives.manifest.ManifestContents;
import com.aranaira.arcanearchives.manifest.ManifestMenu;
import com.aranaira.arcanearchives.manifest.ManifestSync;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ManifestClient {
    public static final KeyMapping KEY = new KeyMapping("arcanearchives.gui.keybinds.manifest",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "arcanearchives.gui.keygroup");

    private static final long TRACKING_DURATION = 10_000;
    private static final long OPEN_WINDOW = 1_500;
    private static final Map<GlobalPos, Map<ManifestContents.Variant, Long>> TRACKED = new LinkedHashMap<>();
    private static final Map<GlobalPos, Integer> DIAGNOSTICS = new LinkedHashMap<>();
    private static final List<ManifestContents.Row> EXACT_MATCHES = new ArrayList<>();
    private static boolean holdingManifest;
    private static List<ItemStack> inspected = List.of();
    private static List<GlobalPos> pendingInspection = List.of();
    private static Screen inspectedScreen;
    private static long inspectUntil;

    private ManifestClient() {}

    static void registerListeners() {
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(ManifestClient::clientTick);
        bus.addListener(ManifestClient::screenKey);
        bus.addListener(EventPriority.LOWEST, ManifestClient::inspectInventory);
        bus.addListener(EventPriority.LOWEST, ManifestClient::containerOpening);
        bus.addListener(ManifestClient::renderInspectedItems);
        bus.addListener(ManifestClient::onLogout);
        bus.addListener(ManifestTrackingRenderer::renderWorld);
        bus.addListener(ManifestTrackingRenderer::renderGui);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft client = Minecraft.getInstance();
            long now = Util.getMillis();
            updateTracking(client, now);
            if (inspectedScreen != null && inspectedScreen != client.screen) {
                clearInspection();
            }
            if (now >= inspectUntil) {
                pendingInspection = List.of();
            }
            while (KEY.consumeClick()) {
                if (client.player != null && client.screen == null) {
                    if (client.player.isShiftKeyDown()) {
                        clearTracking();
                    } else {
                        ManifestSync.request(ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    private static void screenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!KEY.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) {
            return;
        }
        ItemStack item = ModList.get().isLoaded("jei") ? ArchiveJeiPlugin.hoveredItem() : ItemStack.EMPTY;
        if (item.isEmpty() && event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            Slot slot = screen.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                item = slot.getItem();
            }
        }
        if (!item.isEmpty()) {
            ManifestSync.request(item);
            event.setCanceled(true);
        }
    }

    public static void receive(ManifestSync.Snapshot packet) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (packet.exactItemTracking()) {
            if (packet.first()) {
                EXACT_MATCHES.clear();
            }
            EXACT_MATCHES.addAll(packet.rows());
            if (packet.last()) {
                boolean started = track(EXACT_MATCHES, true);
                if (started && !Screen.hasShiftDown() && client.screen instanceof AbstractContainerScreen<?>) {
                    client.player.closeContainer();
                }
                EXACT_MATCHES.clear();
            }
        } else if (client.player.containerMenu instanceof ManifestMenu menu && menu.containerId == packet.menuId()) {
            menu.receive(packet);
        }
    }

    static boolean track(List<ManifestContents.Row> rows, boolean enabled) {
        long now = Util.getMillis();
        Minecraft client = Minecraft.getInstance();
        updateTracking(client, now);
        if (enabled && !mayTrack(client.player)) {
            return false;
        }
        boolean inRange = false;
        for (var row : rows) {
            if (enabled && range(row.location()) == Range.IN_RANGE) {
                TRACKED.computeIfAbsent(row.location(), position -> new LinkedHashMap<>())
                        .put(row.variant(), now + TRACKING_DURATION);
                inRange = true;
            } else if (!enabled) {
                var variants = TRACKED.get(row.location());
                if (variants != null) {
                    variants.remove(row.variant());
                    if (variants.isEmpty()) {
                        TRACKED.remove(row.location());
                    }
                }
            }
        }
        return inRange;
    }

    static boolean tracked(ManifestContents.Row row) {
        return TRACKED.getOrDefault(row.location(), Map.of()).containsKey(row.variant());
    }

    private static void updateTracking(Minecraft client, long now) {
        Player player = client.player;
        var level = client.level;
        boolean held = player != null && mayTrack(player) && (player.getMainHandItem().is(ArcaneArchives.MANIFEST.get())
                || player.getOffhandItem().is(ArcaneArchives.MANIFEST.get()));
        boolean putAway = holdingManifest && !held;
        holdingManifest = held;
        if (level == null || player == null || putAway) {
            TRACKED.clear();
            return;
        }
        TRACKED.entrySet().removeIf(entry -> {
            var location = entry.getKey();
            var pos = location.pos();
            if (!location.dimension().equals(level.dimension())
                    || (level.hasChunkAt(pos) && !level.getBlockState(pos).hasBlockEntity())) {
                return true;
            }
            var variants = entry.getValue();
            variants.values().removeIf(expiresAt -> now >= expiresAt);
            return variants.isEmpty();
        });
    }

    public static void clearTracking() {
        TRACKED.clear();
        DIAGNOSTICS.clear();
        clearInspection();
    }

    private static boolean mayTrack(Player player) {
        return player != null && ArchiveStages.allowsFeature(player, ArchiveFeature.MANIFEST_ACCESS)
                && ArchiveStages.allowsFeature(player, ArchiveFeature.MANIFEST_TRACK);
    }

    static void receiveMarkers(ArchiveNetwork.MarkerBatch packet) {
        if (packet.first()) {
            DIAGNOSTICS.clear();
        }
        for (var marker : packet.markers()) {
            DIAGNOSTICS.put(marker.location(), marker.priority());
        }
    }

    private static void clearInspection() {
        inspected = List.of();
        pendingInspection = List.of();
        inspectedScreen = null;
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clearTracking();
        EXACT_MATCHES.clear();
        holdingManifest = false;
    }

    static Set<GlobalPos> locations() {
        if (DIAGNOSTICS.isEmpty()) {
            return TRACKED.keySet();
        }
        var result = new LinkedHashSet<>(TRACKED.keySet());
        result.addAll(DIAGNOSTICS.keySet());
        return result;
    }

    static int diagnosticPriority(GlobalPos position) {
        return DIAGNOSTICS.getOrDefault(position, -1);
    }

    static Range range(GlobalPos location) {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.level().dimension().equals(location.dimension())) {
            return Range.OTHER_DIMENSION;
        }
        double limit = ArchiveConfig.MANIFEST_RANGE.get();
        return location.pos().distSqr(player.blockPosition()) < limit * limit ? Range.IN_RANGE : Range.OUT_OF_RANGE;
    }

    private static void inspectInventory(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        if (!level.isClientSide || (TRACKED.isEmpty() && DIAGNOSTICS.isEmpty())) {
            return;
        }
        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof GemCuttersTableBlock) {
            pos = GemCuttersTableBlock.inventoryPosition(pos, state);
        }
        GlobalPos location = GlobalPos.of(level.dimension(), pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            GlobalPos connected = GlobalPos.of(level.dimension(), pos.relative(ChestBlock.getConnectedDirection(state)));
            pendingInspection = List.of(location, connected);
        } else {
            pendingInspection = List.of(location);
        }
        inspectUntil = Util.getMillis() + OPEN_WINDOW;
    }

    private static void containerOpening(ScreenEvent.Opening event) {
        long now = Util.getMillis();
        Minecraft client = Minecraft.getInstance();
        if (!(event.getNewScreen() instanceof AbstractContainerScreen<?> screen) || screen instanceof ManifestScreen
                || client.player == null || screen.getMenu() == client.player.inventoryMenu
                || pendingInspection.isEmpty() || now >= inspectUntil) {
            pendingInspection = List.of();
            return;
        }
        updateTracking(client, now);
        Set<ManifestContents.Variant> variants = new LinkedHashSet<>();
        for (var location : pendingInspection) {
            DIAGNOSTICS.remove(location);
            var tracked = TRACKED.remove(location);
            if (tracked != null) {
                variants.addAll(tracked.keySet());
            }
        }
        pendingInspection = List.of();
        inspected = variants.stream().filter(value -> !value.fluid()).map(ManifestContents.Variant::item).toList();
        inspectedScreen = screen;
    }

    private static void renderInspectedItems(ContainerScreenEvent.Render.Foreground event) {
        var screen = event.getContainerScreen();
        if (inspected.isEmpty() || screen != inspectedScreen) {
            return;
        }
        if (!screen.getClass().getPackageName().equals(ManifestClient.class.getPackageName())
                && (!ArchiveConfig.MANIFEST_HIGHLIGHT_OTHER.get()
                        || ArchiveConfig.MANIFEST_HIGHLIGHT_EXCLUDED.get().contains(screen.getClass().getName()))) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof net.minecraft.world.entity.player.Inventory)
                    && slot.isActive() && slot.hasItem() && inspected.stream()
                    .anyMatch(item -> ItemStack.isSameItemSameTags(item, slot.getItem()))) {
                event.getGuiGraphics().fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 180, 0x80A36CEF);
            }
        }
    }

    enum Range {
        IN_RANGE, OUT_OF_RANGE, OTHER_DIMENSION
    }
}
