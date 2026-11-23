package com.aranaira.arcanearchives.integration.crafttweaker;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.ArchiveTag;
import com.aranaira.arcanearchives.arsenal.ArchiveTag.Domain;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import org.openzen.zencode.java.ZenCodeType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@ZenRegister
@ZenCodeType.Name("mods.arcanearchives.Tags")
public final class TagScripts {
    private static final IRecipeManager<ArchiveTag> TAGS = ArcaneArchives.ARCHIVE_TAG_TYPE::get;

    private TagScripts() {}

    @ZenCodeType.Method
    public static void define(String id, ScriptColor[] members) {
        define(Domain.COLOR, id, members);
    }

    @ZenCodeType.Method
    public static void define(String id, ScriptGem[] members) {
        define(Domain.GEM, id, members);
    }

    @ZenCodeType.Method
    public static void define(String id, ScriptCut[] members) {
        define(Domain.CUT, id, members);
    }

    @ZenCodeType.Method
    public static void add(String id, ScriptColor[] members) {
        update(Domain.COLOR, id, members, Change.ADD);
    }

    @ZenCodeType.Method
    public static void add(String id, ScriptGem[] members) {
        update(Domain.GEM, id, members, Change.ADD);
    }

    @ZenCodeType.Method
    public static void add(String id, ScriptCut[] members) {
        update(Domain.CUT, id, members, Change.ADD);
    }

    @ZenCodeType.Method
    public static void remove(String id, ScriptColor[] members) {
        update(Domain.COLOR, id, members, Change.REMOVE);
    }

    @ZenCodeType.Method
    public static void remove(String id, ScriptGem[] members) {
        update(Domain.GEM, id, members, Change.REMOVE);
    }

    @ZenCodeType.Method
    public static void remove(String id, ScriptCut[] members) {
        update(Domain.CUT, id, members, Change.REMOVE);
    }

    @ZenCodeType.Method
    public static void clearColors(String id) {
        update(Domain.COLOR, id, new ScriptColor[0], Change.CLEAR);
    }

    @ZenCodeType.Method
    public static void clearGems(String id) {
        update(Domain.GEM, id, new ScriptGem[0], Change.CLEAR);
    }

    @ZenCodeType.Method
    public static void clearCuts(String id) {
        update(Domain.CUT, id, new ScriptCut[0], Change.CLEAR);
    }

    @ZenCodeType.Method
    public static ScriptColor[] getColors(String id) {
        return members(Domain.COLOR, id).stream().map(ScriptColor::valueOf).toArray(ScriptColor[]::new);
    }

    @ZenCodeType.Method
    public static ScriptGem[] getGems(String id) {
        return members(Domain.GEM, id).stream().map(ScriptGem::valueOf).toArray(ScriptGem[]::new);
    }

    @ZenCodeType.Method
    public static ScriptCut[] getCuts(String id) {
        return members(Domain.CUT, id).stream().map(ScriptCut::valueOf).toArray(ScriptCut[]::new);
    }

    private static List<String> members(Domain domain, String id) {
        return ScriptRecipes.require(TAGS.getRecipeList(), domain.key(ScriptRecipes.id(id))).content().members()
                .stream().map(name -> name.toUpperCase(Locale.ROOT)).toList();
    }

    private static void define(Domain domain, String id, Enum<?>[] values) {
        var key = domain.key(ScriptRecipes.id(id));
        ScriptRecipes.add(TAGS, new ArchiveTag(key, new ArchiveTag.Content(domain, names(values))));
    }

    private static void update(Domain domain, String id, Enum<?>[] values, Change change) {
        var key = domain.key(ScriptRecipes.id(id));
        var names = names(values);
        ScriptRecipes.change(TAGS, change + " members of " + key, tags -> {
            var existing = ScriptRecipes.require(tags, key);
            var entries = new ArrayList<>(existing.content().members());
            switch (change) {
                case ADD -> entries.addAll(names);
                case REMOVE -> entries.removeAll(names);
                case CLEAR -> entries.clear();
            }
            var updated = new ArchiveTag(key, new ArchiveTag.Content(domain, entries));
            tags.remove(key);
            tags.add(key, updated);
        });
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(ArchiveTag::name).toList();
    }

    private enum Change { ADD, REMOVE, CLEAR }
}
