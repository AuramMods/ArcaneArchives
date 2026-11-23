package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record ArsenalEffectRule(ResourceLocation id, Settings settings) implements MetadataRecipe {
    public static RecipeSerializer<ArsenalEffectRule> serializer() {
        return new RuleSerializer<>(Settings.CODEC, ArsenalEffectRule::new, ArsenalEffectRule::settings);
    }

    public ArsenalEffectRule {
        if (!settings.effect().id().equals(id)) {
            throw new IllegalArgumentException("Effect settings for " + settings.effect() + " must use " + settings.effect().id());
        }
    }

    public static Settings settings(Level level, Effect effect) {
        return level.getRecipeManager().byKey(effect.id()).orElse(null) instanceof ArsenalEffectRule rule
                ? rule.settings : effect.defaults;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.ARSENAL_EFFECT_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.ARSENAL_EFFECT_TYPE.get();
    }

    public enum Effect {
        AGEGLEAM(1, 8000, 3.5),
        CLEANSEGLEAM(1, 0, 3.5),
        SWITCHGLEAM(1, 10, 40),
        SALVEGLEAM(1, 20, 1),
        PHOENIXWAY_FIRE_RESISTANCE(12, 600, 0);

        private final ResourceLocation id;
        private final Settings defaults;

        Effect(int charge, int ticks, double amount) {
            id = ArcaneArchives.id("arsenal/effect/" + ArchiveTag.name(this));
            defaults = new Settings(this, charge, ticks, amount);
        }

        public ResourceLocation id() {
            return id;
        }
    }

    public record Settings(Effect effect, int charge, int ticks, double amount) {
        private static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ArchiveTag.names(Effect::valueOf, ArchiveTag::name).fieldOf("effect").forGetter(Settings::effect),
                Codec.INT.fieldOf("charge").forGetter(Settings::charge),
                Codec.INT.fieldOf("ticks").forGetter(Settings::ticks),
                Codec.DOUBLE.fieldOf("amount").forGetter(Settings::amount)
        ).apply(instance, Settings::new));

        public Settings {
            if (charge < 1 || charge > 27000 || ticks < 0 || !Double.isFinite(amount) || amount < 0 || amount > 128) {
                throw new IllegalArgumentException("Effect charge must be 1–27000, ticks must be nonnegative and amount must be 0–128");
            }
        }
    }
}
