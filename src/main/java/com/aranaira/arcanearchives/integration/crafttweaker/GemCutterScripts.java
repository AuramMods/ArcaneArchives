package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.GCT")
public final class GemCutterScripts {
    private static final IRecipeManager<GemCuttingRecipe> RECIPES = ArcaneArchives.GEM_CUTTING_TYPE::get;

    private GemCutterScripts() {}

    @ZenCodeType.Method
    public static void addRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs) {
        addRecipe(name, output, inputs, Integer.MAX_VALUE, GemCutterHive.ANY, false, false);
    }

    @ZenCodeType.Method
    public static void addRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs,
                                 int order, GemCutterHive hive, boolean signed, boolean arsenal) {
        ScriptRecipes.add(RECIPES, recipe(name, output, inputs, order, hive, signed, arsenal));
    }

    @ZenCodeType.Method
    public static void replaceRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs) {
        ResourceLocation id = ScriptRecipes.id(name);
        List<GemCuttingRecipe.Requirement> requirements = requirements(inputs);
        ItemStack result = output.getImmutableInternal();
        ScriptRecipes.change(RECIPES, "Replacing Gem Cutter recipe " + id, recipes -> {
            var recipe = ScriptRecipes.require(recipes, id).withContents(requirements, result);
            recipes.remove(id);
            recipes.add(id, recipe);
        });
    }

    @ZenCodeType.Method
    public static void replaceRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs,
                                     int order, GemCutterHive hive, boolean signed, boolean arsenal) {
        ScriptRecipes.replace(RECIPES, recipe(name, output, inputs, order, hive, signed, arsenal));
    }

    @ZenCodeType.Method
    public static GemCutterRecipeView getRecipe(String name) {
        return new GemCutterRecipeView(ScriptRecipes.require(RECIPES.getRecipeList(), ScriptRecipes.id(name)));
    }

    @ZenCodeType.Method
    public static boolean hasRecipe(String name) {
        return RECIPES.getRecipeList().has(ScriptRecipes.id(name));
    }

    @ZenCodeType.Method
    public static void removeRecipe(String name) {
        ScriptRecipes.remove(RECIPES, ScriptRecipes.id(name));
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack output) {
        ItemStack result = output.getImmutableInternal();
        ScriptRecipes.change(RECIPES, "Removing Gem Cutter recipe for " + result, recipes -> {
            var recipe = recipes.getAllRecipes().stream()
                    .filter(candidate -> ItemStack.isSameItemSameTags(result, candidate.getResultItem(RegistryAccess.EMPTY)))
                    .min(Comparator.comparingInt(GemCuttingRecipe::order).thenComparing(GemCuttingRecipe::getId))
                    .orElseThrow(() -> new IllegalArgumentException("No Gem Cutter recipe produces " + result));
            recipes.remove(recipe.getId());
        });
    }

    private static GemCuttingRecipe recipe(String name, IItemStack output, IIngredientWithAmount[] inputs,
                                           int order, GemCutterHive hive, boolean signed, boolean arsenal) {
        return new GemCuttingRecipe(ScriptRecipes.id(name), order, requirements(inputs),
                output.getImmutableInternal(), hive.requirement, signed, arsenal);
    }

    private static List<GemCuttingRecipe.Requirement> requirements(IIngredientWithAmount[] inputs) {
        return Arrays.stream(inputs)
                .map(input -> new GemCuttingRecipe.Requirement(input.getIngredient().asVanillaIngredient(), input.getAmount()))
                .toList();
    }
}
