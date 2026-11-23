package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.CountedIngredient;
import com.aranaira.arcanearchives.progression.ChestConversionRule;
import com.aranaira.arcanearchives.progression.QuartzBashingRule;
import com.aranaira.arcanearchives.progression.QuartzTrigger;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.block.CTBlockIngredient;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Quartz")
public final class QuartzScripts {
    private static final IRecipeManager<ChestConversionRule> CONVERSIONS = ArcaneArchives.CHEST_CONVERSION_TYPE::get;
    private static final IRecipeManager<QuartzBashingRule> BASHING = ArcaneArchives.QUARTZ_BASHING_TYPE::get;

    private QuartzScripts() {}

    @ZenCodeType.Method
    public static void addChestConversion(String id, IIngredientWithAmount input, @ZenCodeType.Nullable CTBlockIngredient target,
                                          BlockState output, int order) {
        ScriptRecipes.add(CONVERSIONS, new ChestConversionRule(ScriptRecipes.id(id), trigger(input, target, order), output));
    }

    @ZenCodeType.Method
    public static void replaceChestConversion(String id, IIngredientWithAmount input, @ZenCodeType.Nullable CTBlockIngredient target,
                                              BlockState output, int order) {
        ScriptRecipes.replace(CONVERSIONS, new ChestConversionRule(ScriptRecipes.id(id), trigger(input, target, order), output));
    }

    @ZenCodeType.Method
    public static void removeChestConversion(String id) {
        ScriptRecipes.remove(CONVERSIONS, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasChestConversion(String id) {
        return CONVERSIONS.getRecipeList().has(ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static void addBashing(String id, IIngredientWithAmount input, @ZenCodeType.Nullable CTBlockIngredient target,
                                   QuartzDrop primary, @ZenCodeType.Nullable QuartzDrop secondary, int order) {
        ScriptRecipes.add(BASHING, bashing(id, input, target, primary, secondary, order));
    }

    @ZenCodeType.Method
    public static void replaceBashing(String id, IIngredientWithAmount input, @ZenCodeType.Nullable CTBlockIngredient target,
                                       QuartzDrop primary, @ZenCodeType.Nullable QuartzDrop secondary, int order) {
        ScriptRecipes.replace(BASHING, bashing(id, input, target, primary, secondary, order));
    }

    @ZenCodeType.Method
    public static void removeBashing(String id) {
        ScriptRecipes.remove(BASHING, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasBashing(String id) {
        return BASHING.getRecipeList().has(ScriptRecipes.id(id));
    }

    private static QuartzBashingRule bashing(String id, IIngredientWithAmount input, CTBlockIngredient target,
                                            QuartzDrop primary, QuartzDrop secondary, int order) {
        return new QuartzBashingRule(ScriptRecipes.id(id), trigger(input, target, order),
                primary.drop, secondary == null ? null : secondary.drop);
    }

    private static QuartzTrigger trigger(IIngredientWithAmount input, CTBlockIngredient target, int order) {
        return new QuartzTrigger(order, new CountedIngredient(input.getIngredient().asVanillaIngredient(), input.getAmount()),
                target == null ? BlockSelector.ANY : ScriptBlockIngredients.selector(target));
    }
}
