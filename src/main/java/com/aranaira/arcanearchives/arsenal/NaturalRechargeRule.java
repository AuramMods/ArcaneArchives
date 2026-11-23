package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Comparator;

public record NaturalRechargeRule(ResourceLocation id, Settings settings) implements MetadataRecipe {
    static final Comparator<NaturalRechargeRule> ORDER = Comparator.comparingInt((NaturalRechargeRule rule) -> rule.settings.order)
            .thenComparing(NaturalRechargeRule::id);

    public static RecipeSerializer<NaturalRechargeRule> serializer() {
        return new RuleSerializer<>(Settings.CODEC, NaturalRechargeRule::new, NaturalRechargeRule::settings);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.NATURAL_RECHARGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.NATURAL_RECHARGE_TYPE.get();
    }

    public enum Trigger {
        DROPPED_IN_LAVA, DROPPED_IN_WATER, DROPPED_IN_RAIN, HELD_IN_RAIN, ANIMAL_KILL, NATURAL_CRITICAL_HIT,
        TIMER, EXPERIENCE_PICKUP, ENDERMAN_TELEPORT
    }

    public record Settings(GemSelector gems, Trigger trigger, int charge, double scale, int ticks, boolean toggleWhenFull, int order) {
        private static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GemSelector.CODEC.fieldOf("gems").forGetter(Settings::gems),
                ArchiveTag.names(Trigger::valueOf, ArchiveTag::name).fieldOf("trigger").forGetter(Settings::trigger),
                Codec.INT.fieldOf("charge").forGetter(Settings::charge),
                Codec.DOUBLE.optionalFieldOf("scale", 0.0).forGetter(Settings::scale),
                Codec.INT.optionalFieldOf("ticks", 0).forGetter(Settings::ticks),
                Codec.BOOL.optionalFieldOf("toggle_when_full", false).forGetter(Settings::toggleWhenFull),
                Codec.INT.optionalFieldOf("order", 0).forGetter(Settings::order)
        ).apply(instance, Settings::new));

        public Settings {
            if (charge < -1 || !Double.isFinite(scale) || scale < 0 || (charge == 0 && scale == 0)
                    || (trigger == Trigger.TIMER ? ticks <= 0 : ticks != 0)) {
                throw new IllegalArgumentException("Natural recharge needs a positive fixed or scaled charge, or -1 for full. "
                        + "Only timer rules take a positive interval in ticks");
            }
        }

        int restoredCharge(double eventAmount) {
            return charge == -1 ? -1 : (int) Math.min(Integer.MAX_VALUE, charge + scale * eventAmount);
        }
    }
}
