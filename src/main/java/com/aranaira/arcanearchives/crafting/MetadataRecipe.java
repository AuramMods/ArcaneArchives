package com.aranaira.arcanearchives.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/** A server rule distributed through Minecraft's recipe reload and synchronization. */
public interface MetadataRecipe extends Recipe<Container> {
    @Override
    default boolean matches(Container input, Level level) {
        return false;
    }

    @Override
    default ItemStack assemble(Container input, RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    default boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    default ItemStack getResultItem(RegistryAccess registries) {
        return ItemStack.EMPTY;
    }

    @Override
    default boolean isSpecial() {
        return true;
    }

    @Override
    default boolean isIncomplete() {
        return false;
    }
}
