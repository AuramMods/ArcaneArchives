package com.aranaira.arcanearchives.client;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.manifest.ManifestContents;
import com.aranaira.arcanearchives.manifest.ManifestMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public final class ManifestScreen extends AbstractContainerScreen<ManifestMenu> {
    private static final ResourceLocation BASE = ArcaneArchives.id("textures/gui/manifest_base.png");
    private static final ResourceLocation OVERLAY = ArcaneArchives.id("textures/gui/manifest_overlay.png");
    private static final ResourceLocation SIMPLE_BASE = ArcaneArchives.id("textures/gui/simple/manifest_base.png");
    private static final ResourceLocation SIMPLE_OVERLAY = ArcaneArchives.id("textures/gui/simple/manifest_overlay.png");
    private static final String TEXT = "gui.arcanearchives.manifest.";
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 9;
    private static final int CELL = 18;
    private static final int GRID_X = 11;
    private static final int GRID_Y = 29;
    private static final int SCROLL_X = 177;
    private static final int SCROLL_HEIGHT = CELL * VISIBLE_ROWS;
    private static String previousSearch = "";
    private static boolean alphabetical;
    private static boolean ascending;

    private EditBox search;
    private List<Entry> visible = List.of();
    private int lastRevision = -1;
    private BlockPos lastPosition;
    private int firstRow;

    public ManifestScreen(ManifestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 200;
        imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        String query = search == null ? ArchiveConfig.MANIFEST_KEEP_SEARCH.get() ? previousSearch : "" : search.getValue();
        search = new EditBox(font, leftPos + 13, topPos + 13, 86, 12, Component.translatable(TEXT + "search"));
        search.setBordered(false);
        search.setTextColor(0x332618);
        search.setTextColorUneditable(0x665545);
        search.setSuggestion(query.isEmpty() ? Component.translatable(TEXT + "search").getString() : null);
        search.setMaxLength(100);
        search.setValue(query);
        search.setResponder(value -> {
            firstRow = 0;
            search.setSuggestion(value.isEmpty() ? Component.translatable(TEXT + "search").getString() : null);
            updateEntries();
        });
        addRenderableWidget(search);
        setInitialFocus(search);
        updateEntries();
    }

    @Override
    protected void containerTick() {
        search.tick();
        if (menu.revision() != lastRevision || !minecraft.player.blockPosition().equals(lastPosition)) {
            updateEntries();
        }
    }

    @Override
    public void removed() {
        if (search != null) {
            previousSearch = search.getValue();
        }
        super.removed();
    }

    private void updateEntries() {
        lastRevision = menu.revision();
        lastPosition = minecraft.player.blockPosition();
        String query = search.getValue().strip().toLowerCase(Locale.ROOT);
        var groups = new LinkedHashMap<EntryKey, List<ManifestContents.Row>>();
        for (var row : menu.rows()) {
            var key = new EntryKey(row.variant(), ManifestClient.range(row.location()));
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        Comparator<Entry> sort = alphabetical ? Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER)
                : Comparator.comparingLong(Entry::amount);
        if (!ascending) {
            sort = sort.reversed();
        }
        visible = groups.entrySet().stream().map(entry -> Entry.from(entry.getKey(), entry.getValue()))
                .filter(entry -> entry.matches(query))
                .sorted(sort.thenComparing(Entry::range).thenComparing(entry -> entry.variant().id())).toList();
        firstRow = Mth.clamp(firstRow, 0, lastRow());
    }

    private int lastRow() {
        return Math.max(0, (visible.size() + COLUMNS - 1) / COLUMNS - VISIBLE_ROWS);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        Entry at = entryAt(mouseX, mouseY);
        if (at != null) {
            renderEntryTooltip(graphics, at, mouseX, mouseY);
        } else {
            String button = buttonUnder(mouseX, mouseY);
            if (button != null) {
                graphics.renderTooltip(font, Component.translatable(TEXT + button), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        boolean pretty = ArchiveConfig.PRETTY_GUIS.get();
        ResourceLocation base = pretty ? BASE : SIMPLE_BASE;
        ResourceLocation overlay = pretty ? OVERLAY : SIMPLE_OVERLAY;
        graphics.blit(base, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (ArchiveConfig.MANIFEST_GRID.get()) {
            for (int i = 0; i < COLUMNS * VISIBLE_ROWS; i++) {
                graphics.blit(base, gridX(i), gridY(i), 224, 0, CELL, CELL);
            }
        }

        int offset = firstRow * COLUMNS;
        int count = Math.min(COLUMNS * VISIBLE_ROWS, visible.size() - offset);
        for (int i = 0; i < count; i++) {
            Entry entry = visible.get(offset + i);
            int x = gridX(i) + 1;
            int y = gridY(i) + 1;
            if (entry.rows().stream().anyMatch(ManifestClient::tracked)) {
                graphics.fill(x, y, x + 16, y + 16, 0x77754CBC);
            }
            if (entry.variant().fluid()) {
                drawFluid(graphics, entry.fluid(), x, y);
            } else {
                graphics.renderItem(entry.item(), x, y);
                graphics.renderItemDecorations(font, entry.item(), x, y);
            }
            if (entry.range() != ManifestClient.Range.IN_RANGE) {
                graphics.fill(x, y, x + 16, y + 16, 200, 0x77000000);
            }
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 201);
            String amount = compact(entry.amount());
            graphics.drawString(font, amount, x + 17 - font.width(amount), y + 9, 0xFFFFFF, true);
            graphics.pose().popPose();
        }
        if (visible.isEmpty()) {
            Component status = Component.translatable(TEXT + (lastRevision == 0 ? "loading" : "empty"));
            graphics.drawCenteredString(font, status, leftPos + 92, topPos + 101, 0x554437);
        }

        graphics.blit(overlay, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.blit(overlay, leftPos + 112, topPos + 10, 242, alphabetical ? 28 : 42, 14, 14);
        graphics.blit(overlay, leftPos + 130, topPos + 10, 242, ascending ? 56 : 70, 14, 14);
        graphics.drawCenteredString(font, Component.translatable(TEXT + (menu.personalOnly() ? "personal_short" : "network_short")),
                leftPos + 155, topPos + 13, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable(TEXT + "end"), leftPos + 92, topPos + 202, 0xFFFFFF);
        if (lastRow() > 0) {
            int y = topPos + GRID_Y + (SCROLL_HEIGHT - 14) * firstRow / lastRow();
            graphics.fill(leftPos + SCROLL_X + 2, y, leftPos + SCROLL_X + 12, y + 14, 0xFFB28C74);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private void drawFluid(GuiGraphics graphics, FluidStack fluid, int x, int y) {
        if (fluid.isEmpty()) {
            return;
        }
        var extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        var texture = extensions.getStillTexture(fluid);
        if (texture == null) {
            return;
        }
        var sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int tint = extensions.getTintColor(fluid);
        graphics.blit(x, y, 0, 16, 16, sprite, FastColor.ARGB32.red(tint) / 255F,
                FastColor.ARGB32.green(tint) / 255F, FastColor.ARGB32.blue(tint) / 255F, FastColor.ARGB32.alpha(tint) / 255F);
    }

    private void renderEntryTooltip(GuiGraphics graphics, Entry entry, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        if (entry.variant().fluid()) {
            lines.add(entry.fluid().getDisplayName());
        } else {
            lines.addAll(getTooltipFromItem(minecraft, entry.item()));
        }
        lines.add(Component.translatable(TEXT + (entry.variant().fluid() ? "millibuckets" : "amount"), entry.amount())
                .withStyle(ChatFormatting.GOLD));
        if (entry.range() == ManifestClient.Range.IN_RANGE) {
            lines.add(Component.translatable(TEXT + "track").withStyle(ChatFormatting.GRAY));
        } else if (entry.range() == ManifestClient.Range.OUT_OF_RANGE) {
            lines.add(Component.translatable(TEXT + "far", ArchiveConfig.MANIFEST_RANGE.get()).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable(TEXT + "other_dimension").withStyle(ChatFormatting.GRAY));
        }
        if (hasShiftDown()) {
            int count = Math.min(entry.rows().size(), 10);
            for (int i = 0; i < count; i++) {
                var row = entry.rows().get(i);
                var pos = row.location().pos();
                lines.add(Component.translatable(TEXT + "position", row.label(), pos.getX(), pos.getY(), pos.getZ(), row.amount())
                        .withStyle(ChatFormatting.GRAY));
                if (entry.range() == ManifestClient.Range.OTHER_DIMENSION) {
                    lines.add(Component.literal(row.location().dimension().location().toString()).withStyle(ChatFormatting.DARK_GRAY));
                }
            }
            if (count < entry.rows().size()) {
                lines.add(Component.translatable(TEXT + "more", entry.rows().size() - count));
            }
        } else {
            lines.add(Component.translatable(TEXT + "show_positions").withStyle(ChatFormatting.DARK_GRAY));
        }
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && search.isMouseOver(mouseX, mouseY)) {
            search.setValue("");
            return true;
        }
        if (button == 0) {
            String action = buttonUnder(mouseX, mouseY);
            if (action != null) {
                switch (action) {
                    case "sort_count", "sort_name" -> alphabetical = !alphabetical;
                    case "descending", "ascending" -> ascending = !ascending;
                    case "network", "personal" -> clickServerButton(ManifestMenu.TOGGLE_PERSONAL);
                    case "grid" -> {
                        ArchiveConfig.MANIFEST_GRID.set(!ArchiveConfig.MANIFEST_GRID.get());
                        ArchiveConfig.CLIENT_SPEC.save();
                    }
                    case "refresh" -> clickServerButton(ManifestMenu.REFRESH);
                    case "end" -> ManifestClient.clearTracking();
                    default -> throw new IllegalStateException(action);
                }
                updateEntries();
                return true;
            }
            if (scrollAt(mouseX, mouseY)) {
                scrollTo(mouseY);
                return true;
            }
        }
        Entry entry = entryAt(mouseX, mouseY);
        if (entry != null && (button == 0 || button == 1)) {
            boolean started = ManifestClient.track(entry.rows(), button == 0);
            if (started && ArchiveConfig.MANIFEST_SHIFT_KEEPS_OPEN.get() != hasShiftDown()) {
                onClose();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key != GLFW.GLFW_KEY_ESCAPE && search.isFocused()
                && (search.keyPressed(key, scanCode, modifiers) || search.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (button == 0 && scrollAt(x, y)) {
            scrollTo(y);
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta == 0 || lastRow() == 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        firstRow = Mth.clamp(firstRow + (delta > 0 ? -1 : 1), 0, lastRow());
        return true;
    }

    private void clickServerButton(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean scrollAt(double x, double y) {
        return isHovering(SCROLL_X, GRID_Y, 14, SCROLL_HEIGHT, x, y);
    }

    private void scrollTo(double y) {
        firstRow = Mth.clamp((int) Math.round((y - topPos - GRID_Y) * lastRow() / SCROLL_HEIGHT), 0, lastRow());
    }

    private String buttonUnder(double x, double y) {
        if (isHovering(112, 10, 14, 14, x, y)) {
            return alphabetical ? "sort_name" : "sort_count";
        }
        if (isHovering(130, 10, 14, 14, x, y)) {
            return ascending ? "ascending" : "descending";
        }
        if (isHovering(148, 10, 14, 14, x, y)) {
            return menu.personalOnly() ? "personal" : "network";
        }
        if (isHovering(48, 200, 88, 14, x, y)) {
            return "end";
        }
        if (isHovering(158, 200, 14, 14, x, y)) {
            return "grid";
        }
        return isHovering(178, 200, 14, 14, x, y) ? "refresh" : null;
    }

    private int gridX(int index) {
        return leftPos + GRID_X + index % COLUMNS * CELL;
    }

    private int gridY(int index) {
        return topPos + GRID_Y + index / COLUMNS * CELL;
    }

    private Entry entryAt(double x, double y) {
        int relativeX = (int) Math.floor(x) - leftPos - GRID_X;
        int relativeY = (int) Math.floor(y) - topPos - GRID_Y;
        if (relativeX < 0 || relativeY < 0 || relativeX >= COLUMNS * CELL || relativeY >= SCROLL_HEIGHT) {
            return null;
        }
        int index = (firstRow + relativeY / CELL) * COLUMNS + relativeX / CELL;
        return index < visible.size() ? visible.get(index) : null;
    }

    private static String compact(long amount) {
        if (amount < 10000) {
            return Long.toString(amount);
        }
        if (amount < 1_000_000) {
            return amount / 1000 + "k";
        }
        return amount < 1_000_000_000 ? amount / 1_000_000 + "M" : amount / 1_000_000_000 + "B";
    }

    private record EntryKey(ManifestContents.Variant variant, ManifestClient.Range range) {}

    private record Entry(ManifestContents.Variant variant, ManifestClient.Range range, List<ManifestContents.Row> rows,
                         ItemStack item, FluidStack fluid, long amount, String name) {
        static Entry from(EntryKey key, List<ManifestContents.Row> rows) {
            ItemStack item = key.variant().item();
            FluidStack fluid = key.variant().fluidStack();
            String name = key.variant().fluid() ? fluid.getDisplayName().getString() : item.getHoverName().getString();
            return new Entry(key.variant(), key.range(), rows, item, fluid,
                    rows.stream().mapToLong(ManifestContents.Row::amount).sum(), name);
        }

        boolean matches(String query) {
            String namespace = variant.id().getNamespace();
            if (query.startsWith("@")) {
                String mod = ModList.get().getModContainerById(namespace)
                        .map(container -> container.getModInfo().getDisplayName()).orElse(namespace);
                String needle = query.substring(1).replace(" ", "");
                return namespace.contains(needle) || mod.toLowerCase(Locale.ROOT).replace(" ", "").contains(needle);
            }
            if (name.toLowerCase(Locale.ROOT).contains(query) || variant.id().toString().contains(query)) {
                return true;
            }
            return item.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantments(item).entrySet().stream()
                    .anyMatch(enchantment -> enchantment.getKey().getFullname(enchantment.getValue()).getString()
                            .toLowerCase(Locale.ROOT).contains(query));
        }
    }
}
