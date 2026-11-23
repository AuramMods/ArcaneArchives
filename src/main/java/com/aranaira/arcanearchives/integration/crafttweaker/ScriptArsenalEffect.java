package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.ArsenalEffectRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.ArsenalEffect")
public enum ScriptArsenalEffect {
    AGEGLEAM, CLEANSEGLEAM, SWITCHGLEAM, SALVEGLEAM, PHOENIXWAY_FIRE_RESISTANCE;

    ArsenalEffectRule.Effect effect() {
        return ArsenalEffectRule.Effect.valueOf(name());
    }
}
