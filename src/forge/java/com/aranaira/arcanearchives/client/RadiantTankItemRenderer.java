package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class RadiantTankItemRenderer extends BlockEntityWithoutLevelRenderer {
    public RadiantTankItemRenderer(Minecraft client) {
        super(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        blockRenderer.renderSingleBlock(ArcaneArchives.RADIANT_TANK.get().defaultBlockState(), pose, buffers, light, overlay);
        stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(tank ->
                RadiantTankRenderer.renderFluid(tank.getFluidInTank(0), tank.getTankCapacity(0), pose, buffers, light, overlay));
    }
}
