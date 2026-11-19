package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.client.ArchiveClient;
import com.aranaira.arcanearchives.crafting.GemCuttersTableMenu;
import com.aranaira.arcanearchives.manifest.ManifestSync;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public final class ArchiveNetwork {
    private static final String PROTOCOL = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(ArcaneArchives.id("archive"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private ArchiveNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(Contents.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(Contents::write).decoder(Contents::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ArchiveClient.receiveChestContents(packet)))
                .add();
        CHANNEL.messageBuilder(SlotChange.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SlotChange::write).decoder(SlotChange::read)
                .consumerMainThread((packet, context) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> ArchiveClient.receiveChestSlot(packet)))
                .add();
        CHANNEL.messageBuilder(ToggleAmphora.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, data) -> {})
                .decoder(data -> new ToggleAmphora())
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.getMainHandItem().is(ArcaneArchives.RADIANT_AMPHORA.get())) {
                        RadiantAmphoraItem.toggleMode(player.getMainHandItem());
                    }
                })
                .add();
        CHANNEL.messageBuilder(SelectGemRecipe.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.menuId());
                    buffer.writeResourceLocation(packet.recipe());
                })
                .decoder(buffer -> new SelectGemRecipe(buffer.readVarInt(), buffer.readResourceLocation()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && !player.isSpectator() && player.containerMenu instanceof GemCuttersTableMenu table
                            && table.containerId == packet.menuId()) {
                        table.selectRecipe(player, packet.recipe());
                    }
                })
                .add();
        ManifestSync.register(CHANNEL);
        CHANNEL.messageBuilder(SetBrazierRadius.class, 6, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {
                    buffer.writeVarInt(packet.menuId());
                    buffer.writeVarInt(packet.radius());
                })
                .decoder(buffer -> new SetBrazierRadius(buffer.readVarInt(), buffer.readVarInt()))
                .consumerMainThread((packet, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null && player.containerMenu instanceof BrazierSettingsMenu brazier
                            && brazier.containerId == packet.menuId()) {
                        brazier.setRadius(player, packet.radius());
                    }
                })
                .add();
    }

    public static void sendAmphoraToggle() {
        CHANNEL.sendToServer(new ToggleAmphora());
    }

    private record ToggleAmphora() {}

    public static void setBrazierRadius(int menuId, int radius) {
        CHANNEL.sendToServer(new SetBrazierRadius(menuId, radius));
    }

    private record SetBrazierRadius(int menuId, int radius) {}

    public static void selectGemRecipe(int menuId, ResourceLocation recipe) {
        CHANNEL.sendToServer(new SelectGemRecipe(menuId, recipe));
    }

    private record SelectGemRecipe(int menuId, ResourceLocation recipe) {}

    static ContainerSynchronizer chestSynchronizer(ServerPlayer player, ContainerSynchronizer vanilla) {
        return new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> slots,
                                        ItemStack carried, int[] data) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new Contents(menu.containerId, menu.incrementStateId(), slots, carried));
                for (int index = 0; index < data.length; index++) {
                    sendDataChange(menu, index, data[index]);
                }
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu menu, int index, ItemStack stack) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new SlotChange(menu.containerId, menu.incrementStateId(), index, stack));
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
                vanilla.sendCarriedChange(menu, stack);
            }

            @Override
            public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
                vanilla.sendDataChange(menu, index, value);
            }
        };
    }

    public record Contents(int menuId, int stateId, List<ItemStack> stacks, ItemStack carried) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(stateId);
            buffer.writeVarInt(stacks.size());
            for (ItemStack stack : stacks) {
                ExtendedStacks.write(buffer, stack);
            }
            ExtendedStacks.write(buffer, carried);
        }

        static Contents read(FriendlyByteBuf buffer) {
            int menuId = buffer.readVarInt();
            int stateId = buffer.readVarInt();
            int size = buffer.readVarInt();
            if (size != RadiantChestMenu.SLOT_COUNT) {
                throw new IllegalArgumentException("Unexpected Radiant Chest slot count " + size);
            }
            List<ItemStack> stacks = new ArrayList<>(size);
            for (int slot = 0; slot < size; slot++) {
                stacks.add(ExtendedStacks.read(buffer));
            }
            return new Contents(menuId, stateId, stacks, ExtendedStacks.read(buffer));
        }
    }

    public record SlotChange(int menuId, int stateId, int slot, ItemStack stack) {
        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(menuId);
            buffer.writeVarInt(stateId);
            buffer.writeVarInt(slot);
            ExtendedStacks.write(buffer, stack);
        }

        static SlotChange read(FriendlyByteBuf buffer) {
            return new SlotChange(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    ExtendedStacks.read(buffer));
        }
    }
}
