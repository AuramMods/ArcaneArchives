package com.aranaira.arcanearchives.client.tome;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.tome.TomeOfArcanaItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.ModList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TomeContents {
    record Page(String id, String title, Element content) {}

    private final List<Page> pages;
    private final Map<String, Page> linkedPages;
    private final Map<ResourceLocation, String> itemLinks;
    private final ResourceLocation background;

    private TomeContents(Element book) {
        LinkedHashMap<String, Page> chapters = new LinkedHashMap<>();
        Map<String, Page> links = new HashMap<>();
        for (Element chapter : elements(book, "chapter")) {
            String chapterId = chapter.getAttribute("id");
            int index = 0;
            for (Element section : elements(chapter, "section")) {
                if (!shown(section)) {
                    continue;
                }
                String id = section.getAttribute("id");
                String key = id.isEmpty() ? chapterId + ":" + index : chapterId + ":" + id;
                var titles = section.getElementsByTagName("title");
                String title = titles.getLength() == 0 ? book.getAttribute("title") : titles.item(0).getTextContent().strip();
                var page = new Page(key, title, section);
                chapters.put(key, page);
                links.put(key, page);
                links.putIfAbsent(chapterId, page);
                index++;
            }
        }
        pages = List.copyOf(chapters.values());
        linkedPages = Map.copyOf(links);

        Map<ResourceLocation, String> items = new HashMap<>();
        for (Element linksElement : elements(book, "stack-links")) {
            for (Element entry : elements(linksElement, "stack")) {
                ResourceLocation id = ResourceLocation.tryParse(entry.getAttribute("item"));
                String link = entry.getTextContent().strip();
                if (id != null && links.containsKey(link)) {
                    items.put(id, link);
                }
            }
        }
        itemLinks = Map.copyOf(items);
        background = ResourceLocation.tryParse(book.getAttribute("background"));
    }

    static TomeContents load(ResourceManager resources) throws IOException, ParserConfigurationException, SAXException {
        var factory = DocumentBuilderFactory.newDefaultInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (var input = resources.open(TomeOfArcanaItem.BOOK)) {
            return new TomeContents(factory.newDocumentBuilder().parse(input).getDocumentElement());
        }
    }

    List<Page> pages() {
        return pages;
    }

    Page page(String id) {
        return linkedPages.get(id);
    }

    String itemLink(ResourceLocation id) {
        return itemLinks.get(id);
    }

    ResourceLocation background() {
        return background;
    }

    static boolean shown(Element element) {
        String condition = element.getAttribute("condition");
        return switch (condition) {
            case "arsenal_enabled" -> ArchiveConfig.ARSENAL_ENABLED.get();
            case "arsenal_disabled" -> !ArchiveConfig.ARSENAL_ENABLED.get();
            case "false" -> false;
            case "", "true" -> true;
            default -> condition.startsWith("ml_") && ModList.get().isLoaded(condition.substring(3))
                    || condition.startsWith("no_") && !ModList.get().isLoaded(condition.substring(3));
        };
    }

    static List<Element> elements(Element parent, String name) {
        var result = new ArrayList<Element>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && (name.isEmpty() || element.getTagName().equals(name))) {
                result.add(element);
            }
        }
        return result;
    }
}
