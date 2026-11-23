package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.BlockTransformRule;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.FluidSelector;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.block.CTBlockIngredient;
import com.blamejared.crafttweaker.api.fluid.CTFluidIngredient;
import net.minecraft.world.level.block.state.BlockState;
import org.openzen.zencode.java.ZenCodeType;

import java.util.List;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.BlockTransform")
public final class ScriptBlockTransform {
    final BlockTransformRule.Settings settings;

    @ZenCodeType.Constructor
    public ScriptBlockTransform(ScriptGem gem, CTBlockIngredient blocks, BlockState output, int charge, int order) {
        this(gem, ScriptTransformTarget.ANY, blocks, null, output, charge, order, false, false, false, false);
    }

    @ZenCodeType.Constructor
    public ScriptBlockTransform(ScriptGem gem, ScriptTransformTarget target, @ZenCodeType.Nullable CTBlockIngredient blocks,
                                @ZenCodeType.Nullable CTFluidIngredient fluids, BlockState output, int charge, int order,
                                boolean preserveFacing, boolean creativeFree, boolean coolDimensionsOnly, boolean dynamicFire) {
        settings = new BlockTransformRule.Settings(gem.gem(), target.target(),
                blocks == null ? BlockSelector.ANY : ScriptBlockIngredients.selector(blocks),
                fluids == null ? FluidSelector.ANY : fluidSelector(fluids),
                output, charge, order, preserveFacing, creativeFree, coolDimensionsOnly, dynamicFire);
    }

    private static FluidSelector fluidSelector(CTFluidIngredient input) {
        return input.mapTo(stack -> {
            if (stack.isEmpty() || stack.hasTag()) {
                throw new IllegalArgumentException("A placed fluid selector needs a fluid type without stack NBT");
            }
            return new FluidSelector(List.of(stack.getFluid()), List.of());
        }, (tag, amount) -> new FluidSelector(List.of(), List.of(tag)), options -> FluidSelector.anyOf(options.toList()));
    }
}
