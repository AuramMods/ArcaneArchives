package com.aranaira.arcanearchives.integration.jei;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class ArchiveJeiPlugin implements IModPlugin {
    static final RecipeType<GemCuttingRecipe> GEM_CUTTING = RecipeType.create(ArcaneArchives.ID, "gem_cutting", GemCuttingRecipe.class);
    static final RecipeType<ResonatorCategory.Growth> RESONATOR = RecipeType.create(
            ArcaneArchives.ID, "radiant_resonator", ResonatorCategory.Growth.class);
    @Nullable
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ArcaneArchives.id("jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new GemCuttingCategory(gui), new ResonatorCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var recipes = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.GEM_CUTTING_TYPE.get());
            registration.addRecipes(GEM_CUTTING, recipes.stream().filter(GemCuttingRecipe::enabled).toList());
        }
        if (ArchiveConfig.SERVER_SPEC.isLoaded()) {
            registration.addRecipes(RESONATOR, List.of(new ResonatorCategory.Growth(ArchiveConfig.RESONATOR_TICKS.get())));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ArcaneArchives.GEMCUTTERS_TABLE_ITEM.get()), GEM_CUTTING);
        registration.addRecipeCatalyst(new ItemStack(ArcaneArchives.RADIANT_RESONATOR_ITEM.get()), RESONATOR);
        registration.addRecipeCatalyst(new ItemStack(ArcaneArchives.RADIANT_CRAFTING_TABLE_ITEM.get()), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(RadiantCraftingTableMenu.class, ArcaneArchives.RADIANT_CRAFTING_TABLE_MENU.get(),
                RecipeTypes.CRAFTING, 1, 9, 10, 36);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        var excluded = new ArrayList<ItemStack>();
        excluded.add(new ItemStack(ArcaneArchives.ARSENAL_CONDITION.get()));
        excluded.add(new ItemStack(ArcaneArchives.CHROMATIC_POWDER.get()));
        excluded.add(new ItemStack(ArcaneArchives.FULL_SPECTRUM_POWDER.get()));
        if (!ArchiveConfig.SERVER_SPEC.isLoaded() || !ArchiveConfig.ARSENAL_ENABLED.get()) {
            excluded.add(new ItemStack(ArcaneArchives.GEM_SOCKET.get()));
            ArcaneArchives.GEMS.values().forEach(gem -> excluded.add(new ItemStack(gem.get())));
        }
        jeiRuntime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, excluded);
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Nullable
    public static String filter() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static void setFilter(String value) {
        if (runtime != null) {
            runtime.getIngredientFilter().setFilterText(value);
        }
    }

    public static ItemStack hoveredItem() {
        if (runtime == null) {
            return ItemStack.EMPTY;
        }
        ItemStack hovered = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (hovered != null && !hovered.isEmpty()) {
            return hovered;
        }
        var recipe = runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (recipe.isPresent()) {
            return recipe.get();
        }
        hovered = runtime.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        return hovered == null ? ItemStack.EMPTY : hovered;
    }
}
