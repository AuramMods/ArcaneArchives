package com.aranaira.arcanearchives.network;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.BrazierBlockEntity;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.UUID;

public final class ArchiveCommands {
    private static final String TEXT = "commands.arcanearchives.";

    private ArchiveCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("hive").executes(context -> hive(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("brazier").executes(context -> brazier(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("tiles")
                .executes(context -> tiles(context.getSource().getPlayerOrException(), TileScope.DEFAULT))
                .then(Commands.literal("mine").executes(context -> tiles(context.getSource().getPlayerOrException(), TileScope.MINE)))
                .then(Commands.literal("hive").executes(context -> tiles(context.getSource().getPlayerOrException(), TileScope.HIVE)))
                .then(Commands.literal("valid").executes(context -> tiles(context.getSource().getPlayerOrException(), TileScope.VALID)))
                .then(Commands.literal("manifest").executes(context -> tiles(context.getSource().getPlayerOrException(), TileScope.MANIFEST))));
        dispatcher.register(Commands.literal("rebuild_network")
                .executes(context -> rebuild(context.getSource().getPlayerOrException())));
        dispatcher.register(Commands.literal("clipboard").requires(source -> source.hasPermission(2))
                .then(Commands.argument("text", StringArgumentType.greedyString()).executes(context -> {
                    context.getSource().getPlayerOrException().sendSystemMessage(copyText(StringArgumentType.getString(context, "text")));
                    return 1;
                })));
    }

    private static int hive(ServerPlayer player) {
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.HIVE_STATUS)) {
            return 0;
        }
        var networks = ArchiveNetworks.get(player.serverLevel().getServer());
        UUID id = player.getUUID();
        if (!networks.hasHive(id)) {
            player.sendSystemMessage(Component.translatable(TEXT + "no_hive"));
            return 0;
        }
        UUID owner = networks.hiveOwner(id);
        var members = networks.hiveParticipants(id);
        player.sendSystemMessage(Component.translatable(TEXT + "hive_owner", name(player, networks, owner), owner.toString()));
        for (UUID member : members) {
            if (!member.equals(owner)) {
                player.sendSystemMessage(Component.translatable(TEXT + "hive_member", name(player, networks, member), member.toString()));
            }
        }
        return members.size();
    }

    private static String name(ServerPlayer viewer, ArchiveNetworks networks, UUID id) {
        var server = viewer.serverLevel().getServer();
        var online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        String saved = networks.knownMemberName(id);
        if (saved != null) {
            return saved;
        }
        return server.getProfileCache().get(id).map(profile -> profile.getName()).orElse(id.toString());
    }

    private static int brazier(ServerPlayer player) {
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.BRAZIER_ROUTE)) {
            return 0;
        }
        var held = player.getMainHandItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(Component.translatable(TEXT + "empty_hand"));
            return 0;
        }
        var routing = BrazierBlockEntity.trace(player, held);
        var markers = new ArrayList<ArchiveNetwork.DiagnosticMarker>();
        player.sendSystemMessage(Component.translatable(TEXT + "brazier", routing.size(), held.getHoverName()));
        for (var target : routing) {
            var entity = target.device();
            var position = GlobalPos.of(player.level().dimension(), entity.getBlockPos());
            markers.add(new ArchiveNetwork.DiagnosticMarker(position, target.priority()));
            player.sendSystemMessage(Component.translatable(TEXT + "brazier_weight", entity.getBlockState().getBlock().getName(),
                    position.pos().toShortString(), target.priority()));
        }
        ArchiveNetwork.sendMarkers(player, markers);
        return routing.size();
    }

    private static int tiles(ServerPlayer player, TileScope scope) {
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.MONITORING_INSPECT)) {
            return 0;
        }
        var server = player.serverLevel().getServer();
        var networks = ArchiveNetworks.get(server);
        if (scope == TileScope.HIVE && !networks.hasHive(player.getUUID())) {
            player.sendSystemMessage(Component.translatable(TEXT + "no_hive"));
            return 0;
        }
        var markers = new ArrayList<ArchiveNetwork.DiagnosticMarker>();
        for (var device : networks.devicesOwnedBy(player.getUUID(), scope == TileScope.MINE)) {
            boolean valid = networks.resolveDevice(server, device) != null;
            if (scope == TileScope.VALID && !valid || scope == TileScope.MANIFEST && !manifestType(device.type())) {
                continue;
            }
            var location = device.location();
            markers.add(new ArchiveNetwork.DiagnosticMarker(location, -1));
            player.sendSystemMessage(Component.translatable(TEXT + "tile", String.valueOf(device.type()),
                    location.dimension().location().toString(), location.pos().toShortString(), device.id().toString(),
                    Component.translatable(TEXT + (valid ? "tile_loaded" : "tile_unavailable"))));
        }
        player.sendSystemMessage(Component.translatable(TEXT + "tile_total", markers.size()));
        ArchiveNetwork.sendMarkers(player, markers);
        return markers.size();
    }

    private static boolean manifestType(ResourceLocation type) {
        return ArcaneArchives.RADIANT_CHEST_ENTITY.getId().equals(type)
                || ArcaneArchives.RADIANT_TROVE_ENTITY.getId().equals(type)
                || ArcaneArchives.RADIANT_TANK_ENTITY.getId().equals(type)
                || ArcaneArchives.GEMCUTTERS_TABLE_ENTITY.getId().equals(type)
                || ArcaneArchives.RADIANT_CRAFTING_TABLE_ENTITY.getId().equals(type)
                || ArcaneArchives.MONITORING_CRYSTAL_ENTITY.getId().equals(type);
    }

    private static int rebuild(ServerPlayer player) {
        var server = player.serverLevel().getServer();
        var result = ArchiveNetworks.get(server).rebuildLoaded(server, player.getUUID());
        player.sendSystemMessage(Component.translatable(TEXT + "rebuilt", result.refreshed(), result.staleRemoved()));
        return result.refreshed();
    }

    public static Component copyText(String text) {
        return Component.translatable(TEXT + "copy")
                .withStyle(style -> style.withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(text))));
    }

    private enum TileScope {
        DEFAULT, MINE, HIVE, VALID, MANIFEST
    }
}
