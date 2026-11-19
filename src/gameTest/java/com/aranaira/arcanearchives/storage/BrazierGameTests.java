package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class BrazierGameTests {
    private static final BlockPos BRAZIER_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void priorityRangeAndPersonalMode(GameTestHelper test) {
        BrazierBlockEntity brazier = placeBrazier(test);
        UUID owner = brazier.owner();
        UUID member = UUID.randomUUID();
        ArchiveNetworks networks = ArchiveNetworks.get(test.getLevel().getServer());
        test.assertTrue(networks.joinHive(owner, member), "Owner should be able to form a hive");

        BlockPos chestPos = BRAZIER_POS.east();
        test.setBlock(chestPos, ArcaneArchives.RADIANT_CHEST.get());
        RadiantChestBlockEntity chest = (RadiantChestBlockEntity) test.getBlockEntity(chestPos);
        chest.bindTo(member);
        chest.setExistingItemsOnly(true);
        chest.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 30));

        BlockPos trovePos = BRAZIER_POS.west();
        test.setBlock(trovePos, ArcaneArchives.RADIANT_TROVE.get());
        RadiantTroveBlockEntity trove = (RadiantTroveBlockEntity) test.getBlockEntity(trovePos);
        trove.bindTo(owner);
        trove.inventory().insertItem(0, new ItemStack(Items.COBBLESTONE, 5), false);

        ItemStack incoming = new ItemStack(Items.COBBLESTONE, 20);
        IItemHandler ingress = brazier.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        test.assertTrue(ingress.insertItem(0, incoming, true).isEmpty() && chest.inventory().getStackInSlot(0).getCount() == 30,
                "Brazier simulation should leave all destinations untouched");
        test.assertTrue(ingress.insertItem(0, incoming, false).isEmpty() && chest.inventory().getStackInSlot(0).getCount() == 50
                        && trove.inventory().count() == 5,
                "A matching existing-items-only chest should outrank the Trove");
        test.assertTrue(ingress.insertItem(0, new ItemStack(Items.DIAMOND, 3), false).getCount() == 3,
                "A chest set to existing items must decline a new item");
        brazier.setRouting(150, true);
        test.assertTrue(ingress.insertItem(0, incoming, false).isEmpty() && trove.inventory().count() == 25,
                "Personal-only mode should restrict routing to the Brazier owner's devices");
        brazier.setRouting(0, false);
        test.assertTrue(ingress.insertItem(0, incoming, false).getCount() == 20,
                "A destination outside the horizontal range should be passed over");
        networks.leaveHive(member);
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void droppedContainersAndFluidAutomation(GameTestHelper test) {
        BrazierBlockEntity brazier = placeBrazier(test);
        BlockPos tankPos = BRAZIER_POS.east();
        test.setBlock(tankPos, ArcaneArchives.RADIANT_TANK.get());
        RadiantTankBlockEntity tank = (RadiantTankBlockEntity) test.getBlockEntity(tankPos);
        tank.bindTo(brazier.owner());

        IFluidHandler ingress = brazier.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        FluidStack water = new FluidStack(Fluids.WATER, 500);
        test.assertTrue(ingress.fill(water, IFluidHandler.FluidAction.SIMULATE) == 500 && tank.tank().isEmpty(),
                "Routing simulation must leave the Tank empty");
        test.assertTrue(ingress.fill(water, IFluidHandler.FluidAction.EXECUTE) == 500,
                "Piped fluid should reach the nearby Radiant Tank");

        BlockPos position = brazier.getBlockPos();
        ItemEntity bucket = new ItemEntity(test.getLevel(), position.getX() + 0.5, position.getY() + 0.8, position.getZ() + 0.5,
                new ItemStack(Items.WATER_BUCKET));
        brazier.getBlockState().entityInside(test.getLevel(), position, bucket);
        test.assertTrue(bucket.getItem().is(Items.BUCKET) && tank.tank().getFluidAmount() == 1500,
                "A dropped water bucket should route its water and return the bucket");
        test.assertTrue(ingress.drain(1000, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                "A Brazier is a deposit point");
        test.succeed();
    }

    private static BrazierBlockEntity placeBrazier(GameTestHelper test) {
        test.setBlock(BRAZIER_POS, ArcaneArchives.BRAZIER.get());
        BrazierBlockEntity brazier = (BrazierBlockEntity) test.getBlockEntity(BRAZIER_POS);
        brazier.bindTo(UUID.randomUUID());
        return brazier;
    }
}
