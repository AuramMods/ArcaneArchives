package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public final class GemCuttingRecipe implements Recipe<Container> {
    private static final int MAX_INGREDIENTS = 64;
    private static final int MAX_INGREDIENT_COUNT = 4096;

    private final ResourceLocation id;
    private final int order;
    private final List<Requirement> requirements;
    private final ItemStack result;
    private final HiveRequirement hiveRequirement;
    private final boolean signResult;

    public GemCuttingRecipe(ResourceLocation id, int order, List<Requirement> requirements, ItemStack result) {
        this(id, order, requirements, result, HiveRequirement.ANY, false);
    }

    private GemCuttingRecipe(ResourceLocation id, int order, List<Requirement> requirements, ItemStack result,
                             HiveRequirement hiveRequirement, boolean signResult) {
        this.id = id;
        this.order = order;
        this.requirements = List.copyOf(requirements);
        this.result = result.copy();
        this.hiveRequirement = hiveRequirement;
        this.signResult = signResult;
    }

    public int order() {
        return order;
    }

    public List<Requirement> requirements() {
        return requirements;
    }

    boolean allowedInHive(int requirementsMet) {
        return (requirementsMet & hiveRequirement.bit()) != 0;
    }

    ItemStack resultFor(Player player) {
        ItemStack stack = getResultItem(player.level().registryAccess());
        if (signResult) {
            var tag = stack.getOrCreateTag();
            tag.putUUID("creator", player.getUUID());
            tag.putString("creator_name", player.getGameProfile().getName());
        }
        return stack;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return allocate(input.getContainerSize(), input::getItem) != null;
    }

    @Override
    public ItemStack assemble(Container input, RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= requirements.size();
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return result.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        requirements.forEach(requirement -> ingredients.add(requirement.ingredient()));
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ArcaneArchives.GEMCUTTERS_TABLE_ITEM.get());
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.GEM_CUTTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.GEM_CUTTING_TYPE.get();
    }

    @Nullable
    int[] allocate(int slots, IntFunction<ItemStack> getStack) {
        IngredientAllocation allocation = IngredientAllocation.solve(requirements, slots, getStack);
        return allocation == null ? null : allocation.counts();
    }

    public record Requirement(Ingredient ingredient, int count) {}

    private enum HiveRequirement {
        ANY("any"), CAN_INVITE("can_invite"), MEMBER("member"), OWNER("owner");

        private final String id;

        HiveRequirement(String id) {
            this.id = id;
        }

        int bit() {
            return 1 << ordinal();
        }

        static HiveRequirement parse(String id) {
            for (HiveRequirement value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            throw new JsonSyntaxException("Unknown Gem Cutter hive requirement " + id);
        }
    }

    static int requirementsMet(Player player) {
        var server = player.getServer();
        if (server == null) {
            return HiveRequirement.ANY.bit();
        }
        var networks = ArchiveNetworks.get(server);
        var actor = player.getUUID();
        boolean leads = networks.hiveOwner(actor).equals(actor);
        boolean belongs = networks.hasHive(actor);
        int met = HiveRequirement.ANY.bit();
        if (leads) {
            met |= HiveRequirement.CAN_INVITE.bit();
        }
        if (belongs) {
            met |= HiveRequirement.MEMBER.bit();
            if (leads) {
                met |= HiveRequirement.OWNER.bit();
            }
        }
        return met;
    }

    public static final class Serializer implements RecipeSerializer<GemCuttingRecipe> {
        @Override
        public GemCuttingRecipe fromJson(ResourceLocation id, JsonObject json) {
            var entries = GsonHelper.getAsJsonArray(json, "ingredients");
            validateSize(entries.size());
            List<Requirement> requirements = new ArrayList<>(entries.size());
            for (JsonElement entry : entries) {
                JsonObject ingredient = GsonHelper.convertToJsonObject(entry, "ingredient");
                int count = GsonHelper.getAsInt(ingredient, "count", 1);
                validateCount(count);
                requirements.add(new Requirement(Ingredient.fromJson(ingredient.get("ingredient")), count));
            }
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
            if (result.isEmpty() || result.getCount() > result.getMaxStackSize()) {
                throw new JsonSyntaxException("Invalid Gem Cutter result stack");
            }
            return new GemCuttingRecipe(id, GsonHelper.getAsInt(json, "order", 0), requirements, result,
                    HiveRequirement.parse(GsonHelper.getAsString(json, "hive_requirement", "any")),
                    GsonHelper.getAsBoolean(json, "sign_result", false));
        }

        @Override
        public GemCuttingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int order = buffer.readVarInt();
            int size = buffer.readVarInt();
            validateSize(size);
            List<Requirement> requirements = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                Ingredient ingredient = Ingredient.fromNetwork(buffer);
                int count = buffer.readVarInt();
                validateCount(count);
                requirements.add(new Requirement(ingredient, count));
            }
            return new GemCuttingRecipe(id, order, requirements, buffer.readItem(),
                    buffer.readEnum(HiveRequirement.class), buffer.readBoolean());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, GemCuttingRecipe recipe) {
            buffer.writeVarInt(recipe.order);
            buffer.writeVarInt(recipe.requirements.size());
            for (Requirement requirement : recipe.requirements) {
                requirement.ingredient().toNetwork(buffer);
                buffer.writeVarInt(requirement.count());
            }
            buffer.writeItem(recipe.result);
            buffer.writeEnum(recipe.hiveRequirement);
            buffer.writeBoolean(recipe.signResult);
        }

        private static void validateSize(int size) {
            if (size < 1 || size > MAX_INGREDIENTS) {
                throw new JsonSyntaxException("A Gem Cutter recipe requires 1 to " + MAX_INGREDIENTS + " ingredients");
            }
        }

        private static void validateCount(int count) {
            if (count < 1 || count > MAX_INGREDIENT_COUNT) {
                throw new JsonSyntaxException("Gem Cutter ingredient count must be between 1 and " + MAX_INGREDIENT_COUNT);
            }
        }
    }
}
