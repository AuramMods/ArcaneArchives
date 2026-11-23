package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.GemType;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Gem")
public enum ScriptGem {
    SLAUGHTERGLEAM, MURDERGLEAM, AGEGLEAM, CLEANSEGLEAM, SWITCHGLEAM, SALVEGLEAM, MUNCHSTONE,
    ORDERSTONE, MINDSPINDLE, ELIXIRSPINDLE, MOUNTAINTEAR, RIVERTEAR, PARCHTEAR, PHOENIXWAY, STORMWAY;

    GemType gem() {
        return GemType.valueOf(name());
    }
}
