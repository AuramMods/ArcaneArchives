package com.aranaira.arcanearchives.client.tome;

import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class TomeLayout {
    private static final int TEXT_COLOR = 0x392318;
    private static final int LINK_COLOR = 0x643178;
    private static final Pattern WORDS = Pattern.compile("\\s+|\\S+");
    private final TomeContents contents;
    private final Font font;
    private final Level level;
    private final int width;
    private final float artScale;
    private final List<Placed> placed = new ArrayList<>();
    private int height;

    TomeLayout(TomeContents contents, TomeContents.Page page, Font font, Level level, int width) {
        this.contents = contents;
        this.font = font;
        this.level = level;
        this.width = width;
        this.artScale = page.id().startsWith("Home:") ? 0.72F : 1F;
        for (Element part : TomeContents.elements(page.content(), "")) {
            if (!TomeContents.shown(part)) {
                continue;
            }
            switch (part.getTagName()) {
                case "p" -> paragraph(part);
                case "space" -> height += Math.round(number(part, "height", 4) * artScale);
                case "horizontal_rule" -> rule();
                case "recipe" -> recipe(part);
                default -> {}
            }
        }
    }

    List<Placed> elements() {
        return placed;
    }

    int height() {
        return height;
    }

    private void paragraph(Element element) {
        var parts = new ArrayList<Inline>();
        append(element, parts, Style.EMPTY, null, scale(element));
        int indent = Math.min(width / 2, number(element, "indent", 0));
        boolean centered = element.getAttribute("align").equals("center");
        int lineWidth = 0;
        var line = new ArrayList<Inline>();
        for (Inline part : parts) {
            if (part.blank() && (line.isEmpty() || line.get(line.size() - 1).blank())) {
                continue;
            }
            if (lineWidth + part.width() > width - indent && !line.isEmpty()) {
                placeLine(line, indent, centered);
                line.clear();
                lineWidth = 0;
            }
            if (!part.blank() || !line.isEmpty()) {
                line.add(part);
                lineWidth += part.width();
            }
        }
        placeLine(line, indent, centered);
        height += 3;
    }

    private void placeLine(List<Inline> line, int indent, boolean centered) {
        int size = line.size();
        while (size > 0 && line.get(size - 1).blank()) {
            size--;
        }
        if (size == 0) {
            return;
        }
        var shown = line.subList(0, size);
        int lineWidth = shown.stream().mapToInt(Inline::width).sum();
        int lineHeight = shown.stream().mapToInt(Inline::height).max().orElse(font.lineHeight);
        int x = centered ? indent + Math.max(0, width - indent - lineWidth) / 2 : indent;
        for (Inline part : shown) {
            placed.add(new Placed(x, height + (lineHeight - part.height()) / 2, part));
            x += part.width();
        }
        height += lineHeight + 2;
    }

    private void append(Element parent, List<Inline> parts, Style inherited, String link, float scale) {
        Style style = inherited;
        if (parent.hasAttribute("bold")) {
            style = style.withBold(Boolean.parseBoolean(parent.getAttribute("bold")));
        }
        if (parent.hasAttribute("italics")) {
            style = style.withItalic(Boolean.parseBoolean(parent.getAttribute("italics")));
        }
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.TEXT_NODE) {
                var words = WORDS.matcher(child.getNodeValue());
                while (words.find()) {
                    String word = words.group();
                    boolean blank = word.isBlank();
                    parts.add(text(blank ? " " : word, style, link, scale, blank));
                }
            } else if (child instanceof Element element && TomeContents.shown(element)) {
                switch (element.getTagName()) {
                    case "link" -> {
                        String target = element.getAttribute("ref");
                        append(element, parts, style, contents.page(target) == null ? null : target, scale);
                    }
                    case "stack" -> {
                        ItemStack item = stack(element);
                        if (!item.isEmpty()) {
                            int size = Math.max(8, (int) (16 * scale(element)));
                            parts.add(item(() -> item, size, link));
                        }
                    }
                    case "image" -> {
                        Inline image = image(element, link);
                        if (image != null) {
                            parts.add(image);
                        }
                    }
                    default -> append(element, parts, style, link, scale);
                }
            }
        }
    }

    private Inline text(String word, Style style, String link, float scale, boolean blank) {
        Component component = Component.literal(word).withStyle(style);
        return new Inline((int) Math.ceil(font.width(component) * scale), (int) Math.ceil(font.lineHeight * scale),
                link, () -> null, blank, (graphics, x, y, hover) -> {
                    graphics.pose().pushPose();
                    graphics.pose().translate(x, y, 0);
                    graphics.pose().scale(scale, scale, 1F);
                    graphics.drawString(font, component, 0, 0, link == null ? TEXT_COLOR : hover ? 0xA04483 : LINK_COLOR, false);
                    graphics.pose().popPose();
                });
    }

    private Inline image(Element element, String link) {
        ResourceLocation texture = texture(element.getAttribute("src"));
        if (texture == null) {
            return null;
        }
        ResourceLocation hoverTexture = texture(element.getAttribute("hoverSrc"));
        int textureWidth = Math.max(1, number(element, "tw", 16));
        int textureHeight = Math.max(1, number(element, "th", 16));
        float scale = Math.min(scale(element) * artScale, (float) width / textureWidth);
        int w = Math.max(1, (int) (textureWidth * scale));
        int h = Math.max(1, (int) (textureHeight * scale));
        return new Inline(w, h, link, () -> null, false, (graphics, x, y, hover) ->
                graphics.blit(hover && hoverTexture != null ? hoverTexture : texture, x, y, w, h,
                        0, 0, textureWidth, textureHeight, textureWidth, textureHeight));
    }

    private static ResourceLocation texture(String id) {
        if (id.isEmpty()) {
            return null;
        }
        ResourceLocation path = ResourceLocation.tryParse(id);
        return path == null ? null : path.withPath(value -> "textures/"
                + (value.startsWith("items/") ? "item/" + value.substring(6) : value) + ".png");
    }

    private void rule() {
        ResourceLocation book = texture("arcanearchives:gui/book");
        var part = new Inline(120, 7, null, () -> null, false,
                (graphics, x, y, hover) -> graphics.blit(book, x, y, 50, 10, 120, 7));
        placed.add(new Placed(Math.max(0, (width - part.width()) / 2), height + 3, part));
        height += 14;
    }

    private void recipe(Element section) {
        var resultNode = TomeContents.elements(section, "recipe.result").stream().findFirst().orElse(null);
        if (resultNode == null) {
            return;
        }
        ItemStack output = TomeContents.elements(resultNode, "stack").stream().findFirst().map(TomeLayout::stack).orElse(ItemStack.EMPTY);
        if (output.isEmpty()) {
            return;
        }
        String type = section.getAttribute("type");
        Predicate<Recipe<?>> matcher = switch (type) {
            case "shaped" -> recipe -> recipe.getType() == RecipeType.CRAFTING;
            case "shapeless" -> recipe -> recipe instanceof ShapelessRecipe;
            case "arcanearchives:gct_recipe" -> recipe -> recipe instanceof GemCuttingRecipe;
            default -> recipe -> true;
        };
        var recipes = level.getRecipeManager().getRecipes().stream().filter(matcher)
                .filter(recipe -> !(recipe instanceof GemCuttingRecipe cutting) || cutting.enabled())
                .filter(recipe -> recipe.getResultItem(level.registryAccess()).is(output.getItem()))
                .sorted(Comparator.comparing(Recipe::getId)).toList();
        var archive = recipes.stream().filter(recipe -> recipe.getId().getNamespace().equals("arcanearchives")).toList();
        (archive.isEmpty() ? recipes : archive).forEach(this::placeRecipe);
    }

    private void placeRecipe(Recipe<?> recipe) {
        var inputs = new ArrayList<GemCuttingRecipe.Requirement>();
        if (recipe instanceof GemCuttingRecipe cutting) {
            inputs.addAll(cutting.requirements());
        } else {
            recipe.getIngredients().forEach(ingredient -> inputs.add(new GemCuttingRecipe.Requirement(ingredient, 1)));
        }
        int columns = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : Math.min(inputs.size(), 5);
        if (columns == 0) {
            return;
        }
        int rows = (inputs.size() + columns - 1) / columns;
        int left = Math.max(0, (width - columns * 20 - 38) / 2);
        int top = height + 5;
        for (int i = 0; i < inputs.size(); i++) {
            var input = inputs.get(i);
            placeRecipeSlot(left + i % columns * 20, top + i / columns * 20,
                    () -> representative(input.ingredient(), input.count()));
        }
        var arrow = text("→", Style.EMPTY, null, 1, false);
        placed.add(new Placed(left + columns * 20 + 2, top + rows * 10 - 4, arrow));
        ItemStack output = recipe.getResultItem(level.registryAccess());
        placeRecipeSlot(left + columns * 20 + 17, top + rows * 10 - 10, () -> output);
        height += rows * 20 + 8;
    }

    private void placeRecipeSlot(int x, int y, Supplier<ItemStack> input) {
        var border = new Inline(18, 18, null, () -> null, false,
                (graphics, x1, y1, hover) -> {
                    graphics.fill(x1, y1, x1 + 18, y1 + 18, 0x777B5639);
                    graphics.renderOutline(x1, y1, 18, 18, 0xFF8D684B);
                });
        placed.add(new Placed(x, y, border));
        placed.add(new Placed(x + 1, y + 1, item(input, 16, null)));
    }

    private static ItemStack representative(Ingredient ingredient, int count) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[(int) (Util.getMillis() / 1000 % items.length)].copyWithCount(count);
    }

    private Inline item(Supplier<ItemStack> supplier, int size, String link) {
        ItemStack initial = supplier.get();
        String target = link == null && !initial.isEmpty() ? contents.itemLink(BuiltInRegistries.ITEM.getKey(initial.getItem())) : link;
        return new Inline(size, size, target, () -> {
            ItemStack item = supplier.get();
            return item.isEmpty() ? null : item.getHoverName();
        }, false, (graphics, x, y, hover) -> {
            ItemStack item = supplier.get();
            if (!item.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                float scale = size / 16F;
                graphics.pose().scale(scale, scale, 1F);
                graphics.renderItem(item, 0, 0);
                graphics.renderItemDecorations(font, item, 0, 0);
                graphics.pose().popPose();
            }
        });
    }

    private static ItemStack stack(Element element) {
        ResourceLocation id = ResourceLocation.tryParse(element.getAttribute("item"));
        return id == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    private static int number(Element element, String key, int fallback) {
        String value = element.getAttribute(key);
        return value.isEmpty() ? fallback : Math.max(0, Integer.parseInt(value));
    }

    private static float scale(Element element) {
        String value = element.getAttribute("scale");
        return value.isEmpty() ? 1F : Math.max(0.25F, Float.parseFloat(value));
    }

    @FunctionalInterface
    interface Draw {
        void render(GuiGraphics graphics, int x, int y, boolean hover);
    }

    record Inline(int width, int height, String link, Supplier<Component> tooltip, boolean blank, Draw draw) {}

    record Placed(int x, int y, Inline inline) {
        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + inline.width() && py < y + inline.height();
        }
    }
}
