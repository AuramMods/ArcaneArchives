package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class RadiantChestGameTests {
    private static final BlockPos CHEST_POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void automationAndPersistence(GameTestHelper test) {
        RadiantChestBlockEntity chest = placeChest(test);
        IItemHandler handler = chest.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        ItemStack offered = new ItemStack(Items.COBBLESTONE, 300);
        offered.getOrCreateTag().putString("test_marker", "same stack");
        test.assertTrue(handler.insertItem(0, offered, true).getCount() == 44, "Chest should simulate a 256-item insertion");
        test.assertTrue(handler.getStackInSlot(0).isEmpty(), "Simulation should leave the chest untouched");
        test.assertTrue(handler.insertItem(0, offered, false).getCount() == 44, "Chest should return the overflow");
        test.assertTrue(handler.getStackInSlot(0).getCount() == 256, "Chest should hold all 256 items");
        test.assertTrue(handler.extractItem(0, 256, true).getCount() == 64, "Automation should extract at most a normal stack");
        test.assertTrue(handler.getStackInSlot(0).getCount() == 256, "Simulated extraction should leave the stack untouched");

        BlockEntity loaded = BlockEntity.loadStatic(chest.getBlockPos(), chest.getBlockState(), chest.saveWithFullMetadata());
        test.assertTrue(loaded instanceof RadiantChestBlockEntity, "Saved registry ID should restore a Radiant Chest");
        ItemStack restored = ((RadiantChestBlockEntity) loaded).inventory().getStackInSlot(0);
        test.assertTrue(restored.getCount() == 256 && ItemStack.isSameItemSameTags(restored, offered),
                "Saved inventory should preserve the extended item count and tag");

        CompoundTag legacyInventory = new CompoundTag();
        ListTag legacyItems = new ListTag();
        CompoundTag legacyStack = new ItemStack(Items.COBBLESTONE).save(new CompoundTag());
        legacyStack.putByte("Count", (byte) 200);
        legacyStack.putShort("ExtendedCount", (short) 200);
        legacyStack.putInt("Slot", 3);
        legacyItems.add(legacyStack);
        legacyInventory.put("Items", legacyItems);
        ((RadiantChestBlockEntity) loaded).inventory().deserializeNBT(legacyInventory);
        test.assertTrue(((RadiantChestBlockEntity) loaded).inventory().getStackInSlot(3).getCount() == 200,
                "The extended count must take precedence over the signed vanilla byte");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void hopperInsertion(GameTestHelper test) {
        RadiantChestBlockEntity chest = placeChest(test);
        chest.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 255));
        BlockPos hopperPos = CHEST_POS.above();
        test.setBlock(hopperPos, Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) test.getBlockEntity(hopperPos);
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE));
        test.succeedWhen(() -> {
            test.assertTrue(hopper.getItem(0).isEmpty(), "Vanilla hopper should insert into the Radiant Chest");
            test.assertTrue(chest.inventory().getStackInSlot(0).getCount() == 256, "Hopper should fill the oversized stack");
        });
    }

    @GameTest(template = "chest_fixture")
    public static void menuTransfers(GameTestHelper test) {
        RadiantChestBlockEntity chest = placeChest(test);
        chest.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 256));
        Player player = test.makeMockSurvivalPlayer();
        player.setPos(chest.getBlockPos().getX() + 0.5, chest.getBlockPos().getY(), chest.getBlockPos().getZ() + 0.5);
        RadiantChestMenu menu = (RadiantChestMenu) chest.createMenu(1, player.getInventory(), player);

        test.assertTrue(menu.stillValid(player) && menu.slots.size() == 90, "Chest should open with 54 storage slots and 36 player slots");
        menu.clicked(0, 1, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().getCount() == 32 && chest.inventory().getStackInSlot(0).getCount() == 224,
                "Right-click should pick up half of a normal stack");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        test.assertTrue(menu.getCarried().isEmpty() && chest.inventory().getStackInSlot(0).getCount() == 256,
                "The menu should return the carried stack to the oversized slot");
        menu.clicked(0, 0, ClickType.SWAP, player);
        test.assertTrue(player.getInventory().getItem(0).getCount() == 64 && chest.inventory().getStackInSlot(0).getCount() == 192,
                "A hotbar swap into an empty slot should split off one normal stack");
        menu.clicked(81, 0, ClickType.QUICK_MOVE, player);
        test.assertTrue(player.getInventory().getItem(0).isEmpty() && chest.inventory().getStackInSlot(0).getCount() == 256,
                "Shift-click should merge into the existing oversized slot");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        int total = player.getInventory().items.stream().mapToInt(ItemStack::getCount).sum();
        test.assertTrue(total == 256 && chest.inventory().getStackInSlot(0).isEmpty()
                        && player.getInventory().items.stream().allMatch(stack -> stack.getCount() <= 64),
                "Shift-click out should return all items as normal stacks");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void menuPacketCounts(GameTestHelper test) {
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 256);
        stack.getOrCreateTag().putString("test_marker", "payload");
        NonNullList<ItemStack> slots = NonNullList.withSize(RadiantChestMenu.SLOT_COUNT, ItemStack.EMPTY);
        slots.set(0, stack);
        slots.set(53, new ItemStack(Items.SNOWBALL, 128));

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ArchiveNetwork.Contents(8, 2, slots, new ItemStack(Items.DIAMOND, 7)).write(buffer);
            ArchiveNetwork.Contents contents = ArchiveNetwork.Contents.read(buffer);
            ItemStack restored = contents.stacks().get(0);
            test.assertTrue(contents.menuId() == 8 && contents.stateId() == 2 && contents.stacks().size() == 90,
                    "The initial message should preserve slot indices and the menu state");
            test.assertTrue(restored.getCount() == 256 && ItemStack.isSameItemSameTags(restored, stack)
                            && contents.stacks().get(53).getCount() == 128 && contents.carried().getCount() == 7,
                    "Initial menu contents should round-trip oversized quantities, tags and the cursor");

            new ArchiveNetwork.SlotChange(8, 3, 9, stack).write(buffer);
            ArchiveNetwork.SlotChange delta = ArchiveNetwork.SlotChange.read(buffer);
            test.assertTrue(delta.menuId() == 8 && delta.stateId() == 3 && delta.slot() == 9 && delta.stack().getCount() == 256,
                    "Incremental updates should round-trip an oversized quantity");
        } finally {
            buffer.release();
        }
        test.succeed();
    }

    private static RadiantChestBlockEntity placeChest(GameTestHelper test) {
        test.setBlock(CHEST_POS, ArcaneArchives.RADIANT_CHEST.get());
        return (RadiantChestBlockEntity) test.getBlockEntity(CHEST_POS);
    }
}
