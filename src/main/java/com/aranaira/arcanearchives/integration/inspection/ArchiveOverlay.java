package com.aranaira.arcanearchives.integration.inspection;

import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

final class ArchiveOverlay {
    private static final String TEXT = "arcanearchives.data.tooltip.";

    private ArchiveOverlay() {}

    static List<Component> lines(@Nullable BlockEntity device) {
        if (device instanceof RadiantChestBlockEntity chest && chest.hasCustomName()) {
            return List.of(Component.translatable(TEXT + "chest_label", chest.getName()).withStyle(ChatFormatting.GOLD));
        }
        if (device instanceof RadiantTroveBlockEntity trove) {
            var items = trove.inventory();
            return items.count() == 0 ? List.of(Component.translatable(TEXT + "trove_empty"))
                    : List.of(Component.translatable(TEXT + "trove_stored", items.count(), items.reference().getHoverName())
                            .withStyle(ChatFormatting.GOLD));
        }
        if (device instanceof RadiantResonatorBlockEntity resonator) {
            return List.of(Component.translatable(TEXT + "resonator_percentage", resonator.percentGrown())
                            .withStyle(ChatFormatting.GOLD),
                    resonator.growthStatus().label());
        }
        return List.of();
    }
}
