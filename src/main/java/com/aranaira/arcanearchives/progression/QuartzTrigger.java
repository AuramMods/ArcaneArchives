package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.CountedIngredient;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.google.gson.JsonObject;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public record QuartzTrigger(int order, CountedIngredient input, BlockSelector target) {
    boolean matches(ItemStack stack, BlockState state) {
        return input.matches(stack) && target.matches(state);
    }

    static QuartzTrigger fromJson(JsonObject json) {
        var target = json.has("target") ? RuleSerializer.decode(BlockSelector.CODEC, json.get("target"), "target") : BlockSelector.ANY;
        return new QuartzTrigger(GsonHelper.getAsInt(json, "order", 0), CountedIngredient.fromJson(json), target);
    }

    static QuartzTrigger fromNetwork(FriendlyByteBuf buffer) {
        return new QuartzTrigger(buffer.readVarInt(), CountedIngredient.fromNetwork(buffer),
                buffer.readWithCodec(NbtOps.INSTANCE, BlockSelector.CODEC));
    }

    void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarInt(order);
        input.toNetwork(buffer);
        buffer.writeWithCodec(NbtOps.INSTANCE, BlockSelector.CODEC, target);
    }
}
