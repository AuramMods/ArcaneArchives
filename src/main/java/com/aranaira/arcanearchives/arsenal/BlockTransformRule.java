package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.FluidSelector;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.Comparator;

public record BlockTransformRule(ResourceLocation id, Settings settings) implements MetadataRecipe {
    private static final Comparator<BlockTransformRule> ORDER = Comparator.comparingInt((BlockTransformRule rule) -> rule.settings.order)
            .thenComparing(BlockTransformRule::id);

    public static RecipeSerializer<BlockTransformRule> serializer() {
        return new RuleSerializer<>(Settings.CODEC, BlockTransformRule::new, BlockTransformRule::settings);
    }

    @Nullable
    static BlockTransformRule find(Level level, GemType gem, BlockState target) {
        return level.getRecipeManager().getAllRecipesFor(ArcaneArchives.BLOCK_TRANSFORM_TYPE.get()).stream()
                .filter(rule -> rule.settings.gem == gem && rule.settings.matches(level, target)).min(ORDER).orElse(null);
    }

    BlockState output(Level level, BlockPos pos, BlockState target) {
        if (settings.dynamicFire) {
            return FireBlock.getState(level, pos);
        }
        var output = settings.output;
        if (settings.preserveFacing && target.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && output.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return output.setValue(BlockStateProperties.HORIZONTAL_FACING, target.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        return output;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.BLOCK_TRANSFORM_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.BLOCK_TRANSFORM_TYPE.get();
    }

    public enum Target {
        ANY, REPLACEABLE, AIR, LIQUID;

        boolean matches(BlockState state) {
            return switch (this) {
                case ANY -> true;
                case REPLACEABLE -> state.canBeReplaced();
                case AIR -> state.isAir();
                case LIQUID -> state.getBlock() instanceof LiquidBlock;
            };
        }
    }

    public record Settings(GemType gem, Target target, BlockSelector blocks, FluidSelector fluids, BlockState output,
                           int charge, int order, boolean preserveFacing, boolean creativeFree,
                           boolean coolDimensionsOnly, boolean dynamicFire) {
        private static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ArchiveTag.names(GemType::valueOf, GemType::id).fieldOf("gem").forGetter(Settings::gem),
                ArchiveTag.names(Target::valueOf, ArchiveTag::name).optionalFieldOf("target", Target.ANY).forGetter(Settings::target),
                BlockSelector.CODEC.optionalFieldOf("blocks", BlockSelector.ANY).forGetter(Settings::blocks),
                FluidSelector.CODEC.optionalFieldOf("fluids", FluidSelector.ANY).forGetter(Settings::fluids),
                BlockState.CODEC.fieldOf("output").forGetter(Settings::output),
                Codec.INT.optionalFieldOf("charge", 1).forGetter(Settings::charge),
                Codec.INT.optionalFieldOf("order", 0).forGetter(Settings::order),
                Codec.BOOL.optionalFieldOf("preserve_facing", false).forGetter(Settings::preserveFacing),
                Codec.BOOL.optionalFieldOf("creative_free", false).forGetter(Settings::creativeFree),
                Codec.BOOL.optionalFieldOf("cool_dimensions_only", false).forGetter(Settings::coolDimensionsOnly),
                Codec.BOOL.optionalFieldOf("dynamic_fire", false).forGetter(Settings::dynamicFire)
        ).apply(instance, Settings::new));

        public Settings {
            switch (gem) {
                case ORDERSTONE, RIVERTEAR, MOUNTAINTEAR, PARCHTEAR, PHOENIXWAY -> { }
                default -> throw new IllegalArgumentException("This gem has no block transformation: " + gem.id());
            }
            if (charge < 1 || charge > 27000 || (dynamicFire && !output.is(Blocks.FIRE))) {
                throw new IllegalArgumentException("Block transforms need a charge cost from 1 to 27000 and a fire output for dynamic fire");
            }
        }

        boolean matches(Level level, BlockState state) {
            return (!coolDimensionsOnly || !level.dimensionType().ultraWarm()) && target.matches(state)
                    && blocks.matches(state) && fluids.matches(state.getFluidState());
        }
    }
}
