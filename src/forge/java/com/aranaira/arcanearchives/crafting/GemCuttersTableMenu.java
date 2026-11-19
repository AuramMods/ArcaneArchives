package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

import static com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity.DRAWER_SIZE;

public final class GemCuttersTableMenu extends AbstractContainerMenu {
    private static final int OUTPUT = 0;
    private static final int FIRST_DRAWER = 1;
    private static final int FIRST_PLAYER = FIRST_DRAWER + DRAWER_SIZE;
    private final Inventory inventory;
    private final ItemStackHandler drawer;
    private final SimpleContainer output = new SimpleContainer(1);
    private final DataSlot selection = DataSlot.standalone();
    private final DataSlot hiveRequirementsMet = DataSlot.standalone();
    @Nullable
    private final GemCuttersTableBlockEntity table;
    private List<GemCuttingRecipe> loadedRecipes = List.of();
    private List<GemCuttingRecipe> recipes = List.of();

    public GemCuttersTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, null, new ItemStackHandler(DRAWER_SIZE));
        buffer.readBlockPos();
    }

    public GemCuttersTableMenu(int id, Inventory inventory, GemCuttersTableBlockEntity table) {
        this(id, inventory, table, table.drawer());
        updateOutput();
    }

    private GemCuttersTableMenu(int id, Inventory inventory, @Nullable GemCuttersTableBlockEntity table, ItemStackHandler drawer) {
        super(ArcaneArchives.GEMCUTTERS_TABLE_MENU.get(), id);
        this.inventory = inventory;
        this.table = table;
        this.drawer = drawer;
        addDataSlot(selection);
        addDataSlot(hiveRequirementsMet);
        addSlot(new Slot(output, 0, 95, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        for (int slot = 0; slot < DRAWER_SIZE; slot++) {
            addSlot(new SlotItemHandler(drawer, slot, 23 + slot % 9 * 18, 105 + slot / 9 * 18) {
                @Override
                public void setChanged() {
                    super.setChanged();
                    if (table != null) {
                        table.setChanged();
                    }
                }
            });
        }
        for (int slot = 9; slot < Inventory.INVENTORY_SIZE; slot++) {
            addSlot(new Slot(inventory, slot, 23 + slot % 9 * 18, 166 + (slot / 9 - 1) * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 23 + slot * 18, 224));
        }
    }

    public List<GemCuttingRecipe> recipes() {
        List<GemCuttingRecipe> updated = inventory.player.level().getRecipeManager()
                .getAllRecipesFor(ArcaneArchives.GEM_CUTTING_TYPE.get());
        if (!loadedRecipes.equals(updated)) {
            loadedRecipes = updated;
            recipes = updated.stream().sorted(Comparator.comparingInt(GemCuttingRecipe::order)
                    .thenComparing(GemCuttingRecipe::getId)).toList();
        }
        return recipes;
    }

    public int selectedRecipe() {
        return selection.get();
    }

    public boolean canCraft(GemCuttingRecipe recipe) {
        return recipe.allowedInHive(hiveRequirementsMet.get()) && allocation(recipe) != null;
    }

    @Override
    public boolean stillValid(Player player) {
        if (table == null) {
            return true;
        }
        return !table.isRemoved() && player.level().getBlockEntity(table.getBlockPos()) == table
                && player.distanceToSqr(table.getBlockPos().getX() + 0.5, table.getBlockPos().getY() + 0.5,
                table.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    public boolean selectRecipe(Player player, ResourceLocation id) {
        if (table == null || !stillValid(player) || !(player.level().getRecipeManager().byKey(id)
                .orElse(null) instanceof GemCuttingRecipe)) {
            return false;
        }
        table.selectRecipe(id);
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (table != null) {
            updateOutput();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot == OUTPUT) {
            craft(player, type, button);
        } else {
            super.clicked(slot, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index == OUTPUT) {
            return craft(player, ClickType.QUICK_MOVE, 0);
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem();
        ItemStack original = moving.copy();
        boolean moved = index < FIRST_PLAYER
                ? moveItemStackTo(moving, FIRST_PLAYER, slots.size(), true)
                : moveItemStackTo(moving, FIRST_DRAWER, FIRST_PLAYER, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, moving);
        return original;
    }

    private void updateOutput() {
        hiveRequirementsMet.set(GemCuttingRecipe.requirementsMet(inventory.player));
        List<GemCuttingRecipe> available = recipes();
        for (int index = 0; index < available.size(); index++) {
            GemCuttingRecipe recipe = available.get(index);
            if (recipe.getId().equals(table.selectedRecipe())) {
                selection.set(index);
                updateOutputStack(canCraft(recipe) ? recipe.resultFor(inventory.player) : ItemStack.EMPTY);
                return;
            }
        }
        selection.set(-1);
        updateOutputStack(ItemStack.EMPTY);
    }

    private void updateOutputStack(ItemStack stack) {
        if (!ItemStack.matches(output.getItem(0), stack)) {
            output.setItem(0, stack);
        }
    }

    @Nullable
    private int[] allocation(GemCuttingRecipe recipe) {
        return recipe.allocate(DRAWER_SIZE + Inventory.INVENTORY_SIZE, this::inputAt);
    }

    private ItemStack inputAt(int slot) {
        return slot < DRAWER_SIZE ? drawer.getStackInSlot(slot) : inventory.getItem(slot - DRAWER_SIZE);
    }

    private ItemStack craft(Player player, ClickType type, int button) {
        if (table == null || !stillValid(player)) {
            return ItemStack.EMPTY;
        }
        boolean toCursor = type == ClickType.PICKUP && (button == 0 || button == 1);
        boolean toInventory = type == ClickType.QUICK_MOVE;
        boolean toHotbar = type == ClickType.SWAP && button >= 0 && button < 9;
        boolean drop = type == ClickType.THROW && getCarried().isEmpty();
        if (!toCursor && !toInventory && !toHotbar && !drop) {
            return ItemStack.EMPTY;
        }
        if (!(player.level().getRecipeManager().byKey(table.selectedRecipe()).orElse(null) instanceof GemCuttingRecipe recipe)) {
            return ItemStack.EMPTY;
        }
        if (!recipe.allowedInHive(GemCuttingRecipe.requirementsMet(player))) {
            return ItemStack.EMPTY;
        }
        int[] counts = allocation(recipe);
        if (counts == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = recipe.resultFor(player);
        ItemStack[] playerAfter = new ItemStack[Inventory.INVENTORY_SIZE];
        for (int slot = 0; slot < playerAfter.length; slot++) {
            ItemStack stack = inventory.getItem(slot);
            playerAfter[slot] = stack.copyWithCount(stack.getCount() - counts[DRAWER_SIZE + slot]);
        }
        if (toCursor && roomFor(getCarried(), result) < result.getCount()) {
            return ItemStack.EMPTY;
        }
        if (toInventory && !fitInInventory(playerAfter, result)) {
            return ItemStack.EMPTY;
        }
        if (toHotbar) {
            ItemStack stack = playerAfter[button];
            if (roomFor(stack, result) < result.getCount()) {
                return ItemStack.EMPTY;
            }
            playerAfter[button] = result.copyWithCount(stack.getCount() + result.getCount());
        }

        SimpleContainer used = new SimpleContainer(counts.length);
        for (int slot = 0; slot < counts.length; slot++) {
            if (counts[slot] > 0) {
                used.setItem(slot, inputAt(slot).copyWithCount(counts[slot]));
                if (slot < DRAWER_SIZE) {
                    drawer.extractItem(slot, counts[slot], false);
                }
            }
        }
        for (int slot = 0; slot < playerAfter.length; slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), playerAfter[slot])) {
                inventory.setItem(slot, playerAfter[slot]);
            }
        }
        if (toCursor) {
            setCarried(result.copyWithCount(getCarried().getCount() + result.getCount()));
        } else if (drop) {
            player.drop(result.copy(), true);
        }
        returnIngredients(player, used);
        inventory.setChanged();
        result.onCraftedBy(player.level(), player, result.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, result, used);
        broadcastChanges();
        return result;
    }

    private void returnIngredients(Player player, SimpleContainer used) {
        for (int slot = 0; slot < used.getContainerSize(); slot++) {
            ItemStack consumed = used.getItem(slot);
            for (int unit = 0; unit < consumed.getCount(); unit++) {
                ItemStack single = consumed.copyWithCount(1);
                ItemStack remainder;
                if (single.is(Items.FLINT_AND_STEEL)) {
                    single.hurtAndBreak(1, player, entity -> {});
                    remainder = single;
                } else {
                    remainder = single.getCraftingRemainingItem();
                }
                if (!remainder.isEmpty()) {
                    remainder = ItemHandlerHelper.insertItemStacked(drawer, remainder, false);
                    if (!remainder.isEmpty()) {
                        inventory.placeItemBackInInventory(remainder);
                    }
                }
            }
        }
    }

    private static int roomFor(ItemStack present, ItemStack result) {
        if (present.isEmpty() || ItemStack.isSameItemSameTags(present, result)) {
            return result.getMaxStackSize() - present.getCount();
        }
        return 0;
    }

    private static boolean fitInInventory(ItemStack[] inventory, ItemStack result) {
        int remaining = result.getCount();
        for (boolean empty : new boolean[] {false, true}) {
            for (int slot = inventory.length - 1; slot >= 0; slot--) {
                ItemStack stack = inventory[slot];
                if (stack.isEmpty() == empty) {
                    int added = Math.min(remaining, roomFor(stack, result));
                    if (added > 0) {
                        inventory[slot] = result.copyWithCount(stack.getCount() + added);
                        remaining -= added;
                        if (remaining == 0) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
