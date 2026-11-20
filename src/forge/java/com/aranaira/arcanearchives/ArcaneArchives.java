package com.aranaira.arcanearchives;

import com.aranaira.arcanearchives.arsenal.ArcaneGemItem;
import com.aranaira.arcanearchives.arsenal.ChromaticPowderItem;
import com.aranaira.arcanearchives.arsenal.FakeAirBlock;
import com.aranaira.arcanearchives.arsenal.GemSocketItem;
import com.aranaira.arcanearchives.arsenal.GemSocketMenu;
import com.aranaira.arcanearchives.arsenal.GemType;
import com.aranaira.arcanearchives.config.ArchiveConfig;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlock;
import com.aranaira.arcanearchives.crafting.GemCuttersTableBlockEntity;
import com.aranaira.arcanearchives.crafting.GemCuttersTableMenu;
import com.aranaira.arcanearchives.crafting.GemCuttingRecipe;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableBlock;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableBlockEntity;
import com.aranaira.arcanearchives.crafting.RadiantCraftingTableMenu;
import com.aranaira.arcanearchives.manifest.ManifestItem;
import com.aranaira.arcanearchives.manifest.ManifestLecternBlock;
import com.aranaira.arcanearchives.manifest.ManifestMenu;
import com.aranaira.arcanearchives.monitoring.MonitoringCrystalBlock;
import com.aranaira.arcanearchives.monitoring.MonitoringCrystalBlockEntity;
import com.aranaira.arcanearchives.monitoring.RevelationScepterItem;
import com.aranaira.arcanearchives.network.ArchiveCommands;
import com.aranaira.arcanearchives.network.HiveLetterItem;
import com.aranaira.arcanearchives.progression.DirectionalQuartzBlock;
import com.aranaira.arcanearchives.progression.QuartzSliverBlock;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlock;
import com.aranaira.arcanearchives.progression.RadiantResonatorBlockEntity;
import com.aranaira.arcanearchives.progression.RadiantResonatorItem;
import com.aranaira.arcanearchives.progression.RawQuartzItem;
import com.aranaira.arcanearchives.storage.ArchiveNetwork;
import com.aranaira.arcanearchives.storage.BrazierBlock;
import com.aranaira.arcanearchives.storage.BrazierBlockEntity;
import com.aranaira.arcanearchives.storage.BrazierSettingsMenu;
import com.aranaira.arcanearchives.storage.DevouringCharmItem;
import com.aranaira.arcanearchives.storage.DebugOrbItem;
import com.aranaira.arcanearchives.storage.DevouringCharmMenu;
import com.aranaira.arcanearchives.storage.ManipulationScepterItem;
import com.aranaira.arcanearchives.storage.RadiantAmphoraItem;
import com.aranaira.arcanearchives.storage.RadiantChestBlock;
import com.aranaira.arcanearchives.storage.RadiantChestBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantChestMenu;
import com.aranaira.arcanearchives.storage.RadiantKeyItem;
import com.aranaira.arcanearchives.storage.RadiantTankBlock;
import com.aranaira.arcanearchives.storage.RadiantTankBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTankItem;
import com.aranaira.arcanearchives.storage.RadiantTroveBlock;
import com.aranaira.arcanearchives.storage.RadiantTroveBlockEntity;
import com.aranaira.arcanearchives.storage.RadiantTroveItem;
import com.aranaira.arcanearchives.storage.StorageUpgradesMenu;
import com.aranaira.arcanearchives.tome.TomeOfArcanaItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
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

import java.util.EnumMap;
import java.util.Map;

@Mod(ArcaneArchives.ID)
public final class ArcaneArchives {
    public static final String ID = "arcanearchives";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ID);

    public static final Map<GemType, RegistryObject<ArcaneGemItem>> GEMS = registerGems();
    public static final RegistryObject<GemSocketItem> GEM_SOCKET = ITEMS.register("gemsocket",
            () -> new GemSocketItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MenuType<GemSocketMenu>> GEM_SOCKET_MENU = MENUS.register("gemsocket",
            () -> IForgeMenuType.create(GemSocketMenu::new));
    public static final RegistryObject<ChromaticPowderItem> CHROMATIC_POWDER = ITEMS.register("chromatic_powder",
            () -> new ChromaticPowderItem(new Item.Properties()));
    public static final RegistryObject<Item> FULL_SPECTRUM_POWDER = ITEMS.register("full_spectrum_chromatic_powder",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ARSENAL_CONDITION = ITEMS.register("gbook_arsenal_condition",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Block> FAKE_AIR = BLOCKS.register("fake_air", () -> new FakeAirBlock(
            BlockBehaviour.Properties.of().noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.IGNORE)));

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
            () -> new RadiantTroveItem(RADIANT_TROVE.get(), new Item.Properties()));
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
    public static final RegistryObject<MenuType<StorageUpgradesMenu>> STORAGE_UPGRADES_MENU =
            MENUS.register("storage_upgrades", () -> IForgeMenuType.create(StorageUpgradesMenu::new));

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
    public static final RegistryObject<MenuType<BrazierSettingsMenu>> BRAZIER_SETTINGS_MENU =
            MENUS.register("brazier_settings", () -> IForgeMenuType.create(BrazierSettingsMenu::new));
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
    public static final RegistryObject<SoundEvent> RESONATOR_LOOP = SOUNDS.register("resonator.loop",
            () -> SoundEvent.createVariableRangeEvent(id("resonator.loop")));
    public static final RegistryObject<DirectionalQuartzBlock> RAW_QUARTZ_CLUSTER = BLOCKS.register("raw_quartz_cluster",
            () -> new DirectionalQuartzBlock(BlockBehaviour.Properties.of().strength(1.4F).sound(SoundType.STONE)
                    .lightLevel(state -> 15).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<Item> RAW_QUARTZ_CLUSTER_ITEM = ITEMS.register("raw_quartz_cluster",
            () -> new BlockItem(RAW_QUARTZ_CLUSTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> RAW_QUARTZ = ITEMS.register("raw_quartz", () -> new RawQuartzItem(new Item.Properties()));
    public static final RegistryObject<QuartzSliverBlock> QUARTZ_SLIVER = BLOCKS.register("quartz_sliver",
            () -> new QuartzSliverBlock(BlockBehaviour.Properties.of().instabreak().sound(SoundType.GLASS)
                    .noCollission().noOcclusion().lightLevel(state -> 15)));
    public static final RegistryObject<Item> QUARTZ_SLIVER_ITEM = ITEMS.register("quartz_sliver",
            () -> new BlockItem(QUARTZ_SLIVER.get(), new Item.Properties()));
    public static final RegistryObject<DirectionalQuartzBlock> RADIANT_LANTERN = BLOCKS.register("radiant_lantern",
            () -> new DirectionalQuartzBlock(BlockBehaviour.Properties.of().strength(0.3F)
                    .sound(SoundType.GLASS).noCollission().noOcclusion().lightLevel(state -> 15), 5.6));
    public static final RegistryObject<Item> RADIANT_LANTERN_ITEM = ITEMS.register("radiant_lantern",
            () -> new BlockItem(RADIANT_LANTERN.get(), new Item.Properties()));

    public static final RegistryObject<Block> STORAGE_RAW_QUARTZ = quartzStorageBlock("storage_raw_quartz");
    public static final RegistryObject<Block> STORAGE_SHAPED_QUARTZ = quartzStorageBlock("storage_shaped_quartz");
    public static final RegistryObject<Item> STORAGE_RAW_QUARTZ_ITEM = ITEMS.register("storage_raw_quartz",
            () -> new BlockItem(STORAGE_RAW_QUARTZ.get(), new Item.Properties()));
    public static final RegistryObject<Item> STORAGE_SHAPED_QUARTZ_ITEM = ITEMS.register("storage_shaped_quartz",
            () -> new BlockItem(STORAGE_SHAPED_QUARTZ.get(), new Item.Properties()));
    public static final RegistryObject<Item> SHAPED_QUARTZ = material("shaped_quartz");
    public static final RegistryObject<Item> RADIANT_DUST = material("radiant_dust");
    public static final RegistryObject<Item> SCINTILLATING_INLAY = material("scintillating_inlay");
    public static final RegistryObject<Item> MATRIX_BRACE = material("matrix_brace");
    public static final RegistryObject<Item> MATERIAL_INTERFACE = material("material_interface");
    public static final RegistryObject<Item> CONTAINMENT_FIELD = material("containment_field");

    public static final RegistryObject<GemCuttersTableBlock> GEMCUTTERS_TABLE = BLOCKS.register("gemcutters_table",
            () -> new GemCuttersTableBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.METAL)
                    .lightLevel(state -> 15).pushReaction(PushReaction.BLOCK).noOcclusion()));
    public static final RegistryObject<Item> GEMCUTTERS_TABLE_ITEM = ITEMS.register("gemcutters_table",
            () -> new BlockItem(GEMCUTTERS_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<GemCuttersTableBlockEntity>> GEMCUTTERS_TABLE_ENTITY =
            BLOCK_ENTITIES.register("gemcutterstable", () -> BlockEntityType.Builder
                    .of(GemCuttersTableBlockEntity::new, GEMCUTTERS_TABLE.get()).build(null));
    public static final RegistryObject<MenuType<GemCuttersTableMenu>> GEMCUTTERS_TABLE_MENU =
            MENUS.register("gemcutters_table", () -> IForgeMenuType.create(GemCuttersTableMenu::new));
    public static final RegistryObject<RecipeType<GemCuttingRecipe>> GEM_CUTTING_TYPE =
            RECIPE_TYPES.register("gem_cutting", () -> RecipeType.simple(id("gem_cutting")));
    public static final RegistryObject<RecipeSerializer<GemCuttingRecipe>> GEM_CUTTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("gem_cutting", GemCuttingRecipe.Serializer::new);
    public static final RegistryObject<RadiantCraftingTableBlock> RADIANT_CRAFTING_TABLE = BLOCKS.register("radiant_crafting_table",
            () -> new RadiantCraftingTableBlock(BlockBehaviour.Properties.of().strength(3.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> 15).noOcclusion()));
    public static final RegistryObject<Item> RADIANT_CRAFTING_TABLE_ITEM = ITEMS.register("radiant_crafting_table",
            () -> new BlockItem(RADIANT_CRAFTING_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<RadiantCraftingTableBlockEntity>> RADIANT_CRAFTING_TABLE_ENTITY =
            BLOCK_ENTITIES.register("radiantcraftingtable", () -> BlockEntityType.Builder
                    .of(RadiantCraftingTableBlockEntity::new, RADIANT_CRAFTING_TABLE.get()).build(null));
    public static final RegistryObject<MenuType<RadiantCraftingTableMenu>> RADIANT_CRAFTING_TABLE_MENU =
            MENUS.register("radiant_crafting_table", () -> new MenuType<>(RadiantCraftingTableMenu::new, FeatureFlags.DEFAULT_FLAGS));
    public static final RegistryObject<MonitoringCrystalBlock> MONITORING_CRYSTAL = BLOCKS.register("monitoring_crystal",
            () -> new MonitoringCrystalBlock(BlockBehaviour.Properties.of().strength(0.8F).sound(SoundType.GLASS)
                    .lightLevel(state -> 15).noCollission().noOcclusion()));
    public static final RegistryObject<Item> MONITORING_CRYSTAL_ITEM = ITEMS.register("monitoring_crystal",
            () -> new MonitoringCrystalBlock.CrystalItem(MONITORING_CRYSTAL.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<MonitoringCrystalBlockEntity>> MONITORING_CRYSTAL_ENTITY =
            BLOCK_ENTITIES.register("monitoring_crystal_tile_entity", () -> BlockEntityType.Builder
                    .of(MonitoringCrystalBlockEntity::new, MONITORING_CRYSTAL.get()).build(null));
    public static final RegistryObject<Item> SCEPTER_MANIPULATION = ITEMS.register("scepter_manipulation",
            () -> new ManipulationScepterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> SCEPTER_REVELATION = ITEMS.register("scepter_revelation",
            () -> new RevelationScepterItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> DEVOURING_CHARM = ITEMS.register("devouring_charm",
            () -> new DevouringCharmItem(new Item.Properties()));
    public static final RegistryObject<MenuType<DevouringCharmMenu>> DEVOURING_CHARM_MENU =
            MENUS.register("devouring_charm", () -> IForgeMenuType.create(DevouringCharmMenu::new));
    public static final RegistryObject<Item> RADIANT_KEY = ITEMS.register("radiant_key",
            () -> new RadiantKeyItem(new Item.Properties()));
    public static final RegistryObject<Item> DEBUG_ORB = ITEMS.register("debugorb",
            () -> new DebugOrbItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LETTER_INVITATION = ITEMS.register("letter_invitation",
            () -> new HiveLetterItem(new Item.Properties(), HiveLetterItem.Kind.INVITATION));
    public static final RegistryObject<Item> LETTER_RESIGNATION = ITEMS.register("letter_resignation",
            () -> new HiveLetterItem(new Item.Properties(), HiveLetterItem.Kind.RESIGNATION));
    public static final RegistryObject<Item> WRIT_EXPULSION = ITEMS.register("writ_expulsion",
            () -> new HiveLetterItem(new Item.Properties(), HiveLetterItem.Kind.EXPULSION));

    public static final RegistryObject<Item> MANIFEST = ITEMS.register("manifest",
            () -> new ManifestItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TOME_OF_ARCANA = ITEMS.register("tome_arcana",
            () -> new TomeOfArcanaItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<ManifestLecternBlock> MANIFEST_LECTERN = BLOCKS.register("lectern_manifest",
            () -> new ManifestLecternBlock(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.WOOD)
                    .lightLevel(state -> 15).pushReaction(PushReaction.BLOCK).noOcclusion()));
    public static final RegistryObject<Item> MANIFEST_LECTERN_ITEM = ITEMS.register("lectern_manifest",
            () -> new BlockItem(MANIFEST_LECTERN.get(), new Item.Properties()));
    public static final RegistryObject<MenuType<ManifestMenu>> MANIFEST_MENU =
            MENUS.register("manifest", () -> new MenuType<>(ManifestMenu::new, FeatureFlags.DEFAULT_FLAGS));

    static {
        TABS.register(ID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.arcanearchives"))
                .icon(() -> new ItemStack(RADIANT_CHEST_ITEM.get()))
                .displayItems((parameters, output) -> {
                    output.accept(RADIANT_RESONATOR_ITEM.get());
                    output.accept(RAW_QUARTZ_CLUSTER_ITEM.get());
                    output.accept(RAW_QUARTZ.get());
                    output.accept(QUARTZ_SLIVER_ITEM.get());
                    output.accept(RADIANT_LANTERN_ITEM.get());
                    output.accept(GEMCUTTERS_TABLE_ITEM.get());
                    output.accept(RADIANT_CRAFTING_TABLE_ITEM.get());
                    output.accept(STORAGE_RAW_QUARTZ_ITEM.get());
                    output.accept(STORAGE_SHAPED_QUARTZ_ITEM.get());
                    output.accept(SHAPED_QUARTZ.get());
                    output.accept(RADIANT_DUST.get());
                    output.accept(SCINTILLATING_INLAY.get());
                    output.accept(MATRIX_BRACE.get());
                    output.accept(MATERIAL_INTERFACE.get());
                    output.accept(CONTAINMENT_FIELD.get());
                    output.accept(RADIANT_CHEST_ITEM.get());
                    output.accept(RADIANT_TROVE_ITEM.get());
                    output.accept(RADIANT_TANK_ITEM.get());
                    output.accept(BRAZIER_ITEM.get());
                    output.accept(RADIANT_AMPHORA.get());
                    output.accept(MONITORING_CRYSTAL_ITEM.get());
                    output.accept(MANIFEST.get());
                    output.accept(TomeOfArcanaItem.tome());
                    output.accept(MANIFEST_LECTERN_ITEM.get());
                    output.accept(SCEPTER_REVELATION.get());
                    output.accept(SCEPTER_MANIPULATION.get());
                    output.accept(DEVOURING_CHARM.get());
                    output.accept(RADIANT_KEY.get());
                    output.accept(DEBUG_ORB.get());
                    if (ArchiveConfig.ARSENAL_ENABLED.get()) {
                        output.accept(GEM_SOCKET.get());
                        for (GemType gem : GemType.values()) {
                            output.accept(GEMS.get(gem).get());
                        }
                        for (GemType.Color color : GemType.Color.values()) {
                            if (color != GemType.Color.NONE) {
                                output.accept(CHROMATIC_POWDER.get().of(color));
                            }
                        }
                        output.accept(FULL_SPECTRUM_POWDER.get());
                    }
                    output.accept(LETTER_INVITATION.get());
                    output.accept(LETTER_RESIGNATION.get());
                    output.accept(WRIT_EXPULSION.get());
                })
                .build());
    }

    private static Map<GemType, RegistryObject<ArcaneGemItem>> registerGems() {
        var gems = new EnumMap<GemType, RegistryObject<ArcaneGemItem>>(GemType.class);
        for (GemType type : GemType.values()) {
            gems.put(type, ITEMS.register(type.id(), () -> {
                var properties = new Item.Properties().stacksTo(1);
                if (type == GemType.MOUNTAINTEAR) {
                    properties.fireResistant();
                }
                return new ArcaneGemItem(type, properties);
            }));
        }
        return Map.copyOf(gems);
    }

    public ArcaneArchives() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        SOUNDS.register(modBus);
        TABS.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(ArcaneArchives::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(RadiantAmphoraItem::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(DebugOrbItem::onLeftClickBlock);
        MinecraftForge.EVENT_BUS.addListener(ArchiveCommands::register);
        MinecraftForge.EVENT_BUS.addListener(DevouringCharmItem::onItemPickup);
        MinecraftForge.EVENT_BUS.addListener(RawQuartzItem::bashOnBlock);
        MinecraftForge.EVENT_BUS.addListener(HiveLetterItem::onAnvilRepair);
        MinecraftForge.EVENT_BUS.addListener(HiveLetterItem::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(TomeOfArcanaItem::onCraft);
        MinecraftForge.EVENT_BUS.addListener(TomeOfArcanaItem::onBlockBreak);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ArchiveConfig.SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ArchiveConfig.CLIENT_SPEC);
        ArchiveNetwork.register();
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(RadiantAmphoraItem::registerDispenser);
    }

    private static RegistryObject<Block> quartzStorageBlock(String id) {
        return BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().strength(1.7F)
                .sound(SoundType.STONE).lightLevel(state -> 15).requiresCorrectToolForDrops()));
    }

    private static RegistryObject<Item> material(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(ID, path);
    }
}
