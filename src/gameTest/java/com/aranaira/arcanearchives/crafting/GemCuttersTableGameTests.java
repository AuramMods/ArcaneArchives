package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.storage.BrazierBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class GemCuttersTableGameTests {
    private static final BlockPos POSITION = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void placeAndBreakGemCuttersTable(GameTestHelper test) {
        Player player = playerAtTable(test);
        player.setYRot(0);
        BlockPos accessor = POSITION.west();
        test.setBlock(POSITION, Blocks.AIR);
        test.setBlock(accessor, Blocks.STONE);
        ItemStack tableItem = new ItemStack(ArcaneArchives.GEMCUTTERS_TABLE_ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tableItem);
        test.assertTrue(!placeTable(test, player).consumesAction() && tableItem.getCount() == 1
                        && test.getBlockState(accessor).is(Blocks.STONE),
                "An occupied accessor position must preserve the held table and existing block");

        test.setBlock(accessor, Blocks.AIR);
        InteractionResult placed = placeTable(test, player);
        test.assertTrue(placed.consumesAction() && tableItem.isEmpty(),
                "Expected item placement to succeed and consume the stack (result " + placed + ", count "
                        + tableItem.getCount() + ", block " + test.getBlockState(POSITION) + ")");
        GemCuttersTableBlockEntity table = (GemCuttersTableBlockEntity) test.getBlockEntity(POSITION);
        test.assertTrue(test.getBlockState(accessor).getValue(GemCuttersTableBlock.ACCESSOR)
                        && test.getLevel().getBlockEntity(test.absolutePos(accessor)) == null
                        && player.getUUID().equals(table.owner()),
                "Only the main half should open the shared drawer and register its owner");
        table.drawer().setStackInSlot(0, new ItemStack(Items.REDSTONE, 7));
        test.getLevel().destroyBlock(test.absolutePos(accessor), true);
        test.assertTrue(test.getBlockState(POSITION).isAir() && test.getBlockState(accessor).isAir(),
                "Breaking the accessor should remove both halves");
        AABB bounds = new AABB(test.absolutePos(POSITION), test.absolutePos(accessor)).inflate(2);
        List<ItemEntity> drops = test.getLevel().getEntitiesOfClass(ItemEntity.class, bounds);
        test.assertTrue(countDrops(drops, ArcaneArchives.GEMCUTTERS_TABLE_ITEM.get()) == 1
                        && countDrops(drops, Items.REDSTONE) == 7,
                "Breaking the accessor should drop one table and release the drawer contents once");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void craftFromDrawerAndPlayer(GameTestHelper test) {
        Player player = playerAtTable(test);
        test.setBlock(POSITION, ArcaneArchives.GEMCUTTERS_TABLE.get());
        GemCuttersTableBlockEntity table = (GemCuttersTableBlockEntity) test.getBlockEntity(POSITION);
        GemCuttersTableMenu menu = (GemCuttersTableMenu) table.createMenu(1, player.getInventory(), player);
        table.drawer().setStackInSlot(17, new ItemStack(ArcaneArchives.RAW_QUARTZ.get()));
        table.drawer().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 64));
        player.getInventory().setItem(7, new ItemStack(ArcaneArchives.RAW_QUARTZ.get()));

        test.assertTrue(menu.recipes().size() >= 10 && menu.selectRecipe(player, ArcaneArchives.id("shaped_quartz"))
                        && menu.getSlot(0).getItem().is(ArcaneArchives.SHAPED_QUARTZ.get()),
                "Choosing shaped quartz should preview its data-driven recipe from both inventories");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().is(ArcaneArchives.SHAPED_QUARTZ.get()) && menu.getCarried().getCount() == 1
                        && player.getInventory().getItem(7).isEmpty() && table.drawer().getStackInSlot(17).isEmpty()
                        && table.drawer().getStackInSlot(0).getCount() == 64,
                "Taking the output should consume exactly the two quartz from the drawer and inventory");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().getCount() == 1 && menu.getSlot(0).getItem().isEmpty(),
                "Taking the output again without materials must not produce another item");

        var restored = new GemCuttersTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.load(table.saveWithoutMetadata());
        test.assertTrue(restored.selectedRecipe().equals(ArcaneArchives.id("shaped_quartz"))
                        && restored.drawer().getStackInSlot(0).getCount() == 64
                        && restored.drawer().getSlots() == 18,
                "Save and load should retain the selected recipe and the full drawer");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void shiftCraftIntoConsumedSlot(GameTestHelper test) {
        Player player = playerAtTable(test);
        test.setBlock(POSITION, ArcaneArchives.GEMCUTTERS_TABLE.get());
        GemCuttersTableBlockEntity table = (GemCuttersTableBlockEntity) test.getBlockEntity(POSITION);
        GemCuttersTableMenu menu = (GemCuttersTableMenu) table.createMenu(1, player.getInventory(), player);
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }

        inventory.setItem(35, new ItemStack(ArcaneArchives.RAW_QUARTZ.get(), 2));
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        test.assertTrue(inventory.getItem(35).is(ArcaneArchives.RAW_QUARTZ.get()) && inventory.getItem(35).getCount() == 2,
                "A full inventory must preserve the quartz if the result cannot fit");
        inventory.setItem(35, new ItemStack(ArcaneArchives.RAW_QUARTZ.get()));
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        test.assertTrue(inventory.getItem(35).is(ArcaneArchives.RADIANT_DUST.get()) && inventory.getItem(35).getCount() == 2,
                "Shift crafting should use the inventory slot freed when it consumes the last quartz");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void allocateOverlappingIngredients(GameTestHelper test) {
        var recipe = new GemCuttingRecipe(ArcaneArchives.id("overlap_test"), 0,
                List.of(new GemCuttingRecipe.Requirement(Ingredient.of(Items.OAK_LOG, Items.BIRCH_LOG), 2),
                        new GemCuttingRecipe.Requirement(Ingredient.of(Items.OAK_LOG), 1)),
                new ItemStack(Items.STICK));
        var inputs = new SimpleContainer(new ItemStack(Items.OAK_LOG), new ItemStack(Items.BIRCH_LOG));
        test.assertTrue(!recipe.matches(inputs, test.getLevel()), "A shared match cannot satisfy two ingredient counts");
        inputs.setItem(1, new ItemStack(Items.BIRCH_LOG, 2));
        int[] allocation = recipe.allocate(inputs.getContainerSize(), inputs::getItem);
        test.assertTrue(allocation != null && allocation[0] == 1 && allocation[1] == 2,
                "A valid allocation must reserve oak for the more specific ingredient");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void brazierPrefersMatchingGemCutterDrawer(GameTestHelper test) {
        Player player = playerAtTable(test);
        test.setBlock(POSITION, ArcaneArchives.GEMCUTTERS_TABLE.get());
        test.setBlock(POSITION.east(), ArcaneArchives.BRAZIER.get());
        GemCuttersTableBlockEntity table = (GemCuttersTableBlockEntity) test.getBlockEntity(POSITION);
        BrazierBlockEntity brazier = (BrazierBlockEntity) test.getBlockEntity(POSITION.east());
        table.bindTo(player.getUUID());
        brazier.bindTo(player.getUUID());
        table.drawer().setStackInSlot(0, new ItemStack(Items.REDSTONE, 63));

        ItemStack offered = new ItemStack(Items.REDSTONE, 3);
        test.assertTrue(brazier.route(offered, true).getCount() == 2 && table.drawer().getStackInSlot(0).getCount() == 63,
                "Simulation should report only room in matching stacks and leave the drawer alone");
        test.assertTrue(brazier.route(offered, false).getCount() == 2 && table.drawer().getStackInSlot(0).getCount() == 64
                        && table.drawer().getStackInSlot(1).isEmpty(),
                "A routed item should only top up existing matches in the Gem Cutter drawer");
        test.succeed();
    }

    private static Player playerAtTable(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        Vec3 position = Vec3.atCenterOf(test.absolutePos(POSITION.south(2)));
        player.setPos(position.x, position.y, position.z);
        return player;
    }

    private static InteractionResult placeTable(GameTestHelper test, Player player) {
        test.setBlock(POSITION.below(), Blocks.STONE);
        BlockPos floor = test.absolutePos(POSITION.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    private static int countDrops(List<ItemEntity> drops, Item item) {
        return drops.stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }
}
