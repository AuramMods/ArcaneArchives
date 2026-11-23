package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.progression.QuartzBashingRule;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.QuartzDrop")
public final class QuartzDrop {
    final QuartzBashingRule.Drop drop;

    @ZenCodeType.Constructor
    public QuartzDrop(IItemStack minimum, int maximum, int chance, boolean consume) {
        drop = new QuartzBashingRule.Drop(minimum.getImmutableInternal(), chance, minimum.getAmount(), maximum, consume);
    }
}
