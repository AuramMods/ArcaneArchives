package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

public final class RadiantTankRenderer implements BlockEntityRenderer<RadiantTankBlockEntity> {
    private static final float NEAR = 0.112F;
    private static final float FAR = 0.888F;
    private static final float FLOOR = 0.053F;

    public RadiantTankRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RadiantTankBlockEntity tank, float tick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        FluidStack fluid = tank.tank().getFluid();
        if (fluid.isEmpty()) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extensions.getStillTexture(fluid);
        if (texture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        int tint = extensions.getTintColor(fluid);
        float height = Math.min(1.0F, (float) fluid.getAmount() / tank.tank().getCapacity()) * 0.89F;
        float top = FLOOR + height;
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose matrix = pose.last();

        for (Direction side : Direction.Plane.HORIZONTAL) {
            Direction right = side.getCounterClockWise();
            float centerX = 0.5F + side.getStepX() * (FAR - NEAR) * 0.5F;
            float centerZ = 0.5F + side.getStepZ() * (FAR - NEAR) * 0.5F;
            float dx = right.getStepX() * (FAR - NEAR) * 0.5F;
            float dz = right.getStepZ() * (FAR - NEAR) * 0.5F;
            float bottomV = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * height;
            vertex(vertices, matrix, side, centerX - dx, FLOOR, centerZ - dz, sprite.getU0(), bottomV, tint, light, overlay);
            vertex(vertices, matrix, side, centerX + dx, FLOOR, centerZ + dz, sprite.getU1(), bottomV, tint, light, overlay);
            vertex(vertices, matrix, side, centerX + dx, top, centerZ + dz, sprite.getU1(), sprite.getV0(), tint, light, overlay);
            vertex(vertices, matrix, side, centerX - dx, top, centerZ - dz, sprite.getU0(), sprite.getV0(), tint, light, overlay);
        }

        vertex(vertices, matrix, Direction.UP, NEAR, top, NEAR, sprite.getU0(), sprite.getV0(), tint, light, overlay);
        vertex(vertices, matrix, Direction.UP, NEAR, top, FAR, sprite.getU0(), sprite.getV1(), tint, light, overlay);
        vertex(vertices, matrix, Direction.UP, FAR, top, FAR, sprite.getU1(), sprite.getV1(), tint, light, overlay);
        vertex(vertices, matrix, Direction.UP, FAR, top, NEAR, sprite.getU1(), sprite.getV0(), tint, light, overlay);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, Direction face,
                               float x, float y, float z, float u, float v, int tint, int light, int overlay) {
        vertices.vertex(pose.pose(), x, y, z)
                .color(tint >> 16 & 255, tint >> 8 & 255, tint & 255, tint >>> 24)
                .uv(u, v).overlayCoords(overlay).uv2(light)
                .normal(pose.normal(), face.getStepX(), face.getStepY(), face.getStepZ()).endVertex();
    }
}
