package com.aranaira.arcanearchives.network;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class HiveLetterItem extends Item {
    private static final String MESSAGE = "arcanearchives.network.hive.";
    private static final String TOOLTIP = "arcanearchives.tooltip.item.";
    private final Kind kind;

    public HiveLetterItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 64;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!canUse(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player && player.getServer() != null && canUse(player)) {
            MinecraftServer server = player.getServer();
            CompoundTag tag = stack.getTag();
            UUID signer = readId(tag, "creator");
            if (signer == null || tag.getString("creator_name").isBlank()) {
                status(player, "invalid_letter");
                return stack;
            }

            ArchiveNetworks networks = ArchiveNetworks.get(server);
            boolean used = switch (kind) {
                case INVITATION -> join(player, signer, tag.getString("creator_name"), server, networks);
                case RESIGNATION -> resign(player, signer, server, networks);
                case EXPULSION -> expel(player, signer, tag, server, networks);
            };
            if (used) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    private boolean canUse(Player player) {
        var feature = switch (kind) {
            case INVITATION -> ArchiveFeature.HIVE_JOIN;
            case RESIGNATION -> ArchiveFeature.HIVE_LEAVE;
            case EXPULSION -> ArchiveFeature.HIVE_EXPEL;
        };
        return ArchiveStages.allowsFeature(player, feature);
    }

    private static boolean join(Player player, UUID owner, String networkName,
                                MinecraftServer server, ArchiveNetworks networks) {
        UUID reader = player.getUUID();
        if (reader.equals(owner)) {
            status(player, "yours");
            return false;
        }
        if (!networks.joinHive(owner, reader)) {
            status(player, "failed");
            return false;
        }
        networks.rememberMemberName(owner, networkName);
        String name = player.getGameProfile().getName();
        networks.rememberMemberName(reader, name);
        status(player, "joined", networkName);
        notifyMembers(server, networks.hiveParticipants(owner), reader, "joined_your_network", name);
        return true;
    }

    private static boolean resign(Player player, UUID signer, MinecraftServer server, ArchiveNetworks networks) {
        UUID reader = player.getUUID();
        if (!signer.equals(reader)) {
            status(player, "leaving_failed");
            return false;
        }
        Set<UUID> members = networks.hiveParticipants(reader);
        if (!networks.leaveHive(reader)) {
            status(player, "left_failed");
            return false;
        }
        status(player, "left");
        notifyMembers(server, members, reader, members.size() == 2 ? "disbanded" : "left_your_network",
                player.getGameProfile().getName());
        return true;
    }

    private static boolean expel(Player player, UUID signer, CompoundTag tag,
                                 MinecraftServer server, ArchiveNetworks networks) {
        UUID reader = player.getUUID();
        if (!signer.equals(reader) || !networks.hiveOwner(reader).equals(reader)) {
            status(player, "expel_no_permission");
            return false;
        }
        UUID target = readId(tag, "expel");
        if (target == null || tag.getString("expel_name").isBlank()) {
            status(player, "expel_unnamed");
            return false;
        }
        Set<UUID> members = networks.hiveParticipants(reader);
        String name = tag.getString("expel_name");
        if (!networks.expel(reader, target)) {
            status(player, "expel_failed", name);
            return false;
        }
        status(player, "expelled", name);
        ServerPlayer removed = server.getPlayerList().getPlayer(target);
        if (removed != null) {
            removed.sendSystemMessage(Component.translatable(MESSAGE + "were_expelled"));
        }
        notifyMembers(server, members, target, members.size() == 2 ? "disbanded" : "left_your_network", name);
        return true;
    }

    private static void status(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(MESSAGE + key, args), true);
    }

    private static void notifyMembers(MinecraftServer server, Set<UUID> members, UUID changed, String key, String name) {
        Component notice = Component.translatable(MESSAGE + key, name).withStyle(ChatFormatting.GOLD);
        for (UUID id : members) {
            ServerPlayer member = server.getPlayerList().getPlayer(id);
            if (member != null && !id.equals(changed)) {
                member.sendSystemMessage(notice);
            }
        }
    }

    public static void onAnvilRepair(AnvilRepairEvent event) {
        ItemStack output = event.getOutput();
        MinecraftServer server = event.getEntity().getServer();
        if (server == null || !output.is(ArcaneArchives.WRIT_EXPULSION.get())) {
            return;
        }
        CompoundTag tag = output.getTag();
        UUID signer = readId(tag, "creator");
        if (signer == null) {
            return;
        }
        tag.remove("expel");
        tag.remove("expelMost");
        tag.remove("expelLeast");
        tag.remove("expel_name");
        if (!output.hasCustomHoverName()) {
            return;
        }

        ArchiveNetworks networks = ArchiveNetworks.get(server);
        String name = output.getHoverName().getString().trim();
        for (UUID member : networks.hiveParticipants(signer)) {
            if (!member.equals(signer) && name.equalsIgnoreCase(resolveName(server, networks, member))) {
                tag.putUUID("expel", member);
                tag.putString("expel_name", name);
                break;
            }
        }
    }

    @Nullable
    private static String resolveName(MinecraftServer server, ArchiveNetworks networks, UUID id) {
        ServerPlayer member = server.getPlayerList().getPlayer(id);
        if (member != null) {
            return member.getGameProfile().getName();
        }
        var cache = server.getProfileCache();
        return cache == null ? networks.knownMemberName(id)
                : cache.get(id).map(GameProfile::getName).orElseGet(() -> networks.knownMemberName(id));
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArchiveNetworks networks = ArchiveNetworks.get(player.server);
            if (networks.hasHive(player.getUUID())) {
                networks.rememberMemberName(player.getUUID(), player.getGameProfile().getName());
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        String nameKey = switch (kind) {
            case INVITATION -> "creator_name";
            case RESIGNATION -> "";
            case EXPULSION -> "expel_name";
        };
        String name = tag == null || nameKey.isEmpty() ? "" : tag.getString(nameKey);
        Component line = name.isBlank() ? Component.translatable(TOOLTIP + kind.id)
                : Component.translatable(TOOLTIP + kind.id + ".named", name);
        tooltip.add(line.copy().withStyle(ChatFormatting.GOLD));
    }

    @Nullable
    private static UUID readId(@Nullable CompoundTag tag, String key) {
        if (tag != null) {
            if (tag.hasUUID(key)) {
                return tag.getUUID(key);
            }
            if (tag.contains(key + "Most", Tag.TAG_LONG) && tag.contains(key + "Least", Tag.TAG_LONG)) {
                return new UUID(tag.getLong(key + "Most"), tag.getLong(key + "Least"));
            }
        }
        return null;
    }

    public enum Kind {
        INVITATION("letter_invitation"), RESIGNATION("letter_resignation"), EXPULSION("writ_expulsion");

        private final String id;

        Kind(String id) {
            this.id = id;
        }
    }
}
