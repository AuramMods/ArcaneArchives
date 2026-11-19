package com.aranaira.arcanearchives.monitoring;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class MonitoringGameTests {
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void monitoringCrystalFollowsAttachedInventory(GameTestHelper test) {
        test.setBlock(POSITION, Blocks.CHEST);
        test.setBlock(POSITION.above(), Blocks.AIR);
        ChestBlockEntity chest = (ChestBlockEntity) test.getBlockEntity(POSITION);
        chest.setItem(4, new ItemStack(Items.QUARTZ, 12));

        Player player = test.makeMockSurvivalPlayer();
        ItemStack stack = new ItemStack(ArcaneArchives.MONITORING_CRYSTAL_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos target = test.absolutePos(POSITION);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(target).add(0, 0.5, 0), Direction.UP, target, false);
        UseOnContext use = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(stack.onItemUseFirst(use) == InteractionResult.PASS && stack.useOn(use).consumesAction(),
                "A crystal should be placeable on a wooden chest");
        var crystal = (MonitoringCrystalBlockEntity) test.getBlockEntity(POSITION.above());
        ItemStack observed = crystal.targetInventory().orElseThrow(AssertionError::new).getStackInSlot(4);
        test.assertTrue(player.getUUID().equals(crystal.owner()) && crystal.targetPos().equals(target)
                        && observed.is(Items.QUARTZ) && observed.getCount() == 12,
                "The placed crystal should track the attached inventory and join the owner's network");

        test.setBlock(POSITION, ArcaneArchives.RADIANT_CHEST.get());
        ItemStack held = new ItemStack(ArcaneArchives.MONITORING_CRYSTAL_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        test.assertTrue(crystal.targetInventory().isEmpty() && held.onItemUseFirst(use) == InteractionResult.FAIL,
                "An Arcane Archives device must be excluded even if it replaces a previously monitored container");

        BlockPos westOfCrystal = POSITION.above().west();
        test.setBlock(westOfCrystal, Blocks.BARREL);
        BarrelBlockEntity barrel = (BarrelBlockEntity) test.getBlockEntity(westOfCrystal);
        barrel.setItem(0, new ItemStack(Items.REDSTONE, 5));
        test.setBlock(POSITION.above(), crystal.getBlockState().setValue(MonitoringCrystalBlock.FACING, Direction.EAST));
        var reoriented = (MonitoringCrystalBlockEntity) test.getBlockEntity(POSITION.above());
        test.assertTrue(reoriented.targetPos().equals(test.absolutePos(westOfCrystal))
                        && reoriented.targetInventory().orElseThrow(AssertionError::new).getStackInSlot(0).is(Items.REDSTONE),
                "Changing the facing should immediately follow the new target");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void scepterReportsLiveDeviceState(GameTestHelper test) {
        test.setBlock(POSITION, ArcaneArchives.RADIANT_CHEST.get());
        RadiantChestBlockEntity chest = (RadiantChestBlockEntity) test.getBlockEntity(POSITION);
        chest.setCustomName(Component.literal("Workshop"));
        chest.inventory().insertItem(4, new ItemStack(Items.REDSTONE, 8), false);
        var chestLines = RevelationScepterItem.inspect(chest);
        test.assertTrue(contents(chestLines.get(0)).getKey().endsWith("radiant_chest.name")
                        && contents(chestLines.get(1)).getArgs()[0].equals(53),
                "The Scepter should display the custom chest name and current free slots");

        test.setBlock(POSITION, ArcaneArchives.RADIANT_TANK.get());
        RadiantTankBlockEntity tank = (RadiantTankBlockEntity) test.getBlockEntity(POSITION);
        tank.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        var tankLines = RevelationScepterItem.inspect(tank);
        var volume = contents(tankLines.get(1)).getArgs();
        test.assertTrue(contents(tankLines.get(0)).getKey().endsWith("radiant_tank.item") && volume[0].equals(1000)
                        && volume[1].equals(tank.tank().getCapacity()),
                "The Scepter should display the current fluid and capacity");
        test.assertTrue(test.getLevel().getRecipeManager().byKey(ArcaneArchives.id("scepter_revelation")).isPresent()
                        && test.getLevel().getRecipeManager().byKey(ArcaneArchives.id("monitoring_crystal")).isPresent(),
                "Both original survival recipes should load");
        test.succeed();
    }

    private static TranslatableContents contents(Component text) {
        return (TranslatableContents) text.getContents();
    }
}
