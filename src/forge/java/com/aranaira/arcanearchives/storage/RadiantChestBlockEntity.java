package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Objects;

public final class RadiantChestBlockEntity extends ArchiveDeviceBlockEntity implements MenuProvider, Nameable {
    private final RadiantChestInventory inventory = new RadiantChestInventory(this::setChanged);
    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> inventory);
    @Nullable
    private Component customName;
    private boolean existingItemsOnly;
    private ItemStack displayStack = ItemStack.EMPTY;
    private Direction displayFacing = Direction.NORTH;

    public RadiantChestBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.RADIANT_CHEST_ENTITY.get(), pos, state);
    }

    public RadiantChestInventory inventory() {
        return inventory;
    }

    public void setCustomName(@Nullable Component name) {
        if (!Objects.equals(customName, name)) {
            customName = name;
            settingsChanged();
        }
    }

    public void setExistingItemsOnly(boolean existingItemsOnly) {
        if (this.existingItemsOnly != existingItemsOnly) {
            this.existingItemsOnly = existingItemsOnly;
            settingsChanged();
        }
    }

    public boolean existingItemsOnly() {
        return existingItemsOnly;
    }

    boolean canChangeRouting(Player player) {
        return !player.isSpectator() && owner() != null && level instanceof ServerLevel serverLevel
                && ArchiveNetworks.get(serverLevel.getServer()).sharesHive(owner(), player.getUUID());
    }

    public ItemStack displayStack() {
        return displayStack;
    }

    public Direction displayFacing() {
        return displayFacing;
    }

    void setDisplay(ItemStack stack, Direction face) {
        ItemStack label = stack.copyWithCount(1);
        if (!ItemStack.matches(displayStack, label) || !label.isEmpty() && displayFacing != face) {
            displayStack = label;
            displayFacing = face;
            settingsChanged();
        }
    }

    private void settingsChanged() {
        setChanged();
        if (level instanceof ServerLevel server) {
            BlockState state = getBlockState();
            server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getName() {
        return customName == null ? Component.translatable("block.arcanearchives.radiant_chest") : customName;
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    @Nullable
    public Component getCustomName() {
        return customName;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadiantChestMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", inventory.serializeNBT());
        writeSettings(tag);
    }

    private void writeSettings(CompoundTag tag) {
        tag.putInt("routingType", existingItemsOnly ? 1 : 0);
        if (customName != null) {
            tag.putString("CustomName", Component.Serializer.toJson(customName));
        }
        if (!displayStack.isEmpty()) {
            tag.put("displayStack", displayStack.save(new CompoundTag()));
            tag.putInt("displayFacing", displayFacing.get3DDataValue());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
        readSettings(tag);
    }

    private void readSettings(CompoundTag tag) {
        existingItemsOnly = tag.getInt("routingType") == 1;
        if (tag.contains("CustomName", Tag.TAG_STRING)) {
            customName = Component.Serializer.fromJson(tag.getString("CustomName"));
        } else {
            customName = tag.contains("chestName", Tag.TAG_STRING) ? Component.literal(tag.getString("chestName")) : null;
        }
        displayStack = ItemStack.of(tag.getCompound("displayStack")).copyWithCount(1);
        displayFacing = tag.contains("displayFacing", Tag.TAG_ANY_NUMERIC)
                ? Direction.from3DDataValue(tag.getInt("displayFacing")) : Direction.NORTH;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeSettings(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        readSettings(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag());
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved() && cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> inventory);
    }
}
