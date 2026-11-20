package com.aranaira.arcanearchives.integration.inspection;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlock;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestBlock;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveBlock;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class ArchiveJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Provider.INSTANCE, RadiantChestBlockEntity.class);
        registration.registerBlockDataProvider(Provider.INSTANCE, RadiantTroveBlockEntity.class);
        registration.registerBlockDataProvider(Provider.INSTANCE, RadiantResonatorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, RadiantChestBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, RadiantTroveBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, RadiantResonatorBlock.class);
    }

    private enum Provider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;

        private static final ResourceLocation ID = ArcaneArchives.id("device_inspection");
        private static final String DATA = ID.toString();

        @Override
        public ResourceLocation getUid() {
            return ID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            var lines = new ListTag();
            for (Component line : ArchiveOverlay.lines(accessor.getBlockEntity())) {
                lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
            }
            data.put(DATA, lines);
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            var data = accessor.getServerData();
            if (data.contains(DATA, Tag.TAG_LIST)) {
                var lines = data.getList(DATA, Tag.TAG_STRING);
                for (int index = 0; index < lines.size(); index++) {
                    var line = Component.Serializer.fromJson(lines.getString(index));
                    if (line != null) {
                        tooltip.add(line);
                    }
                }
            }
        }
    }
}
