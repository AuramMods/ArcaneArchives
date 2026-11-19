package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public final class RawQuartzItem extends Item {
    public RawQuartzItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (player == null || !player.isShiftKeyDown() || !(state.getBlock() instanceof ChestBlock chest)
                || !ArchiveConfig.CHEST_CONVERSION.get()) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        Container container = ChestBlock.getContainer(chest, state, serverLevel, pos, true);
        if (container == null) {
            return InteractionResult.FAIL;
        }
        Component name = serverLevel.getBlockEntity(pos) instanceof Nameable named ? named.getCustomName() : null;
        List<ItemStack> contents = new ArrayList<>(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            contents.add(container.getItem(slot).copy());
        }
        // Empty the old container so replacing it does not drop a second copy of its contents.
        container.clearContent();
        if (!serverLevel.setBlock(pos, ArcaneArchives.RADIANT_CHEST.get().defaultBlockState(), Block.UPDATE_ALL)) {
            for (int slot = 0; slot < contents.size(); slot++) {
                container.setItem(slot, contents.get(slot));
            }
            return InteractionResult.FAIL;
        }

        RadiantChestBlockEntity radiant = serverLevel.getBlockEntity(pos) instanceof RadiantChestBlockEntity placed ? placed : null;
        if (radiant != null) {
            radiant.bindTo(player.getUUID());
            radiant.setExistingItemsOnly(ArchiveConfig.CHESTS_EXISTING_ITEMS_ONLY.get());
            if (name != null) {
                radiant.setCustomName(name);
            }
        }
        for (ItemStack saved : contents) {
            if (!saved.isEmpty()) {
                ItemStack overflow = radiant == null ? saved : ItemHandlerHelper.insertItemStacked(radiant.inventory(), saved, false);
                if (!overflow.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, overflow.copy());
                }
            }
        }
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    public static void bashOnBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack raw = player.getMainHandItem();
        if (!(event.getLevel() instanceof ServerLevel level) || !raw.is(ArcaneArchives.RAW_QUARTZ.get())
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !level.mayInteract(player, event.getPos())) {
            return;
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (!hit.getBlockPos().equals(event.getPos())) {
            return;
        }
        RandomSource random = level.getRandom();
        int quantity = rollSlivers(raw, random);
        while (quantity > 0) {
            int count = Math.min(64, quantity);
            Vec3 point = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.01));
            ItemEntity shards = new ItemEntity(level, point.x, point.y, point.z, new ItemStack(ArcaneArchives.QUARTZ_SLIVER_ITEM.get(), count));
            shards.setDeltaMovement((random.nextFloat() - 0.5F) * 0.4F, random.nextFloat() * 0.2F + 0.2F,
                    (random.nextFloat() - 0.5F) * 0.4F);
            level.addFreshEntity(shards);
            quantity -= count;
        }
    }

    static int rollSlivers(ItemStack raw, RandomSource random) {
        int roll = random.nextInt(100);
        int cluster = ArchiveConfig.SLIVER_CLUSTER_CHANCE.get();
        if (roll < cluster) {
            int minimum = ArchiveConfig.SLIVER_CLUSTER_MIN.get();
            int maximum = Math.max(minimum, ArchiveConfig.SLIVER_CLUSTER_MAX.get());
            raw.shrink(1);
            return minimum + random.nextInt(maximum - minimum + 1);
        }
        return roll < cluster + ArchiveConfig.SLIVER_SINGLE_CHANCE.get() ? 1 : 0;
    }
}
