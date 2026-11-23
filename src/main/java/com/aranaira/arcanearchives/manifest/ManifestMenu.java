package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ManifestMenu extends AbstractContainerMenu {
    public static final int REFRESH = 0;
    public static final int TOGGLE_PERSONAL = 1;
    private final ContainerLevelAccess access;
    private final Player viewer;
    private final DataSlot personalOnly = DataSlot.standalone();
    private final List<ManifestContents.Row> incoming = new ArrayList<>();
    private List<ManifestContents.Row> rows = List.of();
    private long nextRefresh = Long.MIN_VALUE;
    private boolean sent;
    private int revision;

    public ManifestMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    ManifestMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(ArcaneArchives.MANIFEST_MENU.get(), id);
        this.viewer = inventory.player;
        this.access = access;
        addDataSlot(personalOnly);
    }

    public boolean personalOnly() {
        return personalOnly.get() != 0;
    }

    public List<ManifestContents.Row> rows() {
        return rows;
    }

    public int revision() {
        return revision;
    }

    public void receive(ManifestSync.Snapshot packet) {
        if (packet.first()) {
            incoming.clear();
        }
        incoming.addAll(packet.rows());
        if (packet.last()) {
            rows = List.copyOf(incoming);
            incoming.clear();
            revision++;
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return ArchiveStages.allowsFeature(player, ArchiveFeature.MANIFEST_ACCESS)
                && access.evaluate((level, pos) -> ManifestLecternBlock.isComplete(level, pos)
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer) || player.isSpectator() || !stillValid(player)) {
            return false;
        }
        if (button == TOGGLE_PERSONAL) {
            personalOnly.set(personalOnly() ? 0 : 1);
        } else if (button != REFRESH) {
            return false;
        }
        sent = false;
        nextRefresh = Long.MIN_VALUE;
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (viewer instanceof ServerPlayer player && player.level().getGameTime() >= nextRefresh && stillValid(player)) {
            nextRefresh = player.level().getGameTime() + 20;
            List<ManifestContents.Row> current = ManifestContents.collect(player.server, player.getUUID(), personalOnly());
            if (!sent || !rows.equals(current)) {
                rows = current;
                ManifestSync.sendSnapshot(player, containerId, rows);
                sent = true;
            }
        }
    }
}
