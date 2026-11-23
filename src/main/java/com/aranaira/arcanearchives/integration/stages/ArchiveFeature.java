package com.aranaira.arcanearchives.integration.stages;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.resources.ResourceLocation;

public enum ArchiveFeature {
    GEM_CUTTER_USE("gem_cutter/use"),
    RADIANT_CRAFTING_USE("radiant_crafting/use"),
    STORAGE_USE("storage/use"),
    STORAGE_UPGRADES("storage/upgrades"),
    BRAZIER_ROUTE("brazier/route"),
    BRAZIER_CONFIGURE("brazier/configure"),
    AMPHORA_LINK("amphora/link"),
    AMPHORA_TRANSFER("amphora/transfer"),
    AMPHORA_MODE("amphora/mode"),
    MONITORING_READ("monitoring/read"),
    MONITORING_INSPECT("monitoring/inspect"),
    MANIFEST_ACCESS("manifest/access"),
    MANIFEST_TRACK("manifest/track"),
    HIVE_JOIN("hive/join"),
    HIVE_LEAVE("hive/leave"),
    HIVE_EXPEL("hive/expel"),
    HIVE_STATUS("hive/status"),
    CHARM_CONFIGURE("devouring_charm/configure"),
    CHARM_CONSUME("devouring_charm/consume"),
    TOME_READ("tome/read"),
    TOME_BOOKSHELF("tome/bookshelf"),
    TOME_RESONATOR("tome/resonator"),
    GEM_SOCKET_USE("arsenal/socket/use");

    private final ResourceLocation id;

    ArchiveFeature(String path) {
        id = ArcaneArchives.id(path);
    }

    public ResourceLocation id() {
        return id;
    }
}
