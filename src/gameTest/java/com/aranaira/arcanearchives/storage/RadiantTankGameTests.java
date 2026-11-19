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
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RadiantTankGameTests {
    private static final BlockPos TANK_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void fluidAutomationAndPortability(GameTestHelper test) {
        RadiantTankBlockEntity tank = placeTank(test);
        IFluidHandler handler = tank.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        FluidStack water = new FluidStack(Fluids.WATER, 17000);
        test.assertTrue(handler.fill(water, IFluidHandler.FluidAction.SIMULATE) == 16000 && tank.tank().isEmpty(),
                "Simulation should report the sixteen-bucket capacity without inserting fluid");
        test.assertTrue(handler.fill(water, IFluidHandler.FluidAction.EXECUTE) == 16000,
                "Tank should store sixteen buckets");
        test.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "Tank should reject a second kind of fluid");
        test.assertTrue(handler.drain(1234, IFluidHandler.FluidAction.EXECUTE).getAmount() == 1234,
                "Automation should be able to drain a partial bucket");

        List<ItemStack> drops = Block.getDrops(tank.getBlockState(), test.getLevel(), tank.getBlockPos(), tank);
        test.assertTrue(drops.size() == 1 && drops.get(0).is(ArcaneArchives.RADIANT_TANK_ITEM.get())
                        && FluidUtil.getFluidContained(drops.get(0)).orElseThrow(AssertionError::new).getAmount() == 14766,
                "The portable Tank must retain its fluid and expose the item fluid capability");
        test.setBlock(TANK_POS, Blocks.AIR);
        RadiantTankBlockEntity moved = placeTank(test);
        Player player = test.makeMockSurvivalPlayer();
        moved.getBlockState().getBlock().setPlacedBy(test.getLevel(), moved.getBlockPos(), moved.getBlockState(), player, drops.get(0));
        test.assertTrue(moved.tank().getFluidAmount() == 14766 && moved.tank().getFluid().getFluid() == Fluids.WATER,
                "Placing a filled Tank must restore the exact amount and fluid type");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void bucketInteraction(GameTestHelper test) {
        RadiantTankBlockEntity tank = placeTank(test);
        BlockPos pos = tank.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        Player player = test.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));

        tank.getBlockState().use(test.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(tank.tank().getFluidAmount() == 1000 && player.getMainHandItem().is(Items.BUCKET),
                "Using a filled bucket must pour the fluid into the Tank");
        tank.getBlockState().use(test.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(tank.tank().isEmpty() && player.getMainHandItem().is(Items.WATER_BUCKET),
                "Using an empty bucket must withdraw the fluid");
        test.succeed();
    }

    private static RadiantTankBlockEntity placeTank(GameTestHelper test) {
        test.setBlock(TANK_POS, ArcaneArchives.RADIANT_TANK.get());
        return (RadiantTankBlockEntity) test.getBlockEntity(TANK_POS);
    }
}
