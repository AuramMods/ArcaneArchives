package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class BrazierSettingsMenu extends AbstractContainerMenu {
    public static final int SHRINK = 0;
    public static final int EXPAND = 1;
    public static final int TOGGLE_NETWORK = 2;

    private final BlockPos pos;
    private final ContainerLevelAccess access;
    @Nullable
    private final BrazierBlockEntity brazier;
    private final DataSlot radius = DataSlot.standalone();
    private final DataSlot personal = DataSlot.standalone();

    public BrazierSettingsMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos(), null);
    }

    BrazierSettingsMenu(int id, Inventory inventory, BrazierBlockEntity brazier) {
        this(id, inventory, brazier.getBlockPos(), brazier);
    }

    private BrazierSettingsMenu(int id, Inventory inventory, BlockPos pos, @Nullable BrazierBlockEntity brazier) {
        super(ArcaneArchives.BRAZIER_SETTINGS_MENU.get(), id);
        this.pos = pos;
        this.brazier = brazier;
        access = ContainerLevelAccess.create(inventory.player.level(), pos);
        addDataSlot(radius);
        addDataSlot(personal);
        syncSettings();
    }

    public BlockPos position() {
        return pos;
    }

    public int radius() {
        return radius.get();
    }

    public boolean personalOnly() {
        return personal.get() != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ArcaneArchives.BRAZIER.get())
                && (brazier == null || (!brazier.isRemoved() && player.level().getBlockEntity(pos) == brazier
                && brazier.canConfigure(player)));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int action) {
        if (brazier == null || !stillValid(player)) {
            return false;
        }
        switch (action) {
            case SHRINK -> brazier.setRouting(brazier.routingRadius() - 10, brazier.personalOnly());
            case EXPAND -> brazier.setRouting(brazier.routingRadius() + 10, brazier.personalOnly());
            case TOGGLE_NETWORK -> brazier.setRouting(brazier.routingRadius(), !brazier.personalOnly());
            default -> {
                return false;
            }
        }
        broadcastChanges();
        return true;
    }

    public boolean setRadius(Player player, int range) {
        if (brazier == null || !stillValid(player)) {
            return false;
        }
        brazier.setRouting(range, brazier.personalOnly());
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        syncSettings();
        super.broadcastChanges();
    }

    private void syncSettings() {
        if (brazier != null) {
            radius.set(brazier.routingRadius());
            personal.set(brazier.personalOnly() ? 1 : 0);
        }
    }
}
