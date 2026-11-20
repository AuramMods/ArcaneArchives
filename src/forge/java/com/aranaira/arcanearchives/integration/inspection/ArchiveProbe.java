package com.aranaira.arcanearchives.integration.inspection;

import com.aranaira.arcanearchives.ArcaneArchives;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.InterModComms;

import java.util.function.Function;

public final class ArchiveProbe implements IProbeInfoProvider {
    private ArchiveProbe() {}

    public static void register() {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe", () -> (Function<ITheOneProbe, Void>) probe -> {
            probe.registerProvider(new ArchiveProbe());
            return null;
        });
    }

    @Override
    public ResourceLocation getID() {
        return ArcaneArchives.id("device_inspection");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState block, IProbeHitData hit) {
        if (block.hasBlockEntity()) {
            ArchiveOverlay.lines(level.getBlockEntity(hit.getPos())).forEach(info::text);
        }
    }
}
