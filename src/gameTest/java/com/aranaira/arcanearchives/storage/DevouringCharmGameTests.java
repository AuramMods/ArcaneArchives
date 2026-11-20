package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class DevouringCharmGameTests {
    @GameTest(template = "chest_fixture")
    public static void disposalFiltersAndSlotBinding(GameTestHelper test) {
        Player player = test.makeMockSurvivalPlayer();
        ItemStack charm = new ItemStack(ArcaneArchives.DEVOURING_CHARM.get());
        player.getInventory().setItem(0, charm);
        player.getInventory().setItem(9, new ItemStack(Items.WATER_BUCKET));
        player.getInventory().setItem(10, new ItemStack(Items.GRAVEL, 8));
        var menu = new DevouringCharmMenu(1, player.getInventory(), 0);
        int fluidSlot = DevouringCharmMenu.FLUID_SLOT;
        int discardSlot = DevouringCharmMenu.FIRST_DISCARD_SLOT;
        int filterSlot = DevouringCharmMenu.FIRST_FILTER_SLOT;
        test.assertTrue(menu.stillValid(player) && menu.quickMoveStack(player, 27).isEmpty(),
                "The item currently opening the menu stays in its player slot");
        test.assertTrue(!menu.quickMoveStack(player, 0).isEmpty() && menu.getSlot(fluidSlot).getItem().is(Items.BUCKET),
                "Shift-clicking a water bucket puts an empty bucket in the fluid slot");
        test.assertTrue(!menu.quickMoveStack(player, 1).isEmpty() && menu.getSlot(discardSlot).getItem().getCount() == 8,
                "Solid items stay in the disposal slot until the menu closes");
        menu.clicked(discardSlot, 0, ClickType.SWAP, player);
        test.assertTrue(player.getInventory().getItem(0) == charm && menu.getSlot(discardSlot).getItem().getCount() == 8,
                "Hotbar swapping cannot move the active charm into a disposal slot");
        test.assertTrue(!menu.quickMoveStack(player, fluidSlot).isEmpty(), "The empty bucket can be taken back out");

        test.assertTrue(menu.clickMenuButton(player, DevouringCharmMenu.FLIP) && menu.flipped(), "The back shows pickup filters");
        ItemStack sample = new ItemStack(Items.COBBLESTONE, 21);
        sample.getOrCreateTag().putBoolean("test_sample", true);
        menu.setCarried(sample);
        menu.clicked(filterSlot, 0, ClickType.PICKUP, player);
        var saved = DevouringCharmItem.readFilters(charm.copy()).get(0);
        test.assertTrue(ItemStack.isSameItemSameTags(saved, sample) && saved.getCount() == 1 && sample.getCount() == 21,
                "The filter copies an exact one-item sample and retains the carried stack");
        menu.clicked(filterSlot, 1, ClickType.PICKUP, player);
        test.assertTrue(DevouringCharmItem.readFilters(charm).get(0).isEmpty(), "Right-click clears a filter sample");
        menu.setCarried(ItemStack.EMPTY);
        menu.removed(player);
        test.assertTrue(menu.getSlot(discardSlot).getItem().isEmpty()
                        && player.getInventory().items.stream().noneMatch(stack -> stack.is(Items.GRAVEL)),
                "Closing the menu voids any items still in the disposal slots");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void pickupFilterRespectsTagsAndPickupOwner(GameTestHelper test) {
        ServerPlayer player = new ServerPlayer(test.getLevel().getServer(), test.getLevel(),
                new GameProfile(UUID.randomUUID(), "ArchiveCharmQA"));
        ItemStack charm = new ItemStack(ArcaneArchives.DEVOURING_CHARM.get());
        ItemStack sample = new ItemStack(Items.COBBLESTONE);
        sample.getOrCreateTag().putString("custom_filter", "first");
        var filters = DevouringCharmItem.readFilters(charm);
        filters.set(0, sample);
        DevouringCharmItem.writeFilters(charm, filters);
        player.getInventory().setItem(40, charm.copy());

        var differentlyTagged = pickup(test, player, new ItemStack(Items.COBBLESTONE));
        DevouringCharmItem.onItemPickup(differentlyTagged);
        test.assertTrue(!differentlyTagged.isCanceled() && !differentlyTagged.getItem().isRemoved(),
                "The filter leaves items with different tags available for vanilla pickup");
        var reserved = pickup(test, player, sample.copy());
        reserved.getItem().setTarget(UUID.randomUUID());
        DevouringCharmItem.onItemPickup(reserved);
        test.assertTrue(!reserved.isCanceled() && !reserved.getItem().isRemoved(),
                "The charm cannot destroy an item reserved for another player");
        var matching = pickup(test, player, sample.copyWithCount(23));
        matching.getItem().setTarget(player.getUUID());
        DevouringCharmItem.onItemPickup(matching);
        test.assertTrue(matching.isCanceled() && matching.getItem().isRemoved(),
                "An offhand charm voids a matching pickup without adding it to inventory");
        test.succeed();
    }

    private static EntityItemPickupEvent pickup(GameTestHelper test, Player player, ItemStack item) {
        Vec3 pos = Vec3.atCenterOf(test.absolutePos(net.minecraft.core.BlockPos.ZERO));
        return new EntityItemPickupEvent(player, new ItemEntity(test.getLevel(), pos.x, pos.y, pos.z, item));
    }
}
