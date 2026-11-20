package com.aranaira.arcanearchives.integration.jei;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

final class GemCuttingCategory implements IRecipeCategory<GemCuttingRecipe> {
    private static final int CLASSIC_INPUTS = 8;
    private final IGuiHelper gui;
    private final IDrawable artwork;
    private final IDrawable icon;

    GemCuttingCategory(IGuiHelper gui) {
        this.gui = gui;
        artwork = gui.createDrawable(ArcaneArchives.id("textures/gui/jei/gem_cutters_table.png"), 0, 0, 120, 72);
        icon = gui.createDrawableItemLike(ArcaneArchives.GEMCUTTERS_TABLE_ITEM.get());
    }

    @Override
    public RecipeType<GemCuttingRecipe> getRecipeType() {
        return ArchiveJeiPlugin.GEM_CUTTING;
    }

    @Override
    public Component getTitle() {
        return ArcaneArchives.GEMCUTTERS_TABLE.get().getName();
    }

    @Override
    public int getWidth() {
        return 130;
    }

    @Override
    public int getHeight() {
        return 72;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public ResourceLocation getRegistryName(GemCuttingRecipe recipe) {
        return recipe.getId();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GemCuttingRecipe recipe, IFocusGroup focus) {
        boolean classic = recipe.requirements().size() <= CLASSIC_INPUTS;
        for (int index = 0; index < recipe.requirements().size(); index++) {
            var requirement = recipe.requirements().get(index);
            var slot = classic ? builder.addInputSlot(6 + index % 2 * 18, 1 + index / 2 * 18)
                    : builder.addInputSlot().setStandardSlotBackground();
            slot.addItemStacks(Arrays.stream(requirement.ingredient().getItems())
                    .map(stack -> stack.copyWithCount(requirement.count())).toList());
        }
        var output = builder.addOutputSlot(104, 28);
        if (!classic) {
            output.setOutputSlotBackground();
        }
        var level = Minecraft.getInstance().level;
        output.addItemStack(recipe.getResultItem(level == null ? RegistryAccess.EMPTY : level.registryAccess()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, GemCuttingRecipe recipe, IFocusGroup focus) {
        if (recipe.requirements().size() > CLASSIC_INPUTS) {
            builder.addScrollGridWidget(builder.getRecipeSlots().getSlots(RecipeIngredientRole.INPUT), 4, 4).setPosition(1, 0);
        }
    }

    @Override
    public void draw(GemCuttingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        if (recipe.requirements().size() <= CLASSIC_INPUTS) {
            artwork.draw(graphics, 5, 0);
        } else {
            gui.getRecipeArrow().draw(graphics, 82, 28);
        }
    }
}
