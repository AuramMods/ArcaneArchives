package com.aranaira.arcanearchives.manifest;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.client.ManifestClient;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public final class ManifestItem extends Item {
    public ManifestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ManifestClient::clearTracking);
            }
        } else if (player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, ContainerLevelAccess.NULL);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    static void open(ServerPlayer player, ContainerLevelAccess access) {
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new ManifestMenu(id, inventory, access),
                Component.translatable("item.arcanearchives.manifest")));
    }

    public static boolean hasManifest(Player player) {
        return player.getInventory().items.stream().anyMatch(stack -> stack.is(ArcaneArchives.MANIFEST.get()))
                || player.getOffhandItem().is(ArcaneArchives.MANIFEST.get());
    }

    static boolean mayUseKeybind(ServerPlayer player) {
        if (player.isSpectator()) {
            return false;
        }
        if (ArchiveConfig.REQUIRE_MANIFEST.get() && !hasManifest(player)) {
            player.displayClientMessage(Component.translatable("arcanearchives.gui.missing_manifest")
                    .withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.manifest").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("gui.arcanearchives.manifest.use", Component.keybind("arcanearchives.gui.keybinds.manifest"))
                .withStyle(ChatFormatting.GRAY));
    }
}
