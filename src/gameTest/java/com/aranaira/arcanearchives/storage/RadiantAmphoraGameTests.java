package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RadiantAmphoraGameTests {
    private static final BlockPos TANK_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void bindingAndReplacement(GameTestHelper test) {
        RadiantTankBlockEntity tank = placeTank(test, TANK_POS);
        Player player = test.makeMockSurvivalPlayer();
        tank.bindTo(player.getUUID());
        ItemStack stack = new ItemStack(ArcaneArchives.RADIANT_AMPHORA.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(tank.getBlockPos()), Direction.NORTH, tank.getBlockPos(), false);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        InteractionResult result = stack.getItem().onItemUseFirst(stack, context);
        test.assertTrue(result.consumesAction() && RadiantAmphoraItem.linked(stack),
                "Crouch-use should link an Amphora to the player's Tank");

        IFluidHandlerItem remote = FluidUtil.getFluidHandler(stack).orElseThrow(AssertionError::new);
        test.assertTrue(remote.fill(new FluidStack(Fluids.WATER, 2500), IFluidHandler.FluidAction.EXECUTE) == 2500
                        && tank.tank().getFluidAmount() == 2500,
                "Fluid inserted into the Amphora should land in the real Tank");
        test.assertTrue(remote.drain(1000, IFluidHandler.FluidAction.SIMULATE).getAmount() == 1000
                        && tank.tank().getFluidAmount() == 2500,
                "Remote drain simulation should leave the contents untouched");
        RadiantAmphoraItem.toggleMode(stack);
        test.assertTrue(RadiantAmphoraItem.drainMode(stack), "Mode should switch to dispensing");

        test.setBlock(TANK_POS, Blocks.AIR);
        RadiantTankBlockEntity replacement = placeTank(test, TANK_POS);
        replacement.bindTo(player.getUUID());
        test.assertTrue(remote.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0
                        && replacement.tank().isEmpty(),
                "A stale Amphora should not transfer to a replacement Tank at the same position");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void dispenserReturnsFluidToWorld(GameTestHelper test) {
        RadiantTankBlockEntity tank = placeTank(test, new BlockPos(2, 1, 2));
        tank.bindTo(UUID.randomUUID());
        tank.tank().fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
        ItemStack stack = new ItemStack(ArcaneArchives.RADIANT_AMPHORA.get());
        RadiantAmphoraItem.link(stack, test.getLevel(), tank);

        BlockPos dispenserPos = new BlockPos(1, 1, 1);
        test.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = (DispenserBlockEntity) test.getBlockEntity(dispenserPos);
        dispenser.setItem(0, stack);
        test.setBlock(dispenserPos.west(), Blocks.REDSTONE_BLOCK);
        test.succeedWhen(() -> {
            test.assertTrue(test.getBlockState(dispenserPos.east()).getFluidState().is(Fluids.WATER),
                    "A dispenser should place a source of water from the linked Tank");
            test.assertTrue(tank.tank().getFluidAmount() == 1000 && dispenser.getItem(0).is(ArcaneArchives.RADIANT_AMPHORA.get())
                            && !RadiantAmphoraItem.drainMode(dispenser.getItem(0)),
                    "A successful dispense should consume one bucket and keep the same Amphora and mode");
        });
    }

    private static RadiantTankBlockEntity placeTank(GameTestHelper test, BlockPos pos) {
        test.setBlock(pos, ArcaneArchives.RADIANT_TANK.get());
        return (RadiantTankBlockEntity) test.getBlockEntity(pos);
    }
}
