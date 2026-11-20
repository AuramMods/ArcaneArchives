package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.level.BlockEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

final class GemCasting {
    private static final Map<Block, Block> ORDERED = Map.ofEntries(
            Map.entry(Blocks.SAND, Blocks.COARSE_DIRT), Map.entry(Blocks.COARSE_DIRT, Blocks.DIRT),
            Map.entry(Blocks.DIRT, Blocks.MYCELIUM), Map.entry(Blocks.MYCELIUM, Blocks.PODZOL),
            Map.entry(Blocks.PODZOL, Blocks.GRASS_BLOCK), Map.entry(Blocks.GRAVEL, Blocks.COBBLESTONE),
            Map.entry(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE), Map.entry(Blocks.COBBLESTONE, Blocks.STONE),
            Map.entry(Blocks.MOSSY_STONE_BRICKS, Blocks.STONE_BRICKS), Map.entry(Blocks.CRACKED_STONE_BRICKS, Blocks.STONE_BRICKS),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS), Map.entry(Blocks.DAMAGED_ANVIL, Blocks.CHIPPED_ANVIL),
            Map.entry(Blocks.CHIPPED_ANVIL, Blocks.ANVIL));

    private GemCasting() {}

    static void cast(ServerPlayer player, GemType type, ArsenalGems.Source gem) {
        if (!GemCharge.available(gem.stack(), 1)) {
            GemRecharge.recharge(player, gem);
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
        var nearby = player.serverLevel().getEntitiesOfClass(AgeableMob.class, player.getBoundingBox().inflate(3.5),
                animal -> animal.getAge() != 0);
        int affected = 0;
        for (AgeableMob baby : nearby) {
            if (!GemCharge.available(gem.stack(), affected + 1)) {
                break;
            }
            int age = baby.getAge();
            baby.setAge(age < 0 ? Math.min(0, age + 8000) : Math.max(0, age - 8000));
            particles(player.serverLevel(), GemType.AGEGLEAM, baby.position().add(0, .45, 0));
            affected++;
        }
        if (affected > 0) {
            ArsenalGems.spend(player, gem, affected);
        }
    }

    private static void cleanse(ServerPlayer player, ArsenalGems.Source gem) {
        List<LivingEntity> targets = player.isShiftKeyDown()
                ? player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(3.5)) : List.of(player);
        int affected = 0;
        for (LivingEntity target : targets) {
            if (!GemCharge.available(gem.stack(), affected + 1)) {
                break;
            }
            boolean removed = target.removeEffect(MobEffects.HUNGER);
            removed |= target.removeEffect(MobEffects.CONFUSION);
            removed |= target.removeEffect(MobEffects.POISON);
            if (removed) {
                particles(player.serverLevel(), GemType.CLEANSEGLEAM, target.position().add(0, 1, 0));
                affected++;
            }
        }
        if (affected > 0) {
            ArsenalGems.spend(player, gem, affected);
        }
    }

    private static void swap(ServerPlayer player, ArsenalGems.Source gem) {
        var level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getLookAngle().scale(40));
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
        ArsenalGems.spend(player, gem, 1);
        player.getCooldowns().addCooldown(gem.stack().getItem(), 10);
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
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        if (!editable(player, target, hit.getDirection(), gem.stack())) {
            return;
        }
        BlockState state = level.getBlockState(target);
        boolean changed = switch (type) {
            case RIVERTEAR -> !level.dimensionType().ultraWarm() && state.canBeReplaced()
                    && change(player, target, Blocks.WATER.defaultBlockState(), hit.getDirection());
            case MOUNTAINTEAR -> state.canBeReplaced()
                    && change(player, target, Blocks.LAVA.defaultBlockState(), hit.getDirection());
            case PHOENIXWAY -> state.isAir() && change(player, target, FireBlock.getState(level, target), hit.getDirection());
            case STORMWAY -> level.canSeeSky(target) && lightning(level, Vec3.atBottomCenterOf(target), player, false);
            default -> false;
        };
        if (changed) {
            if (!player.isCreative() || (type != GemType.RIVERTEAR && type != GemType.MOUNTAINTEAR)) {
                ArsenalGems.spend(player, gem, 1);
            }
            trail(level, type, from, Vec3.atCenterOf(target));
        }
    }

    private static void parch(ServerPlayer player, ArsenalGems.Source gem, BlockHitResult hit) {
        int radius = player.isShiftKeyDown() ? 0 : 1;
        var level = player.serverLevel();
        var center = hit.getBlockPos();
        int affected = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (!GemCharge.available(gem.stack(), affected + 1)) {
                break;
            }
            if (editable(player, pos, hit.getDirection(), gem.stack()) && level.getBlockState(pos).getBlock() instanceof LiquidBlock
                    && change(player, pos, ArcaneArchives.FAKE_AIR.get().defaultBlockState(), hit.getDirection())) {
                affected++;
            }
        }
        if (affected > 0) {
            ArsenalGems.spend(player, gem, affected);
            trail(level, GemType.PARCHTEAR, player.getEyePosition(), Vec3.atCenterOf(center));
        }
    }

    static InteractionResult onBlock(UseOnContext context, GemType type) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var stack = context.getItemInHand();
        BlockState current = level.getBlockState(pos);
        Block changed = ORDERED.get(current.getBlock());
        int food = type == GemType.MUNCHSTONE ? foodValue(current) : 0;
        if (type == GemType.MUNCHSTONE ? food <= 0 : changed == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server) || !editable(server, pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        var gem = ArsenalGems.held(player, stack);
        if (!GemCharge.available(stack, 1)) {
            GemRecharge.recharge(player, gem);
            return InteractionResult.CONSUME;
        }
        if (type == GemType.ORDERSTONE) {
            int cost = current.is(BlockTags.ANVIL) ? 25 : current.is(Blocks.STONE_BRICKS) ? 4 : 1;
            var replacement = changed.defaultBlockState();
            if (current.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && replacement.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                replacement = replacement.setValue(BlockStateProperties.HORIZONTAL_FACING, current.getValue(BlockStateProperties.HORIZONTAL_FACING));
            }
            if (GemCharge.available(stack, cost) && change(server, pos, replacement, context.getClickedFace())) {
                ArsenalGems.spend(player, gem, cost);
                particles(server.serverLevel(), type, Vec3.atCenterOf(pos));
            }
        } else {
            var hunger = player.getFoodData();
            int foodGain = Math.min(food, 20 - hunger.getFoodLevel());
            float saturation = Math.min(food, hunger.getFoodLevel() + foodGain - hunger.getSaturationLevel());
            int cost = foodGain + (int) Math.ceil(Math.max(0, saturation));
            if (cost > 0 && GemCharge.available(stack, cost)
                    && !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(server.serverLevel(), pos, current, player))
                    && change(server, pos, Blocks.AIR.defaultBlockState(), context.getClickedFace())) {
                hunger.setFoodLevel(hunger.getFoodLevel() + foodGain);
                hunger.setSaturation(hunger.getSaturationLevel() + Math.max(0, saturation));
                ArsenalGems.spend(player, gem, cost);
                particles(server.serverLevel(), type, Vec3.atCenterOf(pos));
            }
        }
        return InteractionResult.CONSUME;
    }

    private static int foodValue(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        for (String entry : ArchiveConfig.MUNCHSTONE_FOODS.get()) {
            int comma = entry.indexOf(',');
            if (comma < 0) {
                continue;
            }
            String name = entry.substring(0, comma).strip();
            boolean matches = name.equals(id.toString()) || name.startsWith("#") && state.is(BlockTags.create(new ResourceLocation(name.substring(1))));
            if (matches) {
                return Integer.parseInt(entry.substring(comma + 1).strip());
            }
        }
        return 0;
    }

    private static boolean editable(ServerPlayer player, BlockPos target, Direction face, ItemStack stack) {
        var level = player.serverLevel();
        return level.isInWorldBounds(target) && level.hasChunkAt(target) && level.getWorldBorder().isWithinBounds(target)
                && level.mayInteract(player, target) && player.mayUseItemAt(target, face, stack);
    }

    private static boolean change(ServerPlayer player, BlockPos pos, BlockState target, Direction face) {
        var level = player.serverLevel();
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
