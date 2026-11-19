package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RadiantCraftingTableGameTests {
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);
    private static final ResourceLocation STICKS = new ResourceLocation("minecraft", "stick");

    @GameTest(template = "chest_fixture")
    public static void radiantCraftingGridSharedAndPersistent(GameTestHelper test) {
        RadiantCraftingTableBlockEntity table = placeTable(test);
        Player first = playerAtTable(test);
        Player second = playerAtTable(test);
        table.bindTo(first.getUUID());
        RadiantCraftingTableMenu firstMenu = open(table, first);
        RadiantCraftingTableMenu secondMenu = open(table, second);
        table.grid().setItem(2, new ItemStack(Items.OAK_PLANKS));
        table.grid().setItem(5, new ItemStack(Items.BIRCH_PLANKS));
        test.assertTrue(firstMenu.getSlot(0).getItem().is(Items.STICK) && secondMenu.getSlot(0).getItem().is(Items.STICK),
                "Both viewers should see the recipe from the shared grid");
        firstMenu.clicked(0, 0, ClickType.PICKUP, first);
        test.assertTrue(firstMenu.getCarried().is(Items.STICK) && firstMenu.getCarried().getCount() == 4,
                "The normal result slot should return the vanilla recipe");
        test.assertTrue(secondMenu.getSlot(0).getItem().isEmpty() && table.grid().isEmpty(),
                "The second viewer should see the result and consumed inputs clear immediately");
        secondMenu.clicked(0, 0, ClickType.PICKUP, second);
        test.assertTrue(secondMenu.getCarried().isEmpty(), "The second viewer cannot take a stale result");

        test.assertTrue(firstMenu.clickMenuButton(first, RadiantCraftingTableMenu.bookmarkButton(1, true)),
                "Shift-clicking an empty bookmark should save the last crafted recipe");
        test.assertTrue(STICKS.equals(table.bookmark(1)), "The bookmark should store the recipe ID");
        table.grid().setItem(8, new ItemStack(Items.QUARTZ, 5));
        firstMenu.removed(first);
        secondMenu.removed(second);
        var restored = new RadiantCraftingTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.load(table.saveWithoutMetadata());
        test.assertTrue(restored.grid().getItem(8).is(Items.QUARTZ) && restored.grid().getItem(8).getCount() == 5
                        && restored.grid().getItem(0).isEmpty() && STICKS.equals(restored.bookmark(1))
                        && first.getUUID().equals(restored.owner()),
                "Reload should preserve recipe IDs, owner, and the positions of leftover ingredients");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void radiantCraftingBookmarkReturnsBuckets(GameTestHelper test) {
        RadiantCraftingTableBlockEntity table = placeTable(test);
        Player player = playerAtTable(test);
        RadiantCraftingTableMenu menu = open(table, player);
        var grid = table.grid();
        for (int column = 0; column < 3; column++) {
            grid.setItem(column, new ItemStack(Items.MILK_BUCKET));
            grid.setItem(6 + column, new ItemStack(Items.WHEAT));
        }
        grid.setItem(3, new ItemStack(Items.SUGAR));
        grid.setItem(4, new ItemStack(Items.EGG));
        grid.setItem(5, new ItemStack(Items.SUGAR));
        test.assertTrue(menu.getSlot(0).getItem().is(Items.CAKE) && menu.clickMenuButton(player, 0),
                "Click an empty bookmark while the cake recipe is in the matrix");
        test.assertTrue(menu.bookmarkPreview(0).is(Items.CAKE) && menu.canUseBookmark(0),
                "The saved cake recipe should be immediately available");

        grid.setItem(0, ItemStack.EMPTY);
        grid.setItem(1, new ItemStack(Items.COBBLESTONE));
        player.getInventory().setItem(17, new ItemStack(Items.MILK_BUCKET));
        menu.broadcastChanges();
        test.assertTrue(!menu.canUseBookmark(0) && !menu.clickMenuButton(player, 0)
                        && grid.getItem(2).is(Items.MILK_BUCKET) && count(player, Items.MILK_BUCKET) == 1,
                "An incomplete saved recipe should leave both inventories untouched");

        player.getInventory().setItem(18, new ItemStack(Items.MILK_BUCKET));
        test.assertTrue(menu.clickMenuButton(player, 0), "Craft the saved cake recipe from both inventories");
        test.assertTrue(count(player, Items.CAKE) == 1 && count(player, Items.BUCKET) == 3
                        && count(player, Items.MILK_BUCKET) == 0 && grid.getItem(1).is(Items.COBBLESTONE)
                        && grid.getItem(2).isEmpty() && grid.getItem(6).isEmpty(),
                "Craft exactly one cake, return all buckets, and keep unrelated items");
        test.assertTrue(menu.clickMenuButton(player, RadiantCraftingTableMenu.bookmarkButton(0, true))
                        && table.bookmark(0) == null && menu.bookmarkPreview(0).isEmpty(),
                "Shift-click should clear an occupied bookmark");
        menu.removed(player);
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void radiantCraftingTableDropsItsGrid(GameTestHelper test) {
        RadiantCraftingTableBlockEntity table = placeTable(test);
        Player player = playerAtTable(test);
        RadiantCraftingTableMenu menu = open(table, player);
        table.grid().setItem(4, new ItemStack(Items.QUARTZ, 6));
        var level = test.getLevel();
        level.destroyBlock(table.getBlockPos(), true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(table.getBlockPos()).inflate(2));
        int quartz = drops.stream().filter(item -> item.getItem().is(Items.QUARTZ)).mapToInt(item -> item.getItem().getCount()).sum();
        int tables = drops.stream().filter(item -> item.getItem().is(ArcaneArchives.RADIANT_CRAFTING_TABLE_ITEM.get()))
                .mapToInt(item -> item.getItem().getCount()).sum();
        test.assertTrue(quartz == 6 && tables == 1 && !menu.stillValid(player),
                "Breaking the table should drop the grid exactly once and invalidate any open menus");
        menu.removed(player);
        test.succeed();
    }

    private static Player playerAtTable(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        Vec3 pos = Vec3.atCenterOf(test.absolutePos(POSITION.south()));
        player.setPos(pos.x, pos.y, pos.z);
        return player;
    }

    private static RadiantCraftingTableBlockEntity placeTable(GameTestHelper test) {
        test.setBlock(POSITION, ArcaneArchives.RADIANT_CRAFTING_TABLE.get());
        return (RadiantCraftingTableBlockEntity) test.getBlockEntity(POSITION);
    }

    private static RadiantCraftingTableMenu open(RadiantCraftingTableBlockEntity table, Player player) {
        return (RadiantCraftingTableMenu) table.createMenu(1, player.getInventory(), player);
    }

    private static int count(Player player, Item item) {
        return player.getInventory().items.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
