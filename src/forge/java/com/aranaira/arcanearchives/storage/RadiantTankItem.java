package com.aranaira.arcanearchives.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
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
        return new FluidHandlerItemStack(stack, RadiantTankBlockEntity.BASE_CAPACITY);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FluidUtil.getFluidContained(stack).ifPresent(fluid -> tooltip.add(Component.translatable(
                "tooltip.arcanearchives.radiant_tank.contents", fluid.getDisplayName(), fluid.getAmount(),
                RadiantTankBlockEntity.BASE_CAPACITY)));
    }
}
