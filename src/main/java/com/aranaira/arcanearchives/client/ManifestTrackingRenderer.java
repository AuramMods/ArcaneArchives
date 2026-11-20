package com.aranaira.arcanearchives.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.core.GlobalPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class ManifestTrackingRenderer {
    private static final List<ProjectedRay> RAYS = new ArrayList<>();

    private ManifestTrackingRenderer() {}

    static void renderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        RAYS.clear();
        Minecraft client = Minecraft.getInstance();
        var locations = ManifestClient.locations();
        if (client.player == null || client.level == null || locations.isEmpty()) {
            return;
        }
        projectRays(event, client, locations);
        renderPriorities(event, client, locations);
    }

    private static void projectRays(RenderLevelStageEvent event, Minecraft client, Set<GlobalPos> locations) {
        Vec3 camera = event.getCamera().getPosition();
        Matrix4f viewProjection = new Matrix4f(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());
        Vec3 feet = client.player.getPosition(event.getPartialTick()).subtract(camera);
        Vector4f origin = project(viewProjection, feet);
        for (var location : locations) {
            if (!location.dimension().equals(client.level.dimension())) {
                continue;
            }
            Vector4f start = new Vector4f(origin);
            Vector4f end = project(viewProjection, Vec3.atCenterOf(location.pos()).subtract(camera));
            if (clipToFrustum(start, end)) {
                RAYS.add(new ProjectedRay(start.x / start.w, start.y / start.w, end.x / end.w, end.y / end.w));
            }
        }
    }

    private static Vector4f project(Matrix4f viewProjection, Vec3 point) {
        return viewProjection.transform(new Vector4f((float) point.x, (float) point.y, (float) point.z, 1));
    }

    // Clip before dividing by w so the player's feet can be behind the first-person camera.
    private static boolean clipToFrustum(Vector4f start, Vector4f end) {
        for (int axis = 0; axis < 3; axis++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                float startDistance = start.w + sign * start.get(axis);
                float endDistance = end.w + sign * end.get(axis);
                if (startDistance < 0 && endDistance < 0) {
                    return false;
                }
                if (startDistance < 0) {
                    start.lerp(end, startDistance / (startDistance - endDistance));
                } else if (endDistance < 0) {
                    end.lerp(start, endDistance / (endDistance - startDistance));
                }
            }
        }
        return start.w > 0 && end.w > 0;
    }

    static void renderGui(RenderGuiEvent.Pre event) {
        Minecraft client = Minecraft.getInstance();
        if (RAYS.isEmpty() || client.level == null || client.player == null) {
            return;
        }

        // Draw through terrain after the shader pack has composited the world.
        var graphics = event.getGuiGraphics();
        var pose = graphics.pose();
        double scale = event.getWindow().getGuiScale();
        float halfWidth = (float) (event.getWindow().getWidth() / scale * 0.5);
        float halfHeight = (float) (event.getWindow().getHeight() / scale * 0.5);
        float hue = ((client.level.getGameTime() % 120) + event.getPartialTick()) / 120.0F;
        int color = 0xFF000000 | Mth.hsvToRgb(hue, 0.65F, 1.0F);
        for (ProjectedRay ray : RAYS) {
            float dx = (ray.endX() - ray.startX()) * halfWidth;
            float dy = (ray.startY() - ray.endY()) * halfHeight;
            pose.pushPose();
            pose.translate((1 + ray.startX()) * halfWidth, (1 - ray.startY()) * halfHeight, 0);
            pose.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
            pose.scale(Mth.sqrt(dx * dx + dy * dy), (float) (1.5 / scale), 1);
            graphics.fill(0, -1, 1, 1, color);
            pose.popPose();
        }
    }

    private static void renderPriorities(RenderLevelStageEvent event, Minecraft client, Set<GlobalPos> locations) {
        Vec3 camera = event.getCamera().getPosition();
        var buffers = client.renderBuffers().bufferSource();
        boolean drewWeight = false;
        for (var location : locations) {
            int priority = ManifestClient.diagnosticPriority(location);
            var pos = location.pos();
            if (priority >= 0 && location.dimension().equals(client.level.dimension()) && pos.distToCenterSqr(camera) < 4096) {
                DebugRenderer.renderFloatingText(event.getPoseStack(), buffers, Integer.toString(priority),
                        pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0xFFF3C985, 0.025F, true, 0, true);
                drewWeight = true;
            }
        }
        if (drewWeight) {
            buffers.endBatch();
        }
    }

    private record ProjectedRay(float startX, float startY, float endX, float endY) {}
}
