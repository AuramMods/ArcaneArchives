package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.StageRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import org.openzen.zencode.java.ZenCodeType;

import java.util.List;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Stages")
public final class StageScripts {
    private static final IRecipeManager<StageRule> RULES = ArcaneArchives.STAGE_RULE_TYPE::get;

    private StageScripts() {}

    @ZenCodeType.Method
    public static void add(StageScope scope, String target, String stage) {
        add(scope, target, new String[]{stage}, new String[0]);
    }

    @ZenCodeType.Method
    public static void add(StageScope scope, String target, String[] all, String[] any) {
        ScriptRecipes.add(RULES, rule(scope, target, all, any));
    }

    @ZenCodeType.Method
    public static void replace(StageScope scope, String target, String[] all, String[] any) {
        ScriptRecipes.replace(RULES, rule(scope, target, all, any));
    }

    @ZenCodeType.Method
    public static void remove(StageScope scope, String target) {
        ScriptRecipes.remove(RULES, scope.scope.key(ScriptRecipes.id(target)));
    }

    @ZenCodeType.Method
    public static boolean has(StageScope scope, String target) {
        return RULES.getRecipeList().has(scope.scope.key(ScriptRecipes.id(target)));
    }

    private static StageRule rule(StageScope scope, String target, String[] all, String[] any) {
        return new StageRule(scope.scope.key(ScriptRecipes.id(target)), List.of(all), List.of(any));
    }
}
