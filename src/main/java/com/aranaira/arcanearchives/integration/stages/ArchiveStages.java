package com.aranaira.arcanearchives.integration.stages;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.arsenal.GemType;
import com.blamejared.recipestages.RecipeStagesUtil;
import net.darkhax.gamestages.GameStageHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.aranaira.arcanearchives.integration.stages.StageRule.Scope.FAMILY;
import static com.aranaira.arcanearchives.integration.stages.StageRule.Scope.FEATURE;
import static com.aranaira.arcanearchives.integration.stages.StageRule.Scope.RECIPE;

public final class ArchiveStages {
    public static final ResourceLocation GEM_CUTTING = ArcaneArchives.id("gem_cutting");
    public static final ResourceLocation RADIANT_CRAFTING = ArcaneArchives.id("radiant_crafting");
    public static final ResourceLocation RESONATOR_GROWTH = ArcaneArchives.id("resonator_growth");
    public static final ResourceLocation CHEST_CONVERSION = ArcaneArchives.id("chest_conversion");
    public static final ResourceLocation QUARTZ_BASHING = ArcaneArchives.id("quartz_bashing");
    public static final ResourceLocation INVENTORY_RECHARGE = ArcaneArchives.id("inventory_recharge");
    public static final ResourceLocation NATURAL_RECHARGE = ArcaneArchives.id("natural_recharge");
    public static final ResourceLocation BLOCK_TRANSFORM = ArcaneArchives.id("block_transform");
    public static final ResourceLocation MUNCHSTONE_FOOD = ArcaneArchives.id("munchstone_food");
    public static final ResourceLocation BOTANIA_APOTHECARY = ArcaneArchives.id("arsenal/rivertear/botania_apothecary");
    public static final ResourceLocation ARSENAL_RECHARGE = ArcaneArchives.id("arsenal/recharge");
    public static final ResourceLocation ARSENAL_CAST = ArcaneArchives.id("arsenal/cast");

    private ArchiveStages() {}

    public static ResourceLocation placement(ResourceLocation block) {
        return ArcaneArchives.id("place/" + block.getPath());
    }

    public static List<ResourceLocation> features() {
        var features = new ArrayList<>(Arrays.stream(ArchiveFeature.values()).map(ArchiveFeature::id).toList());
        features.addAll(List.of(ARSENAL_CAST, ARSENAL_RECHARGE, BOTANIA_APOTHECARY));
        for (GemType gem : GemType.values()) {
            features.add(gemFeature(gem, "cast"));
            features.add(gemFeature(gem, "recharge"));
        }
        for (String device : List.of("radiant_chest", "radiant_trove", "radiant_tank", "brazier_of_hoarding",
                "gemcutters_table", "radiant_crafting_table", "radiant_resonator", "manifest_lectern", "monitoring_crystal")) {
            features.add(placement(ArcaneArchives.id(device)));
        }
        return features.stream().sorted().toList();
    }

    public static List<ResourceLocation> families() {
        return List.of(GEM_CUTTING, RADIANT_CRAFTING, RESONATOR_GROWTH, CHEST_CONVERSION,
                QUARTZ_BASHING, INVENTORY_RECHARGE, NATURAL_RECHARGE, BLOCK_TRANSFORM, MUNCHSTONE_FOOD);
    }

    public static boolean allowsFeature(Player actor, ArchiveFeature feature) {
        return allowsFeature(actor.level(), actor, feature.id());
    }

    public static boolean allowsFeature(Level level, @Nullable Player actor, ResourceLocation feature) {
        return allows(level, actor, FEATURE, feature);
    }

    public static boolean allowsFamily(Level level, @Nullable Player actor, ResourceLocation family) {
        return allows(level, actor, FAMILY, family);
    }

    public static boolean allowsRecipe(Level level, @Nullable Player actor, ResourceLocation family, ResourceLocation recipe) {
        return allowsFamily(level, actor, family) && allows(level, actor, RECIPE, recipe);
    }

    public static boolean allowsOwner(ServerLevel level, @Nullable UUID owner, ResourceLocation feature) {
        return allowsFeature(level, onlineOwner(level, owner), feature);
    }

    @Nullable
    public static Player onlineOwner(ServerLevel level, @Nullable UUID owner) {
        return owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
    }

    public static boolean allowsRadiantCrafting(Player player, CraftingRecipe recipe) {
        return allowsRecipe(player.level(), player, RADIANT_CRAFTING, recipe.getId())
                && (!ModList.get().isLoaded("recipestages") || RecipeStagesAccess.allows(recipe, player));
    }

    public static boolean allowsGemRecharge(Level level, @Nullable Player actor, GemType gem, ResourceLocation family,
                                            ResourceLocation recipe) {
        return allowsFeature(level, actor, ARSENAL_RECHARGE)
                && allowsFeature(level, actor, gemFeature(gem, "recharge"))
                && allowsRecipe(level, actor, family, recipe);
    }

    public static boolean allowsGemCast(Level level, @Nullable Player actor, GemType gem) {
        return allowsFeature(level, actor, ARSENAL_CAST)
                && allowsFeature(level, actor, gemFeature(gem, "cast"));
    }

    private static ResourceLocation gemFeature(GemType gem, String action) {
        return ArcaneArchives.id("arsenal/" + gem.id() + "/" + action);
    }

    private static boolean allows(Level level, @Nullable Player actor, StageRule.Scope scope, ResourceLocation target) {
        if (!(level.getRecipeManager().byKey(scope.key(target)).orElse(null) instanceof StageRule rule)) {
            return true;
        }
        return actor != null && !(actor instanceof FakePlayer) && ModList.get().isLoaded("gamestages")
                && GameStagesAccess.allows(actor, rule);
    }

    private static final class GameStagesAccess {
        private static boolean allows(Player actor, StageRule rule) {
            return rule.all().stream().allMatch(stage -> GameStageHelper.hasStage(actor, stage))
                    && (rule.any().isEmpty() || rule.any().stream().anyMatch(stage -> GameStageHelper.hasStage(actor, stage)));
        }
    }

    private static final class RecipeStagesAccess {
        private static boolean allows(CraftingRecipe recipe, Player player) {
            return RecipeStagesUtil.hasStageForRecipe(recipe, player);
        }
    }
}
