package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArsenalGems;
import com.aranaira.arcanearchives.arsenal.GemSocketItem;
import com.aranaira.arcanearchives.client.ArchiveClient;
import com.aranaira.arcanearchives.crafting.GemCuttersTableMenu;
import com.aranaira.arcanearchives.manifest.ManifestSync;
import net.minecraft.core.NonNullList;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public final class ArchiveNetwork {
    private static final String PROTOCOL = "10";
    private static final int MARKERS_PER_PACKET = 256;
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(ArcaneArchives.id("archive"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ArchiveNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Contents.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Contents::write).decoder(Contents::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ArchiveClient.receiveChestContents(packet)))
                .add();
        CHANNEL.messageBuilder(SlotChange.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SlotChange::write).decoder(SlotChange::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ArchiveClient.receiveChestSlot(packet)))
                .add();
        CHANNEL.messageBuilder(ToggleAmphora.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, data) -> {})
                .decoder(data -> new ToggleAmphora())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.getMainHandItem().is(ArcaneArchives.RADIANT_AMPHORA.get())
                            && ArchiveStages.allowsFeature(player, ArchiveFeature.AMPHORA_MODE)) {
                        RadiantAmphoraItem.toggleMode(player.getMainHandItem());
                    }
                })
                .add();
        CHANNEL.messageBuilder(SelectGemRecipe.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.menuId());
                    buffer.writeResourceLocation(packet.recipe());
                })
                .decoder(buffer -> new SelectGemRecipe(buffer.readVarInt(), buffer.readResourceLocation()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && !player.isSpectator() && player.containerMenu instanceof GemCuttersTableMenu table
                            && table.containerId == packet.menuId()) {
                        table.selectRecipe(player, packet.recipe());
                    }
                })
                .add();
        ManifestSync.register(CHANNEL);
        CHANNEL.messageBuilder(SetBrazierRadius.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.menuId());
                    buffer.writeVarInt(packet.radius());
                })
                .decoder(buffer -> new SetBrazierRadius(buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof BrazierSettingsMenu brazier
                            && brazier.containerId == packet.menuId()) {
                        brazier.setRadius(player, packet.radius());
                    }
                })
                .add();
        CHANNEL.messageBuilder(RenameChest.class, 7, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.menuId());
                    buffer.writeUtf(packet.name(), RadiantChestMenu.NAME_LIMIT);
                })
                .decoder(buffer -> new RenameChest(buffer.readVarInt(), buffer.readUtf(RadiantChestMenu.NAME_LIMIT)))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof RadiantChestMenu chest
                            && chest.containerId == packet.menuId()) {
                        chest.rename(player, packet.name());
                    }
                })
                .add();
        CHANNEL.messageBuilder(MarkerBatch.class, 8, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MarkerBatch::write).decoder(MarkerBatch::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ArchiveClient.receiveMarkers(packet)))
                .add();
        CHANNEL.messageBuilder(ArsenalControl.class, 9, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.openSocket()))
                .decoder(buffer -> new ArsenalControl(buffer.readBoolean()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player == null || player.containerMenu != player.inventoryMenu) {
                        return;
                    }
                    if (packet.openSocket()) {
                        GemSocketItem.openAvailable(player);
                    } else {
                        ArsenalGems.toggleHeld(player);
                    }
                }).add();
    }

    public static void sendAmphoraToggle() {
        CHANNEL.sendToServer(new ToggleAmphora());
    }

    public static void sendArsenalControl(boolean openSocket) {
        CHANNEL.sendToServer(new ArsenalControl(openSocket));
    }

    private record ArsenalControl(boolean openSocket) {}

    private record ToggleAmphora() {}

    public static void setBrazierRadius(int menuId, int radius) {
        CHANNEL.sendToServer(new SetBrazierRadius(menuId, radius));
    }

    private record SetBrazierRadius(int menuId, int radius) {}

    public static void renameChest(int menuId, String name) {
        CHANNEL.sendToServer(new RenameChest(menuId, name));
    }

    private record RenameChest(int menuId, String name) {}

    public static void sendMarkers(ServerPlayer player, List<DiagnosticMarker> markers) {
        var target = PacketDistributor.PLAYER.with(() -> player);
        if (markers.isEmpty()) {
            CHANNEL.send(target, new MarkerBatch(true, List.of()));
        }
        for (int i = 0; i < markers.size(); i += MARKERS_PER_PACKET) {
            CHANNEL.send(target, new MarkerBatch(i == 0, markers.subList(i, Math.min(markers.size(), i + MARKERS_PER_PACKET))));
        }
    }

    public record DiagnosticMarker(GlobalPos location, int priority) {}

    public record MarkerBatch(boolean first, List<DiagnosticMarker> markers) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(first);
            buffer.writeVarInt(markers.size());
            for (var marker : markers) {
                buffer.writeGlobalPos(marker.location());
                buffer.writeVarInt(marker.priority());
            }
        }

        static MarkerBatch read(FriendlyByteBuf buffer) {
            boolean first = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (size < 0 || size > MARKERS_PER_PACKET) {
                throw new IllegalArgumentException("Invalid device marker packet length " + size);
            }
            var markers = new ArrayList<DiagnosticMarker>(size);
            for (int i = 0; i < size; i++) {
                markers.add(new DiagnosticMarker(buffer.readGlobalPos(), buffer.readVarInt()));
            }
            return new MarkerBatch(first, markers);
        }
    }

    public static void selectGemRecipe(int menuId, ResourceLocation recipe) {
        CHANNEL.sendToServer(new SelectGemRecipe(menuId, recipe));
    }

    private record SelectGemRecipe(int menuId, ResourceLocation recipe) {}

    static ContainerSynchronizer chestSynchronizer(ServerPlayer player, ContainerSynchronizer vanilla) {
        return new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> slots,
                                        ItemStack carried, int[] data) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new Contents(menu.containerId, menu.incrementStateId(), slots, carried));
                for (int index = 0; index < data.length; index++) {
                    sendDataChange(menu, index, data[index]);
                }
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu menu, int index, ItemStack stack) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SlotChange(menu.containerId, menu.incrementStateId(), index, stack));
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
                vanilla.sendCarriedChange(menu, stack);
            }

            @Override
            public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
                vanilla.sendDataChange(menu, index, value);
            }
        };
    }

    public record Contents(int menuId, int stateId, List<ItemStack> stacks, ItemStack carried) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(stateId);
            buffer.writeVarInt(stacks.size());
            for (ItemStack stack : stacks) {
                ExtendedStacks.write(buffer, stack);
            }
            ExtendedStacks.write(buffer, carried);
        }

        static Contents read(FriendlyByteBuf buffer) {
            int menuId = buffer.readVarInt();
            int stateId = buffer.readVarInt();
            int size = buffer.readVarInt();
            if (size != RadiantChestMenu.SLOT_COUNT) {
                throw new IllegalArgumentException("Unexpected Radiant Chest slot count " + size);
            }
            List<ItemStack> stacks = new ArrayList<>(size);
            for (int slot = 0; slot < size; slot++) {
                stacks.add(ExtendedStacks.read(buffer));
            }
            return new Contents(menuId, stateId, stacks, ExtendedStacks.read(buffer));
        }
    }

    public record SlotChange(int menuId, int stateId, int slot, ItemStack stack) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(stateId);
            buffer.writeVarInt(slot);
            ExtendedStacks.write(buffer, stack);
        }

        static SlotChange read(FriendlyByteBuf buffer) {
            return new SlotChange(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    ExtendedStacks.read(buffer));
        }
    }
}
