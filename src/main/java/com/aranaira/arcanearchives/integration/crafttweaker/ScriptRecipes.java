package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.recipe.ActionRecipeBase;
import com.blamejared.crafttweaker.api.recipe.RecipeList;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.function.Consumer;

final class ScriptRecipes {
    private ScriptRecipes() {}

    static ResourceLocation id(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name.indexOf(':') < 0 ? ArcaneArchives.ID + ':' + name : name);
        if (id == null || id.getPath().isEmpty() || name.startsWith(":")) {
            throw new IllegalArgumentException("Invalid Arcane Archives ID: " + name);
        }
        return id;
    }

    static <T extends Recipe<?>> void add(IRecipeManager<T> manager, T recipe) {
        var id = recipe.getId();
        change(manager, "Adding " + id, recipes -> {
            if (recipes.getByName().containsKey(id)) {
                throw new IllegalArgumentException("A recipe named " + id + " already exists");
            }
            recipes.add(id, recipe);
        });
    }

    static <T extends Recipe<?>> void replace(IRecipeManager<T> manager, T recipe) {
        var id = recipe.getId();
        change(manager, "Replacing " + id, recipes -> {
            require(recipes, id);
            recipes.remove(id);
            recipes.add(id, recipe);
        });
    }

    static <T extends Recipe<?>> void remove(IRecipeManager<T> manager, ResourceLocation id) {
        change(manager, "Removing " + id, recipes -> {
            require(recipes, id);
            recipes.remove(id);
        });
    }

    static <T extends Recipe<?>> T require(RecipeList<T> recipes, ResourceLocation id) {
        T recipe = recipes.get(id);
        if (recipe == null) {
            throw new IllegalArgumentException("No " + recipes.getRecipeType() + " recipe named " + id);
        }
        return recipe;
    }

    static <T extends Recipe<?>> void change(IRecipeManager<T> manager, String description, Consumer<RecipeList<T>> action) {
        CraftTweakerAPI.apply(new ActionRecipeBase<T>(manager) {
            @Override
            public String describe() {
                return description;
            }

            @Override
            public void apply() {
                action.accept(getRecipeMutator());
            }
        });
    }
}
