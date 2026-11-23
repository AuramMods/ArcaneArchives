package com.aranaira.arcanearchives.storage;

import com.aranaira.arcanearchives.integration.stages.ArchiveFeature;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class DevouringCharmItem extends Item {
    static final int FILTER_COUNT = 6;
    private static final String FILTERS = "autovoid_handler";
    private static final Map<ServerPlayer, Long> LAST_SOUND = new WeakHashMap<>();

    public DevouringCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ArchiveStages.allowsFeature(player, ArchiveFeature.CHARM_CONFIGURE)) {
            return InteractionResultHolder.fail(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((id, inventory, viewer) ->
                    new DevouringCharmMenu(id, inventory, slot), stack.getHoverName()), buffer -> buffer.writeVarInt(slot));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("arcanearchives.tooltip.item.devouring_charm").withStyle(ChatFormatting.GOLD));
    }

    static NonNullList<ItemStack> readFilters(ItemStack charm) {
        var items = NonNullList.withSize(FILTER_COUNT, ItemStack.EMPTY);
        CompoundTag filterTag = charm.getTagElement(FILTERS);
        if (filterTag != null) {
            ContainerHelper.loadAllItems(filterTag, items);
        }
        return items;
    }

    static void writeFilters(ItemStack charm, NonNullList<ItemStack> items) {
        if (items.stream().allMatch(ItemStack::isEmpty)) {
            charm.removeTagKey(FILTERS);
        } else {
            charm.addTagElement(FILTERS, ContainerHelper.saveAllItems(new CompoundTag(), items));
        }
    }

    private static boolean matches(ItemStack charm, ItemStack item) {
        return charm.is(ArcaneArchives.DEVOURING_CHARM.get()) && charm.getTagElement(FILTERS) != null
                && readFilters(charm).stream().anyMatch(sample -> ItemStack.isSameItemSameTags(sample, item));
    }

    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || !ArchiveStages.allowsFeature(player, ArchiveFeature.CHARM_CONSUME)) {
            return;
        }
        var entity = event.getItem();
        var item = entity.getItem();
        if (item.isEmpty() || !matches(player.getOffhandItem(), item)
                && player.getInventory().items.stream().noneMatch(charm -> matches(charm, item))) {
            return;
        }
        // Forge posts this event before vanilla checks the item's pickup target.
        CompoundTag pickup = new CompoundTag();
        entity.addAdditionalSaveData(pickup);
        if (pickup.hasUUID("Owner") && !pickup.getUUID("Owner").equals(player.getUUID())) {
            return;
        }
        event.setCanceled(true);
        entity.discard();
        long time = player.level().getGameTime();
        Long last = LAST_SOUND.get(player);
        if (last == null || time < last || time - last >= 10) {
            LAST_SOUND.put(player, time);
            player.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EAT,
                    SoundSource.PLAYERS, 0.4F, 0.7F + player.getRandom().nextFloat() * 0.6F);
        }
    }
}
