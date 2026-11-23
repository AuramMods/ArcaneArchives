package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.integration.stages.ArchiveStages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import javax.annotation.Nullable;
import java.util.List;

public final class RadiantCraftingTableMenu extends AbstractContainerMenu {
    public static final int BOOKMARK_COUNT = RadiantCraftingTableBlockEntity.BOOKMARKS;
    public static final int BOOKMARK_X = 174;
    public static final int FIRST_BOOKMARK_Y = 16;
    public static final int BOOKMARK_SPACING = 26;

    private static final int RESULT_SLOT = 0;
    private static final int FIRST_GRID_SLOT = 1;
    private static final int FIRST_PLAYER_SLOT = FIRST_GRID_SLOT + CraftingGrid.SIZE;
    private static final int FIRST_BOOKMARK_SLOT = FIRST_PLAYER_SLOT + Inventory.INVENTORY_SIZE;

    private final Inventory inventory;
    @Nullable
    private final RadiantCraftingTableBlockEntity table;
    private final CraftingGrid grid;
    private final ContainerListener gridListener = container -> refreshResult();
    private final ResultContainer result = new ResultContainer();
    private final SimpleContainer previews = new SimpleContainer(BOOKMARK_COUNT + 1);
    private final DataSlot craftableBookmarks = DataSlot.standalone();
    @Nullable
    private CraftingRecipe currentRecipe;
    private RecipeManager lastRecipeManager;
    private boolean lastResultAllowed;
    @Nullable
    private ResourceLocation lastCrafted;

    public RadiantCraftingTableMenu(int id, Inventory inventory) {
        this(id, inventory, null, new CraftingGrid());
    }

    RadiantCraftingTableMenu(int id, Inventory inventory, @Nullable RadiantCraftingTableBlockEntity table, CraftingGrid grid) {
        super(ArcaneArchives.RADIANT_CRAFTING_TABLE_MENU.get(), id);
        this.inventory = inventory;
        this.table = table;
        this.grid = grid;
        addDataSlot(craftableBookmarks);
        addSlot(new ResultSlot(inventory.player, grid, result, 0, 136, 42) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                CraftingRecipe crafted = currentRecipe;
                super.onTake(player, stack);
                if (table != null && crafted != null) {
                    remember(crafted);
                }
            }
        });
        for (int slot = 0; slot < CraftingGrid.SIZE; slot++) {
            addSlot(new Slot(grid, slot, 24 + slot % 3 * 18, 24 + slot / 3 * 18));
        }
        for (int slot = 9; slot < Inventory.INVENTORY_SIZE; slot++) {
            addSlot(new Slot(inventory, slot, 23 + slot % 9 * 18, 115 + (slot / 9 - 1) * 18));
        }
        for (int slot = 0; slot < 9; slot++) {
            addSlot(new Slot(inventory, slot, 23 + slot * 18, 173));
        }
        for (int slot = 0; slot < BOOKMARK_COUNT; slot++) {
            addSlot(new PreviewSlot(slot, BOOKMARK_X, FIRST_BOOKMARK_Y + slot * BOOKMARK_SPACING));
        }
        addSlot(new PreviewSlot(BOOKMARK_COUNT, 0, 0) {
            @Override
            public boolean isActive() {
                return false;
            }
        });
        if (table != null) {
            grid.addListener(gridListener);
            refreshResult();
            refreshBookmarks();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return table == null || (player.level().getBlockEntity(table.getBlockPos()) == table
                && stillValid(ContainerLevelAccess.create(player.level(), table.getBlockPos()), player,
                ArcaneArchives.RADIANT_CRAFTING_TABLE.get()));
    }

    @Override
    public void removed(Player player) {
        grid.removeListener(gridListener);
        super.removed(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= FIRST_BOOKMARK_SLOT) {
            return;
        }
        if (table != null && (slotId == RESULT_SLOT || type == ClickType.PICKUP_ALL)) {
            refreshResult();
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != result && slot.container != previews && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < RESULT_SLOT || index >= FIRST_BOOKMARK_SLOT) {
            return ItemStack.EMPTY;
        }
        if (table != null && index == RESULT_SLOT) {
            refreshResult();
        }
        Slot slot = slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = stack.copy();
        boolean moved = index < FIRST_PLAYER_SLOT
                ? moveItemStackTo(stack, FIRST_PLAYER_SLOT, FIRST_BOOKMARK_SLOT, true)
                : moveItemStackTo(stack, FIRST_GRID_SLOT, FIRST_PLAYER_SLOT, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (index == RESULT_SLOT) {
            slot.onQuickCraft(stack, original);
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        if (index == RESULT_SLOT && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        return original;
    }

    public ItemStack bookmarkPreview(int index) {
        return previews.getItem(index);
    }

    public ItemStack previewToSave(boolean last) {
        return last ? previews.getItem(BOOKMARK_COUNT) : result.getItem(0);
    }

    public boolean canUseBookmark(int index) {
        return (craftableBookmarks.get() & (1 << index)) != 0;
    }

    public static int bookmarkButton(int index, boolean alternate) {
        return alternate ? index + BOOKMARK_COUNT : index;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (table == null || !stillValid(player) || player.isSpectator() || button < 0 || button >= BOOKMARK_COUNT * 2) {
            return false;
        }
        boolean alternate = button >= BOOKMARK_COUNT;
        int index = button % BOOKMARK_COUNT;
        CraftingRecipe saved = recipeById(table.bookmark(index));
        if (saved == null) {
            refreshResult();
            CraftingRecipe toSave = alternate ? recipeById(lastCrafted) : currentRecipe;
            if (toSave == null || !canCraft(toSave) || toSave.getIngredients().isEmpty()
                    || toSave.getResultItem(player.level().registryAccess()).isEmpty()) {
                return false;
            }
            table.setBookmark(index, toSave.getId());
        } else if (alternate) {
            table.setBookmark(index, null);
        } else if (!craftBookmark(player, saved)) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    public void broadcastChanges() {
        if (table != null) {
            if (lastRecipeManager != inventory.player.level().getRecipeManager()
                    || (currentRecipe != null && (recipeById(currentRecipe.getId()) != currentRecipe
                    || canCraft(currentRecipe) != lastResultAllowed))) {
                refreshResult();
            }
            refreshBookmarks();
        }
        super.broadcastChanges();
    }

    private void refreshResult() {
        Player player = inventory.player;
        var level = player.level();
        Player previous = ForgeHooks.getCraftingPlayer();
        ForgeHooks.setCraftingPlayer(player);
        try {
            var recipes = level.getRecipeManager();
            lastRecipeManager = recipes;
            if (currentRecipe == null || recipeById(currentRecipe.getId()) != currentRecipe || !currentRecipe.matches(grid, level)) {
                currentRecipe = recipes.getRecipeFor(RecipeType.CRAFTING, grid, level).orElse(null);
            }
            ItemStack output = ItemStack.EMPTY;
            result.setRecipeUsed(null);
            lastResultAllowed = currentRecipe != null && canCraft(currentRecipe);
            if (lastResultAllowed) {
                output = currentRecipe.assemble(grid, level.registryAccess());
                if (!output.isEmpty()) {
                    result.setRecipeUsed(currentRecipe);
                }
            }
            result.setItem(0, output);
        } finally {
            ForgeHooks.setCraftingPlayer(previous);
        }
    }

    private void refreshBookmarks() {
        int ready = 0;
        var level = inventory.player.level();
        for (int index = 0; index < BOOKMARK_COUNT; index++) {
            CraftingRecipe recipe = recipeById(table.bookmark(index));
            ItemStack display = recipe == null ? ItemStack.EMPTY : recipe.getResultItem(level.registryAccess());
            if (!ItemStack.matches(display, previews.getItem(index))) {
                previews.setItem(index, display.copy());
            }
            if (recipe != null && canCraft(recipe) && BookmarkCraft.prepare(recipe, grid, inventory, level) != null) {
                ready |= 1 << index;
            }
        }
        craftableBookmarks.set(ready);
    }

    private boolean canCraft(CraftingRecipe recipe) {
        return ArchiveStages.allowsRadiantCrafting(inventory.player, recipe)
                && (recipe.isSpecial() || !(inventory.player instanceof ServerPlayer serverPlayer)
                || !serverPlayer.level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_LIMITED_CRAFTING)
                || serverPlayer.getRecipeBook().contains(recipe));
    }

    private boolean craftBookmark(Player player, CraftingRecipe recipe) {
        if (!canCraft(recipe)) {
            return false;
        }
        var level = player.level();
        BookmarkCraft craft = BookmarkCraft.prepare(recipe, grid, inventory, level);
        if (craft == null) {
            return false;
        }
        List<ItemStack> remainders;
        Player previous = ForgeHooks.getCraftingPlayer();
        ForgeHooks.setCraftingPlayer(player);
        try {
            remainders = recipe.getRemainingItems(craft.input());
        } finally {
            ForgeHooks.setCraftingPlayer(previous);
        }

        int[] consumed = craft.consumed();
        for (int slot = 0; slot < consumed.length; slot++) {
            if (consumed[slot] > 0) {
                if (slot < CraftingGrid.SIZE) {
                    grid.removeItem(slot, consumed[slot]);
                } else {
                    inventory.removeItem(slot - CraftingGrid.SIZE, consumed[slot]);
                }
            }
        }
        ItemStack crafted = craft.output();
        crafted.onCraftedBy(level, player, crafted.getCount());
        ForgeEventFactory.firePlayerCraftingEvent(player, crafted, craft.input());
        if (!recipe.isSpecial()) {
            player.awardRecipes(List.of(recipe));
        }
        if (player instanceof ServerPlayer serverPlayer) {
            net.minecraft.advancements.CriteriaTriggers.RECIPE_CRAFTED.trigger(serverPlayer, recipe.getId(),
                    craft.input().getItems());
        }
        PlayerMainInvWrapper destination = new PlayerMainInvWrapper(inventory);
        giveOrDrop(crafted, destination);
        for (ItemStack remainder : remainders) {
            giveOrDrop(remainder, destination);
        }
        inventory.setChanged();
        remember(recipe);
        return true;
    }

    private void giveOrDrop(ItemStack stack, PlayerMainInvWrapper destination) {
        if (!stack.isEmpty()) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(destination, stack.copy(), false);
            if (!remainder.isEmpty()) {
                var pos = table.getBlockPos();
                Containers.dropItemStack(inventory.player.level(), pos.getX(), pos.getY(), pos.getZ(), remainder);
            }
        }
    }

    private void remember(CraftingRecipe recipe) {
        lastCrafted = recipe.getId();
        previews.setItem(BOOKMARK_COUNT, recipe.getResultItem(inventory.player.level().registryAccess()).copy());
    }

    @Nullable
    private CraftingRecipe recipeById(@Nullable ResourceLocation id) {
        return id != null && inventory.player.level().getRecipeManager().byKey(id).orElse(null) instanceof CraftingRecipe recipe
                ? recipe : null;
    }

    private class PreviewSlot extends Slot {
        PreviewSlot(int index, int x, int y) {
            super(previews, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
