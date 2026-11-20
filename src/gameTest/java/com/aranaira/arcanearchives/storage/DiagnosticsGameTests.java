package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveNetworks;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder(ArcaneArchives.ID)
@PrefixGameTestTemplate(false)
public final class DiagnosticsGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "chest_fixture")
    public static void rebuildRepairsLoadedRegistrationsAndRetainsUnloadedLocations(GameTestHelper test) {
        var level = test.getLevel();
        var server = level.getServer();
        var networks = ArchiveNetworks.get(server);
        var owner = UUID.randomUUID();
        var other = UUID.randomUUID();
        var type = ArcaneArchives.RADIANT_CHEST_ENTITY.getId();
        test.setBlock(POS, ArcaneArchives.RADIANT_CHEST.get());
        var chest = (RadiantChestBlockEntity) test.getBlockEntity(POS);
        chest.bindTo(owner);
        var location = GlobalPos.of(level.dimension(), chest.getBlockPos());
        networks.removeDevice(location, chest.deviceId());
        var stale = GlobalPos.of(level.dimension(), chest.getBlockPos().above());
        var outside = GlobalPos.of(level.dimension(), chest.getBlockPos().offset(16000, 0, 0));
        var unrelated = GlobalPos.of(level.dimension(), chest.getBlockPos().below());
        networks.registerDevice(stale, UUID.randomUUID(), owner, type);
        networks.registerDevice(outside, UUID.randomUUID(), owner, type);
        networks.registerDevice(unrelated, UUID.randomUUID(), other, type);

        var result = networks.rebuildLoaded(server, owner);
        var indexed = networks.devicesOwnedBy(owner, true);
        test.assertTrue(result.refreshed() == 1 && result.staleRemoved() == 1 && indexed.size() == 2,
                "The rebuild restores the live registration and removes only the stale loaded entry");
        test.assertTrue(indexed.stream().anyMatch(device -> device.location().equals(location) && device.id().equals(chest.deviceId()))
                        && indexed.stream().anyMatch(device -> device.location().equals(outside))
                        && networks.devicesOwnedBy(other, true).size() == 1,
                "Identity, unloaded registrations and unrelated networks survive the rebuild");
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void brazierTraceUsesLivePrioritiesAndMarkerPackets(GameTestHelper test) {
        var player = player(test, new AtomicBoolean());
        test.setBlock(POS, ArcaneArchives.RADIANT_CHEST.get());
        var chest = (RadiantChestBlockEntity) test.getBlockEntity(POS);
        chest.bindTo(player.getUUID());
        chest.setExistingItemsOnly(true);
        chest.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 15));
        test.setBlock(POS.east(), ArcaneArchives.RADIANT_TROVE.get());
        var trove = (RadiantTroveBlockEntity) test.getBlockEntity(POS.east());
        trove.bindTo(player.getUUID());
        trove.inventory().insertItem(0, new ItemStack(Items.COBBLESTONE, 12), false);

        var routing = BrazierBlockEntity.trace(player, new ItemStack(Items.COBBLESTONE, 7));
        test.assertTrue(routing.size() == 2 && routing.get(0).device() == chest && routing.get(1).device() == trove
                        && routing.get(0).priority() > routing.get(1).priority(),
                "The trace ranks actual available destinations in their routing order");
        test.assertTrue(chest.inventory().getStackInSlot(0).getCount() == 15 && trove.inventory().count() == 12,
                "Creating a trace never moves the item");

        var markers = routing.stream().map(entry -> new ArchiveNetwork.DiagnosticMarker(
                GlobalPos.of(test.getLevel().dimension(), entry.device().getBlockPos()), entry.priority())).toList();
        var original = new ArchiveNetwork.MarkerBatch(true, markers);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            original.write(buffer);
            test.assertTrue(ArchiveNetwork.MarkerBatch.read(buffer).equals(original),
                    "Markers retain positions, dimensions and priorities over the network");
        } finally {
            buffer.release();
        }
        test.succeed();
    }

    @GameTest(template = "chest_fixture")
    public static void debugOrbFillsTroveOnlyInCreative(GameTestHelper test) {
        var creative = new AtomicBoolean();
        var player = player(test, creative);
        test.setBlock(POS, ArcaneArchives.RADIANT_TROVE.get());
        var trove = (RadiantTroveBlockEntity) test.getBlockEntity(POS);
        var orb = new ItemStack(ArcaneArchives.DEBUG_ORB.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, orb);
        player.setShiftKeyDown(true);
        var hit = new BlockHitResult(Vec3.atCenterOf(trove.getBlockPos()), Direction.SOUTH, trove.getBlockPos(), false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        orb.getItem().onItemUseFirst(orb, context);
        test.assertTrue(trove.inventory().count() == 0, "Survival players cannot create debug items");
        creative.set(true);
        orb.getItem().onItemUseFirst(orb, context);
        test.assertTrue(trove.inventory().count() == RadiantTroveInventory.BASE_CAPACITY
                        && trove.inventory().reference().is(Items.SNOWBALL), "Creative debug use fills the Trove with snowballs");
        orb.getItem().onItemUseFirst(orb, context);
        test.assertTrue(trove.inventory().count() == 0, "An additional creative use clears the debug contents");
        test.succeed();
    }

    private static ServerPlayer player(GameTestHelper test, AtomicBoolean creative) {
        var player = new ServerPlayer(test.getLevel().getServer(), test.getLevel(),
                new GameProfile(UUID.randomUUID(), "DiagnosticsGameTest")) {
            @Override
            public boolean isCreative() {
                return creative.get();
            }

            @Override
            public void displayClientMessage(Component message, boolean actionBar) {}
        };
        var location = Vec3.atCenterOf(test.absolutePos(POS.south()));
        player.setPos(location.x, location.y, location.z);
        return player;
    }
}
