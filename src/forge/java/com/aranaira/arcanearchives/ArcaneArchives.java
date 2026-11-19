package com.aranaira.arcanearchives;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.RadiantChestBlock;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.core.registries.Registries;

@Mod(ArcaneArchives.ID)
public final class ArcaneArchives {
    public static final String ID = "arcanearchives";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final RegistryObject<RadiantChestBlock> RADIANT_CHEST = BLOCKS.register("radiant_chest",
            () -> new RadiantChestBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6000.0F).sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion()));
    public static final RegistryObject<Item> RADIANT_CHEST_ITEM = ITEMS.register("radiant_chest",
            () -> new BlockItem(RADIANT_CHEST.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RadiantChestBlockEntity>> RADIANT_CHEST_ENTITY =
            BLOCK_ENTITIES.register("radiantchest",
                    () -> BlockEntityType.Builder.of(RadiantChestBlockEntity::new, RADIANT_CHEST.get()).build(null));
    public static final RegistryObject<MenuType<RadiantChestMenu>> RADIANT_CHEST_MENU =
            MENUS.register("radiant_chest", () -> IForgeMenuType.create(RadiantChestMenu::new));

    static {
        TABS.register(ID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.arcanearchives"))
                .icon(() -> new ItemStack(RADIANT_CHEST_ITEM.get()))
                .displayItems((parameters, output) -> output.accept(RADIANT_CHEST_ITEM.get()))
                .build());
    }

    public ArcaneArchives() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        TABS.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArchiveConfig.SERVER_SPEC);
        ArchiveNetwork.register();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ID, path);
    }
}
