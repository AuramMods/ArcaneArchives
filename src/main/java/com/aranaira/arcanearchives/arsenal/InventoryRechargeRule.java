package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.CountedIngredient;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;
import java.util.Comparator;

public record InventoryRechargeRule(ResourceLocation id, int order, GemSelector gems, CountedIngredient input, int maximum,
                                    int chargePerItem, boolean emptyOnly, boolean matchGemColor, @Nullable ResourceLocation powderColorTag,
                                    ItemStack remainder, boolean preferLastSlot) implements MetadataRecipe {
    static final Comparator<InventoryRechargeRule> ORDER = Comparator.comparingInt(InventoryRechargeRule::order)
            .thenComparing(InventoryRechargeRule::id);

    public InventoryRechargeRule {
        if (maximum < input.count() || maximum > 4096 || chargePerItem < -1 || chargePerItem == 0
                || remainder.getCount() > remainder.getMaxStackSize()) {
            throw new IllegalArgumentException("Invalid gem recharge. Maximum must be between the ingredient minimum and 4096, "
                    + "charge must be positive or -1 for full, and the remainder must fit in one stack");
        }
        remainder = remainder.copy();
    }

    boolean matches(Level level, GemType gem, ItemStack stack) {
        if (!input.matches(stack)) {
            return false;
        }
        if (!matchGemColor && powderColorTag == null) {
            return true;
        }
        if (!(stack.getItem() instanceof ChromaticPowderItem)) {
            return false;
        }
        var color = ChromaticPowderItem.color(stack);
        return (!matchGemColor || gem.color == color)
                && (powderColorTag == null || ArchiveTag.contains(level, ArchiveTag.Domain.COLOR, powderColorTag, color));
    }

    @Override
    public ItemStack remainder() {
        return remainder.copy();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return remainder();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.INVENTORY_RECHARGE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.INVENTORY_RECHARGE_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<InventoryRechargeRule> {
        @Override
        public InventoryRechargeRule fromJson(ResourceLocation id, JsonObject json) {
            var input = CountedIngredient.fromJson(json);
            return new InventoryRechargeRule(id, GsonHelper.getAsInt(json, "order", 0),
                    json.has("gems") ? RuleSerializer.decode(GemSelector.CODEC, json.get("gems"), "gems") : GemSelector.ANY,
                    input, GsonHelper.getAsInt(json, "maximum", input.count()), GsonHelper.getAsInt(json, "charge_per_item", -1),
                    GsonHelper.getAsBoolean(json, "empty_only", false), GsonHelper.getAsBoolean(json, "match_gem_color", false),
                    json.has("powder_color_tag") ? RuleSerializer.decode(ResourceLocation.CODEC, json.get("powder_color_tag"), "powder_color_tag") : null,
                    json.has("remainder") ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "remainder"), true) : ItemStack.EMPTY,
                    GsonHelper.getAsBoolean(json, "prefer_last_slot", false));
        }

        @Override
        public InventoryRechargeRule fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new InventoryRechargeRule(id, buffer.readVarInt(), buffer.readWithCodec(NbtOps.INSTANCE, GemSelector.CODEC),
                    CountedIngredient.fromNetwork(buffer), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readNullable(FriendlyByteBuf::readResourceLocation), buffer.readItem(), buffer.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InventoryRechargeRule rule) {
            buffer.writeVarInt(rule.order());
            buffer.writeWithCodec(NbtOps.INSTANCE, GemSelector.CODEC, rule.gems());
            rule.input().toNetwork(buffer);
            buffer.writeVarInt(rule.maximum());
            buffer.writeVarInt(rule.chargePerItem());
            buffer.writeBoolean(rule.emptyOnly());
            buffer.writeBoolean(rule.matchGemColor());
            buffer.writeNullable(rule.powderColorTag(), FriendlyByteBuf::writeResourceLocation);
            buffer.writeItem(rule.remainder);
            buffer.writeBoolean(rule.preferLastSlot());
        }
    }
}
