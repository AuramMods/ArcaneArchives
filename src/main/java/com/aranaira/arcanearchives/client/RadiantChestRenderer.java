package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;

final class RadiantChestRenderer implements BlockEntityRenderer<RadiantChestBlockEntity> {
    private final ItemRenderer items;

    RadiantChestRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(RadiantChestBlockEntity chest, float tick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        var stack = chest.displayStack();
        var face = chest.displayFacing();
        if (stack.isEmpty() || !face.getAxis().isHorizontal()) {
            return;
        }

        pose.pushPose();
        pose.translate(0.5 + 0.47 * face.getStepX(), 0.435, 0.5 + 0.47 * face.getStepZ());
        pose.mulPose(Axis.YP.rotationDegrees(180 - face.toYRot()));
        pose.scale(0.6F, 0.6F, 0.6F);
        items.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, overlay, pose, buffers,
                chest.getLevel(), (int) chest.getBlockPos().asLong());
        pose.popPose();
    }
}
