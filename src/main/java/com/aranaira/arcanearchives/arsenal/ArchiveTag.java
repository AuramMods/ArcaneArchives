package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.aranaira.arcanearchives.crafting.RuleSerializer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public record ArchiveTag(ResourceLocation id, Content content) implements MetadataRecipe {
    public static RecipeSerializer<ArchiveTag> serializer() {
        return new RuleSerializer<>(Content.CODEC, ArchiveTag::new, ArchiveTag::content);
    }

    public static boolean contains(Level level, Domain domain, ResourceLocation tag, Enum<?> member) {
        return level.getRecipeManager().byKey(domain.key(tag)).orElse(null) instanceof ArchiveTag entries
                && entries.content.domain == domain && entries.content.members.contains(name(member));
    }

    public static String name(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    static <T> Codec<T> names(Function<String, T> parse, Function<T, String> format) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(parse.apply(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException error) {
                return DataResult.error(() -> "Unknown value: " + name);
            }
        }, format);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.ARCHIVE_TAG_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.ARCHIVE_TAG_TYPE.get();
    }

    public enum Domain {
        COLOR(GemType.Color.values()), GEM(GemType.values()), CUT(GemType.Cut.values());

        final List<String> values;

        Domain(Enum<?>[] values) {
            this.values = Arrays.stream(values).map(ArchiveTag::name).toList();
        }

        public ResourceLocation key(ResourceLocation id) {
            return ArcaneArchives.id("custom_tags/" + ArchiveTag.name(this) + '/' + id.getNamespace() + '/' + id.getPath());
        }
    }

    public record Content(Domain domain, List<String> members) {
        private static final Codec<Content> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                names(Domain::valueOf, ArchiveTag::name).fieldOf("domain").forGetter(Content::domain),
                Codec.STRING.listOf().fieldOf("members").forGetter(Content::members)
        ).apply(instance, Content::new));

        public Content {
            for (String member : members) {
                if (!domain.values.contains(member)) {
                    throw new IllegalArgumentException("Unknown " + ArchiveTag.name(domain) + " tag member " + member);
                }
            }
            members = members.stream().distinct().sorted().toList();
        }
    }
}
