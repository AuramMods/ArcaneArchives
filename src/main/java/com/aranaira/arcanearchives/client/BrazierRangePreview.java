package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.storage.BrazierBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.GlobalPos;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.LinkedHashSet;
import java.util.Set;

final class BrazierRangePreview {
    private static final Set<GlobalPos> VISIBLE = new LinkedHashSet<>();

    private BrazierRangePreview() {}

    static boolean showing(GlobalPos position) {
        return VISIBLE.contains(position);
    }

    static void toggle(GlobalPos position) {
        if (!VISIBLE.remove(position)) {
            VISIBLE.add(position);
        }
    }

    static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        VISIBLE.clear();
    }

    static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || VISIBLE.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        var camera = event.getCamera().getPosition();
        var poses = event.getPoseStack();
        var buffers = client.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        var entries = VISIBLE.iterator();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        while (entries.hasNext()) {
            var location = entries.next();
            if (!location.dimension().equals(client.level.dimension()) || !client.level.hasChunkAt(location.pos())) {
                continue;
            }
            var pos = location.pos();
            if (!(client.level.getBlockEntity(pos) instanceof BrazierBlockEntity brazier)) {
                entries.remove();
                continue;
            }
            double range = brazier.routingRadius();
            double x = pos.getX() + 0.5;
            double z = pos.getZ() + 0.5;
            LevelRenderer.renderLineBox(poses, lines, x - range, client.level.getMinBuildHeight(), z - range,
                    x + range, client.level.getMaxBuildHeight(), z + range, 1, 0.66F, 0.3F, 0.45F);
            LevelRenderer.renderLineBox(poses, lines, x - range, pos.getY() + 0.05, z - range,
                    x + range, pos.getY() + 0.10, z + range, 1, 0.66F, 0.3F, 0.9F);
        }
        poses.popPose();
        buffers.endBatch(RenderType.lines());
    }
}
