package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableBlockEntity;
import com.aranaira.arcanearchives.monitoring.MonitoringCrystalBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ManifestContents {
    private ManifestContents() {}

    public static List<Row> collect(MinecraftServer server, UUID player, boolean personalOnly) {
        var networks = ArchiveNetworks.get(server);
        Map<Source, Long> counts = new LinkedHashMap<>();
        Set<GlobalPos> monitored = new HashSet<>();
        for (var device : networks.loadedDevices(server, player, personalOnly)) {
            Level level = device.getLevel();
            GlobalPos location = GlobalPos.of(level.dimension(), device.getBlockPos());
            Component label = device.getBlockState().getBlock().getName();
            if (device instanceof RadiantChestBlockEntity chest) {
                addInventory(counts, location, chest.getDisplayName(), chest.inventory());
            } else if (device instanceof RadiantTroveBlockEntity trove) {
                addStack(counts, location, label, trove.inventory().getStackInSlot(0));
            } else if (device instanceof RadiantTankBlockEntity tank) {
                FluidStack fluid = tank.tank().getFluid();
                if (!fluid.isEmpty()) {
                    add(counts, location, label, Variant.of(fluid), fluid.getAmount());
                }
            } else if (device instanceof GemCuttersTableBlockEntity table) {
                addInventory(counts, location, label, table.drawer());
            } else if (device instanceof RadiantCraftingTableBlockEntity table) {
                for (ItemStack item : table.grid().getItems()) {
                    addStack(counts, location, label, item);
                }
            } else if (device instanceof MonitoringCrystalBlockEntity crystal) {
                crystal.targetInventory().ifPresent(inventory -> {
                    BlockPos target = inventoryLocation(level, crystal.targetPos(), inventory);
                    GlobalPos observed = GlobalPos.of(level.dimension(), target);
                    if (monitored.add(observed)) {
                        Component targetName = level.getBlockEntity(target) instanceof Nameable named
                                ? named.getDisplayName() : level.getBlockState(target).getBlock().getName();
                        addInventory(counts, observed, targetName, inventory);
                    }
                });
            }
        }
        List<Row> rows = new ArrayList<>(counts.size());
        counts.forEach((source, count) -> rows.add(new Row(source.variant(), source.location(), source.label(), count)));
        return List.copyOf(rows);
    }

    private static BlockPos inventoryLocation(Level level, BlockPos target, IItemHandler inventory) {
        BlockState state = level.getBlockState(target);
        if (inventory.getSlots() == 54 && state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = target.relative(ChestBlock.getConnectedDirection(state));
            if (level.hasChunkAt(other)) {
                BlockState partner = level.getBlockState(other);
                if (partner.is(state.getBlock()) && partner.getValue(ChestBlock.FACING) == state.getValue(ChestBlock.FACING)
                        && partner.getValue(ChestBlock.TYPE) == state.getValue(ChestBlock.TYPE).getOpposite()) {
                    return target.compareTo(other) <= 0 ? target : other;
                }
            }
        }
        return target;
    }

    private static void addInventory(Map<Source, Long> counts, GlobalPos location, Component label, IItemHandler inventory) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            addStack(counts, location, label, inventory.getStackInSlot(slot));
        }
    }

    private static void addStack(Map<Source, Long> counts, GlobalPos location, Component label, ItemStack stack) {
        if (!stack.isEmpty()) {
            add(counts, location, label, Variant.of(stack), stack.getCount());
        }
    }

    private static void add(Map<Source, Long> counts, GlobalPos location, Component label, Variant variant, long amount) {
        counts.merge(new Source(variant, location, label), amount, Long::sum);
    }

    private record Source(Variant variant, GlobalPos location, Component label) {}

    public record Variant(ResourceLocation id, boolean fluid, CompoundTag prototype) {
        public static Variant of(ItemStack stack) {
            return new Variant(BuiltInRegistries.ITEM.getKey(stack.getItem()), false,
                    stack.copyWithCount(1).save(new CompoundTag()));
        }

        static Variant of(FluidStack stack) {
            FluidStack prototype = stack.copy();
            prototype.setAmount(1);
            return new Variant(ForgeRegistries.FLUIDS.getKey(stack.getFluid()), true, prototype.writeToNBT(new CompoundTag()));
        }

        public ItemStack item() {
            return fluid ? ItemStack.EMPTY : ItemStack.of(prototype.copy());
        }

        public FluidStack fluidStack() {
            return fluid ? FluidStack.loadFluidStackFromNBT(prototype.copy()) : FluidStack.EMPTY;
        }
    }

    public record Row(Variant variant, GlobalPos location, Component label, long amount) {}
}
