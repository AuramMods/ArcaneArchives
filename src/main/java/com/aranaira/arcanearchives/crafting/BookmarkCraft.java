package com.aranaira.arcanearchives.crafting;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

record BookmarkCraft(CraftingContainer input, int[] consumed, ItemStack output) {
    @Nullable
    static BookmarkCraft prepare(CraftingRecipe recipe, CraftingContainer matrix, Inventory inventory, Level level) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty() || ingredients.size() > CraftingGrid.SIZE
                || !recipe.canCraftInDimensions(CraftingGrid.WIDTH, CraftingGrid.WIDTH)) {
            return null;
        }
        int width = recipe instanceof IShapedRecipe<?> shaped ? shaped.getRecipeWidth() : CraftingGrid.WIDTH;
        int[] cells = new int[ingredients.size()];
        List<GemCuttingRecipe.Requirement> requirements = new ArrayList<>(ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (ingredient != Ingredient.EMPTY) {
                cells[requirements.size()] = i % width + i / width * CraftingGrid.WIDTH;
                requirements.add(new GemCuttingRecipe.Requirement(ingredient, 1));
            }
        }
        if (requirements.isEmpty()) {
            return null;
        }
        IntFunction<ItemStack> source = slot -> slot < CraftingGrid.SIZE
                ? matrix.getItem(slot) : inventory.getItem(slot - CraftingGrid.SIZE);
        IngredientAllocation allocation = IngredientAllocation.solve(requirements, CraftingGrid.SIZE + Inventory.INVENTORY_SIZE,
                source);
        if (allocation == null) {
            return null;
        }
        CraftingGrid input = new CraftingGrid();
        for (int requirement = 0; requirement < requirements.size(); requirement++) {
            input.setItem(cells[requirement], source.apply(allocation.sourceFor(requirement)).copyWithCount(1));
        }
        Player previous = ForgeHooks.getCraftingPlayer();
        ForgeHooks.setCraftingPlayer(inventory.player);
        try {
            if (!recipe.matches(input, level)) {
                return null;
            }
            ItemStack output = recipe.assemble(input, level.registryAccess());
            return output.isEmpty() ? null : new BookmarkCraft(input, allocation.counts(), output);
        } finally {
            ForgeHooks.setCraftingPlayer(previous);
        }
    }
}
