package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.crafting.CountedIngredient;
import com.aranaira.arcanearchives.crafting.FluidSelector;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.aranaira.arcanearchives.integration.stages.StageRule;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);

    private ArsenalGameTests() {}

    @GameTest(template = "chest_fixture")
    public static void releasedRecipesAndGating(GameTestHelper test) {
        var manager = test.getLevel().getRecipeManager();
        var recipes = manager.getAllRecipesFor(ArcaneArchives.GEM_CUTTING_TYPE.get());
        var socket = (GemCuttingRecipe) manager.byKey(ArcaneArchives.id("gemsocket")).orElseThrow();
        var player = player(test);
        var gem = gem(GemType.MURDERGLEAM, 8);
        player.setItemInHand(InteractionHand.MAIN_HAND, gem);
        withArsenal(false, () -> {
            test.assertTrue(!socket.enabled() && ArsenalGems.available(player).isEmpty(), "Disabled Arsenal suppresses recipes and gem effects");
            ArsenalGems.toggleHeld(player);
            test.assertTrue(!GemCharge.toggled(gem), "Disabled Arsenal cannot toggle a held gem");
        });
        withArsenal(true, () -> {
            test.assertTrue(socket.enabled() && ArsenalGems.available(player).size() == 1, "Enabling the Arsenal exposes its recipes and gems");
            for (GemType type : GemType.values()) {
                test.assertTrue(recipes.stream().anyMatch(recipe -> recipe.getId().equals(ArcaneArchives.id(type.id()))
                                && recipe.enabled() && recipe.getResultItem(test.getLevel().registryAccess()).is(ArcaneArchives.GEMS.get(type).get())),
                        "Missing available recipe for " + type.id());
            }
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void socketPersistsAndRechargesMatchingPowder(GameTestHelper test) {
        withArsenal(true, () -> {
            var player = player(test);
            var inventory = player.getInventory();
            ItemStack socket = new ItemStack(ArcaneArchives.GEM_SOCKET.get());
            var ruby = gem(GemType.SALVEGLEAM, 2);
            ruby.getOrCreateTag().putByte("upgrades", (byte) 2);
            inventory.setItem(0, socket);
            inventory.setItem(9, ruby);
            var menu = new GemSocketMenu(1, inventory, new GemSocketItem.SlotRef(0, "", -1));
            player.containerMenu = menu;
            test.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && inventory.getItem(9).isEmpty(), "Shift-click inserts a gem into the socket");
            test.assertTrue(GemCharge.remaining(GemSocketItem.gem(socket.copy())) == 2, "A gem and remaining charge survive copying the socket");
            var match = ArcaneArchives.CHROMATIC_POWDER.get().of(GemType.Color.PINK);
            var other = ArcaneArchives.CHROMATIC_POWDER.get().of(GemType.Color.BLUE);
            var rainbow = new ItemStack(ArcaneArchives.FULL_SPECTRUM_POWDER.get());
            inventory.setItem(1, match);
            inventory.setItem(2, other);
            inventory.setItem(3, rainbow);
            test.assertTrue(menu.clickMenuButton(player, GemSocketMenu.RECHARGE) && match.isEmpty() && other.getCount() == 1
                    && rainbow.getCount() == 1 && GemCharge.remaining(GemSocketItem.gem(socket)) == 150,
                    "The matching color recharges the installed gem to its upgraded capacity before using universal powder");
            menu.clicked(37, 0, ClickType.SWAP, player);
            test.assertTrue(inventory.getItem(0) == socket && !GemSocketItem.gem(socket).isEmpty(), "The menu protects the active socket from hotbar swaps");
            test.assertTrue(!menu.quickMoveStack(player, 37).isEmpty() && player.getOffhandItem().is(ArcaneArchives.GEMS.get(GemType.SALVEGLEAM).get())
                    && GemSocketItem.gem(socket).isEmpty(), "Using shift-click to remove the gem prefers the offhand and clears the persisted slot");
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void customGemTagsChangeRechargeMatching(GameTestHelper test) {
        var level = test.getLevel();
        var player = player(test);
        var inventory = player.getInventory();
        var gem = gem(GemType.SALVEGLEAM, 0);
        var powder = ArcaneArchives.CHROMATIC_POWDER.get().of(GemType.Color.BLUE);
        inventory.setItem(0, gem);
        inventory.setItem(1, powder);
        var source = ArsenalGems.held(player, gem);
        var manager = level.getRecipeManager();
        var original = List.copyOf(manager.getRecipes());
        var recipes = new ArrayList<>(original);
        var tag = ArcaneArchives.id("test/recharge");
        var tagKey = ArchiveTag.Domain.CUT.key(tag);
        var selector = new GemSelector(List.of(), Optional.empty(), Optional.of(tag), Optional.empty());
        var input = new CountedIngredient(Ingredient.of(ArcaneArchives.CHROMATIC_POWDER.get()), 1);
        recipes.add(new InventoryRechargeRule(ArcaneArchives.id("test/recharge"), -10, selector, input, 1, 7,
                false, false, null, ItemStack.EMPTY, false));
        try {
            recipes.add(new ArchiveTag(tagKey, new ArchiveTag.Content(ArchiveTag.Domain.CUT, List.of("oval"))));
            manager.replaceRecipes(recipes);
            test.assertTrue(!GemRecharge.recharge(player, source) && powder.getCount() == 1 && GemCharge.remaining(gem) == 0,
                    "A custom rule must reject a gem whose cut is absent from its tag");
            recipes.set(recipes.size() - 1, new ArchiveTag(tagKey,
                    new ArchiveTag.Content(ArchiveTag.Domain.CUT, List.of("asscher"))));
            manager.replaceRecipes(recipes);
            test.assertTrue(GemRecharge.recharge(player, source) && powder.isEmpty() && GemCharge.remaining(gem) == 7,
                    "Changing tag membership must change the same rule without replacing it");
        } finally {
            manager.replaceRecipes(original);
        }
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void orderstoneUsesScriptedOutputCostAndStage(GameTestHelper test) {
        withArsenal(true, () -> {
            var level = test.getLevel();
            var player = player(test);
            var gem = gem(GemType.ORDERSTONE, 9);
            player.setItemInHand(InteractionHand.MAIN_HAND, gem);
            var pos = test.absolutePos(TARGET);
            test.setBlock(TARGET, Blocks.SAND);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
            var manager = level.getRecipeManager();
            var previous = List.copyOf(manager.getRecipes());
            var recipes = new ArrayList<>(previous);
            var id = ArcaneArchives.id("test/orderstone");
            var selector = new BlockSelector(List.of(Blocks.SAND), List.of(), List.of());
            var settings = new BlockTransformRule.Settings(GemType.ORDERSTONE, BlockTransformRule.Target.ANY, selector,
                    FluidSelector.ANY, Blocks.GLASS.defaultBlockState(), 3, -10, false, false, false, false);
            recipes.add(new BlockTransformRule(id, settings));
            var staged = new ArrayList<>(recipes);
            staged.add(new StageRule(StageRule.Scope.RECIPE.key(id), List.of("test_access"), List.of()));
            try {
                manager.replaceRecipes(staged);
                gem.getItem().onItemUseFirst(gem, context);
                test.assertTrue(test.getBlockState(TARGET).is(Blocks.SAND) && GemCharge.remaining(gem) == 9,
                        "A gated transformation leaves the block and gem untouched");
                manager.replaceRecipes(recipes);
                gem.getItem().onItemUseFirst(gem, context);
                test.assertTrue(test.getBlockState(TARGET).is(Blocks.GLASS) && GemCharge.remaining(gem) == 6,
                        "The custom rule changes both the resulting block and the charge cost");
            } finally {
                manager.replaceRecipes(previous);
            }
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void combatEffectsSpendAndRestoreCharge(GameTestHelper test) {
        withArsenal(true, () -> {
            var player = player(test);
            var level = test.getLevel();
            var victim = EntityType.COW.create(level);
            var slaughter = gem(GemType.SLAUGHTERGLEAM, 1);
            var salve = gem(GemType.SALVEGLEAM, 6);
            player.setItemInHand(InteractionHand.MAIN_HAND, slaughter);
            player.setItemInHand(InteractionHand.OFF_HAND, salve);
            var kill = level.damageSources().playerAttack(player);
            ArsenalEvents.onDeath(new LivingDeathEvent(victim, kill));
            var loot = new LootingLevelEvent(victim, kill, 2);
            ArsenalEvents.onLooting(loot);
            test.assertTrue(loot.getLootingLevel() == 3 && GemCharge.remaining(slaughter) == 0
                    && GemCharge.remaining(salve) == 11, "The last Slaughtergleam charge still increases looting and animal kills recharge Salvegleam");
            var nextLoot = new LootingLevelEvent(EntityType.COW.create(level), kill, 2);
            ArsenalEvents.onLooting(nextLoot);
            test.assertTrue(nextLoot.getLootingLevel() == 2, "A depleted Slaughtergleam gives no bonus on the next animal");
            GemCharge.toggle(salve, true);
            player.setHealth(12);
            ArsenalEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            ArsenalEvents.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            test.assertTrue(player.getHealth() == 13F && GemCharge.remaining(salve) == 10, "Salvegleam heals once per cooldown");

            var murder = gem(GemType.MURDERGLEAM, 9);
            player.setItemInHand(InteractionHand.MAIN_HAND, murder);
            var critical = new CriticalHitEvent(player, victim, 1.5F, true);
            ArsenalEvents.onCritical(critical);
            test.assertTrue(GemCharge.remaining(murder) == 12, "A vanilla critical hit restores charge while toggled off");
            ArsenalGems.toggleHeld(player);
            var normal = new CriticalHitEvent(player, victim, 1F, false);
            ArsenalEvents.onCritical(normal);
            test.assertTrue(normal.getResult() == Event.Result.ALLOW && normal.getDamageModifier() == 1.5F
                    && GemCharge.remaining(murder) == 11, "Murdergleam turns a normal hit into a critical hit at one charge");
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void experiencePotionAndLightningEffects(GameTestHelper test) {
        withArsenal(true, () -> {
            var player = player(test);
            var level = test.getLevel();
            var order = gem(GemType.ORDERSTONE, 60);
            var mind = gem(GemType.MINDSPINDLE, 50);
            player.setItemInHand(InteractionHand.MAIN_HAND, order);
            player.setItemInHand(InteractionHand.OFF_HAND, mind);
            var orb = new ExperienceOrb(level, player.getX(), player.getY(), player.getZ(), 12);
            ArsenalEvents.onExperience(new PlayerXpEvent.PickupXp(player, orb));
            test.assertTrue(orb.value == 18 && GemCharge.remaining(mind) == 38 && GemCharge.remaining(order) == 72,
                    "Orderstone recovers the orb's original value while Mindspindle amplifies it by 50 percent");

            var elixir = gem(GemType.ELIXIRSPINDLE, 4);
            var potion = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.SWIFTNESS);
            player.setItemInHand(InteractionHand.MAIN_HAND, potion);
            player.setItemInHand(InteractionHand.OFF_HAND, elixir);
            var drink = new LivingEntityUseItemEvent.Start(player, potion, 32);
            ArsenalEvents.onDrink(drink);
            test.assertTrue(drink.isCanceled() && player.hasEffect(MobEffects.MOVEMENT_SPEED)
                    && GemCharge.remaining(elixir) == 3 && potion.getCount() == 1,
                    "Elixirspindle applies a potion immediately without consuming the bottle");

            var phoenix = gem(GemType.PHOENIXWAY, 15);
            var storm = gem(GemType.STORMWAY, 18);
            player.setItemInHand(InteractionHand.MAIN_HAND, phoenix);
            player.setItemInHand(InteractionHand.OFF_HAND, storm);
            var fire = new LivingAttackEvent(player, level.damageSources().onFire(), 3);
            ArsenalEvents.onAttack(fire);
            test.assertTrue(fire.isCanceled() && player.hasEffect(MobEffects.FIRE_RESISTANCE) && GemCharge.remaining(phoenix) == 3,
                    "Phoenixway prevents the first fire hit and grants resistance for twelve charge");
            var lightning = new EntityStruckByLightningEvent(player, EntityType.LIGHTNING_BOLT.create(level));
            ArsenalEvents.onLightning(lightning);
            ArsenalEvents.onLightning(lightning);
            test.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION) && player.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                    && GemCharge.remaining(storm) == 12, "Stormway applies its lightning buffs once per cooldown");
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void orderFeedAgeAndCleanse(GameTestHelper test) {
        withArsenal(true, () -> {
            var player = player(test);
            var order = gem(GemType.ORDERSTONE, 99);
            test.setBlock(TARGET, Blocks.GRAVEL);
            useBlock(test, player, order);
            test.assertBlockPresent(Blocks.COBBLESTONE, TARGET);
            test.assertTrue(GemCharge.remaining(order) == 98, "A gravel conversion costs one charge");

            var munch = gem(GemType.MUNCHSTONE, 55);
            player.getFoodData().setFoodLevel(10);
            player.getFoodData().setSaturation(2);
            test.setBlock(TARGET, Blocks.OAK_LOG);
            useBlock(test, player, munch);
            test.assertBlockNotPresent(Blocks.OAK_LOG, TARGET);
            test.assertTrue(player.getFoodData().getFoodLevel() == 14 && player.getFoodData().getSaturationLevel() == 6
                    && GemCharge.remaining(munch) == 47, "Munchstone consumes a configured edible block and spends the restored hunger and saturation");

            var age = gem(GemType.AGEGLEAM, 20);
            var baby = test.spawn(EntityType.COW, TARGET);
            baby.setAge(-18000);
            GemCasting.cast(player, GemType.AGEGLEAM, ArsenalGems.held(player, age));
            test.assertTrue(baby.getAge() == -10000 && GemCharge.remaining(age) == 19, "Agegleam advances the age and spends one charge per animal");

            var cleanse = gem(GemType.CLEANSEGLEAM, 25);
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100));
            GemCasting.cast(player, GemType.CLEANSEGLEAM, ArsenalGems.held(player, cleanse));
            test.assertTrue(!player.hasEffect(MobEffects.POISON) && player.hasEffect(MobEffects.REGENERATION)
                    && GemCharge.remaining(cleanse) == 24, "Cleansegleam removes the detrimental effect while retaining beneficial effects");
        });
        test.succeed();
    }

    @GameTest(template = "chest_fixture", timeoutTicks = 250)
    public static void parchtearAirHoldsBackWaterAndExpires(GameTestHelper test) {
        withArsenal(true, () -> {
            var player = player(test);
            var target = test.absolutePos(TARGET);
            test.setBlock(TARGET, Blocks.WATER);
            test.setBlock(TARGET.west(), Blocks.WATER);
            player.setPos(target.getX() + .5, target.getY() - 1.12, target.getZ() + 1.6);
            player.setYRot(180);
            player.setXRot(0);
            player.setShiftKeyDown(true);
            var parch = gem(GemType.PARCHTEAR, 10);
            GemCasting.cast(player, GemType.PARCHTEAR, ArsenalGems.held(player, parch));
            test.assertBlockPresent(ArcaneArchives.FAKE_AIR.get(), TARGET);
            test.assertBlockPresent(Blocks.WATER, TARGET.west());
            test.assertTrue(GemCharge.remaining(parch) == 9, "Sneaking removes only the targeted fluid source");
        });
        test.runAtTickTime(100, () -> test.assertBlockPresent(ArcaneArchives.FAKE_AIR.get(), TARGET));
        test.runAtTickTime(210, () -> {
            test.assertBlockNotPresent(ArcaneArchives.FAKE_AIR.get(), TARGET);
            test.succeed();
        });
    }

    private static void useBlock(GameTestHelper test, ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = test.absolutePos(TARGET);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        stack.getItem().onItemUseFirst(stack, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static ItemStack gem(GemType type, int charge) {
        var stack = new ItemStack(ArcaneArchives.GEMS.get(type).get());
        stack.getOrCreateTag().putInt("charge", charge);
        return stack;
    }

    private static void withArsenal(boolean enabled, Runnable action) {
        boolean prior = ArchiveConfig.ARSENAL_ENABLED.get();
        ArchiveConfig.ARSENAL_ENABLED.set(enabled);
        try {
            action.run();
        } finally {
            ArchiveConfig.ARSENAL_ENABLED.set(prior);
        }
    }

    private static ServerPlayer player(GameTestHelper test) {
        var player = new ServerPlayer(test.getLevel().getServer(), test.getLevel(), new GameProfile(UUID.randomUUID(), "ArsenalQA")) {
            @Override
            public void displayClientMessage(Component message, boolean actionBar) {}

            @Override
            protected void onEffectAdded(MobEffectInstance effect, Entity source) {}

            @Override
            protected void onEffectUpdated(MobEffectInstance effect, boolean forced, Entity source) {}

            @Override
            protected void onEffectRemoved(MobEffectInstance effect) {}

            @Override
            public void awardStat(Stat<?> stat, int amount) {}
        };
        var center = Vec3.atCenterOf(test.absolutePos(TARGET.south()));
        player.setPos(center.x, center.y, center.z);
        return player;
    }
}
