package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.GemCutterHive")
public enum GemCutterHive {
    ANY(GemCuttingRecipe.HiveRequirement.ANY),
    CAN_INVITE(GemCuttingRecipe.HiveRequirement.CAN_INVITE),
    MEMBER(GemCuttingRecipe.HiveRequirement.MEMBER),
    OWNER(GemCuttingRecipe.HiveRequirement.OWNER);

    final GemCuttingRecipe.HiveRequirement requirement;

    GemCutterHive(GemCuttingRecipe.HiveRequirement requirement) {
        this.requirement = requirement;
    }
}
