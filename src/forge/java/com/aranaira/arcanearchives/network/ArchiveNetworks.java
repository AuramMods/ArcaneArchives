package com.aranaira.arcanearchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArchiveNetworks extends SavedData {
    private final Map<GlobalPos, Device> devices = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<UUID>> hives = new LinkedHashMap<>();
    private final Map<UUID, String> memberNames = new LinkedHashMap<>();

    public static ArchiveNetworks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(ArchiveNetworks::load, ArchiveNetworks::new,
                "arcanearchives_networks");
    }

    public void registerDevice(GlobalPos location, UUID deviceId, UUID owner, ResourceLocation type) {
        Device device = new Device(location, deviceId, owner, type);
        if (!device.equals(devices.put(location, device))) {
            setDirty();
        }
    }

    public void removeDevice(GlobalPos location, UUID deviceId) {
        Device current = devices.get(location);
        if (current != null && current.id().equals(deviceId)) {
            devices.remove(location);
            setDirty();
        }
    }

    public List<Device> devicesOwnedBy(UUID player, boolean personalOnly) {
        Set<UUID> owners = personalOnly ? Set.of(player) : hiveParticipants(player);
        return devices.values().stream().filter(device -> owners.contains(device.owner())).toList();
    }

    public long countPersonalDevices(UUID owner, ResourceLocation type) {
        return devices.values().stream().filter(device -> owner.equals(device.owner()) && type.equals(device.type())).count();
    }

    public List<ArchiveDeviceBlockEntity> loadedDevices(MinecraftServer server, UUID player, boolean personalOnly) {
        List<ArchiveDeviceBlockEntity> loaded = new ArrayList<>();
        for (Device device : devicesOwnedBy(player, personalOnly)) {
            ServerLevel level = server.getLevel(device.location().dimension());
            BlockPos pos = device.location().pos();
            if (level != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ArchiveDeviceBlockEntity entity
                    && device.id().equals(entity.deviceId()) && device.owner().equals(entity.owner())) {
                loaded.add(entity);
            }
        }
        return loaded;
    }

    public boolean sharesHive(UUID first, UUID second) {
        return hiveOwner(first).equals(hiveOwner(second));
    }

    public boolean hasHive(UUID player) {
        return hives.containsKey(hiveOwner(player));
    }

    void rememberMemberName(UUID id, String name) {
        if (!name.isBlank() && !name.equals(memberNames.put(id, name))) {
            setDirty();
        }
    }

    @Nullable
    String knownMemberName(UUID id) {
        return memberNames.get(id);
    }

    public UUID hiveOwner(UUID player) {
        if (!hives.containsKey(player)) {
            for (Map.Entry<UUID, LinkedHashSet<UUID>> hive : hives.entrySet()) {
                if (hive.getValue().contains(player)) {
                    return hive.getKey();
                }
            }
        }
        return player;
    }

    public Set<UUID> hiveParticipants(UUID player) {
        UUID owner = hiveOwner(player);
        LinkedHashSet<UUID> members = hives.get(owner);
        if (members == null) {
            return Set.of(player);
        }
        LinkedHashSet<UUID> participants = new LinkedHashSet<>();
        participants.add(owner);
        participants.addAll(members);
        return participants;
    }

    public boolean joinHive(UUID owner, UUID player) {
        if (owner.equals(player) || !hiveOwner(owner).equals(owner) || hiveParticipants(player).size() != 1) {
            return false;
        }
        hives.computeIfAbsent(owner, key -> new LinkedHashSet<>()).add(player);
        setDirty();
        return true;
    }

    public boolean leaveHive(UUID player) {
        UUID owner = hiveOwner(player);
        LinkedHashSet<UUID> members = hives.get(owner);
        if (members == null) {
            return false;
        }

        if (owner.equals(player)) {
            UUID successor = members.iterator().next();
            members.remove(successor);
            hives.remove(owner);
            if (!members.isEmpty()) {
                hives.put(successor, members);
            }
        } else {
            members.remove(player);
            if (members.isEmpty()) {
                hives.remove(owner);
            }
        }
        setDirty();
        return true;
    }

    public boolean expel(UUID owner, UUID member) {
        return !owner.equals(member) && hiveOwner(member).equals(owner) && leaveHive(member);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag locations = new ListTag();
        for (Device device : devices.values()) {
            CompoundTag location = new CompoundTag();
            location.putString("Dimension", device.location().dimension().location().toString());
            location.putLong("Position", device.location().pos().asLong());
            location.putUUID("Id", device.id());
            location.putUUID("Owner", device.owner());
            if (device.type() != null) {
                location.putString("Type", device.type().toString());
            }
            locations.add(location);
        }
        tag.put("Devices", locations);

        ListTag hiveList = new ListTag();
        for (Map.Entry<UUID, LinkedHashSet<UUID>> hive : hives.entrySet()) {
            CompoundTag hiveTag = new CompoundTag();
            hiveTag.putUUID("Owner", hive.getKey());
            ListTag members = new ListTag();
            for (UUID member : hive.getValue()) {
                CompoundTag memberTag = new CompoundTag();
                memberTag.putUUID("Id", member);
                members.add(memberTag);
            }
            hiveTag.put("Members", members);
            hiveList.add(hiveTag);
        }
        tag.put("Hives", hiveList);
        ListTag names = new ListTag();
        memberNames.forEach((id, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            entry.putString("Name", name);
            names.add(entry);
        });
        tag.put("MemberNames", names);
        return tag;
    }

    public static ArchiveNetworks load(CompoundTag tag) {
        ArchiveNetworks networks = new ArchiveNetworks();
        ListTag locations = tag.getList("Devices", Tag.TAG_COMPOUND);
        for (int i = 0; i < locations.size(); i++) {
            CompoundTag entry = locations.getCompound(i);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (dimension != null && entry.contains("Position", Tag.TAG_LONG) && entry.hasUUID("Id") && entry.hasUUID("Owner")) {
                GlobalPos location = GlobalPos.of(ResourceKey.create(Registries.DIMENSION, dimension),
                        BlockPos.of(entry.getLong("Position")));
                ResourceLocation type = entry.contains("Type", Tag.TAG_STRING) ? ResourceLocation.tryParse(entry.getString("Type")) : null;
                networks.devices.put(location, new Device(location, entry.getUUID("Id"), entry.getUUID("Owner"), type));
            }
        }

        ListTag hiveList = tag.getList("Hives", Tag.TAG_COMPOUND);
        for (int i = 0; i < hiveList.size(); i++) {
            CompoundTag entry = hiveList.getCompound(i);
            if (!entry.hasUUID("Owner")) {
                continue;
            }
            UUID owner = entry.getUUID("Owner");
            LinkedHashSet<UUID> members = new LinkedHashSet<>();
            ListTag memberList = entry.getList("Members", Tag.TAG_COMPOUND);
            for (int j = 0; j < memberList.size(); j++) {
                CompoundTag member = memberList.getCompound(j);
                if (member.hasUUID("Id") && !owner.equals(member.getUUID("Id"))) {
                    members.add(member.getUUID("Id"));
                }
            }
            if (!members.isEmpty()) {
                networks.hives.put(owner, members);
            }
        }
        ListTag names = tag.getList("MemberNames", Tag.TAG_COMPOUND);
        for (int i = 0; i < names.size(); i++) {
            CompoundTag entry = names.getCompound(i);
            if (entry.hasUUID("Id") && !entry.getString("Name").isBlank()) {
                networks.memberNames.put(entry.getUUID("Id"), entry.getString("Name"));
            }
        }
        return networks;
    }

    public record Device(GlobalPos location, UUID id, UUID owner, @Nullable ResourceLocation type) {}
}
