package com.aranaira.arcanearchives.client.tome;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

public final class TomeOfArcanaScreen extends Screen {
    private static final String TEXT = "gui.arcanearchives.tome.";
    private final TomeContents contents;
    private final Deque<Visit> history = new ArrayDeque<>();
    private TomeContents.Page page;
    private TomeLayout layout;
    private Button back;
    private Button previous;
    private Button next;
    private int scroll;
    private int x;
    private int y;
    private int panelWidth;
    private int panelHeight;
    private int left;
    private int top;
    private int right;
    private int bottom;

    private TomeOfArcanaScreen(TomeContents contents) {
        super(Component.translatable("item.arcanearchives.tome_arcana"));
        this.contents = contents;
        page = contents.pages().get(0);
    }

    public static void open() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        try {
            TomeContents contents = TomeContents.load(client.getResourceManager());
            if (contents.pages().isEmpty()) {
                throw new IOException("The Tome has no readable pages");
            }
            client.setScreen(new TomeOfArcanaScreen(contents));
        } catch (IOException | ParserConfigurationException | SAXException exception) {
            LogUtils.getLogger().warn("Unable to read the Tome of Arcana", exception);
            client.player.displayClientMessage(Component.translatable(TEXT + "unavailable"), true);
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(width - 12, 512);
        panelHeight = Math.min(height - 12, 350);
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        left = x + panelWidth * 39 / 256;
        top = y + 25;
        right = x + panelWidth * 228 / 256;
        bottom = y + panelHeight - 32;
        int footer = bottom + 3;
        int buttonWidth = Math.min(58, Math.max(22, (right - left - 12) / 4));
        back = control("back", left, footer, buttonWidth, button -> goBack());
        control("home", left + buttonWidth + 4, footer, buttonWidth, button -> navigate(contents.pages().get(0)));
        previous = control("previous", right - 2 * buttonWidth - 4, footer, buttonWidth, button -> turn(-1));
        next = control("next", right - buttonWidth, footer, buttonWidth, button -> turn(1));
        layout();
    }

    private Button control(String label, int x, int y, int width, Button.OnPress action) {
        return addRenderableWidget(Button.builder(Component.translatable(TEXT + label), action).bounds(x, y, width, 18).build());
    }

    private void layout() {
        layout = new TomeLayout(contents, page, font, minecraft.level, Math.max(1, right - left));
        setScroll(scroll);
        int index = contents.pages().indexOf(page);
        back.active = !history.isEmpty();
        previous.active = index > 0;
        next.active = index + 1 < contents.pages().size();
    }

    private void navigate(TomeContents.Page destination) {
        if (destination != null && !destination.equals(page)) {
            history.push(new Visit(page, scroll));
            page = destination;
            scroll = 0;
            layout();
        }
    }

    private void turn(int offset) {
        int index = contents.pages().indexOf(page) + offset;
        if (index >= 0 && index < contents.pages().size()) {
            navigate(contents.pages().get(index));
        }
    }

    private void goBack() {
        Visit visit = history.poll();
        if (visit != null) {
            page = visit.page();
            scroll = visit.scroll();
            layout();
        }
    }

    private int maxScroll() {
        return Math.max(0, layout.height() - Math.max(1, bottom - top));
    }

    private void setScroll(int value) {
        scroll = Mth.clamp(value, 0, maxScroll());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        if (contents.background() != null) {
            graphics.blit(contents.background(), x, y, panelWidth, panelHeight, 0, 0, 256, 256, 256, 256);
        } else {
            graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xFFDEC6A6);
        }
        String title = font.plainSubstrByWidth(page.title(), right - left);
        if (!page.id().startsWith("Home:")) {
            graphics.drawString(font, title, (left + right - font.width(title)) / 2, y + 10, 0x392318, false);
        }
        boolean pointerInside = inPage(mouseX, mouseY);
        int hoveredX = mouseX - left;
        int hoveredY = mouseY - top + scroll;
        Component tip = null;
        graphics.enableScissor(left, top, right, bottom);
        for (var element : layout.elements()) {
            int viewY = top + element.y() - scroll;
            var inline = element.inline();
            if (viewY + inline.height() <= top || viewY >= bottom) {
                continue;
            }
            boolean hovered = pointerInside && element.contains(hoveredX, hoveredY);
            inline.draw().render(graphics, left + element.x(), viewY, hovered);
            if (hovered) {
                tip = inline.tooltip().get();
            }
        }
        graphics.disableScissor();
        int range = maxScroll();
        if (range > 0) {
            int viewport = bottom - top;
            int thumb = Math.max(10, viewport * viewport / layout.height());
            int offset = (viewport - thumb) * scroll / range;
            graphics.fill(right + 2, top, right + 5, bottom, 0x44735536);
            graphics.fill(right + 2, top + offset, right + 5, top + offset + thumb, 0xB5735536);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        if (tip != null) {
            graphics.renderTooltip(font, tip, mouseX, mouseY);
        }
    }

    private boolean inPage(double x, double y) {
        return x >= left && x < right && y >= top && y < bottom;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && inPage(x, y)) {
            int px = (int) (x - left);
            int py = (int) (y - top) + scroll;
            for (var element : layout.elements()) {
                String link = element.inline().link();
                if (link != null && element.contains(px, py)) {
                    navigate(contents.page(link));
                    return true;
                }
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (inPage(x, y) && maxScroll() > 0) {
            setScroll(scroll - (int) (delta * 26));
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> turn(-1);
            case GLFW.GLFW_KEY_RIGHT -> turn(1);
            case GLFW.GLFW_KEY_UP -> setScroll(scroll - 12);
            case GLFW.GLFW_KEY_DOWN -> setScroll(scroll + 12);
            case GLFW.GLFW_KEY_PAGE_UP -> setScroll(scroll - (bottom - top));
            case GLFW.GLFW_KEY_PAGE_DOWN -> setScroll(scroll + (bottom - top));
            case GLFW.GLFW_KEY_BACKSPACE -> goBack();
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record Visit(TomeContents.Page page, int scroll) {}
}
