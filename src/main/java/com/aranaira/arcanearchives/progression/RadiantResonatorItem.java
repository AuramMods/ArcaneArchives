package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.integration.stages.DeviceBlockItem;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.util.FakePlayer;

public final class RadiantResonatorItem extends DeviceBlockItem {
    public RadiantResonatorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null && !(player instanceof FakePlayer) && context.getLevel() instanceof ServerLevel level) {
            int limit = ArchiveConfig.RESONATOR_LIMIT.get();
            if (ArchiveNetworks.get(level.getServer())
                    .countPersonalDevices(player.getUUID(), ArcaneArchives.RADIANT_RESONATOR_ENTITY.getId()) >= limit) {
                player.displayClientMessage(Component.translatable("message.arcanearchives.resonator.limit", limit), true);
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }
}
