package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveCommands;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public final class DebugOrbItem extends Item {
    public DebugOrbItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        var level = context.getLevel();
        if (player == null || !player.isCreative()
                || !(level.getBlockEntity(context.getClickedPos()) instanceof ArchiveDeviceBlockEntity entity)
                || !(entity instanceof RadiantChestBlockEntity || entity instanceof PortableStorageBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown() && entity instanceof RadiantTroveBlockEntity trove) {
                String change = trove.inventory().toggleDebugContents() ? "filled" : "cleared";
                player.displayClientMessage(Component.translatable("message.arcanearchives.debugorb." + change), true);
            } else {
                showReport(serverPlayer, entity);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return player.isCreative();
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.debugorb").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("arcanearchives.tooltip.creativeonly").withStyle(ChatFormatting.RED));
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        var player = event.getEntity();
        if (player.isCreative() && player.getMainHandItem().is(ArcaneArchives.DEBUG_ORB.get())
                && event.getLevel().getBlockEntity(event.getPos()) instanceof RadiantChestBlockEntity chest) {
            event.setCanceled(true);
            if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && player instanceof ServerPlayer serverPlayer) {
                showReport(serverPlayer, chest);
            }
        }
    }

    private static void showReport(ServerPlayer player, ArchiveDeviceBlockEntity entity) {
        var pos = GlobalPos.of(player.serverLevel().dimension(), entity.getBlockPos());
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType());
        var networks = ArchiveNetworks.get(player.serverLevel().getServer());
        String report = "Device " + type + " at " + pos.pos().toShortString() + " in " + pos.dimension().location()
                + "\nOwner " + entity.owner() + "\nDevice ID " + entity.deviceId();
        if (entity.owner() != null) {
            boolean indexed = networks.devicesOwnedBy(entity.owner(), true).stream().anyMatch(device -> device.location().equals(pos)
                    && device.id().equals(entity.deviceId()));
            report += "\nHive " + networks.hiveOwner(entity.owner()) + "\nIndexed " + indexed;
        }
        if (entity instanceof RadiantChestBlockEntity chest) {
            var display = chest.displayStack();
            long count = 0;
            for (int i = 0; i < chest.inventory().getSlots(); i++) {
                count += chest.inventory().getStackInSlot(i).getCount();
            }
            report += "\nName " + (chest.getCustomName() == null ? "" : chest.getCustomName().getString())
                    + "\nItems " + count + "\nExisting items only " + chest.existingItemsOnly()
                    + "\nDisplay " + BuiltInRegistries.ITEM.getKey(display.getItem()) + " facing " + chest.displayFacing();
        } else if (entity instanceof RadiantTroveBlockEntity trove) {
            var inventory = trove.inventory();
            report += "\nStored " + BuiltInRegistries.ITEM.getKey(inventory.reference().getItem())
                    + "\nItems " + inventory.count() + " / " + inventory.getSlotLimit(0)
                    + "\nLocked " + trove.upgrades().hasOption(ArcaneArchives.RADIANT_KEY.get())
                    + "\nVoiding " + trove.upgrades().hasOption(ArcaneArchives.DEVOURING_CHARM.get());
        } else if (entity instanceof RadiantTankBlockEntity tank) {
            report += "\nStored " + ForgeRegistries.FLUIDS.getKey(tank.tank().getFluid().getFluid())
                    + "\nFluid " + tank.tank().getFluidAmount() + " / " + tank.tank().getCapacity() + " mB"
                    + "\nVoiding " + tank.upgrades().hasOption(ArcaneArchives.DEVOURING_CHARM.get());
        }
        player.sendSystemMessage(Component.literal(report).append("\n").append(ArchiveCommands.copyText(report)));
    }
}
