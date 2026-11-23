package com.aranaira.arcanearchives.integration.stages;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class DeviceBlockItem extends BlockItem {
    public DeviceBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        var id = ArchiveStages.placement(BuiltInRegistries.BLOCK.getKey(getBlock()));
        return ArchiveStages.allowsFeature(context.getLevel(), context.getPlayer(), id)
                ? super.place(context) : InteractionResult.FAIL;
    }
}
