package com.aranaira.arcanearchives.arsenal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public record GemSelector(List<GemType> gems, Optional<ResourceLocation> gemTag, Optional<ResourceLocation> cutTag,
                          Optional<ResourceLocation> colorTag) {
    public static final GemSelector ANY = new GemSelector(List.of(), Optional.empty(), Optional.empty(), Optional.empty());
    public static final Codec<GemSelector> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ArchiveTag.names(GemType::valueOf, GemType::id).listOf().optionalFieldOf("gems", List.of()).forGetter(GemSelector::gems),
            ResourceLocation.CODEC.optionalFieldOf("gem_tag").forGetter(GemSelector::gemTag),
            ResourceLocation.CODEC.optionalFieldOf("cut_tag").forGetter(GemSelector::cutTag),
            ResourceLocation.CODEC.optionalFieldOf("color_tag").forGetter(GemSelector::colorTag)
    ).apply(instance, GemSelector::new));

    public GemSelector {
        gems = List.copyOf(gems);
    }

    public boolean matches(Level level, GemType gem) {
        return (gems.isEmpty() || gems.contains(gem))
                && gemTag.map(tag -> ArchiveTag.contains(level, ArchiveTag.Domain.GEM, tag, gem)).orElse(true)
                && cutTag.map(tag -> ArchiveTag.contains(level, ArchiveTag.Domain.CUT, tag, gem.cut)).orElse(true)
                && colorTag.map(tag -> ArchiveTag.contains(level, ArchiveTag.Domain.COLOR, tag, gem.color)).orElse(true);
    }
}
