package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.BlockTransformRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.TransformTarget")
public enum ScriptTransformTarget {
    ANY, REPLACEABLE, AIR, LIQUID;

    BlockTransformRule.Target target() {
        return BlockTransformRule.Target.valueOf(name());
    }
}
