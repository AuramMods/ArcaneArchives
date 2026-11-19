package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.List;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RadiantTroveGameTests {
    private static final BlockPos TROVE_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void storageAndPortableLoot(GameTestHelper test) {
        RadiantTroveBlockEntity trove = placeTrove(test);
        IItemHandler handler = trove.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
                .orElseThrow(AssertionError::new);
        int capacity = RadiantTroveInventory.BASE_CAPACITY;
        ItemStack offered = new ItemStack(Items.COBBLESTONE, capacity + 20);
        offered.getOrCreateTag().putString("storage_test", "original");
        test.assertTrue(handler.insertItem(0, offered, true).getCount() == 20 && handler.getStackInSlot(0).isEmpty(),
                "Simulated insertion should report capacity without setting the item type");
        test.assertTrue(handler.insertItem(0, offered, false).getCount() == 20 && handler.getStackInSlot(0).getCount() == capacity,
                "The Trove should retain its full original capacity");
        ItemStack different = new ItemStack(Items.COBBLESTONE, 8);
        different.getOrCreateTag().putString("storage_test", "different");
        test.assertTrue(handler.insertItem(0, different, false).getCount() == 8,
                "The Trove should keep different tags separate");
        test.assertTrue(handler.extractItem(0, 512, false).getCount() == 64, "Automation should extract a normal stack");

        List<ItemStack> drops = Block.getDrops(trove.getBlockState(), test.getLevel(), trove.getBlockPos(), trove);
        test.assertTrue(drops.size() == 1 && drops.get(0).is(ArcaneArchives.RADIANT_TROVE_ITEM.get()),
                "Breaking a Trove should produce a portable Trove item");
        Player player = test.makeMockSurvivalPlayer();
        test.setBlock(TROVE_POS, Blocks.AIR);
        RadiantTroveBlockEntity moved = placeTrove(test);
        moved.getBlockState().getBlock().setPlacedBy(test.getLevel(), moved.getBlockPos(), moved.getBlockState(), player, drops.get(0));
        test.assertTrue(moved.inventory().count() == capacity - 64
                        && ItemStack.isSameItemSameTags(moved.inventory().reference(), offered)
                        && player.getUUID().equals(moved.owner()),
                "Replaced Trove should retain its item type, count, tags and new placement ownership");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void handDepositAndRepeatedClick(GameTestHelper test) {
        RadiantTroveBlockEntity trove = placeTrove(test);
        Player player = test.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SNOWBALL, 8));
        player.getInventory().setItem(9, new ItemStack(Items.SNOWBALL, 16));
        player.getInventory().setItem(10, new ItemStack(Items.SLIME_BALL, 5));
        BlockPos pos = trove.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);

        trove.getBlockState().use(test.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(trove.inventory().count() == 8 && player.getMainHandItem().isEmpty(), "The first use should deposit the held stack");
        trove.getBlockState().use(test.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(trove.inventory().count() == 24 && player.getInventory().getItem(9).isEmpty()
                        && player.getInventory().getItem(10).getCount() == 5,
                "A repeat use should deposit the other matching stacks");
        trove.dispense(player);
        test.assertTrue(trove.inventory().count() == 23 && player.getMainHandItem().getCount() == 1,
                "Standing extraction should dispense one item");
        test.succeed();
    }

    private static RadiantTroveBlockEntity placeTrove(GameTestHelper test) {
        test.setBlock(TROVE_POS, ArcaneArchives.RADIANT_TROVE.get());
        return (RadiantTroveBlockEntity) test.getBlockEntity(TROVE_POS);
    }
}
