package com.aranaira.arcanearchives.integration.jei;

import com.aranaira.arcanearchives.ArcaneArchives;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.text.NumberFormat;

final class ResonatorCategory implements IRecipeCategory<ResonatorCategory.Growth> {
    private final IDrawable artwork;
    private final IDrawable icon;
    private final IDrawable arrow;

    ResonatorCategory(IGuiHelper gui) {
        artwork = gui.createDrawable(ArcaneArchives.id("textures/gui/jei/radiant_resonator.png"), 0, 0, 22, 62);
        icon = gui.createDrawableItemLike(ArcaneArchives.RADIANT_RESONATOR_ITEM.get());
        arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<Growth> getRecipeType() {
        return ArchiveJeiPlugin.RESONATOR;
    }

    @Override
    public Component getTitle() {
        return ArcaneArchives.RADIANT_RESONATOR.get().getName();
    }

    @Override
    public int getWidth() {
        return 146;
    }

    @Override
    public int getHeight() {
        return 88;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Growth growth, IFocusGroup focus) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 87, 23).setOutputSlotBackground()
                .addItemLike(ArcaneArchives.RAW_QUARTZ_CLUSTER_ITEM.get());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 23).setOutputSlotBackground()
                .addItemLike(ArcaneArchives.RAW_QUARTZ.get());
    }

    @Override
    public void draw(Growth growth, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        artwork.draw(graphics, 22, 0);
        arrow.draw(graphics, 54, 23);
        String seconds = NumberFormat.getNumberInstance().format(growth.ticks() / 20.0);
        Component time = Component.translatable("gui.arcanearchives.jei.resonator_time", seconds);
        var font = Minecraft.getInstance().font;
        int y = 66;
        for (var line : font.split(time, getWidth() - 8)) {
            graphics.drawString(font, line, (getWidth() - font.width(line)) / 2, y, 0x363636, false);
            y += font.lineHeight;
        }
    }

    record Growth(int ticks) {}
}
