package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.arsenal.InventoryRechargeRule;
import com.aranaira.arcanearchives.crafting.CountedIngredient;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.ingredient.IIngredientWithAmount;
import com.blamejared.crafttweaker.api.item.IItemStack;
import net.minecraft.world.item.ItemStack;
import org.openzen.zencode.java.ZenCodeType;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.InventoryRecharge")
public final class ScriptInventoryRecharge {
    private final InventoryRechargeRule rule;

    @ZenCodeType.Constructor
    public ScriptInventoryRecharge(ScriptGemSelector gems, IIngredientWithAmount input, int maximum, int chargePerItem,
                                   boolean emptyOnly, int order) {
        this(gems, input, maximum, chargePerItem, emptyOnly, order, null, false, null, false);
    }

    @ZenCodeType.Constructor
    public ScriptInventoryRecharge(ScriptGemSelector gems, IIngredientWithAmount input, int maximum, int chargePerItem,
                                   boolean emptyOnly, int order, @ZenCodeType.Nullable IItemStack remainder,
                                   boolean matchGemColor, @ZenCodeType.Nullable String powderColorTag, boolean preferLastSlot) {
        rule = new InventoryRechargeRule(ScriptRecipes.id("unregistered"), order, gems.selector,
                new CountedIngredient(input.getIngredient().asVanillaIngredient(), input.getAmount()), maximum, chargePerItem,
                emptyOnly, matchGemColor, powderColorTag == null ? null : ScriptRecipes.id(powderColorTag),
                remainder == null ? ItemStack.EMPTY : remainder.getImmutableInternal(), preferLastSlot);
    }

    InventoryRechargeRule named(String id) {
        return new InventoryRechargeRule(ScriptRecipes.id(id), rule.order(), rule.gems(), rule.input(), rule.maximum(),
                rule.chargePerItem(), rule.emptyOnly(), rule.matchGemColor(), rule.powderColorTag(), rule.remainder(),
                rule.preferLastSlot());
    }
}
