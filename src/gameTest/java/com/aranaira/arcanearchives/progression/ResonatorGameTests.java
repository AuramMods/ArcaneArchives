package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
        resonator.advance(test.getLevel(), false);
        test.assertTrue(test.getBlockState(pos.above()).isAir(), "An offline owner should pause growth");

        test.setBlock(pos.above(), Blocks.STONE);
        resonator.advance(test.getLevel(), true);
        test.assertTrue(resonator.saveWithoutMetadata().getInt("current_tick") == required - 1 && resonator.comparatorSignal() == 0,
                "An obstruction should pause growth and suppress the comparator signal");
        test.setBlock(pos.above(), Blocks.AIR);
        resonator.advance(test.getLevel(), true);
        test.assertTrue(test.getBlockState(pos.above()).is(ArcaneArchives.RAW_QUARTZ_CLUSTER.get()) && resonator.comparatorSignal() == 15,
                "The Resonator should grow a harvestable Raw Quartz Cluster");

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
}
