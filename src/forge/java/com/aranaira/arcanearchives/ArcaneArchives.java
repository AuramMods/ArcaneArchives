package com.aranaira.arcanearchives;

import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.progression.QuartzClusterBlock;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlock;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import com.aranaira.arcanearchives.progression.RadiantResonatorItem;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.BrazierBlock;
import com.aranaira.arcanearchives.storage.BrazierBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantAmphoraItem;
import com.aranaira.arcanearchives.storage.RadiantChestBlock;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import com.aranaira.arcanearchives.storage.RadiantTankBlock;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankItem;
import com.aranaira.arcanearchives.storage.RadiantTroveBlock;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
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

    public static final RegistryObject<RadiantTroveBlock> RADIANT_TROVE = BLOCKS.register("radiant_trove",
            () -> new RadiantTroveBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6000.0F).sound(SoundType.METAL).lightLevel(state -> 15).noOcclusion()));
    public static final RegistryObject<Item> RADIANT_TROVE_ITEM = ITEMS.register("radiant_trove",
            () -> new BlockItem(RADIANT_TROVE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RadiantTroveBlockEntity>> RADIANT_TROVE_ENTITY =
            BLOCK_ENTITIES.register("radianttrove",
                    () -> BlockEntityType.Builder.of(RadiantTroveBlockEntity::new, RADIANT_TROVE.get()).build(null));

    public static final RegistryObject<RadiantTankBlock> RADIANT_TANK = BLOCKS.register("radiant_tank",
            () -> new RadiantTankBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F, 6000.0F).sound(SoundType.GLASS).lightLevel(state -> 15).noOcclusion()));
    public static final RegistryObject<Item> RADIANT_TANK_ITEM = ITEMS.register("radiant_tank",
            () -> new RadiantTankItem(RADIANT_TANK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RadiantTankBlockEntity>> RADIANT_TANK_ENTITY =
            BLOCK_ENTITIES.register("radianttank",
                    () -> BlockEntityType.Builder.of(RadiantTankBlockEntity::new, RADIANT_TANK.get()).build(null));

    public static final RegistryObject<BrazierBlock> BRAZIER = BLOCKS.register("brazier_of_hoarding",
            () -> new BrazierBlock(BlockBehaviour.Properties.of()
                    .strength(3.0F).sound(SoundType.METAL).lightLevel(state -> 15).noOcclusion()));
    public static final RegistryObject<Block> BRAZIER_FIRE = BLOCKS.register("brazier_of_hoarding_fire",
            () -> new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion().noLootTable().lightLevel(state -> 15)));
    public static final RegistryObject<Item> BRAZIER_ITEM = ITEMS.register("brazier_of_hoarding",
            () -> new BlockItem(BRAZIER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<BrazierBlockEntity>> BRAZIER_ENTITY =
            BLOCK_ENTITIES.register("brazier",
                    () -> BlockEntityType.Builder.of(BrazierBlockEntity::new, BRAZIER.get()).build(null));
    public static final RegistryObject<SoundEvent> BRAZIER_ABSORB = SOUNDS.register("brazier.absorb",
            () -> SoundEvent.createVariableRangeEvent(id("brazier.absorb")));
    public static final RegistryObject<RadiantAmphoraItem> RADIANT_AMPHORA = ITEMS.register("radiant_amphora",
            () -> new RadiantAmphoraItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<RadiantResonatorBlock> RADIANT_RESONATOR = BLOCKS.register("radiant_resonator",
            () -> new RadiantResonatorBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Item> RADIANT_RESONATOR_ITEM = ITEMS.register("radiant_resonator",
            () -> new RadiantResonatorItem(RADIANT_RESONATOR.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RadiantResonatorBlockEntity>> RADIANT_RESONATOR_ENTITY =
            BLOCK_ENTITIES.register("radiant_resonator_tile_entity", () -> BlockEntityType.Builder
                    .of(RadiantResonatorBlockEntity::new, RADIANT_RESONATOR.get()).build(null));
    public static final RegistryObject<SoundEvent> RESONATOR_COMPLETE = SOUNDS.register("resonator.complete",
            () -> SoundEvent.createVariableRangeEvent(id("resonator.complete")));
    public static final RegistryObject<QuartzClusterBlock> RAW_QUARTZ_CLUSTER = BLOCKS.register("raw_quartz_cluster",
            () -> new QuartzClusterBlock(BlockBehaviour.Properties.of().strength(1.4F).sound(SoundType.STONE)
                    .lightLevel(state -> 15).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Item> RAW_QUARTZ_CLUSTER_ITEM = ITEMS.register("raw_quartz_cluster",
            () -> new BlockItem(RAW_QUARTZ_CLUSTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> RAW_QUARTZ = ITEMS.register("raw_quartz", () -> new Item(new Item.Properties()));

    static {
        TABS.register(ID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.arcanearchives"))
                .icon(() -> new ItemStack(RADIANT_CHEST_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(RADIANT_RESONATOR_ITEM.get());
                    output.accept(RAW_QUARTZ_CLUSTER_ITEM.get());
                    output.accept(RAW_QUARTZ.get());
                    output.accept(RADIANT_CHEST_ITEM.get());
                    output.accept(RADIANT_TROVE_ITEM.get());
                    output.accept(RADIANT_TANK_ITEM.get());
                    output.accept(BRAZIER_ITEM.get());
                    output.accept(RADIANT_AMPHORA.get());
                })
                .build());
    }

    public ArcaneArchives() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(ArcaneArchives::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(RadiantAmphoraItem::onLeftClickBlock);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArchiveConfig.SERVER_SPEC);
        ArchiveNetwork.register();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RadiantAmphoraItem::registerDispenser);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ID, path);
    }
}
