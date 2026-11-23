package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Comparator;

public record MunchstoneFoodRule(ResourceLocation id, Settings settings) implements MetadataRecipe {
    public static final ResourceLocation DEFAULT = ArcaneArchives.id("arsenal/munchstone/server_config");
    static final Comparator<MunchstoneFoodRule> ORDER = Comparator.comparingInt((MunchstoneFoodRule rule) -> rule.settings.order)
            .thenComparing(MunchstoneFoodRule::id);

    public MunchstoneFoodRule {
        if (settings.serverConfig && !id.equals(DEFAULT)) {
            throw new IllegalArgumentException("Only " + DEFAULT + " can use the server configuration's food list");
        }
    }

    public static RecipeSerializer<MunchstoneFoodRule> serializer() {
        return new RuleSerializer<>(Settings.CODEC, MunchstoneFoodRule::new, MunchstoneFoodRule::settings);
    }

    @Nullable
    Nutrition forBlock(BlockState block) {
        if (block.isAir() || block.hasBlockEntity() || !settings.blocks.matches(block)) {
            return null;
        }
        if (settings.serverConfig) {
            int value = configFood(block);
            return value > 0 ? new Nutrition(value, value) : null;
        }
        return new Nutrition(settings.food, settings.saturation);
    }

    private static int configFood(BlockState block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block.getBlock());
        for (String entry : ArchiveConfig.MUNCHSTONE_FOODS.get()) {
            int comma = entry.indexOf(',');
            if (comma < 0) {
                continue;
            }
            String selector = entry.substring(0, comma).strip();
            if (selector.equals(id.toString()) || matchesTag(block, selector)) {
                try {
                    return Integer.parseInt(entry.substring(comma + 1).strip());
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static boolean matchesTag(BlockState block, String selector) {
        ResourceLocation id = selector.startsWith("#") ? ResourceLocation.tryParse(selector.substring(1)) : null;
        return id != null && block.is(BlockTags.create(id));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.MUNCHSTONE_FOOD_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.MUNCHSTONE_FOOD_TYPE.get();
    }

    record Nutrition(int food, float saturation) {}

    public record Settings(BlockSelector blocks, int food, float saturation, int fixedCost, int order, boolean serverConfig) {
        private static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockSelector.CODEC.optionalFieldOf("blocks", BlockSelector.ANY).forGetter(Settings::blocks),
                Codec.INT.optionalFieldOf("food", 0).forGetter(Settings::food),
                Codec.FLOAT.optionalFieldOf("saturation", 0F).forGetter(Settings::saturation),
                Codec.INT.optionalFieldOf("fixed_cost", 0).forGetter(Settings::fixedCost),
                Codec.INT.optionalFieldOf("order", 0).forGetter(Settings::order),
                Codec.BOOL.optionalFieldOf("server_config", false).forGetter(Settings::serverConfig)
        ).apply(instance, Settings::new));

        public Settings {
            if (food < 0 || food > 20 || !Float.isFinite(saturation) || saturation < 0 || saturation > 20
                    || fixedCost < 0 || fixedCost > 27000 || (!serverConfig && food == 0 && saturation == 0)) {
                throw new IllegalArgumentException("Munchstone food and saturation must be from 0 to 20 with a positive total, "
                        + "and cost must be zero (nutrition based) or a positive charge up to 27000");
            }
        }
    }
}
