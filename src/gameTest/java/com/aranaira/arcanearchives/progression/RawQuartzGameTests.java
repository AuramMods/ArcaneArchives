package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RawQuartzGameTests {
    @GameTest(template = "chest_fixture")
    public static void convertDoubleChestWithoutDroppingContents(GameTestHelper test) {
        BlockPos left = new BlockPos(1, 1, 1);
        BlockState initial = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        BlockState leftState = initial.setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockPos right = left.relative(ChestBlock.getConnectedDirection(leftState));
        test.setBlock(left, leftState);
        test.setBlock(right, initial.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ChestBlockEntity main = (ChestBlockEntity) test.getBlockEntity(left);
        ChestBlockEntity other = (ChestBlockEntity) test.getBlockEntity(right);
        main.setCustomName(Component.literal("Archive chest"));
        main.setItem(3, new ItemStack(Items.COBBLESTONE, 64));
        other.setItem(22, new ItemStack(Items.COBBLESTONE, 61));

        Player player = test.makeMockSurvivalPlayer();
        player.setShiftKeyDown(true);
        ItemStack quartz = new ItemStack(ArcaneArchives.RAW_QUARTZ.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, quartz);
        BlockPos pos = test.absolutePos(left);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = quartz.getItem().onItemUseFirst(quartz, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        test.assertTrue(result.consumesAction() && quartz.getCount() == 1, "A successful conversion should consume one quartz");
        RadiantChestBlockEntity radiant = (RadiantChestBlockEntity) test.getBlockEntity(left);
        test.assertTrue(radiant.inventory().getStackInSlot(0).getCount() == 125 && other.isEmpty(),
                "Both halves should transfer into the new Radiant Chest");
        test.assertTrue(radiant.getDisplayName().getString().equals("Archive chest") && player.getUUID().equals(radiant.owner()),
                "The converted chest should retain its name and belong to the player");
        test.assertTrue(test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).isEmpty(),
                "No items should drop during a successful conversion");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void quartzShatterYield(GameTestHelper test) {
        RandomSource rng = RandomSource.create(29);
        int singles = 0;
        int clusters = 0;
        for (int i = 0; i < 100; i++) {
            ItemStack quartz = new ItemStack(ArcaneArchives.RAW_QUARTZ.get());
            int result = RawQuartzItem.rollSlivers(quartz, rng);
            if (quartz.isEmpty()) {
                clusters++;
                test.assertTrue(result >= ArchiveConfig.SLIVER_CLUSTER_MIN.get() && result <= ArchiveConfig.SLIVER_CLUSTER_MAX.get(),
                        "A shattered crystal should produce a cluster within the configured bounds");
            } else if (result == 1) {
                singles++;
            }
        }
        test.assertTrue(singles > 0 && clusters > 0, "The released defaults should provide both quartz-bashing outcomes");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void packingAndUnpackingQuartz(GameTestHelper test) {
        var recipes = test.getLevel().getRecipeManager();
        var registry = test.getLevel().registryAccess();
        var rawBlock = recipes.byKey(ArcaneArchives.id("storage_raw_quartz")).orElseThrow(AssertionError::new);
        var shapedBlock = recipes.byKey(ArcaneArchives.id("storage_shaped_quartz")).orElseThrow(AssertionError::new);
        var rawQuartz = recipes.byKey(ArcaneArchives.id("destorage_rawquartz")).orElseThrow(AssertionError::new);
        var shapedQuartz = recipes.byKey(ArcaneArchives.id("destorage_shapedquartz")).orElseThrow(AssertionError::new);
        test.assertTrue(rawBlock.getIngredients().size() == 9 && rawBlock.getResultItem(registry).is(ArcaneArchives.STORAGE_RAW_QUARTZ_ITEM.get())
                        && shapedBlock.getIngredients().size() == 9 && shapedBlock.getResultItem(registry).is(ArcaneArchives.STORAGE_SHAPED_QUARTZ_ITEM.get()),
                "Both quartz forms should compress nine items into a block");
        test.assertTrue(rawQuartz.getResultItem(registry).is(ArcaneArchives.RAW_QUARTZ.get()) && rawQuartz.getResultItem(registry).getCount() == 9
                        && shapedQuartz.getResultItem(registry).is(ArcaneArchives.SHAPED_QUARTZ.get()) && shapedQuartz.getResultItem(registry).getCount() == 9,
                "Unpacking either quartz block should return nine matching items");
        test.succeed();
    }
}
