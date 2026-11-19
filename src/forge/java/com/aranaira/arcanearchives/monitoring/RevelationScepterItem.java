package com.aranaira.arcanearchives.monitoring;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableBlockEntity;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public final class RevelationScepterItem extends Item {
    private static final String MESSAGE = "arcanearchives.data.scepter.";

    public RevelationScepterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide && player != null && context.getHand() == InteractionHand.MAIN_HAND) {
            BlockPos pos = context.getClickedPos();
            BlockEntity device = level.getBlockEntity(pos);
            if (device == null && isSupported(level, pos)) {
                player.sendSystemMessage(message("missing_te").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            } else {
                inspect(device).forEach(line -> player.sendSystemMessage(line.copy().withStyle(ChatFormatting.GOLD)));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.scepter_revelation").withStyle(ChatFormatting.GOLD));
    }

    private static boolean isSupported(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(ArcaneArchives.RADIANT_CHEST.get()) || state.is(ArcaneArchives.RADIANT_TROVE.get())
                || state.is(ArcaneArchives.RADIANT_TANK.get()) || state.is(ArcaneArchives.RADIANT_CRAFTING_TABLE.get())
                || state.is(ArcaneArchives.RADIANT_RESONATOR.get()) || state.is(ArcaneArchives.MONITORING_CRYSTAL.get());
    }

    static List<Component> inspect(@Nullable BlockEntity device) {
        if (device instanceof RadiantChestBlockEntity chest) {
            int free = 0;
            for (int slot = 0; slot < chest.inventory().getSlots(); slot++) {
                if (chest.inventory().getStackInSlot(slot).isEmpty()) {
                    free++;
                }
            }
            return List.of(chest.hasCustomName() ? message("radiant_chest.name", chest.getName())
                            : message("radiant_chest.unnamed"), message("radiant_chest.slots", free));
        }
        if (device instanceof RadiantCraftingTableBlockEntity table) {
            MutableComponent names = Component.empty();
            boolean empty = true;
            for (ItemStack stack : table.grid().getItems()) {
                if (!stack.isEmpty()) {
                    if (!empty) {
                        names.append(", ");
                    }
                    names.append(stack.getHoverName());
                    empty = false;
                }
            }
            return List.of(empty ? message("radiant_crafting.empty") : message("radiant_crafting.contains", names));
        }
        if (device instanceof RadiantTankBlockEntity tank) {
            var fluid = tank.tank().getFluid();
            return List.of(fluid.isEmpty() ? message("radiant_tank.empty") : message("radiant_tank.item", fluid.getDisplayName()),
                    message("radiant_tank.count", fluid.getAmount(), tank.tank().getCapacity()));
        }
        if (device instanceof RadiantTroveBlockEntity trove) {
            var inventory = trove.inventory();
            ItemStack reference = inventory.reference();
            return List.of(reference.isEmpty() ? message("radiant_trove.empty") : message("radiant_trove.item", reference.getHoverName()),
                    message("radiant_trove.count", inventory.count(), inventory.getSlotLimit(0)));
        }
        if (device instanceof RadiantResonatorBlockEntity resonator) {
            var status = resonator.growthStatus();
            var color = switch (status) {
                case HARVESTABLE -> ChatFormatting.GOLD;
                case OBSTRUCTED -> ChatFormatting.RED;
                case RESONATING -> ChatFormatting.GREEN;
                case OFFLINE -> ChatFormatting.DARK_RED;
            };
            return List.of(message("radiant_resonator.progress", resonator.percentGrown()),
                    message("radiant_resonator.status",
                            Component.translatable("arcanearchives.data.tooltip.resonator_status." + status.key()).withStyle(color)));
        }
        if (device instanceof MonitoringCrystalBlockEntity crystal) {
            return List.of(message("monitoring_crystal.facing", crystal.targetDirection().getName()));
        }
        return List.of();
    }

    private static MutableComponent message(String key, Object... args) {
        return Component.translatable(MESSAGE + key, args);
    }
}
