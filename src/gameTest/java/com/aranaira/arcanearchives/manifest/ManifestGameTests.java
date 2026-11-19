package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableBlockEntity;
import com.aranaira.arcanearchives.monitoring.MonitoringCrystalBlock;
import com.aranaira.arcanearchives.monitoring.MonitoringCrystalBlockEntity;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class ManifestGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void manifestReportsNetworkContentsAndLivePermissions(GameTestHelper test) {
        UUID owner = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        var server = test.getLevel().getServer();
        ArchiveNetworks networks = ArchiveNetworks.get(server);
        test.assertTrue(networks.joinHive(owner, member), "Create a shared network");

        test.setBlock(CENTER, ArcaneArchives.RADIANT_CHEST.get());
        var chest = (RadiantChestBlockEntity) test.getBlockEntity(CENTER);
        chest.bindTo(owner);
        chest.setCustomName(Component.literal("Workshop"));
        chest.inventory().insertItem(0, new ItemStack(Items.COBBLESTONE, 256), false);
        chest.inventory().insertItem(1, new ItemStack(Items.COBBLESTONE, 17), false);
        chest.inventory().insertItem(2, new ItemStack(Items.COBBLESTONE, 2).setHoverName(Component.literal("Marked stones")), false);

        test.setBlock(CENTER.east(), ArcaneArchives.RADIANT_TROVE.get());
        var trove = (RadiantTroveBlockEntity) test.getBlockEntity(CENTER.east());
        trove.bindTo(member);
        trove.inventory().insertItem(0, new ItemStack(Items.COBBLESTONE, 16000), false);

        test.setBlock(CENTER.west(), ArcaneArchives.RADIANT_TANK.get());
        var tank = (RadiantTankBlockEntity) test.getBlockEntity(CENTER.west());
        tank.bindTo(member);
        tank.tank().fill(new FluidStack(Fluids.WATER, 6000), IFluidHandler.FluidAction.EXECUTE);

        test.setBlock(CENTER.north(), ArcaneArchives.GEMCUTTERS_TABLE.get());
        var gemCutter = (GemCuttersTableBlockEntity) test.getBlockEntity(CENTER.north());
        gemCutter.bindTo(owner);
        gemCutter.drawer().setStackInSlot(0, new ItemStack(Items.QUARTZ, 12));

        test.setBlock(CENTER.south(), ArcaneArchives.RADIANT_CRAFTING_TABLE.get());
        var craftTable = (RadiantCraftingTableBlockEntity) test.getBlockEntity(CENTER.south());
        craftTable.bindTo(owner);
        craftTable.grid().setItem(0, new ItemStack(Items.QUARTZ, 5));

        var all = ManifestContents.collect(server, owner, false);
        var stone = ManifestContents.Variant.of(new ItemStack(Items.COBBLESTONE));
        test.assertTrue(amount(all, stone) == 16273 && all.stream().anyMatch(
                        row -> row.label().getString().equals("Workshop") && row.amount() == 273 && row.variant().equals(stone)),
                "Counts must preserve oversized slots, collate the same source, and keep its custom name");
        var marked = ManifestContents.Variant.of(new ItemStack(Items.COBBLESTONE).setHoverName(Component.literal("Marked stones")));
        test.assertTrue(amount(all, marked) == 2, "The custom-tagged stack must remain its own variant");
        test.assertTrue(amount(all, ManifestContents.Variant.of(new FluidStack(Fluids.WATER, 1))) == 6000,
                "Count Tank contents in millibuckets");
        test.assertTrue(amount(all, ManifestContents.Variant.of(new ItemStack(Items.QUARTZ))) == 17,
                "Include the Gem Cutter drawer and the Radiant Crafting Table grid");

        var personal = ManifestContents.collect(server, owner, true);
        test.assertTrue(amount(personal, stone) == 273 && personal.stream().noneMatch(row -> row.variant().fluid())
                        && ManifestContents.collect(server, stranger, false).isEmpty(),
                "Personal scope and unrelated viewers must exclude hive storage");
        test.assertTrue(networks.expel(owner, member), "Revoke the member's network access");
        test.assertTrue(amount(ManifestContents.collect(server, owner, false), stone) == 273
                        && amount(ManifestContents.collect(server, member, false), stone) == 16000,
                "Changing hive membership immediately changes what each viewer can read");
        test.setBlock(CENTER, Blocks.AIR);
        test.assertTrue(amount(ManifestContents.collect(server, owner, false), stone) == 0,
                "Removed devices should disappear from a refreshed snapshot");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void manifestDeduplicatesMonitoredDoubleChest(GameTestHelper test) {
        UUID owner = UUID.randomUUID();
        BlockPos first = CENTER;
        var firstState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH)
                .setValue(ChestBlock.TYPE, ChestType.LEFT);
        BlockPos second = first.relative(ChestBlock.getConnectedDirection(firstState));
        test.setBlock(first, firstState);
        test.setBlock(second, firstState.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        ((ChestBlockEntity) test.getBlockEntity(first)).setItem(3, new ItemStack(Items.GOLD_INGOT, 7));
        ((ChestBlockEntity) test.getBlockEntity(second)).setItem(9, new ItemStack(Items.GOLD_INGOT, 8));
        for (BlockPos chest : List.of(first, second)) {
            test.setBlock(chest.above(), ArcaneArchives.MONITORING_CRYSTAL.get().defaultBlockState()
                    .setValue(MonitoringCrystalBlock.FACING, Direction.UP));
            ((MonitoringCrystalBlockEntity) test.getBlockEntity(chest.above())).bindTo(owner);
        }
        var rows = ManifestContents.collect(test.getLevel().getServer(), owner, false);
        test.assertTrue(rows.size() == 1 && amount(rows, ManifestContents.Variant.of(new ItemStack(Items.GOLD_INGOT))) == 15,
                "Crystals on both halves should display one row with the entire double chest counted once");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void manifestLecternPlacesAndDropsAsOneBlock(GameTestHelper test) {
        test.setBlock(CENTER.below(), Blocks.STONE);
        test.setBlock(CENTER, Blocks.AIR);
        test.setBlock(CENTER.above(), Blocks.STONE);
        Player player = test.makeMockSurvivalPlayer();
        ItemStack lectern = new ItemStack(ArcaneArchives.MANIFEST_LECTERN_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, lectern);
        BlockPos support = test.absolutePos(CENTER.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        test.assertTrue(!lectern.useOn(context).consumesAction() && lectern.getCount() == 1,
                "A blocked upper half should prevent placement and preserve the held item");

        test.setBlock(CENTER.above(), Blocks.AIR);
        test.assertTrue(lectern.useOn(context).consumesAction() && ManifestLecternBlock.isComplete(test.getLevel(), support.above()),
                "The block item should create a complete two-block Lectern");
        test.getLevel().destroyBlock(support.above(2), true);
        test.assertTrue(test.getBlockState(CENTER).isAir() && test.getBlockState(CENTER.above()).isAir(),
                "Breaking the upper half should remove both blocks");
        var drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(support).inflate(3));
        int count = drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(ArcaneArchives.MANIFEST_LECTERN_ITEM.get()))
                .mapToInt(ItemStack::getCount).sum();
        test.assertTrue(count == 1, "Breaking a complete Lectern should drop exactly one item");
        test.succeed();
    }

    private static long amount(List<ManifestContents.Row> rows, ManifestContents.Variant variant) {
        return rows.stream().filter(row -> row.variant().equals(variant)).mapToLong(ManifestContents.Row::amount).sum();
    }
}
