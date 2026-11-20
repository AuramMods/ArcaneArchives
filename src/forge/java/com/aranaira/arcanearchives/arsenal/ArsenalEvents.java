package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.integration.botania.BotaniaRivertear;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ArcaneArchives.ID)
public final class ArsenalEvents {
    private static final Map<LivingEntity, LootingBonus> DEATH_LOOTING = new WeakHashMap<>();

    private ArsenalEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onApothecaryUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator() && event.getUseBlock() != Event.Result.DENY
                && ArchiveConfig.ARSENAL_ENABLED.get() && ModList.get().isLoaded("botania")
                && player.mayInteract(player.serverLevel(), event.getPos())) {
            BotaniaRivertear.fill(player, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        if (event.getDamageSource() != null && event.getDamageSource().getEntity() instanceof ServerPlayer player) {
            LootingBonus deathBonus = DEATH_LOOTING.get(event.getEntity());
            int bonus = deathBonus != null && deathBonus.player().equals(player.getUUID()) ? deathBonus.amount()
                    : (int) ArsenalGems.available(player).stream().filter(gem -> gem.is(GemType.SLAUGHTERGLEAM)
                            && GemCharge.available(gem.stack(), 1)).count();
            event.setLootingLevel(event.getLootingLevel() + bonus);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int bonus = 0;
            for (var gem : ArsenalGems.available(player)) {
                if (gem.is(GemType.SLAUGHTERGLEAM) && GemCharge.available(gem.stack(), 1)) {
                    bonus++;
                    ArsenalGems.spend(player, gem, 1);
                } else if (gem.is(GemType.SALVEGLEAM) && event.getEntity() instanceof Animal) {
                    ArsenalGems.restore(gem, (int) event.getEntity().getMaxHealth() / 2);
                    if (GemCharge.remaining(gem.stack()) == GemCharge.capacity(gem.stack())) {
                        ArsenalGems.toggle(gem, true);
                    }
                }
            }
            // Forge can evaluate looting more than once after the gems have paid for this kill.
            DEATH_LOOTING.put(event.getEntity(), new LootingBonus(player.getUUID(), bonus));
        }
    }

    private record LootingBonus(UUID player, int amount) {}

    @SubscribeEvent
    public static void onCritical(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        for (var gem : ArsenalGems.available(player)) {
            if (!gem.is(GemType.MURDERGLEAM)) {
                continue;
            }
            var stack = gem.stack();
            if (GemCharge.toggled(stack) && GemCharge.available(stack, 1)) {
                event.setResult(Event.Result.ALLOW);
                event.setDamageModifier(Math.max(1.5F, event.getDamageModifier()));
                ArsenalGems.spend(player, gem, 1);
                if (!GemCharge.available(stack, 1)) {
                    ArsenalGems.toggle(gem, false);
                }
                break;
            }
            if (event.isVanillaCritical()) {
                ArsenalGems.restore(gem, 3);
                if (GemCharge.remaining(stack) == GemCharge.capacity(stack)) {
                    ArsenalGems.toggle(gem, true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        long time = player.serverLevel().getGameTime();
        for (var gem : ArsenalGems.available(player)) {
            var stack = gem.stack();
            if (gem.is(GemType.SALVEGLEAM) && player.getHealth() < player.getMaxHealth() && GemCharge.toggled(stack)
                    && GemCharge.available(stack, 1) && GemCharge.cooldown(stack, "last_pulse", time, 20)) {
                player.heal(1);
                ArsenalGems.spend(player, gem, 1);
                if (!GemCharge.available(stack, 1)) {
                    ArsenalGems.toggle(gem, false);
                }
            } else if (gem.is(GemType.STORMWAY) && GemCharge.remaining(stack) < GemCharge.capacity(stack)
                    && player.serverLevel().isRainingAt(player.blockPosition())) {
                ArsenalGems.restore(gem, -1);
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1, 1);
            } else if (gem.is(GemType.AGEGLEAM) && GemCharge.remaining(stack) < GemCharge.capacity(stack)) {
                int ticks = stack.getOrCreateTag().getInt("recharge");
                if (ticks <= 1 && stack.getTag().contains("recharge")) {
                    GemCharge.restore(stack, 1);
                    ticks = 3601;
                } else if (ticks <= 1) {
                    ticks = 3601;
                }
                stack.getTag().putInt("recharge", ticks - 1);
                gem.changed().run();
            }
        }
    }

    @SubscribeEvent
    public static void onExperience(PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int value = event.getOrb().value;
        boolean amplified = false;
        for (var gem : ArsenalGems.available(player)) {
            if (gem.is(GemType.ORDERSTONE)) {
                ArsenalGems.restore(gem, value);
            } else if (gem.is(GemType.MINDSPINDLE) && !amplified && GemCharge.available(gem.stack(), 1)) {
                int used = GemCharge.infinite(gem.stack()) ? value : Math.min(value, GemCharge.remaining(gem.stack()));
                event.getOrb().value = value + Math.round(used * .5F);
                ArsenalGems.spend(player, gem, used);
                amplified = true;
            }
        }
    }

    @SubscribeEvent
    public static void onEndermanTeleport(EntityTeleportEvent.EnderEntity event) {
        if (event.getEntityLiving() instanceof EnderMan enderman && !enderman.level().isClientSide) {
            boolean stopped = false;
            for (var player : enderman.level().getEntitiesOfClass(ServerPlayer.class, enderman.getBoundingBox().inflate(10.5))) {
                for (var gem : ArsenalGems.available(player)) {
                    if (gem.is(GemType.SWITCHGLEAM)) {
                        ArsenalGems.restore(gem, 3);
                        stopped = true;
                    }
                }
            }
            if (stopped) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isInvulnerableTo(event.getSource())) {
            return;
        }
        var level = player.serverLevel();
        for (var gem : ArsenalGems.available(player)) {
            var stack = gem.stack();
            if (gem.is(GemType.PHOENIXWAY) && event.getSource().is(DamageTypeTags.IS_FIRE)
                    && !player.hasEffect(MobEffects.FIRE_RESISTANCE) && GemCharge.available(stack, 12)) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600));
                ArsenalGems.spend(player, gem, 12);
                event.setCanceled(true);
                return;
            }
            if (gem.is(GemType.STORMWAY) && GemCharge.toggled(stack) && GemCharge.available(stack, 1)
                    && event.getSource().is(DamageTypeTags.IS_PROJECTILE) && event.getSource().getEntity() instanceof Mob attacker
                    && attacker.isAlive() && GemCasting.lightning(level, attacker.position().add(0, .5, 0), player, false)) {
                var effect = attacker.isInvertedHealAndHarm() ? MobEffects.HEAL : MobEffects.HARM;
                effect.applyInstantenousEffect(player, player, attacker, 10, 1);
                ArsenalGems.spend(player, gem, 1);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long time = player.serverLevel().getGameTime();
        for (var gem : ArsenalGems.available(player)) {
            if (gem.is(GemType.STORMWAY) && GemCharge.available(gem.stack(), 6)
                    && GemCharge.cooldown(gem.stack(), "last_strike", time, 20)) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 2));
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200));
                ArsenalGems.spend(player, gem, 6);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player) || !event.getItem().is(Items.POTION)) {
            return;
        }
        var gem = potionGem(player);
        if (gem == null) {
            return;
        }
        event.setCanceled(true);
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        var effects = PotionUtils.getMobEffects(event.getItem());
        if (effects.isEmpty() || effects.stream().anyMatch(effect -> !effect.getEffect().isInstantenous() && player.hasEffect(effect.getEffect()))) {
            return;
        }
        for (var effect : effects) {
            if (effect.getEffect().isInstantenous()) {
                effect.getEffect().applyInstantenousEffect(server, server, server, effect.getAmplifier(), 1);
            } else {
                server.addEffect(new MobEffectInstance(effect));
            }
        }
        player.awardStat(Stats.ITEM_USED.get(Items.POTION));
        ArsenalGems.spend(player, gem, 1);
    }

    @SubscribeEvent
    public static void onThrowPotion(PlayerInteractEvent.RightClickItem event) {
        var potion = event.getItemStack();
        boolean lingering = potion.is(Items.LINGERING_POTION);
        if (!lingering && !potion.is(Items.SPLASH_POTION)) {
            return;
        }
        var player = event.getEntity();
        var gem = potionGem(player);
        if (gem == null) {
            return;
        }
        var level = event.getLevel();
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        event.setCanceled(true);
        if (player instanceof ServerPlayer server) {
            var thrown = new ThrownPotion(level, server);
            thrown.setItem(potion.copyWithCount(1));
            thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), -20F, .5F, 1F);
            if (level.addFreshEntity(thrown)) {
                var sound = lingering ? SoundEvents.LINGERING_POTION_THROW : SoundEvents.SPLASH_POTION_THROW;
                level.playSound(null, player.blockPosition(), sound, SoundSource.NEUTRAL, .5F, 1F);
                player.awardStat(Stats.ITEM_USED.get(potion.getItem()));
                ArsenalGems.spend(player, gem, 1);
            }
        }
    }

    private static ArsenalGems.Source potionGem(net.minecraft.world.entity.player.Player player) {
        return ArsenalGems.available(player).stream().filter(gem -> gem.is(GemType.ELIXIRSPINDLE)
                && GemCharge.available(gem.stack(), 1)).findFirst().orElse(null);
    }
}
