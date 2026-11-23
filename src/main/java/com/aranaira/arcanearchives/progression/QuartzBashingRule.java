package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;
import java.util.Comparator;

public record QuartzBashingRule(ResourceLocation id, QuartzTrigger trigger, @Nullable Drop primary, @Nullable Drop secondary)
        implements MetadataRecipe {
    public static final ResourceLocation DEFAULT = ArcaneArchives.id("quartz/bashing");
    static final Comparator<QuartzBashingRule> ORDER = Comparator.comparingInt((QuartzBashingRule rule) -> rule.trigger.order())
            .thenComparing(QuartzBashingRule::id);

    public QuartzBashingRule {
        if (primary == null && (!id.equals(DEFAULT) || secondary != null)) {
            throw new IllegalArgumentException("A bashing rule requires a primary drop");
        }
        if (secondary != null && primary.chance() + secondary.chance() > 100) {
            throw new IllegalArgumentException("The combined bashing drop chance exceeds 100%");
        }
    }

    ItemStack roll(ItemStack input, RandomSource random) {
        Drop first = primary;
        Drop second = secondary;
        if (first == null) {
            var output = new ItemStack(ArcaneArchives.QUARTZ_SLIVER_ITEM.get());
            int min = ArchiveConfig.SLIVER_CLUSTER_MIN.get();
            first = new Drop(output, ArchiveConfig.SLIVER_CLUSTER_CHANCE.get(), min,
                    Math.max(min, ArchiveConfig.SLIVER_CLUSTER_MAX.get()), true);
            second = new Drop(output, ArchiveConfig.SLIVER_SINGLE_CHANCE.get(), 1, 1, false);
        }
        int roll = random.nextInt(100);
        Drop selected = roll < first.chance() ? first
                : second != null && roll < first.chance() + second.chance() ? second : null;
        if (selected == null) {
            return ItemStack.EMPTY;
        }
        if (selected.consumes()) {
            input.shrink(trigger.input().count());
        }
        int amount = selected.minimum;
        if (selected.maximum > selected.minimum) {
            amount += random.nextInt(selected.maximum - selected.minimum + 1);
        }
        return selected.output.copyWithCount(amount);
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return primary == null ? new ItemStack(ArcaneArchives.QUARTZ_SLIVER_ITEM.get()) : primary.output();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.QUARTZ_BASHING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.QUARTZ_BASHING_TYPE.get();
    }

    public record Drop(ItemStack output, int chance, int minimum, int maximum, boolean consumes) {
        public Drop {
            if (output.isEmpty() || chance < 0 || chance > 100 || minimum < 1 || maximum < minimum || maximum > 4096) {
                throw new IllegalArgumentException("Bashing drops require an item, 0 to 100% chance and a yield of 1 to 4096");
            }
            output = output.copyWithCount(1);
        }

        @Override
        public ItemStack output() {
            return output.copy();
        }

        static Drop fromJson(JsonObject json) {
            var output = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "output"), true);
            return new Drop(output, GsonHelper.getAsInt(json, "chance"), output.getCount(),
                    GsonHelper.getAsInt(json, "maximum", output.getCount()), GsonHelper.getAsBoolean(json, "consumes", true));
        }

        static Drop fromNetwork(FriendlyByteBuf buffer) {
            return new Drop(buffer.readItem(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean());
        }

        void toNetwork(FriendlyByteBuf buffer) {
            buffer.writeItem(output);
            buffer.writeVarInt(chance);
            buffer.writeVarInt(minimum);
            buffer.writeVarInt(maximum);
            buffer.writeBoolean(consumes);
        }
    }

    public static final class Serializer implements RecipeSerializer<QuartzBashingRule> {
        @Override
        public QuartzBashingRule fromJson(ResourceLocation id, JsonObject json) {
            return new QuartzBashingRule(id, QuartzTrigger.fromJson(json),
                    json.has("primary") ? Drop.fromJson(GsonHelper.getAsJsonObject(json, "primary")) : null,
                    json.has("secondary") ? Drop.fromJson(GsonHelper.getAsJsonObject(json, "secondary")) : null);
        }

        @Override
        public QuartzBashingRule fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new QuartzBashingRule(id, QuartzTrigger.fromNetwork(buffer),
                    buffer.readNullable(Drop::fromNetwork), buffer.readNullable(Drop::fromNetwork));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, QuartzBashingRule rule) {
            rule.trigger().toNetwork(buffer);
            buffer.writeNullable(rule.primary(), (buf, drop) -> drop.toNetwork(buf));
            buffer.writeNullable(rule.secondary(), (buf, drop) -> drop.toNetwork(buf));
        }
    }
}
