package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredient;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import net.minecraft.core.RegistryAccess;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.GemCutterRecipe")
public final class GemCutterRecipeView {
    private final GemCuttingRecipe recipe;

    GemCutterRecipeView(GemCuttingRecipe recipe) {
        this.recipe = recipe;
    }

    @ZenCodeType.Getter("id")
    public String getId() {
        return recipe.getId().toString();
    }

    @ZenCodeType.Getter("order")
    public int getOrder() {
        return recipe.order();
    }

    @ZenCodeType.Getter("inputs")
    public IIngredientWithAmount[] getInputs() {
        return recipe.requirements().stream()
                .map(input -> IIngredient.fromIngredient(input.ingredient()).mul(input.count()))
                .toArray(IIngredientWithAmount[]::new);
    }

    @ZenCodeType.Getter("output")
    public IItemStack getOutput() {
        return IItemStack.of(recipe.getResultItem(RegistryAccess.EMPTY));
    }

    @ZenCodeType.Getter("hive")
    public GemCutterHive getHive() {
        return GemCutterHive.valueOf(recipe.hiveRequirement().name());
    }

    @ZenCodeType.Getter("signed")
    public boolean signsResult() {
        return recipe.signsResult();
    }

    @ZenCodeType.Getter("requiresArsenal")
    public boolean requiresArsenal() {
        return recipe.requiresArsenal();
    }
}
