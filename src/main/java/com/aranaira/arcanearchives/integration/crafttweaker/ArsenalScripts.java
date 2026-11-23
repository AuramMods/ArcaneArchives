package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.InventoryRechargeRule;
import com.aranaira.arcanearchives.arsenal.NaturalRechargeRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Arsenal")
public final class ArsenalScripts {
    private static final IRecipeManager<InventoryRechargeRule> RECHARGE = ArcaneArchives.INVENTORY_RECHARGE_TYPE::get;
    private static final IRecipeManager<NaturalRechargeRule> NATURAL = ArcaneArchives.NATURAL_RECHARGE_TYPE::get;

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
}
