package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.client.ManifestClient;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public final class ManifestSync {
    private static final int ROWS_PER_PACKET = 64;
    private static final int TRACK_ITEM = -1;
    private static SimpleChannel channel;

    private ManifestSync() {}

    public static void register(SimpleChannel archiveChannel) {
        channel = archiveChannel;
        channel.messageBuilder(Request.class, 4, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeItem(packet.item()))
                .decoder(buffer -> new Request(buffer.readItem()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player == null || !ManifestItem.mayUseKeybind(player)) {
                        return;
                    }
                    if (packet.item().isEmpty()) {
                        ManifestItem.open(player, ContainerLevelAccess.NULL);
                    } else {
                        var variant = ManifestContents.Variant.of(packet.item());
                        var matches = ManifestContents.collect(player.server, player.getUUID(), false).stream()
                                .filter(row -> row.variant().equals(variant)).toList();
                        sendSnapshot(player, TRACK_ITEM, matches);
                    }
                })
                .add();
        channel.messageBuilder(Snapshot.class, 5, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Snapshot::write).decoder(Snapshot::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ManifestClient.receive(packet)))
                .add();
    }

    public static void request(ItemStack item) {
        channel.sendToServer(new Request(item.copyWithCount(1)));
    }

    static void sendSnapshot(ServerPlayer player, int menuId, List<ManifestContents.Row> rows) {
        var destination = PacketDistributor.PLAYER.with(() -> player);
        if (rows.isEmpty()) {
            channel.send(destination, new Snapshot(menuId, true, true, rows));
        }
        for (int offset = 0; offset < rows.size(); offset += ROWS_PER_PACKET) {
            int end = Math.min(rows.size(), offset + ROWS_PER_PACKET);
            channel.send(destination, new Snapshot(menuId, offset == 0, end == rows.size(), rows.subList(offset, end)));
        }
    }

    private record Request(ItemStack item) {}

    public record Snapshot(int menuId, boolean first, boolean last, List<ManifestContents.Row> rows) {
        public boolean exactItemTracking() {
            return menuId == TRACK_ITEM;
        }

        private void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeBoolean(first);
            buffer.writeBoolean(last);
            buffer.writeVarInt(rows.size());
            for (var row : rows) {
                buffer.writeResourceLocation(row.variant().id());
                buffer.writeBoolean(row.variant().fluid());
                buffer.writeNbt(row.variant().prototype());
                buffer.writeResourceKey(row.location().dimension());
                buffer.writeBlockPos(row.location().pos());
                buffer.writeComponent(row.label());
                buffer.writeVarLong(row.amount());
            }
        }

        private static Snapshot read(FriendlyByteBuf buffer) {
            int menu = buffer.readVarInt();
            boolean first = buffer.readBoolean();
            boolean last = buffer.readBoolean();
            int size = buffer.readVarInt();
            if (size < 0 || size > ROWS_PER_PACKET) {
                throw new IllegalArgumentException("Invalid Manifest row count");
            }
            List<ManifestContents.Row> rows = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                var variant = new ManifestContents.Variant(buffer.readResourceLocation(), buffer.readBoolean(), buffer.readNbt());
                GlobalPos location = GlobalPos.of(buffer.readResourceKey(net.minecraft.core.registries.Registries.DIMENSION),
                        buffer.readBlockPos());
                rows.add(new ManifestContents.Row(variant, location, buffer.readComponent(), buffer.readVarLong()));
            }
            return new Snapshot(menu, first, last, rows);
        }
    }
}
