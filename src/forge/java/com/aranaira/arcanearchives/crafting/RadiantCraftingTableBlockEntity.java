package com.aranaira.arcanearchives.crafting;

import com.aranaira.arcanearchives.ArcaneArchives;
import com.aranaira.arcanearchives.network.ArchiveDeviceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Objects;

public final class RadiantCraftingTableBlockEntity extends ArchiveDeviceBlockEntity implements MenuProvider {
    static final int BOOKMARKS = 3;
    private final CraftingGrid grid = new CraftingGrid();
    private final ResourceLocation[] bookmarks = new ResourceLocation[BOOKMARKS];

    public RadiantCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ArcaneArchives.RADIANT_CRAFTING_TABLE_ENTITY.get(), pos, state);
        grid.addListener(inventory -> setChanged());
    }

    public CraftingContainer grid() {
        return grid;
    }

    @Nullable
    ResourceLocation bookmark(int index) {
        return bookmarks[index];
    }

    void setBookmark(int index, @Nullable ResourceLocation id) {
        if (!Objects.equals(bookmarks[index], id)) {
            bookmarks[index] = id;
            setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return ArcaneArchives.RADIANT_CRAFTING_TABLE.get().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadiantCraftingTableMenu(id, inventory, this, grid);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", grid.save());
        for (int index = 0; index < BOOKMARKS; index++) {
            if (bookmarks[index] != null) {
                tag.putString("recipe" + (index + 1), bookmarks[index].toString());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        grid.load(tag.getCompound("inventory"));
        for (int index = 0; index < BOOKMARKS; index++) {
            String key = "recipe" + (index + 1);
            bookmarks[index] = tag.contains(key, Tag.TAG_STRING) ? ResourceLocation.tryParse(tag.getString(key)) : null;
        }
    }
}
