package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttersTableMenu;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GemCuttersTableScreen extends AbstractContainerScreen<GemCuttersTableMenu> {
    private static final ResourceLocation BACKGROUND = ArcaneArchives.id("textures/gui/gemcutterstable.png");
    private static final int RECIPES_PER_PAGE = 7;
    private static final int FIRST_RECIPE_X = 41;
    private static final int RECIPE_Y = 70;
    private int page;
    private int selection = -1;
    private List<Boolean> visibleCraftable = List.of();

    public GemCuttersTableScreen(GemCuttersTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 206;
        imageHeight = 254;
    }

    @Override
    protected void init() {
        super.init();
        containerTick();
    }

    @Override
    protected void containerTick() {
        List<GemCuttingRecipe> recipes = menu.recipes();
        if (selection != menu.selectedRecipe()) {
            selection = menu.selectedRecipe();
            if (selection >= 0) {
                page = selection / RECIPES_PER_PAGE;
            }
        }
        page = Math.min(page, pageCount(recipes) - 1);
        refreshAvailability(recipes);
    }

    private void refreshAvailability(List<GemCuttingRecipe> recipes) {
        int start = page * RECIPES_PER_PAGE;
        visibleCraftable = recipes.subList(start, Math.min(start + RECIPES_PER_PAGE, recipes.size()))
                .stream().map(menu::canCraft).toList();
    }

    private static int pageCount(List<GemCuttingRecipe> recipes) {
        return Math.max(1, (recipes.size() + RECIPES_PER_PAGE - 1) / RECIPES_PER_PAGE);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = recipeAt(mouseX, mouseY);
        if (hovered >= 0) {
            renderRecipeTooltip(graphics, hovered, mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        List<GemCuttingRecipe> recipes = menu.recipes();
        for (int slot = 0; slot < visibleCraftable.size(); slot++) {
            int index = page * RECIPES_PER_PAGE + slot;
            if (index >= recipes.size()) {
                break;
            }
            int x = leftPos + FIRST_RECIPE_X + slot * 18;
            int y = topPos + RECIPE_Y;
            ItemStack result = recipes.get(index).getResultItem(minecraft.level.registryAccess());
            if (index == selection) {
                graphics.blit(BACKGROUND, x - 2, y - 2, 206, 0, 20, 20);
            }
            graphics.renderItem(result, x, y);
            graphics.renderItemDecorations(font, result, x, y);
            if (!visibleCraftable.get(slot)) {
                graphics.fill(x, y, x + 16, y + 16, 200, 0x9924313F);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 4, 0xFFE7CC, false);
    }

    private void renderRecipeTooltip(GuiGraphics graphics, int slot, int mouseX, int mouseY) {
        GemCuttingRecipe recipe = menu.recipes().get(page * RECIPES_PER_PAGE + slot);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(recipe.getResultItem(minecraft.level.registryAccess()).getHoverName().copy()
                .withStyle(visibleCraftable.get(slot) ? ChatFormatting.GREEN : ChatFormatting.RED));
        for (GemCuttingRecipe.Requirement requirement : recipe.requirements()) {
            ItemStack[] alternatives = requirement.ingredient().getItems();
            Component name = alternatives.length > 0
                    ? alternatives[(int) (Util.getMillis() / 1000 % alternatives.length)].getHoverName()
                    : Component.translatable("gui.arcanearchives.gem_cutting.missing_ingredient");
            tooltip.add(Component.translatable("gui.arcanearchives.gem_cutting.ingredient", requirement.count(), name));
        }
        graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<GemCuttingRecipe> recipes = menu.recipes();
            int recipe = recipeAt(mouseX, mouseY);
            if (recipe >= 0) {
                select(page * RECIPES_PER_PAGE + recipe);
                return true;
            }
            boolean previous = isHovering(25, 69, 11, 18, mouseX, mouseY);
            if (previous || isHovering(170, 69, 11, 18, mouseX, mouseY)) {
                page = Math.floorMod(page + (previous ? -1 : 1), pageCount(recipes));
                refreshAvailability(recipes);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        List<GemCuttingRecipe> recipes = menu.recipes();
        if (scroll != 0 && !recipes.isEmpty()) {
            int step = scroll < 0 ? 1 : -1;
            int start = selection < 0 && step < 0 ? 0 : selection;
            for (int i = 1; i <= recipes.size(); i++) {
                int candidate = Math.floorMod(start + i * step, recipes.size());
                if (menu.canCraft(recipes.get(candidate))) {
                    select(candidate);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    private int recipeAt(double mouseX, double mouseY) {
        int count = Math.min(visibleCraftable.size(), menu.recipes().size() - page * RECIPES_PER_PAGE);
        for (int slot = 0; slot < count; slot++) {
            if (isHovering(FIRST_RECIPE_X + slot * 18, RECIPE_Y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return -1;
    }

    private void select(int index) {
        ArchiveNetwork.selectGemRecipe(menu.containerId, menu.recipes().get(index).getId());
    }
}
