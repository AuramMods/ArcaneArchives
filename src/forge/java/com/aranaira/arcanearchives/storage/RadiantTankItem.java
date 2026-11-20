package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;

import javax.annotation.Nullable;
import java.util.List;

public final class RadiantTankItem extends BlockItem {
    public RadiantTankItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag tag) {
        return new FluidHandlerItemStack(stack, RadiantTankBlockEntity.BASE_CAPACITY) {
            @Override
            public int getTankCapacity(int tank) {
                return capacity(stack);
            }

            @Override
            public int fill(FluidStack fluid, FluidAction action) {
                capacity = capacity(stack);
                return super.fill(fluid, action);
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        var fluid = FluidUtil.getFluidContained(stack);
        if (fluid.isPresent()) {
            FluidStack stored = fluid.get();
            tooltip.add(Component.translatable("tooltip.arcanearchives.radiant_tank.contents",
                    stored.getDisplayName(), stored.getAmount(), capacity(stack)));
        } else if (stack.getTagElement(StorageUpgrades.SIZE_TAG) != null) {
            tooltip.add(Component.translatable("tooltip.arcanearchives.radiant_tank.capacity", capacity(stack)));
        }
        if (StorageUpgrades.portableHasOption(stack, ArcaneArchives.DEVOURING_CHARM.get())) {
            tooltip.add(Component.translatable("arcanearchives.tooltip.tank.voiding"));
        }
    }

    private static int capacity(ItemStack stack) {
        return RadiantTankBlockEntity.BASE_CAPACITY * StorageUpgrades.portableMultiplier(stack.getTag());
    }
}
