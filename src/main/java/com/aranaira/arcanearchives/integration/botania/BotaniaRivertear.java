package com.aranaira.arcanearchives.integration.botania;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArsenalGems;
import com.aranaira.arcanearchives.arsenal.GemType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import vazkii.botania.api.block.PetalApothecary;

public final class BotaniaRivertear {
    private BotaniaRivertear() {}

    public static boolean isApothecary(BlockEntity target) {
        return target instanceof PetalApothecary;
    }

    public static void fill(ServerPlayer player, BlockPos pos) {
        var rivertear = ArcaneArchives.GEMS.get(GemType.RIVERTEAR).get();
        if (player.serverLevel().getBlockEntity(pos) instanceof PetalApothecary apothecary
                && apothecary.getFluid() == PetalApothecary.State.EMPTY
                && (player.getInventory().items.stream().anyMatch(stack -> stack.is(rivertear))
                    || ArsenalGems.available(player).stream().anyMatch(gem -> gem.is(GemType.RIVERTEAR)))) {
            apothecary.setFluid(PetalApothecary.State.WATER);
        }
    }
}
