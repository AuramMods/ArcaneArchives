package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.integration.stages.StageRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.StageScope")
public enum StageScope {
    FEATURE(StageRule.Scope.FEATURE),
    FAMILY(StageRule.Scope.FAMILY),
    RECIPE(StageRule.Scope.RECIPE);

    final StageRule.Scope scope;

    StageScope(StageRule.Scope scope) {
        this.scope = scope;
    }
}
