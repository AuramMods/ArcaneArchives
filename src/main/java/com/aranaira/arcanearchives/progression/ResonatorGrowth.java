package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Comparator;
import java.util.Optional;

public record ResonatorGrowth(ResourceLocation id, Settings settings) implements MetadataRecipe {
    public static final ResourceLocation DEFAULT = ArcaneArchives.id("resonator/raw_quartz_cluster");
    public static final Comparator<ResonatorGrowth> ORDER = Comparator.comparingInt((ResonatorGrowth rule) -> rule.settings.order)
            .thenComparing(ResonatorGrowth::id);

    public static RecipeSerializer<ResonatorGrowth> serializer() {
        return new RuleSerializer<>(Settings.CODEC, ResonatorGrowth::new, ResonatorGrowth::settings);
    }

    public boolean matches(Level level, BlockPos pos) {
        return settings.dimension.map(level.dimension().location()::equals).orElse(true)
                && settings.foundation.matches(level.getBlockState(pos.below()))
                && (settings.minimumLight == 0 || level.getMaxLocalRawBrightness(pos.above()) >= settings.minimumLight);
    }

    public int duration() {
        return settings.ticks == 0 ? ArchiveConfig.RESONATOR_TICKS.get() : settings.ticks;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.RESONATOR_GROWTH_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.RESONATOR_GROWTH_TYPE.get();
    }

    public record Settings(int order, int ticks, boolean requiresOnlineOwner, BlockSelector foundation,
                           Optional<ResourceLocation> dimension, int minimumLight, BlockState output) {
        static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("order", 0).forGetter(Settings::order),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("ticks", 0).forGetter(Settings::ticks),
                Codec.BOOL.optionalFieldOf("requires_online_owner", true).forGetter(Settings::requiresOnlineOwner),
                BlockSelector.CODEC.optionalFieldOf("foundation", BlockSelector.ANY).forGetter(Settings::foundation),
                ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(Settings::dimension),
                Codec.intRange(0, 15).optionalFieldOf("minimum_light", 0).forGetter(Settings::minimumLight),
                BlockState.CODEC.fieldOf("output").forGetter(Settings::output)
        ).apply(instance, Settings::new));

        public Settings {
            if (ticks < 0 || minimumLight < 0 || minimumLight > 15 || output.isAir()) {
                throw new IllegalArgumentException("Resonator growth requires a non-air output, a nonnegative duration and light between 0 and 15");
            }
        }
    }
}
