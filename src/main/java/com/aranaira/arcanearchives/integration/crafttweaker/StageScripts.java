package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import com.aranaira.arcanearchives.integration.stages.StageRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.resources.ResourceLocation;
import org.openzen.zencode.java.ZenCodeType;

import java.util.List;
import java.util.Arrays;

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

    @ZenCodeType.Method
    public static String[] features() {
        return ArchiveStages.features().stream().map(ResourceLocation::toString).toArray(String[]::new);
    }

    @ZenCodeType.Method
    public static String[] families() {
        return ArchiveStages.families().stream().map(ResourceLocation::toString).toArray(String[]::new);
    }

    @ZenCodeType.Method
    public static void addFeatureGroup(String[] features, String[] all, String[] any) {
        var rules = Arrays.stream(features).map(id -> rule(StageScope.FEATURE, id, all, any)).distinct().toList();
        ScriptRecipes.change(RULES, "Adding stage requirements for " + Arrays.toString(features), recipes -> {
            for (var rule : rules) {
                if (recipes.getByName().containsKey(rule.id())) {
                    throw new IllegalArgumentException("A stage rule named " + rule.id() + " already exists");
                }
            }
            rules.forEach(rule -> recipes.add(rule.id(), rule));
        });
    }

    private static StageRule rule(StageScope scope, String target, String[] all, String[] any) {
        ResourceLocation id = ScriptRecipes.id(target);
        if (scope == StageScope.FEATURE && !ArchiveStages.features().contains(id)) {
            throw new IllegalArgumentException("Unknown Arcane Archives feature " + id);
        }
        if (scope == StageScope.FAMILY && !ArchiveStages.families().contains(id)) {
            throw new IllegalArgumentException("Unknown Arcane Archives processing family " + id);
        }
        return new StageRule(scope.scope.key(id), List.of(all), List.of(any));
    }
}
