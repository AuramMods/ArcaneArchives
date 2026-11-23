package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.progression.ResonatorGrowth;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.block.CTBlockIngredient;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Optional;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Resonator")
public final class ResonatorScripts {
    private static final IRecipeManager<ResonatorGrowth> RULES = ArcaneArchives.RESONATOR_GROWTH_TYPE::get;

    private ResonatorScripts() {}

    @ZenCodeType.Method
    public static void add(String id, BlockState output, int ticks, int order, boolean onlineOwner,
                           @ZenCodeType.Nullable CTBlockIngredient foundation,
                           @ZenCodeType.Nullable ResourceLocation dimension, int minimumLight) {
        ScriptRecipes.add(RULES, rule(id, output, ticks, order, onlineOwner, foundation, dimension, minimumLight));
    }

    @ZenCodeType.Method
    public static void replace(String id, BlockState output, int ticks, int order, boolean onlineOwner,
                               @ZenCodeType.Nullable CTBlockIngredient foundation,
                               @ZenCodeType.Nullable ResourceLocation dimension, int minimumLight) {
        ScriptRecipes.replace(RULES, rule(id, output, ticks, order, onlineOwner, foundation, dimension, minimumLight));
    }

    @ZenCodeType.Method
    public static void remove(String id) {
        ScriptRecipes.remove(RULES, ScriptRecipes.id(id));
    }

    @ZenCodeType.Method
    public static boolean has(String id) {
        return RULES.getRecipeList().has(ScriptRecipes.id(id));
    }

    private static ResonatorGrowth rule(String id, BlockState output, int ticks, int order, boolean onlineOwner,
                                       CTBlockIngredient foundation, ResourceLocation dimension, int minimumLight) {
        return new ResonatorGrowth(ScriptRecipes.id(id), new ResonatorGrowth.Settings(order, ticks, onlineOwner,
                foundation == null ? BlockSelector.ANY : ScriptBlockIngredients.selector(foundation),
                Optional.ofNullable(dimension), minimumLight, output));
    }
}
