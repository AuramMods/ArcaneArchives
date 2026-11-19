package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public final class ResonatorLoopSound extends AbstractTickableSoundInstance {
    private static final Map<RadiantResonatorBlockEntity, ResonatorLoopSound> LOOPS = new WeakHashMap<>();
    private static final double HEARING_RANGE_SQUARED = 16.0 * 16.0;
    private final WeakReference<RadiantResonatorBlockEntity> resonator;

    private ResonatorLoopSound(RadiantResonatorBlockEntity resonator, Vec3 center) {
        super(ArcaneArchives.RESONATOR_LOOP.get(), SoundSource.BLOCKS, RandomSource.create());
        this.resonator = new WeakReference<>(resonator);
        x = center.x;
        y = center.y;
        z = center.z;
        looping = true;
        volume = ArchiveConfig.RESONATOR_LOOP_VOLUME.get().floatValue();
    }

    public static void startIfNeeded(RadiantResonatorBlockEntity resonator) {
        Minecraft client = Minecraft.getInstance();
        if (!audible(client) || client.level != resonator.getLevel()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(resonator.getBlockPos());
        if (client.gameRenderer.getMainCamera().getPosition().distanceToSqr(center) >= HEARING_RANGE_SQUARED) {
            return;
        }
        var manager = client.getSoundManager();
        ResonatorLoopSound existing = LOOPS.get(resonator);
        if (existing == null || !manager.isActive(existing)) {
            ResonatorLoopSound sound = new ResonatorLoopSound(resonator, center);
            LOOPS.put(resonator, sound);
            manager.play(sound);
        }
    }

    @Override
    public void tick() {
        RadiantResonatorBlockEntity active = resonator.get();
        Minecraft client = Minecraft.getInstance();
        if (active == null || active.getLevel() != client.level || !active.isResonating() || !audible(client)
                || client.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) >= HEARING_RANGE_SQUARED) {
            LOOPS.remove(active, this);
            stop();
        } else {
            volume = ArchiveConfig.RESONATOR_LOOP_VOLUME.get().floatValue();
        }
    }

    private static boolean audible(Minecraft client) {
        return ArchiveConfig.RESONATOR_LOOP_ENABLED.get() && ArchiveConfig.RESONATOR_LOOP_VOLUME.get() > 0
                && client.options.getSoundSourceVolume(SoundSource.MASTER) > 0
                && client.options.getSoundSourceVolume(SoundSource.BLOCKS) > 0;
    }
}
