package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.botania.BotaniaRivertear;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.ModList;
import org.joml.Vector3f;

import java.util.List;

final class GemCasting {
    private GemCasting() {}

    static void cast(ServerPlayer player, GemType type, ArsenalGems.Source gem) {
        if (!GemCharge.available(gem.stack(), 1)) {
            GemRecharge.recharge(player, gem);
            return;
        }
        if (!ArchiveStages.allowsGemCast(player.level(), player, type)) {
            return;
        }
        switch (type) {
            case AGEGLEAM -> age(player, gem);
            case CLEANSEGLEAM -> cleanse(player, gem);
            case SWITCHGLEAM -> swap(player, gem);
            case MOUNTAINTEAR, RIVERTEAR, PHOENIXWAY, STORMWAY, PARCHTEAR -> castAtBlock(player, type, gem);
            default -> GemRecharge.recharge(player, gem);
        }
    }

    private static void age(ServerPlayer player, ArsenalGems.Source gem) {
        var tuning = ArsenalEffectRule.settings(player.level(), ArsenalEffectRule.Effect.AGEGLEAM);
        if (tuning.ticks() == 0) {
            return;
        }
        var nearby = player.serverLevel().getEntitiesOfClass(AgeableMob.class, player.getBoundingBox().inflate(tuning.amount()),
                animal -> animal.getAge() != 0);
        int chargeSpent = 0;
        for (AgeableMob baby : nearby) {
            if (!GemCharge.available(gem.stack(), chargeSpent + tuning.charge())) {
                break;
            }
            int age = baby.getAge();
            baby.setAge(age < 0 ? Math.min(0, age + tuning.ticks()) : Math.max(0, age - tuning.ticks()));
            particles(player.serverLevel(), GemType.AGEGLEAM, baby.position().add(0, .45, 0));
            chargeSpent += tuning.charge();
        }
        if (chargeSpent > 0) {
            ArsenalGems.spend(player, gem, chargeSpent);
        }
    }

    private static void cleanse(ServerPlayer player, ArsenalGems.Source gem) {
        var tuning = ArsenalEffectRule.settings(player.level(), ArsenalEffectRule.Effect.CLEANSEGLEAM);
        List<LivingEntity> targets = player.isShiftKeyDown()
                ? player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(tuning.amount())) : List.of(player);
        int chargeSpent = 0;
        for (LivingEntity target : targets) {
            if (!GemCharge.available(gem.stack(), chargeSpent + tuning.charge())) {
                break;
            }
            boolean removed = target.removeEffect(MobEffects.HUNGER);
            removed |= target.removeEffect(MobEffects.CONFUSION);
            removed |= target.removeEffect(MobEffects.POISON);
            if (removed) {
                particles(player.serverLevel(), GemType.CLEANSEGLEAM, target.position().add(0, 1, 0));
                chargeSpent += tuning.charge();
            }
        }
        if (chargeSpent > 0) {
            ArsenalGems.spend(player, gem, chargeSpent);
        }
    }

    private static void swap(ServerPlayer player, ArsenalGems.Source gem) {
        var level = player.serverLevel();
        var tuning = ArsenalEffectRule.settings(level, ArsenalEffectRule.Effect.SWITCHGLEAM);
        if (!GemCharge.available(gem.stack(), tuning.charge())) {
            return;
        }
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(tuning.amount()));
        Vec3 end = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(level, player, from, end, new AABB(from, end).inflate(1),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator());
        if (hit == null || hit.getEntity().isPassenger() || player.isPassenger()) {
            return;
        }
        Entity target = hit.getEntity();
        Vec3 first = player.position();
        Vec3 second = target.position();
        if (!level.getWorldBorder().isWithinBounds(target.blockPosition())
                || level.getBlockCollisions(player, player.getBoundingBox().move(second.subtract(first))).iterator().hasNext()
                || level.getBlockCollisions(target, target.getBoundingBox().move(first.subtract(second))).iterator().hasNext()) {
            return;
        }
        target.teleportTo(first.x, first.y, first.z);
        player.connection.teleport(second.x, second.y, second.z, player.getYRot() + 180, player.getXRot());
        ArsenalGems.spend(player, gem, tuning.charge());
        player.getCooldowns().addCooldown(gem.stack().getItem(), tuning.ticks());
        particles(level, GemType.SWITCHGLEAM, first.add(0, 1, 0));
        particles(level, GemType.SWITCHGLEAM, second.add(0, 1, 0));
    }

    private static void castAtBlock(ServerPlayer player, GemType type, ArsenalGems.Source gem) {
        var level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(type == GemType.STORMWAY ? 30 : 40));
        var hit = level.clip(new ClipContext(from, to, ClipContext.Block.OUTLINE,
                type == GemType.PARCHTEAR ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (type == GemType.PARCHTEAR) {
            parch(player, gem, hit);
            return;
        }
        if (type == GemType.RIVERTEAR && ModList.get().isLoaded("botania")
                && BotaniaRivertear.isApothecary(level.getBlockEntity(hit.getBlockPos()))) {
            return;
        }
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        if (!editable(player, target, hit.getDirection(), gem.stack())) {
            return;
        }
        if (type == GemType.STORMWAY) {
            if (level.canSeeSky(target) && lightning(level, Vec3.atBottomCenterOf(target), player, false)) {
                ArsenalGems.spend(player, gem, 1);
                trail(level, type, from, Vec3.atCenterOf(target));
            }
            return;
        }
        BlockState state = level.getBlockState(target);
        var rule = BlockTransformRule.find(level, type, state);
        if (rule == null || !allowsTransform(player, rule)) {
            return;
        }
        int cost = transformCost(player, rule);
        if (GemCharge.available(gem.stack(), cost) && change(player, target, rule.output(level, target, state), hit.getDirection())) {
            ArsenalGems.spend(player, gem, cost);
            trail(level, type, from, Vec3.atCenterOf(target));
        }
    }

    private static void parch(ServerPlayer player, ArsenalGems.Source gem, BlockHitResult hit) {
        int radius = player.isShiftKeyDown() ? 0 : 1;
        var level = player.serverLevel();
        var center = hit.getBlockPos();
        int spent = 0;
        boolean affected = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (!editable(player, pos, hit.getDirection(), gem.stack())) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            var rule = BlockTransformRule.find(level, GemType.PARCHTEAR, state);
            if (rule == null || !allowsTransform(player, rule)) {
                continue;
            }
            int cost = transformCost(player, rule);
            if (GemCharge.available(gem.stack(), spent + cost)
                    && change(player, pos, rule.output(level, pos, state), hit.getDirection())) {
                spent += cost;
                affected = true;
            }
        }
        if (affected) {
            ArsenalGems.spend(player, gem, spent);
            trail(level, GemType.PARCHTEAR, player.getEyePosition(), Vec3.atCenterOf(center));
        }
    }

    private static boolean allowsTransform(Player player, BlockTransformRule rule) {
        return ArchiveStages.allowsRecipe(player.level(), player, ArchiveStages.BLOCK_TRANSFORM, rule.id());
    }

    private static int transformCost(Player player, BlockTransformRule rule) {
        return player.isCreative() && rule.settings().creativeFree() ? 0 : rule.settings().charge();
    }

    static InteractionResult onBlock(UseOnContext context, GemType type) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var stack = context.getItemInHand();
        BlockState current = level.getBlockState(pos);
        var transform = type == GemType.ORDERSTONE ? BlockTransformRule.find(level, type, current) : null;
        var food = type == GemType.MUNCHSTONE ? munchstoneFood(level, current) : null;
        if (transform == null && food == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server) || !editable(server, pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!ArchiveStages.allowsGemCast(level, player, type)
                || (transform != null && !allowsTransform(player, transform))
                || (food != null && !ArchiveStages.allowsRecipe(level, player, ArchiveStages.MUNCHSTONE_FOOD, food.rule().id()))) {
            return InteractionResult.FAIL;
        }
        var gem = ArsenalGems.held(player, stack);
        if (!GemCharge.available(stack, 1)) {
            GemRecharge.recharge(player, gem);
            return InteractionResult.CONSUME;
        }
        if (transform != null) {
            int cost = transformCost(player, transform);
            var replacement = transform.output(level, pos, current);
            if (GemCharge.available(stack, cost) && change(server, pos, replacement, context.getClickedFace())) {
                ArsenalGems.spend(player, gem, cost);
                particles(server.serverLevel(), type, Vec3.atCenterOf(pos));
            }
        } else {
            var hunger = player.getFoodData();
            int foodGain = Math.min(food.nutrition().food(), 20 - hunger.getFoodLevel());
            float saturation = Math.max(0, Math.min(food.nutrition().saturation(),
                    hunger.getFoodLevel() + foodGain - hunger.getSaturationLevel()));
            int cost = food.rule().settings().fixedCost();
            if (cost == 0) {
                cost = foodGain + (int) Math.ceil(saturation);
            }
            if ((foodGain > 0 || saturation > 0) && GemCharge.available(stack, cost)
                    && !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server.serverLevel(), pos, current, player))
                    && change(server, pos, Blocks.AIR.defaultBlockState(), context.getClickedFace())) {
                hunger.setFoodLevel(hunger.getFoodLevel() + foodGain);
                hunger.setSaturation(hunger.getSaturationLevel() + saturation);
                ArsenalGems.spend(player, gem, cost);
                particles(server.serverLevel(), type, Vec3.atCenterOf(pos));
            }
        }
        return InteractionResult.CONSUME;
    }

    private static FoodMatch munchstoneFood(Level level, BlockState state) {
        return level.getRecipeManager().getAllRecipesFor(ArcaneArchives.MUNCHSTONE_FOOD_TYPE.get()).stream()
                .sorted(MunchstoneFoodRule.ORDER).map(rule -> new FoodMatch(rule, rule.forBlock(state)))
                .filter(match -> match.nutrition() != null).findFirst().orElse(null);
    }

    private record FoodMatch(MunchstoneFoodRule rule, MunchstoneFoodRule.Nutrition nutrition) {}

    private static boolean editable(ServerPlayer player, BlockPos target, Direction face, ItemStack stack) {
        var level = player.serverLevel();
        return level.isInWorldBounds(target) && level.hasChunkAt(target) && level.getWorldBorder().isWithinBounds(target)
                && level.mayInteract(player, target) && player.mayUseItemAt(target, face, stack);
    }

    private static boolean change(ServerPlayer player, BlockPos pos, BlockState target, Direction face) {
        var level = player.serverLevel();
        if (level.getBlockState(pos).hasBlockEntity()) {
            return false;
        }
        var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
        if (!level.setBlock(pos, target, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            return false;
        }
        if (ForgeEventFactory.onBlockPlace(player, snapshot, face)) {
            snapshot.restore(true, false);
            return false;
        }
        level.updateNeighborsAt(pos, target.getBlock());
        return true;
    }

    static boolean lightning(ServerLevel level, Vec3 pos, ServerPlayer caster, boolean visual) {
        var bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return false;
        }
        bolt.moveTo(pos);
        bolt.setCause(caster);
        bolt.setVisualOnly(visual);
        return level.addFreshEntity(bolt);
    }

    private static void trail(ServerLevel level, GemType type, Vec3 start, Vec3 end) {
        var dust = dust(type);
        int steps = Math.max(1, Math.min(32, (int) start.distanceTo(end)));
        for (int i = 0; i < steps; i++) {
            Vec3 point = start.lerp(end, (i + 1.0) / steps);
            level.sendParticles(dust, point.x, point.y, point.z, 1, .03, .03, .03, 0);
        }
    }

    private static void particles(ServerLevel level, GemType type, Vec3 point) {
        level.sendParticles(dust(type), point.x, point.y, point.z, 10, .25, .25, .25, 0);
    }

    private static DustParticleOptions dust(GemType type) {
        int rgb = type.color.rgb;
        return new DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F), 1.1F);
    }
}
