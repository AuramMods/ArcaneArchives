package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

public final class GemCuttingRecipe implements Recipe<Container> {
    private static final int MAX_INGREDIENTS = 64;
    private static final int MAX_INGREDIENT_COUNT = 4096;

    private final ResourceLocation id;
    private final int order;
    private final List<Requirement> requirements;
    private final ItemStack result;

    public GemCuttingRecipe(ResourceLocation id, int order, List<Requirement> requirements, ItemStack result) {
        this.id = id;
        this.order = order;
        this.requirements = List.copyOf(requirements);
        this.result = result.copy();
    }

    public int order() {
        return order;
    }

    public List<Requirement> requirements() {
        return requirements;
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
        int firstRequirement = slots + 1;
        int sink = firstRequirement + requirements.size();
        int[][] capacity = new int[sink + 1][sink + 1];
        int demand = 0;
        for (int index = 0; index < requirements.size(); index++) {
            int count = requirements.get(index).count();
            capacity[firstRequirement + index][sink] = count;
            demand += count;
        }

        int[] counts = new int[slots];
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = getStack.apply(slot);
            if (!stack.isEmpty()) {
                counts[slot] = stack.getCount();
                capacity[0][slot + 1] = counts[slot];
                for (int index = 0; index < requirements.size(); index++) {
                    if (requirements.get(index).ingredient().test(stack)) {
                        capacity[slot + 1][firstRequirement + index] = counts[slot];
                    }
                }
            }
        }

        // Residual paths can reassign a shared match to a recipe's more specific ingredient.
        int[] parents = new int[sink + 1];
        int[] queue = new int[sink + 1];
        while (demand > 0) {
            Arrays.fill(parents, -1);
            parents[0] = 0;
            queue[0] = 0;
            int tail = 1;
            for (int head = 0; head < tail && parents[sink] == -1; head++) {
                int from = queue[head];
                for (int to = 1; to <= sink; to++) {
                    if (parents[to] == -1 && capacity[from][to] > 0) {
                        parents[to] = from;
                        queue[tail++] = to;
                    }
                }
            }
            if (parents[sink] == -1) {
                return null;
            }

            int moved = demand;
            for (int node = sink; node != 0; node = parents[node]) {
                moved = Math.min(moved, capacity[parents[node]][node]);
            }
            for (int node = sink; node != 0; node = parents[node]) {
                int from = parents[node];
                capacity[from][node] -= moved;
                capacity[node][from] += moved;
            }
            demand -= moved;
        }

        for (int slot = 0; slot < slots; slot++) {
            counts[slot] -= capacity[0][slot + 1];
        }
        return counts;
    }

    public record Requirement(Ingredient ingredient, int count) {}

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
            return new GemCuttingRecipe(id, GsonHelper.getAsInt(json, "order", 0), requirements, result);
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
            return new GemCuttingRecipe(id, order, requirements, buffer.readItem());
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
