package com.aranaira.arcanearchives.tome;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class TomeGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void tomeRecipeAndAdvancementsLoad(GameTestHelper test) {
        var level = test.getLevel();
        var recipe = level.getRecipeManager().byKey(ArcaneArchives.id("tome_arcana")).orElseThrow();
        ItemStack output = recipe.getResultItem(level.registryAccess());
        test.assertTrue(output.is(ArcaneArchives.TOME_OF_ARCANA.get()) && output.getCount() == 1
                        && output.getTag().getString("Book").equals(TomeOfArcanaItem.BOOK.toString()),
                "The recipe produces a Tome carrying its original book resource ID");
        var manager = level.getServer().getAdvancements();
        var book = manager.getAdvancement(ArcaneArchives.id("guidebook"));
        long count = manager.getAllAdvancements().stream().filter(advancement ->
                advancement.getId().getNamespace().equals(ArcaneArchives.ID)).count();
        test.assertTrue(count >= 24 && book != null && book.getParent() != null
                        && book.getParent().getId().equals(ArcaneArchives.id("root")),
                "The released advancement tree loads and includes the Tome under the root");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void tomeAcquisitionOccursOnceAcrossBothSources(GameTestHelper test) {
        var level = test.getLevel();
        var pos = test.absolutePos(POS);
        var player = nearbyPlayer(test, pos);
        var crafter = nearbyPlayer(test, pos);
        var alreadyCrafted = nearbyPlayer(test, pos);
        var bookshelf = Blocks.BOOKSHELF.defaultBlockState();
        var resonator = new ItemStack(ArcaneArchives.RADIANT_RESONATOR_ITEM.get());
        var craftingGrid = new SimpleContainer(9);

        MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, bookshelf, player));
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, resonator, craftingGrid));
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(crafter, resonator, craftingGrid));
        MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, bookshelf, crafter));
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(alreadyCrafted, TomeOfArcanaItem.tome(), craftingGrid));
        MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, bookshelf, alreadyCrafted));

        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1),
                entity -> entity.getItem().is(ArcaneArchives.TOME_OF_ARCANA.get()));
        test.assertTrue(drops.size() == 2, "A player's first eligible action offers one Tome and direct crafting counts as acquisition");
        test.assertTrue(drops.stream().allMatch(entity -> entity.getItem().getTag().getString("Book")
                        .equals(TomeOfArcanaItem.BOOK.toString())), "Both acquisition routes produce the same Tome");
        test.succeed();
    }

    private static ServerPlayer nearbyPlayer(GameTestHelper test, BlockPos pos) {
        var player = new ServerPlayer(test.getLevel().getServer(), test.getLevel(),
                new GameProfile(UUID.randomUUID(), "TomeGameTest")) {
            @Override
            public void sendSystemMessage(Component message) {}
        };
        var center = Vec3.atCenterOf(pos);
        player.setPos(center.x, center.y, center.z);
        return player;
    }
}
