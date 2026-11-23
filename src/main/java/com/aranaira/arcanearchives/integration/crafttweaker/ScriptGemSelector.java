package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.GemSelector;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import net.minecraft.resources.ResourceLocation;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Arrays;
import java.util.Optional;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.GemSelector")
public final class ScriptGemSelector {
    final GemSelector selector;

    @ZenCodeType.Constructor
    public ScriptGemSelector(ScriptGem[] gems, @ZenCodeType.Nullable String gemTag, @ZenCodeType.Nullable String cutTag,
                             @ZenCodeType.Nullable String colorTag) {
        selector = new GemSelector(Arrays.stream(gems).map(ScriptGem::gem).toList(),
                optionalId(gemTag), optionalId(cutTag), optionalId(colorTag));
    }

    @ZenCodeType.Constructor
    public ScriptGemSelector(ScriptGem[] gems) {
        this(gems, null, null, null);
    }

    private static Optional<ResourceLocation> optionalId(String value) {
        return Optional.ofNullable(value).map(ScriptRecipes::id);
    }
}
