package com.aranaira.arcanearchives.integration.stages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.MetadataRecipe;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public record StageRule(ResourceLocation id, List<String> all, List<String> any) implements MetadataRecipe {
    private static final Pattern STAGE_NAME = Pattern.compile("[a-z0-9_:]{1,64}");
    private static final int MAX_STAGES = 64;

    public StageRule {
        all = validate(all);
        any = validate(any);
        if (all.isEmpty() && any.isEmpty()) {
            throw new IllegalArgumentException("A stage rule requires at least one stage");
        }
    }

    private static List<String> validate(List<String> stages) {
        if (stages.size() > MAX_STAGES) {
            throw new IllegalArgumentException("A stage list may contain at most " + MAX_STAGES + " stages");
        }
        for (String stage : stages) {
            if (!STAGE_NAME.matcher(stage).matches()) {
                throw new IllegalArgumentException("Invalid GameStages name: " + stage);
            }
        }
        return List.copyOf(stages);
    }

    public enum Scope {
        FEATURE, FAMILY, RECIPE;

        public ResourceLocation key(ResourceLocation target) {
            return ArcaneArchives.id("stages/" + name().toLowerCase(Locale.ROOT)
                    + "/" + target.getNamespace() + "/" + target.getPath());
        }
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ArcaneArchives.STAGE_RULE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ArcaneArchives.STAGE_RULE_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<StageRule> {
        @Override
        public StageRule fromJson(ResourceLocation id, JsonObject json) {
            return new StageRule(id, stageList(json, "all"), stageList(json, "any"));
        }

        @Override
        public StageRule fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new StageRule(id, readStages(buffer), readStages(buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, StageRule rule) {
            buffer.writeCollection(rule.all(), (buf, stage) -> buf.writeUtf(stage, 64));
            buffer.writeCollection(rule.any(), (buf, stage) -> buf.writeUtf(stage, 64));
        }

        private static List<String> stageList(JsonObject json, String key) {
            if (!json.has(key)) {
                return List.of();
            }
            return GsonHelper.getAsJsonArray(json, key).asList().stream()
                    .map(element -> GsonHelper.convertToString(element, key)).toList();
        }

        private static List<String> readStages(FriendlyByteBuf buffer) {
            return buffer.readCollection(FriendlyByteBuf.limitValue(java.util.ArrayList::new, MAX_STAGES),
                    buf -> buf.readUtf(64));
        }
    }
}
