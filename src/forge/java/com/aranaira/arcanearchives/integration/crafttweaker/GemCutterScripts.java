package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionAddRecipe;
import com.blamejared.crafttweaker.api.action.recipe.ActionRecipeBase;
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
        var recipe = new GemCuttingRecipe(recipeId(name), Integer.MAX_VALUE, requirements(inputs), output.getImmutableInternal());
        CraftTweakerAPI.apply(new ActionAddRecipe<>(RECIPES, recipe));
    }

    @ZenCodeType.Method
    public static void replaceRecipe(String name, IItemStack output, IIngredientWithAmount[] inputs) {
        ResourceLocation id = recipeId(name);
        List<GemCuttingRecipe.Requirement> requirements = requirements(inputs);
        ItemStack result = output.getImmutableInternal();
        CraftTweakerAPI.apply(new ActionRecipeBase<GemCuttingRecipe>(RECIPES) {
            @Override
            public String describe() {
                return "Replacing Gem Cutter recipe " + id;
            }

            @Override
            public void apply() {
                var recipes = getRecipeMutator();
                var existing = recipes.get(id);
                if (existing == null) {
                    throw new IllegalArgumentException("No Gem Cutter recipe named " + id);
                }
                var recipe = existing.withContents(requirements, result);
                recipes.remove(id);
                recipes.add(id, recipe);
            }
        });
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack output) {
        ItemStack result = output.getImmutableInternal();
        CraftTweakerAPI.apply(new ActionRecipeBase<GemCuttingRecipe>(RECIPES) {
            @Override
            public String describe() {
                return "Removing Gem Cutter recipe for " + result;
            }

            @Override
            public void apply() {
                var recipes = getRecipeMutator();
                var recipe = recipes.getAllRecipes().stream()
                        .filter(candidate -> ItemStack.isSameItemSameTags(result, candidate.getResultItem(RegistryAccess.EMPTY)))
                        .min(Comparator.comparingInt(GemCuttingRecipe::order).thenComparing(GemCuttingRecipe::getId))
                        .orElseThrow(() -> new IllegalArgumentException("No Gem Cutter recipe produces " + result));
                recipes.remove(recipe.getId());
            }
        });
    }

    private static ResourceLocation recipeId(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name.indexOf(':') < 0 ? ArcaneArchives.ID + ':' + name : name);
        if (id == null) {
            throw new IllegalArgumentException("Invalid Gem Cutter recipe name " + name);
        }
        return id;
    }

    private static List<GemCuttingRecipe.Requirement> requirements(IIngredientWithAmount[] inputs) {
        return Arrays.stream(inputs)
                .map(input -> new GemCuttingRecipe.Requirement(input.getIngredient().asVanillaIngredient(), input.getAmount()))
                .toList();
    }
}
