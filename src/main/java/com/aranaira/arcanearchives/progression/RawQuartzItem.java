package com.aranaira.arcanearchives.progression;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class RawQuartzItem extends Item {
    public RawQuartzItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return convertChest(stack, context);
    }

    public static void convertOnBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || stack.getItem() instanceof RawQuartzItem || event.getUseItem() == Event.Result.DENY
                || event.getUseBlock() == Event.Result.DENY) {
            return;
        }
        var result = convertChest(stack, new UseOnContext(event.getEntity(), event.getHand(), event.getHitVec()));
        if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private static InteractionResult convertChest(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (player == null || !player.isShiftKeyDown() || !(state.getBlock() instanceof ChestBlock chest)
                || !ArchiveConfig.CHEST_CONVERSION.get()) {
            return InteractionResult.PASS;
        }
        ChestConversionRule rule = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.CHEST_CONVERSION_TYPE.get())
                .stream().filter(candidate -> candidate.trigger().matches(stack, state)).min(ChestConversionRule.ORDER).orElse(null);
        if (rule == null) {
            return InteractionResult.PASS;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            return InteractionResult.FAIL;
        }
        if (!ArchiveStages.allowsRecipe(level, player, ArchiveStages.CHEST_CONVERSION, rule.id())) {
            return InteractionResult.FAIL;
        }
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos partner = pos.relative(ChestBlock.getConnectedDirection(state));
            if (!level.mayInteract(player, partner) || !player.mayUseItemAt(partner, context.getClickedFace(), stack)) {
                return InteractionResult.FAIL;
            }
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
        if (!serverLevel.setBlock(pos, rule.output(), Block.UPDATE_ALL)) {
            for (int slot = 0; slot < contents.size(); slot++) {
                container.setItem(slot, contents.get(slot));
            }
            return InteractionResult.FAIL;
        }

        var placed = serverLevel.getBlockEntity(pos);
        var destination = switchDestination(placed, player, name);
        for (ItemStack saved : contents) {
            if (!saved.isEmpty()) {
                ItemStack overflow = destination == null ? saved : ItemHandlerHelper.insertItemStacked(destination, saved, false);
                if (!overflow.isEmpty()) {
                    Containers.dropItemStack(serverLevel, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, overflow.copy());
                }
            }
        }
        if (!player.isCreative()) {
            stack.shrink(rule.trigger().input().count());
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    private static IItemHandler switchDestination(@Nullable BlockEntity placed, Player player, @Nullable Component name) {
        if (placed instanceof RadiantChestBlockEntity radiant) {
            radiant.bindTo(player.getUUID());
            radiant.setExistingItemsOnly(ArchiveConfig.CHESTS_EXISTING_ITEMS_ONLY.get());
            if (name != null) {
                radiant.setCustomName(name);
            }
            return radiant.inventory();
        }
        if (placed instanceof ChestBlockEntity chestEntity) {
            if (name != null) {
                chestEntity.setCustomName(name);
            }
            return new InvWrapper(chestEntity);
        }
        return null;
    }

    public static void bashOnBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack raw = player.getMainHandItem();
        if (!(event.getLevel() instanceof ServerLevel level) || raw.isEmpty()
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || event.getUseItem() == Event.Result.DENY || event.getUseBlock() == Event.Result.DENY
                || !level.mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getFace(), raw)) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        QuartzBashingRule rule = level.getRecipeManager().getAllRecipesFor(ArcaneArchives.QUARTZ_BASHING_TYPE.get())
                .stream().filter(candidate -> candidate.trigger().matches(raw, state)).min(QuartzBashingRule.ORDER).orElse(null);
        if (rule == null || !ArchiveStages.allowsRecipe(level, player, ArchiveStages.QUARTZ_BASHING, rule.id())) {
            return;
        }
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (!hit.getBlockPos().equals(event.getPos())) {
            return;
        }
        RandomSource random = level.getRandom();
        ItemStack output = rule.roll(raw, random);
        int quantity = output.getCount();
        while (quantity > 0) {
            int count = Math.min(output.getMaxStackSize(), quantity);
            Vec3 point = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(0.01));
            ItemEntity shards = new ItemEntity(level, point.x, point.y, point.z, output.copyWithCount(count));
            shards.setDeltaMovement((random.nextFloat() - 0.5F) * 0.4F, random.nextFloat() * 0.2F + 0.2F,
                    (random.nextFloat() - 0.5F) * 0.4F);
            level.addFreshEntity(shards);
            quantity -= count;
        }
    }
}
