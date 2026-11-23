package com.aranaira.arcanearchives.crafting;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.BiFunction;
import java.util.function.Function;

public record RuleSerializer<R extends MetadataRecipe, D>(Codec<D> codec, BiFunction<ResourceLocation, D, R> make,
                                                          Function<R, D> data) implements RecipeSerializer<R> {
    @Override
    public R fromJson(ResourceLocation id, JsonObject json) {
        return make.apply(id, decode(codec, json, id.toString()));
    }

    @Override
    public R fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return make.apply(id, buffer.readWithCodec(NbtOps.INSTANCE, codec));
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, R recipe) {
        buffer.writeWithCodec(NbtOps.INSTANCE, codec, data.apply(recipe));
    }

    public static <T> T decode(Codec<T> codec, JsonElement json, String field) {
        return codec.parse(JsonOps.INSTANCE, json).getOrThrow(false, error -> {
            throw new JsonSyntaxException("Invalid " + field + ": " + error);
        });
    }
}
