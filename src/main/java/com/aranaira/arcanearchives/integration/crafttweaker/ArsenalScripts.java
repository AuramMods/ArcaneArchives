package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArsenalEffectRule;
import com.aranaira.arcanearchives.arsenal.BlockTransformRule;
import com.aranaira.arcanearchives.arsenal.InventoryRechargeRule;
import com.aranaira.arcanearchives.arsenal.MunchstoneFoodRule;
import com.aranaira.arcanearchives.arsenal.NaturalRechargeRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.block.CTBlockIngredient;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Arsenal")
public final class ArsenalScripts {
    private static final IRecipeManager<InventoryRechargeRule> RECHARGE = ArcaneArchives.INVENTORY_RECHARGE_TYPE::get;
    private static final IRecipeManager<NaturalRechargeRule> NATURAL = ArcaneArchives.NATURAL_RECHARGE_TYPE::get;
    private static final IRecipeManager<BlockTransformRule> TRANSFORMS = ArcaneArchives.BLOCK_TRANSFORM_TYPE::get;
    private static final IRecipeManager<MunchstoneFoodRule> FOODS = ArcaneArchives.MUNCHSTONE_FOOD_TYPE::get;
    private static final IRecipeManager<ArsenalEffectRule> EFFECTS = ArcaneArchives.ARSENAL_EFFECT_TYPE::get;

    private ArsenalScripts() {}

    @ZenCodeType.Method
    public static void addRecharge(String id, ScriptInventoryRecharge recharge) {
        ScriptRecipes.add(RECHARGE, recharge.named(id));
    }

    @ZenCodeType.Method
    public static void replaceRecharge(String id, ScriptInventoryRecharge recharge) {
        ScriptRecipes.replace(RECHARGE, recharge.named(id));
    }

    @ZenCodeType.Method
    public static void removeRecharge(String id) {
        ScriptRecipes.remove(RECHARGE, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasRecharge(String id) {
        return RECHARGE.getRecipeList().has(ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static void addNaturalRecharge(String id, ScriptGemSelector gems, RechargeTrigger trigger,
                                          int charge, double scale, int ticks, boolean toggleWhenFull, int order) {
        ScriptRecipes.add(NATURAL, natural(id, gems, trigger, charge, scale, ticks, toggleWhenFull, order));
    }

    @ZenCodeType.Method
    public static void replaceNaturalRecharge(String id, ScriptGemSelector gems, RechargeTrigger trigger,
                                              int charge, double scale, int ticks, boolean toggleWhenFull, int order) {
        ScriptRecipes.replace(NATURAL, natural(id, gems, trigger, charge, scale, ticks, toggleWhenFull, order));
    }

    @ZenCodeType.Method
    public static void removeNaturalRecharge(String id) {
        ScriptRecipes.remove(NATURAL, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasNaturalRecharge(String id) {
        return NATURAL.getRecipeList().has(ScriptRecipes.id(id));
    }

    private static NaturalRechargeRule natural(String id, ScriptGemSelector gems, RechargeTrigger trigger,
                                               int charge, double scale, int ticks, boolean toggleWhenFull, int order) {
        return new NaturalRechargeRule(ScriptRecipes.id(id),
                new NaturalRechargeRule.Settings(gems.selector, trigger.trigger(), charge, scale, ticks, toggleWhenFull, order));
    }

    @ZenCodeType.Method
    public static void addTransform(String id, ScriptBlockTransform transform) {
        ScriptRecipes.add(TRANSFORMS, new BlockTransformRule(ScriptRecipes.id(id), transform.settings));
    }

    @ZenCodeType.Method
    public static void replaceTransform(String id, ScriptBlockTransform transform) {
        ScriptRecipes.replace(TRANSFORMS, new BlockTransformRule(ScriptRecipes.id(id), transform.settings));
    }

    @ZenCodeType.Method
    public static void removeTransform(String id) {
        ScriptRecipes.remove(TRANSFORMS, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasTransform(String id) {
        return TRANSFORMS.getRecipeList().has(ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static BlockState fluidState(IFluidStack fluid) {
        if (fluid.isEmpty() || fluid.hasTag()) {
            throw new IllegalArgumentException("A placed fluid output needs a fluid type without stack NBT");
        }
        var state = fluid.getFluid().defaultFluidState().createLegacyBlock();
        if (state.isAir()) {
            throw new IllegalArgumentException("This fluid has no placed block state");
        }
        return state;
    }

    @ZenCodeType.Method
    public static void addFood(String id, CTBlockIngredient block, int food, float saturation, int fixedCost, int order) {
        ScriptRecipes.add(FOODS, foodRule(id, block, food, saturation, fixedCost, order));
    }

    @ZenCodeType.Method
    public static void replaceFood(String id, CTBlockIngredient block, int food, float saturation, int fixedCost, int order) {
        ScriptRecipes.replace(FOODS, foodRule(id, block, food, saturation, fixedCost, order));
    }

    @ZenCodeType.Method
    public static void removeFood(String id) {
        ScriptRecipes.remove(FOODS, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean hasFood(String id) {
        return FOODS.getRecipeList().has(ScriptRecipes.id(id));
    }

    private static MunchstoneFoodRule foodRule(String id, CTBlockIngredient block, int food, float saturation, int fixedCost, int order) {
        return new MunchstoneFoodRule(ScriptRecipes.id(id),
                new MunchstoneFoodRule.Settings(ScriptBlockIngredients.selector(block), food, saturation, fixedCost, order, false));
    }

    @ZenCodeType.Method
    public static void setEffect(ScriptArsenalEffect effect, int charge, int ticks, double amount) {
        var id = effect.effect().id();
        var rule = new ArsenalEffectRule(id, new ArsenalEffectRule.Settings(effect.effect(), charge, ticks, amount));
        if (EFFECTS.getRecipeList().has(id)) {
            ScriptRecipes.replace(EFFECTS, rule);
        } else {
            ScriptRecipes.add(EFFECTS, rule);
        }
    }

    @ZenCodeType.Method
    public static void resetEffect(ScriptArsenalEffect effect) {
        var id = effect.effect().id();
        if (EFFECTS.getRecipeList().has(id)) {
            ScriptRecipes.remove(EFFECTS, id);
        }
    }
}
