package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.BlockSelector;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class ResonatorGameTests {
    @GameTest(template = "chest_fixture")
    public static void growthAndHarvest(GameTestHelper test) {
        BlockPos pos = new BlockPos(1, 1, 1);
        test.setBlock(pos, ArcaneArchives.RADIANT_RESONATOR.get());
        RadiantResonatorBlockEntity resonator = (RadiantResonatorBlockEntity) test.getBlockEntity(pos);
        CompoundTag snapshot = resonator.saveWithoutMetadata();
        int required = ArchiveConfig.RESONATOR_TICKS.get();
        snapshot.putInt("current_tick", required - 1);
        resonator.load(snapshot);
        resonator.bindTo(UUID.randomUUID());
        resonator.advance(test.getLevel(), null, false);
        test.assertTrue(test.getBlockState(pos.above()).isAir(), "An offline owner should pause growth");
        test.assertTrue(!resonator.getUpdateTag().getBoolean("can_tick"), "An offline owner must not start the client loop");

        test.setBlock(pos.above(), Blocks.STONE);
        resonator.advance(test.getLevel(), null, true);
        test.assertTrue(resonator.saveWithoutMetadata().getInt("current_tick") == required - 1 && resonator.comparatorSignal() == 0,
                "An obstruction should pause growth and suppress the comparator signal");
        test.assertTrue(!resonator.isResonating(), "An obstruction must stop the loop");
        test.setBlock(pos.above(), Blocks.AIR);
        var replica = new RadiantResonatorBlockEntity(resonator.getBlockPos(), resonator.getBlockState());
        replica.setLevel(test.getLevel());
        replica.load(resonator.getUpdateTag());
        test.assertTrue(replica.isResonating(), "The update tag should enable the loop for clients when the owner is online");
        resonator.advance(test.getLevel(), null, true);
        test.assertTrue(test.getBlockState(pos.above()).is(ArcaneArchives.RAW_QUARTZ_CLUSTER.get()) && resonator.comparatorSignal() == 15,
                "The Resonator should grow a harvestable Raw Quartz Cluster");
        test.assertTrue(!replica.isResonating(), "A ready quartz cluster must stop the loop");

        BlockPos cluster = test.absolutePos(pos.above());
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        test.assertTrue(Block.getDrops(test.getBlockState(pos.above()), test.getLevel(), cluster, null, null, tool)
                .stream().anyMatch(stack -> stack.is(ArcaneArchives.RAW_QUARTZ.get())), "Normal harvest should yield Raw Radiant Quartz");
        tool.enchant(Enchantments.SILK_TOUCH, 1);
        test.assertTrue(Block.getDrops(test.getBlockState(pos.above()), test.getLevel(), cluster, null, null, tool)
                .stream().anyMatch(stack -> stack.is(ArcaneArchives.RAW_QUARTZ_CLUSTER_ITEM.get())), "Silk Touch should yield the cluster");
        test.assertTrue(test.getLevel().getRecipeManager().byKey(ArcaneArchives.id("radiant_chest1")).isPresent()
                        && test.getLevel().getRecipeManager().byKey(ArcaneArchives.id("radiant_chest2")).isPresent(),
                "Both released chest recipes should load");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void personalPlacementLimit(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        player.setUUID(UUID.randomUUID());
        int limit = ArchiveConfig.RESONATOR_LIMIT.get();
        for (int i = 0; i < limit; i++) {
            BlockPos existing = new BlockPos(0, 1 + i, 0);
            test.setBlock(existing, ArcaneArchives.RADIANT_RESONATOR.get());
            ((RadiantResonatorBlockEntity) test.getBlockEntity(existing)).bindTo(player.getUUID());
        }

        BlockPos target = test.absolutePos(new BlockPos(1, 1, 1));
        ItemStack stack = new ItemStack(ArcaneArchives.RADIANT_RESONATOR_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false);
        BlockPlaceContext placement = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit);
        RadiantResonatorItem item = (RadiantResonatorItem) stack.getItem();
        test.assertTrue(item.place(placement) == InteractionResult.FAIL, "A player at the limit must be unable to place a fourth Resonator");
        test.setBlock(new BlockPos(0, 1, 0), Blocks.AIR);
        test.assertTrue(item.place(placement).consumesAction(), "Breaking an owned Resonator should make room for a new one");
        test.assertTrue(ArchiveNetworks.get(test.getLevel().getServer())
                        .countPersonalDevices(player.getUUID(), ArcaneArchives.RADIANT_RESONATOR_ENTITY.getId()) == limit,
                "Placement should update the persistent personal device count");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void growthRuleChangesResetProgressAndUpdateComparator(GameTestHelper test) {
        BlockPos pos = new BlockPos(1, 2, 1);
        test.setBlock(pos, ArcaneArchives.RADIANT_RESONATOR.get());
        test.setBlock(pos.below(), Blocks.DIAMOND_BLOCK);
        RadiantResonatorBlockEntity resonator = (RadiantResonatorBlockEntity) test.getBlockEntity(pos);
        var level = test.getLevel();
        var recipes = level.getRecipeManager();
        var original = List.copyOf(recipes.getRecipes());
        var updated = new ArrayList<>(original);
        var id = ArcaneArchives.id("test/resonator_growth");
        var foundation = new BlockSelector(List.of(Blocks.DIAMOND_BLOCK), List.of(), List.of());
        var firstOutput = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        var secondOutput = firstOutput.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
        try {
            updated.add(new ResonatorGrowth(id, new ResonatorGrowth.Settings(-1, 3, false, foundation,
                    Optional.of(level.dimension().location()), 0, firstOutput)));
            recipes.replaceRecipes(updated);
            resonator.advance(level, null, false);
            resonator.advance(level, null, false);
            test.assertTrue(resonator.percentGrown() == 66 && resonator.comparatorSignal() == 10,
                    "The custom rule should accrue progress without an online owner");
            updated.set(updated.size() - 1, new ResonatorGrowth(id, new ResonatorGrowth.Settings(-1, 2, false,
                    foundation, Optional.empty(), 0, secondOutput)));
            recipes.replaceRecipes(updated);
            resonator.advance(level, null, false);
            test.assertTrue(test.getBlockState(pos.above()).isAir() && resonator.percentGrown() == 50,
                    "A changed rule should start from zero instead of spending progress earned for the previous output");
            resonator.advance(level, null, false);
            test.assertTrue(test.getBlockState(pos.above()) == secondOutput && resonator.comparatorSignal() == 15
                            && resonator.growthStatus() == RadiantResonatorBlockEntity.GrowthStatus.HARVESTABLE,
                    "The exact configured output state should be harvestable and reach the full comparator signal");
        } finally {
            recipes.replaceRecipes(original);
        }
        test.succeed();
    }
}
