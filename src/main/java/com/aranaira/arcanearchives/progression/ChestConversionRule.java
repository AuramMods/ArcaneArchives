package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.aranaira.arcanearchives.storage.RadiantChestBlock;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.Comparator;

public record ChestConversionRule(ResourceLocation id, QuartzTrigger trigger, BlockState output) implements MetadataRecipe {
    static final Comparator<ChestConversionRule> ORDER = Comparator.comparingInt((ChestConversionRule rule) -> rule.trigger.order())
            .thenComparing(ChestConversionRule::id);

    public ChestConversionRule {
        if (!(output.getBlock() instanceof RadiantChestBlock)
                && (!(output.getBlock() instanceof ChestBlock) || output.getValue(ChestBlock.TYPE) != ChestType.SINGLE)) {
            throw new IllegalArgumentException("Chest conversion must produce a Radiant Chest or a single vanilla-style chest");
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.CHEST_CONVERSION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.CHEST_CONVERSION_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<ChestConversionRule> {
        @Override
        public ChestConversionRule fromJson(ResourceLocation id, JsonObject json) {
            return new ChestConversionRule(id, QuartzTrigger.fromJson(json),
                    RuleSerializer.decode(BlockState.CODEC, json.get("output"), "output"));
        }

        @Override
        public ChestConversionRule fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new ChestConversionRule(id, QuartzTrigger.fromNetwork(buffer), buffer.readWithCodec(NbtOps.INSTANCE, BlockState.CODEC));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ChestConversionRule rule) {
            rule.trigger().toNetwork(buffer);
            buffer.writeWithCodec(NbtOps.INSTANCE, BlockState.CODEC, rule.output());
        }
    }
}
