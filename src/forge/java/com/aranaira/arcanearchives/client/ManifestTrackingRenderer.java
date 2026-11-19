package com.aranaira.arcanearchives.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.OptionalDouble;

final class ManifestTrackingRenderer extends RenderType {
    private static final RenderType RAYS = create("arcanearchives_manifest_rays", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1536, false, true, CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(2.5)))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private ManifestTrackingRenderer(String name, VertexFormat format, VertexFormat.Mode mode, int size,
                                     boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || ManifestClient.locations().isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        Vec3 start = client.player.getPosition(event.getPartialTick()).add(0, 1.0, 0).subtract(camera);
        if (!event.getCamera().isDetached()) {
            var look = client.player.getViewVector(event.getPartialTick());
            start = start.add(look.x * 0.4, 0, look.z * 0.4);
        }
        var buffers = client.renderBuffers().bufferSource();
        VertexConsumer vertices = buffers.getBuffer(RAYS);
        var pose = event.getPoseStack().last();
        float hue = (client.level.getGameTime() % 120) / 120.0F;
        int color = Mth.hsvToRgb(hue, 0.48F, 1.0F);
        for (var location : ManifestClient.locations()) {
            if (!location.dimension().equals(client.level.dimension())) {
                continue;
            }
            Vec3 target = Vec3.atCenterOf(location.pos()).subtract(camera);
            Vec3 normal = target.subtract(start).normalize();
            vertex(vertices, pose, start, normal, color);
            vertex(vertices, pose, target, normal, color);
        }
        buffers.endBatch(RAYS);
    }

    private static void vertex(VertexConsumer vertices, com.mojang.blaze3d.vertex.PoseStack.Pose pose, Vec3 pos,
                               Vec3 normal, int color) {
        vertices.vertex(pose.pose(), (float) pos.x, (float) pos.y, (float) pos.z)
                .color(color >> 16 & 255, color >> 8 & 255, color & 255, 206)
                .normal(pose.normal(), (float) normal.x, (float) normal.y, (float) normal.z).endVertex();
    }
}
