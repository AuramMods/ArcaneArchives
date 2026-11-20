package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class BrazierBlockEntity extends ArchiveDeviceBlockEntity {
    private static final String REJECTED = "arcanearchives:brazier_rejected";
    private static final int DEFAULT_RADIUS = 150;

    private final ItemIngress itemIngress = new ItemIngress();
    private final FluidIngress fluidIngress = new FluidIngress();
    private LazyOptional<IItemHandler> items = LazyOptional.of(() -> itemIngress);
    private LazyOptional<IFluidHandler> fluids = LazyOptional.of(() -> fluidIngress);
    private int radius = DEFAULT_RADIUS;
    private boolean personalOnly;
    private long lastSound = -6;
    private long lastDeposit = -6;
    private UUID lastDepositor;
    private ItemStack lastOffered = ItemStack.EMPTY;

    public BrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.BRAZIER_ENTITY.get(), pos, state);
    }

    public int routingRadius() {
        return radius;
    }

    public boolean personalOnly() {
        return personalOnly;
    }

    public boolean canConfigure(Player player) {
        return !player.isSpectator() && owner() != null && level instanceof ServerLevel serverLevel
                && ArchiveNetworks.get(serverLevel.getServer()).sharesHive(owner(), player.getUUID());
    }

    public void setRouting(int range, boolean personal) {
        int bounded = Math.max(0, Math.min(300, range));
        if (radius != bounded || personalOnly != personal) {
            radius = bounded;
            personalOnly = personal;
            setChanged();
            if (level instanceof ServerLevel server) {
                BlockState state = getBlockState();
                server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
    }

    public ItemStack route(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack;
        for (ItemTarget target : itemTargets(nearbyDevices(), stack)) {
            remaining = target.insert().apply(remaining, simulate);
            if (remaining.isEmpty()) {
                break;
            }
        }
        if (!simulate && remaining.getCount() < stack.getCount()) {
            playAbsorptionSound();
        }
        return remaining;
    }

    public static List<RoutingWeight> trace(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        return itemTargets(nearbyDevices(player.serverLevel(), player.getUUID(), player.blockPosition(), DEFAULT_RADIUS, false), stack)
                .stream().filter(target -> target.insert().apply(stack, true).getCount() < stack.getCount())
                .map(target -> new RoutingWeight(target.device(), target.priority())).toList();
    }

    public record RoutingWeight(ArchiveDeviceBlockEntity device, int priority) {}

    private static List<ItemTarget> itemTargets(List<ArchiveDeviceBlockEntity> devices, ItemStack stack) {
        List<ItemTarget> targets = new ArrayList<>();
        for (ArchiveDeviceBlockEntity device : devices) {
            if (device instanceof GemCuttersTableBlockEntity table) {
                targets.add(new ItemTarget(table, 5000, table::acceptExisting));
            } else if (device instanceof RadiantTroveBlockEntity trove
                    && ItemStack.isSameItemSameTags(trove.inventory().reference(), stack)) {
                targets.add(new ItemTarget(trove, 4500, (incoming, test) ->
                        ItemHandlerHelper.insertItemStacked(trove.inventory(), incoming, test)));
            } else if (device instanceof RadiantChestBlockEntity chest) {
                int priority = chestPriority(chest, stack);
                if (priority >= 0) {
                    targets.add(new ItemTarget(chest, priority, (incoming, test) ->
                            ItemHandlerHelper.insertItemStacked(chest.inventory(), incoming, test)));
                }
            }
        }
        targets.sort(Comparator.comparingInt(ItemTarget::priority).reversed());
        return targets;
    }

    private static int chestPriority(RadiantChestBlockEntity chest, ItemStack stack) {
        int matches = 0;
        int occupied = 0;
        RadiantChestInventory inventory = chest.inventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (!stored.isEmpty()) {
                occupied++;
                if (ItemStack.isSameItemSameTags(stored, stack)) {
                    matches += stored.getCount();
                }
            }
        }
        if (chest.existingItemsOnly()) {
            return matches > 0 ? 4999 : -1;
        }
        if (matches == 0) {
            return occupied;
        }
        int limit = stack.getMaxStackSize() == 1 ? 1
                : stack.getMaxStackSize() * ArchiveConfig.RADIANT_MULTIPLIER.get();
        double fullness = (double) matches / (limit * inventory.getSlots());
        return (int) Math.ceil(fullness * 1000.0 + 500.0);
    }

    private List<ArchiveDeviceBlockEntity> nearbyDevices() {
        if (!(level instanceof ServerLevel serverLevel) || owner() == null) {
            return List.of();
        }
        return nearbyDevices(serverLevel, owner(), worldPosition, radius, personalOnly);
    }

    private static List<ArchiveDeviceBlockEntity> nearbyDevices(ServerLevel level, UUID owner, BlockPos position,
                                                                int radius, boolean personalOnly) {
        int rangeSquared = radius * radius;
        return ArchiveNetworks.get(level.getServer()).loadedDevices(level.getServer(), owner, personalOnly)
                .stream().filter(device -> device.getLevel() == level && horizontalDistanceSquared(position, device.getBlockPos()) <= rangeSquared)
                .toList();
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    void acceptDropped(ItemEntity entity) {
        if (!entity.isAlive() || owner() == null || entity.getPersistentData().getBoolean(REJECTED)) {
            return;
        }
        ItemStack incoming = entity.getItem();
        if (incoming.getCount() == 1) {
            FluidActionResult emptied = FluidUtil.tryEmptyContainer(incoming, fluidIngress, Integer.MAX_VALUE, null, true);
            if (emptied.isSuccess()) {
                entity.setItem(emptied.getResult());
                reject(entity);
                return;
            }
        }
        ItemStack remainder = route(incoming, false);
        if (remainder.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remainder);
            reject(entity);
        }
    }

    private void reject(ItemEntity entity) {
        entity.getPersistentData().putBoolean(REJECTED, true);
        entity.setPickUpDelay(20);
        entity.setDeltaMovement(0.2, 0.2, 0.0);
        entity.hasImpulse = true;
    }

    void deposit(Player player) {
        if (owner() == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Inventory inventory = player.getInventory();
        long now = serverLevel.getGameTime();
        boolean repeat = player.getUUID().equals(lastDepositor) && now - lastDeposit <= 6;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && isFavourite(held)) {
            return;
        }

        ItemStack reference = held.isEmpty() && repeat ? lastOffered : held;
        if (reference.isEmpty()) {
            return;
        }
        lastDeposit = now;
        lastDepositor = player.getUUID();
        lastOffered = reference.copyWithCount(1);
        if (!held.isEmpty()) {
            if (held.getCount() == 1) {
                FluidActionResult emptied = FluidUtil.tryEmptyContainer(held, fluidIngress, Integer.MAX_VALUE, player, true);
                if (emptied.isSuccess()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, emptied.getResult());
                    return;
                }
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, route(held, false));
        }
        if (repeat) {
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (ItemStack.isSameItemSameTags(stack, reference) && !isFavourite(stack)) {
                    inventory.setItem(slot, route(stack, false));
                }
            }
        }
    }

    private static boolean isFavourite(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("Quark:FavoriteItem");
    }

    private void playAbsorptionSound() {
        if (level instanceof ServerLevel serverLevel && ArchiveConfig.MACHINE_SOUNDS.get() && ArchiveConfig.BRAZIER_SOUND.get()) {
            long now = serverLevel.getGameTime();
            if (now - lastSound >= 6) {
                lastSound = now;
                serverLevel.playSound(null, worldPosition, ArcaneArchives.BRAZIER_ABSORB.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSettings(tag);
    }

    private CompoundTag writeSettings(CompoundTag tag) {
        tag.putInt("range", radius);
        tag.putBoolean("subnetwork", personalOnly);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        handleUpdateTag(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return writeSettings(new CompoundTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        radius = tag.contains("range", Tag.TAG_INT) ? Math.max(0, Math.min(300, tag.getInt("range"))) : 150;
        personalOnly = tag.getBoolean("subnetwork");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (!isRemoved()) {
            if (cap == ForgeCapabilities.ITEM_HANDLER) {
                return items.cast();
            }
            if (cap == ForgeCapabilities.FLUID_HANDLER) {
                return fluids.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        items.invalidate();
        fluids.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        items = LazyOptional.of(() -> itemIngress);
        fluids = LazyOptional.of(() -> fluidIngress);
    }

    private record ItemTarget(ArchiveDeviceBlockEntity device, int priority, ItemInsertion insert) {}

    @FunctionalInterface
    private interface ItemInsertion {
        ItemStack apply(ItemStack stack, boolean simulate);
    }

    private final class ItemIngress implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == 0 ? route(stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 999 : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && !stack.isEmpty();
        }
    }

    private final class FluidIngress implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? Integer.MAX_VALUE : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluid) {
            return tank == 0 && !fluid.isEmpty();
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            if (fluid.isEmpty()) {
                return 0;
            }
            List<FluidTank> targets = new ArrayList<>();
            for (ArchiveDeviceBlockEntity device : nearbyDevices()) {
                if (device instanceof RadiantTankBlockEntity target) {
                    FluidTank tank = target.tank();
                    if (tank.isEmpty() || tank.getFluid().isFluidEqual(fluid)) {
                        targets.add(tank);
                    }
                }
            }
            targets.sort(Comparator.comparing(FluidTank::isEmpty));
            FluidStack remaining = fluid.copy();
            for (FluidTank target : targets) {
                remaining.shrink(target.fill(remaining, action));
                if (remaining.isEmpty()) {
                    break;
                }
            }
            int moved = fluid.getAmount() - remaining.getAmount();
            if (action.execute() && moved > 0) {
                playAbsorptionSound();
            }
            return moved;
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
