package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.NaturalRechargeRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.RechargeTrigger")
public enum RechargeTrigger {
    DROPPED_IN_LAVA, DROPPED_IN_WATER, DROPPED_IN_RAIN, HELD_IN_RAIN, ANIMAL_KILL, NATURAL_CRITICAL_HIT,
    TIMER, EXPERIENCE_PICKUP, ENDERMAN_TELEPORT;

    NaturalRechargeRule.Trigger trigger() {
        return NaturalRechargeRule.Trigger.valueOf(name());
    }
}
