package com.aranaira.arcanearchives.arsenal;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

public final class GemSocketItem extends Item {
    public GemSocketItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!ArchiveConfig.ARSENAL_ENABLED.get()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer server) {
            open(server, new SlotRef(hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40, "", -1));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.bauble.gemsocket").withStyle(ChatFormatting.GOLD));
        ItemStack gem = gem(stack);
        if (!gem.isEmpty()) {
            tooltip.add(Component.translatable("gui.arcanearchives.socket.contains", gem.getHoverName(),
                    GemCharge.remaining(gem), GemCharge.capacity(gem)));
        }
    }

    static ItemStack gem(ItemStack socket) {
        CompoundTag data = socket.getTagElement("gem");
        if (data == null) {
            return ItemStack.EMPTY;
        }
        var items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(data, items);
        return items.get(0).getItem() instanceof ArcaneGemItem ? items.get(0) : ItemStack.EMPTY;
    }

    static void store(ItemStack socket, ItemStack gem) {
        if (gem.isEmpty()) {
            socket.removeTagKey("gem");
        } else {
            var items = NonNullList.withSize(1, ItemStack.EMPTY);
            items.set(0, gem.copyWithCount(1));
            CompoundTag tag = ContainerHelper.saveAllItems(new CompoundTag(), items);
            tag.putInt("Size", 1);
            socket.addTagElement("gem", tag);
        }
    }

    public static void openAvailable(ServerPlayer player) {
        if (!ArchiveConfig.ARSENAL_ENABLED.get() || player.isSpectator()) {
            return;
        }
        int selected = player.getInventory().selected;
        if (player.getMainHandItem().is(ArcaneArchives.GEM_SOCKET.get())) {
            open(player, new SlotRef(selected, "", -1));
            return;
        }
        var equipped = equipped(player);
        if (!equipped.isEmpty()) {
            open(player, equipped.get(0));
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ArcaneArchives.GEM_SOCKET.get())) {
                open(player, new SlotRef(i, "", -1));
                return;
            }
        }
    }

    private static void open(ServerPlayer player, SlotRef ref) {
        ItemStack stack = ref.stack(player);
        if (!player.isSpectator() && stack.is(ArcaneArchives.GEM_SOCKET.get())) {
            NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inventory, viewer) ->
                    new GemSocketMenu(id, inventory, ref), stack.getHoverName()), ref::write);
        }
    }

    static List<SlotRef> equipped(Player player) {
        return ModList.get().isLoaded("curios") ? CuriosSockets.equipped(player) : List.of();
    }

    record SlotRef(int inventorySlot, String curio, int curioIndex) {
        ItemStack stack(Player player) {
            if (inventorySlot >= 0) {
                return player.getInventory().getItem(inventorySlot);
            }
            return ModList.get().isLoaded("curios") ? CuriosSockets.stack(player, this) : ItemStack.EMPTY;
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(inventorySlot);
            buffer.writeUtf(curio, 64);
            buffer.writeVarInt(curioIndex);
        }

        static SlotRef read(FriendlyByteBuf buffer) {
            return new SlotRef(buffer.readVarInt(), buffer.readUtf(64), buffer.readVarInt());
        }
    }
}
