package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.blamejared.crafttweaker.api.block.CTBlockIngredient;

import java.util.List;

final class ScriptBlockIngredients {
    private ScriptBlockIngredients() {}

    static BlockSelector selector(CTBlockIngredient input) {
        return input.mapTo(block -> new BlockSelector(List.of(block), List.of(), List.of()),
                state -> new BlockSelector(List.of(), List.of(), List.of(state)),
                (tag, amount) -> new BlockSelector(List.of(), List.of(tag), List.of()),
                options -> BlockSelector.anyOf(options.toList()));
    }
}
