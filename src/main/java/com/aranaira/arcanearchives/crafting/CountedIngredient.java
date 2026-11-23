package com.aranaira.arcanearchives.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record CountedIngredient(Ingredient ingredient, int count) {
    public CountedIngredient {
        if (ingredient == Ingredient.EMPTY || count < 1 || count > 4096) {
            throw new IllegalArgumentException("A processing ingredient requires a selector and a count from 1 to 4096");
        }
    }

    public boolean matches(ItemStack stack) {
        return stack.getCount() >= count && ingredient.test(stack);
    }

    public static CountedIngredient fromJson(JsonObject json) {
        return new CountedIngredient(Ingredient.fromJson(json.get("input")), GsonHelper.getAsInt(json, "count", 1));
    }

    public static CountedIngredient fromNetwork(FriendlyByteBuf buffer) {
        return new CountedIngredient(Ingredient.fromNetwork(buffer), buffer.readVarInt());
    }

    public void toNetwork(FriendlyByteBuf buffer) {
        ingredient.toNetwork(buffer);
        buffer.writeVarInt(count);
    }
}
