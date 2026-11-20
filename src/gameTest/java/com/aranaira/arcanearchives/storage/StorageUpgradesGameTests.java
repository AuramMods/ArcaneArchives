package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class StorageUpgradesGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int PLAYER_HOTBAR_SLOT = 27;
    private static final int FIRST_UPGRADE_SLOT = 36;

    @GameTest(template = "chest_fixture")
    public static void troveOptionalUpgradesRetainLockAndVoidOverflow(GameTestHelper test) {
        test.setBlock(POS, ArcaneArchives.RADIANT_TROVE.get());
        var trove = (RadiantTroveBlockEntity) test.getBlockEntity(POS);
        Player player = adjacentPlayer(test);
        var menu = new StorageUpgradesMenu(1, player.getInventory(), trove);
        var key = new ItemStack(ArcaneArchives.RADIANT_KEY.get());
        var charm = new ItemStack(ArcaneArchives.DEVOURING_CHARM.get());
        var stone = new ItemStack(Items.COBBLESTONE);
        var gravel = new ItemStack(Items.GRAVEL);
        test.assertTrue(!offer(menu, player, key.copy()).isEmpty() && offer(menu, player, key).isEmpty()
                        && !offer(menu, player, charm).isEmpty(),
                "The Trove accepts a Radiant Key and a Devouring Charm and rejects duplicate options");
        trove.inventory().insertItem(0, stone, false);
        trove.inventory().extractItem(0, 1, false);
        test.assertTrue(trove.inventory().count() == 0 && !trove.inventory().isItemValid(0, gravel)
                        && trove.inventory().isItemValid(0, stone),
                "The Radiant Key preserves the item type after the Trove is emptied");

        ItemStack portable = Block.getDrops(trove.getBlockState(), test.getLevel(), trove.getBlockPos(), trove).get(0);
        var handler = portable.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        test.assertTrue(!handler.isItemValid(0, gravel) && handler.isItemValid(0, stone),
                "Both optional upgrades and the lock type survive becoming a portable item");
        int base = RadiantTroveInventory.BASE_CAPACITY;
        ItemStack overflow = stone.copyWithCount(base + 8);
        test.assertTrue(handler.insertItem(0, overflow, true).isEmpty() && handler.getStackInSlot(0).isEmpty(),
                "Portable voiding simulates accepting matching overflow without changing contents");
        test.assertTrue(handler.insertItem(0, overflow, false).isEmpty() && handler.getStackInSlot(0).getCount() == base
                        && overflow.getCount() == base + 8 && handler.insertItem(0, gravel, false).getCount() == 1,
                "The charm discards only matching overflow without changing the input stack object");
        test.assertTrue(!menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 3).isEmpty()
                        && trove.inventory().isItemValid(0, gravel),
                "Removing the key from an empty Trove releases the remembered item type");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void tankCharmVoidsMatchingFluidWhenFull(GameTestHelper test) {
        test.setBlock(POS, ArcaneArchives.RADIANT_TANK.get());
        var tank = (RadiantTankBlockEntity) test.getBlockEntity(POS);
        Player player = adjacentPlayer(test);
        var menu = new StorageUpgradesMenu(1, player.getInventory(), tank);
        test.assertTrue(offer(menu, player, new ItemStack(ArcaneArchives.RADIANT_KEY.get())).isEmpty()
                        && !offer(menu, player, new ItemStack(ArcaneArchives.DEVOURING_CHARM.get())).isEmpty(),
                "Only the Devouring Charm is active in a Tank");
        FluidStack water = new FluidStack(Fluids.WATER, 17000);
        FluidStack lava = new FluidStack(Fluids.LAVA, 1000);
        test.assertTrue(tank.tank().fill(water, FluidAction.EXECUTE) == 16000
                        && tank.tank().fill(lava, FluidAction.EXECUTE) == 0,
                "The Tank fills to its capacity and preserves its fluid type");
        test.assertTrue(tank.tank().fill(water, FluidAction.SIMULATE) == 17000
                        && tank.tank().fill(water, FluidAction.EXECUTE) == 17000
                        && tank.tank().getFluidAmount() == 16000 && water.getAmount() == 17000,
                "A full upgraded Tank reports consuming matching overflow without changing any stored fluid");
        ItemStack portable = Block.getDrops(tank.getBlockState(), test.getLevel(), tank.getBlockPos(), tank).get(0);
        test.assertTrue(StorageUpgrades.portableHasOption(portable, ArcaneArchives.DEVOURING_CHARM.get()),
                "The optional upgrade accompanies the portable Tank");
        test.assertTrue(!menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 3).isEmpty()
                        && tank.tank().fill(water, FluidAction.EXECUTE) == 0,
                "Removing the charm restores normal automation behavior");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void troveUpgradeOrderAndPortableCapability(GameTestHelper test) {
        test.setBlock(POS, ArcaneArchives.RADIANT_TROVE.get());
        var trove = (RadiantTroveBlockEntity) test.getBlockEntity(POS);
        Player player = adjacentPlayer(test);
        var menu = new StorageUpgradesMenu(1, player.getInventory(), trove);
        var brace = new ItemStack(ArcaneArchives.MATRIX_BRACE.get(), 2);
        brace.getOrCreateTag().putString("upgrade_test", "original");
        var material = new ItemStack(ArcaneArchives.MATERIAL_INTERFACE.get());
        var shaped = new ItemStack(ArcaneArchives.STORAGE_SHAPED_QUARTZ_ITEM.get());
        int base = RadiantTroveInventory.BASE_CAPACITY;

        test.assertTrue(offer(menu, player, material).isEmpty(), "The second tier requires the first upgrade");
        test.assertTrue(!offer(menu, player, brace).isEmpty() && player.getInventory().getItem(0).getCount() == 1
                        && trove.inventory().getSlotLimit(0) == 3 * base,
                "Shift-click installs exactly one Matrix Brace for triple capacity");
        test.assertTrue(!offer(menu, player, material).isEmpty() && trove.inventory().getSlotLimit(0) == 6 * base,
                "The Material Interface unlocks six times base capacity");
        test.assertTrue(!offer(menu, player, shaped).isEmpty() && trove.inventory().getSlotLimit(0) == 10 * base,
                "The Shaped Quartz block unlocks ten times base capacity");
        test.assertTrue(menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 1).isEmpty(),
                "The second upgrade cannot be removed while the third is installed");
        test.assertTrue(trove.inventory().insertItem(0, new ItemStack(Items.COBBLESTONE, base * 7), false).isEmpty(),
                "The block capability inserts contents beyond the third-tier removal limit");
        test.assertTrue(!menu.getSlot(FIRST_UPGRADE_SLOT + 2).mayPickup(player)
                        && menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 2).isEmpty(),
                "Both click paths block a downgrade when the contents would not fit");

        var saved = trove.saveWithoutMetadata();
        var restored = new RadiantTroveBlockEntity(trove.getBlockPos(), trove.getBlockState());
        restored.load(saved);
        test.assertTrue(restored.inventory().getSlotLimit(0) == base * 10 && restored.inventory().count() == base * 7
                        && restored.upgrades().getStackInSlot(0).getTag().getString("upgrade_test").equals("original"),
                "Block save and reload preserve the upgrade components and oversized contents");

        ItemStack portable = Block.getDrops(trove.getBlockState(), test.getLevel(), trove.getBlockPos(), trove).get(0);
        var handler = portable.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new);
        test.assertTrue(handler.getSlotLimit(0) == base * 10 && handler.getStackInSlot(0).getCount() == base * 7,
                "The dropped Trove exposes its installed capacity and contents as an item handler");
        test.assertTrue(handler.extractItem(0, 64, true).getCount() == 64 && handler.getStackInSlot(0).getCount() == base * 7,
                "Simulating extraction from the item does not write NBT");
        test.assertTrue(handler.extractItem(0, 64, false).getCount() == 64,
                "Real extraction updates the portable item");
        ItemStack copied = portable.copy();
        test.assertTrue(copied.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(AssertionError::new)
                        .getStackInSlot(0).getCount() == base * 7 - 64,
                "Portable mutations survive stack copying");
        test.setBlock(POS, Blocks.AIR);
        test.setBlock(POS, ArcaneArchives.RADIANT_TROVE.get());
        var moved = (RadiantTroveBlockEntity) test.getBlockEntity(POS);
        moved.getBlockState().getBlock().setPlacedBy(test.getLevel(), moved.getBlockPos(), moved.getBlockState(), player, copied);
        test.assertTrue(moved.inventory().getSlotLimit(0) == base * 10 && moved.inventory().count() == base * 7 - 64,
                "Placing the portable Trove restores both the count and the installed upgrades");
        test.assertTrue(!menu.stillValid(player), "Replacing the block invalidates a previously opened menu");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void tankUpgradeRemovalAndPortableFill(GameTestHelper test) {
        test.setBlock(POS, ArcaneArchives.RADIANT_TANK.get());
        var tank = (RadiantTankBlockEntity) test.getBlockEntity(POS);
        Player player = adjacentPlayer(test);
        var menu = new StorageUpgradesMenu(1, player.getInventory(), tank);
        int base = RadiantTankBlockEntity.BASE_CAPACITY;

        offer(menu, player, new ItemStack(ArcaneArchives.MATRIX_BRACE.get()));
        test.assertTrue(offer(menu, player, new ItemStack(ArcaneArchives.MATERIAL_INTERFACE.get())).isEmpty(),
                "The Trove's second component is invalid in a Tank");
        offer(menu, player, new ItemStack(ArcaneArchives.CONTAINMENT_FIELD.get()));
        test.assertTrue(tank.tank().getCapacity() == base * 6, "The Containment Field unlocks six times base capacity");
        FluidStack water = new FluidStack(Fluids.WATER, 60000);
        test.assertTrue(tank.tank().fill(water, FluidAction.EXECUTE) == 60000
                        && menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 1).isEmpty(),
                "A Tank holding more than 48,000 mB cannot be downgraded to that capacity");
        tank.tank().drain(12000, FluidAction.EXECUTE);
        test.assertTrue(!menu.quickMoveStack(player, FIRST_UPGRADE_SLOT + 1).isEmpty()
                        && tank.tank().getCapacity() == 48000 && tank.tank().getFluidAmount() == 48000,
                "Downgrading a Tank at the exact threshold retains all its fluid");
        ItemStack portable = Block.getDrops(tank.getBlockState(), test.getLevel(), tank.getBlockPos(), tank).get(0);
        var handler = portable.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(AssertionError::new);
        test.assertTrue(handler.getTankCapacity(0) == 48000 && handler.getFluidInTank(0).getAmount() == 48000,
                "Loot restores the Tank upgrade even when the item capability already exists");
        handler.drain(2000, FluidAction.EXECUTE);
        test.assertTrue(handler.fill(water, FluidAction.SIMULATE) == 2000 && handler.getFluidInTank(0).getAmount() == 46000,
                "The portable Tank uses the upgraded capacity for fluid simulations");
        test.assertTrue(handler.fill(water, FluidAction.EXECUTE) == 2000 && handler.getFluidInTank(0).getAmount() == 48000,
                "The portable Tank can fill back to the upgraded capacity");
        test.setBlock(POS, Blocks.AIR);
        test.setBlock(POS, ArcaneArchives.RADIANT_TANK.get());
        var moved = (RadiantTankBlockEntity) test.getBlockEntity(POS);
        moved.getBlockState().getBlock().setPlacedBy(test.getLevel(), moved.getBlockPos(), moved.getBlockState(), player, portable);
        test.assertTrue(moved.tank().getCapacity() == 48000 && moved.tank().getFluidAmount() == 48000,
                "Placing the Tank applies its upgrade before loading the fluid");
        test.succeed();
    }

    private static ItemStack offer(StorageUpgradesMenu menu, Player player, ItemStack stack) {
        player.getInventory().setItem(0, stack);
        return menu.quickMoveStack(player, PLAYER_HOTBAR_SLOT);
    }

    private static Player adjacentPlayer(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        Vec3 position = Vec3.atCenterOf(test.absolutePos(POS.south()));
        player.setPos(position.x, position.y, position.z);
        return player;
    }
}
