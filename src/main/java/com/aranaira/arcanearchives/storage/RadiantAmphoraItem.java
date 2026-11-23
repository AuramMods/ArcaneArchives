package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public final class RadiantAmphoraItem extends Item {
    private static final String TANK_POSITION = "homeTank";
    private static final String TANK_DIMENSION = "homeTankDim";
    private static final String TANK_ID = "homeTankId";
    private static final String MODE = "mode";
    private static final ThreadLocal<TransferActor> TRANSFER_ACTOR = new ThreadLocal<>();

    public RadiantAmphoraItem(Properties properties) {
        super(properties);
    }

    public static boolean linked(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TANK_POSITION, Tag.TAG_LONG)
                && tag.contains(TANK_DIMENSION, Tag.TAG_STRING) && tag.hasUUID(TANK_ID);
    }

    public static boolean drainMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getInt(MODE) == 1;
    }

    public static void toggleMode(ItemStack stack) {
        stack.getOrCreateTag().putInt(MODE, drainMode(stack) ? 0 : 1);
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!event.getLevel().isClientSide && event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START
                && player.isShiftKeyDown() && player.getMainHandItem().is(ArcaneArchives.RADIANT_AMPHORA.get())
                && ArchiveStages.allowsFeature(player, ArchiveFeature.AMPHORA_MODE)) {
            toggleMode(player.getMainHandItem());
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()
                || !(level.getBlockEntity(context.getClickedPos()) instanceof RadiantTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.AMPHORA_LINK)) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (tank.owner() == null || tank.deviceId() == null
                    || !ArchiveNetworks.get(serverLevel.getServer()).sharesHive(player.getUUID(), tank.owner())) {
                player.displayClientMessage(Component.translatable("message.arcanearchives.amphora.denied"), true);
                return InteractionResult.FAIL;
            }
            link(stack, serverLevel, tank);
            player.displayClientMessage(Component.translatable("message.arcanearchives.amphora.linked"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static void link(ItemStack stack, ServerLevel level, RadiantTankBlockEntity tank) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TANK_POSITION, tank.getBlockPos().asLong());
        tag.putString(TANK_DIMENSION, level.dimension().location().toString());
        tag.putUUID(TANK_ID, tank.deviceId());
    }

    @Nullable
    private static RadiantTankBlockEntity resolveTank(ItemStack stack) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread() || !linked(stack)) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        ResourceLocation dimension = ResourceLocation.tryParse(tag.getString(TANK_DIMENSION));
        if (dimension == null) {
            return null;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        BlockPos pos = BlockPos.of(tag.getLong(TANK_POSITION));
        if (level != null && level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof RadiantTankBlockEntity tank
                && tag.getUUID(TANK_ID).equals(tank.deviceId())) {
            return tank;
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack amphora = player.getItemInHand(hand);
        if (!linked(amphora)) {
            return InteractionResultHolder.pass(amphora);
        }
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.AMPHORA_TRANSFER)) {
            return InteractionResultHolder.fail(amphora);
        }
        boolean drain = drainMode(amphora);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, drain ? ClipContext.Fluid.NONE : ClipContext.Fluid.SOURCE_ONLY);
        if (!hit.getType().equals(HitResult.Type.BLOCK)) {
            return InteractionResultHolder.pass(amphora);
        }
        BlockPos target = drain ? hit.getBlockPos().relative(hit.getDirection()) : hit.getBlockPos();
        if (!level.mayInteract(player, hit.getBlockPos()) || !player.mayUseItemAt(target, hit.getDirection(), amphora)) {
            return InteractionResultHolder.fail(amphora);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(amphora);
        }
        RadiantTankBlockEntity tank = resolveTank(amphora);
        if (tank == null || tank.owner() == null
                || !ArchiveNetworks.get(player.getServer()).sharesHive(player.getUUID(), tank.owner())) {
            return InteractionResultHolder.fail(amphora);
        }
        TransferActor previous = TRANSFER_ACTOR.get();
        TRANSFER_ACTOR.set(new TransferActor(tank.deviceId(), player));
        try {
            FluidActionResult result = drain ? placeFluid(amphora, level, player, hand, target)
                    : FluidUtil.tryPickUpFluid(amphora, player, level, target, hit.getDirection());
            return result.isSuccess() ? InteractionResultHolder.consume(result.getResult()) : InteractionResultHolder.fail(amphora);
        } finally {
            if (previous == null) {
                TRANSFER_ACTOR.remove();
            } else {
                TRANSFER_ACTOR.set(previous);
            }
        }
    }

    private record TransferActor(UUID tankId, Player player) {}

    @Nullable
    private static RadiantTankBlockEntity transferTank(ItemStack stack) {
        RadiantTankBlockEntity tank = resolveTank(stack);
        if (tank == null || !(tank.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        TransferActor actor = TRANSFER_ACTOR.get();
        boolean allowed = actor != null && actor.tankId().equals(tank.deviceId())
                ? ArchiveStages.allowsFeature(actor.player(), ArchiveFeature.AMPHORA_TRANSFER)
                : ArchiveStages.allowsOwner(level, tank.owner(), ArchiveFeature.AMPHORA_TRANSFER.id());
        return allowed ? tank : null;
    }

    private static FluidActionResult placeFluid(ItemStack stack, Level level, @Nullable Player player,
                                                InteractionHand hand, BlockPos target) {
        RadiantTankBlockEntity tank = transferTank(stack);
        if (tank == null) {
            return FluidActionResult.FAILURE;
        }
        FluidStack bucket = tank.tank().drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (bucket.getAmount() != FluidType.BUCKET_VOLUME) {
            return FluidActionResult.FAILURE;
        }
        return FluidUtil.tryPlaceFluid(player, level, hand, target, stack, bucket);
    }

    public static void registerDispenser() {
        DispenserBlock.registerBehavior(ArcaneArchives.RADIANT_AMPHORA.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                if (transferTank(stack) == null) {
                    return stack;
                }
                Direction facing = source.getBlockState().getValue(DispenserBlock.FACING);
                BlockPos target = source.getPos().relative(facing);
                ServerLevel level = source.getLevel();
                BlockState targetState = level.getBlockState(target);
                FluidActionResult result = targetState.canBeReplaced() && targetState.getFluidState().isEmpty()
                        ? placeFluid(stack, level, null, InteractionHand.MAIN_HAND, target)
                        : FluidUtil.tryPickUpFluid(stack, null, level, target, facing.getOpposite());
                return result.isSuccess() ? result.getResult() : stack;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (!linked(stack)) {
            tooltip.add(Component.translatable("tooltip.arcanearchives.amphora.unlinked"));
            return;
        }
        CompoundTag tag = stack.getTag();
        BlockPos pos = BlockPos.of(tag.getLong(TANK_POSITION));
        tooltip.add(Component.translatable("tooltip.arcanearchives.amphora.target", pos.getX(), pos.getY(), pos.getZ(),
                tag.getString(TANK_DIMENSION)));
        tooltip.add(Component.translatable(drainMode(stack) ? "tooltip.arcanearchives.amphora.drain" : "tooltip.arcanearchives.amphora.fill"));
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new RemoteTank(stack);
    }

    private static final class RemoteTank implements IFluidHandlerItem, ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IFluidHandlerItem> capability = LazyOptional.of(() -> this);

        private RemoteTank(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getContainer() {
            return stack;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return capability == ForgeCapabilities.FLUID_HANDLER_ITEM ? this.capability.cast() : LazyOptional.empty();
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int slot) {
            RadiantTankBlockEntity tank = resolveTank(stack);
            return tank == null || slot != 0 ? FluidStack.EMPTY : tank.tank().getFluid().copy();
        }

        @Override
        public int getTankCapacity(int slot) {
            RadiantTankBlockEntity tank = resolveTank(stack);
            return tank == null || slot != 0 ? 0 : tank.tank().getCapacity();
        }

        @Override
        public boolean isFluidValid(int slot, FluidStack fluid) {
            RadiantTankBlockEntity tank = resolveTank(stack);
            return tank != null && slot == 0 && tank.tank().isFluidValid(fluid);
        }

        @Override
        public int fill(FluidStack fluid, FluidAction action) {
            RadiantTankBlockEntity tank = transferTank(stack);
            return tank == null ? 0 : tank.tank().fill(fluid, action);
        }

        @Override
        public FluidStack drain(FluidStack fluid, FluidAction action) {
            RadiantTankBlockEntity tank = transferTank(stack);
            return tank == null ? FluidStack.EMPTY : tank.tank().drain(fluid, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
            RadiantTankBlockEntity tank = transferTank(stack);
            return tank == null ? FluidStack.EMPTY : tank.tank().drain(amount, action);
        }
    }
}
