package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.item.FactoryBlockItem;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.other.BlockWithTooltip;
import eu.pb4.polyfactory.item.configuration.ClipboardItem;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.factorytools.api.item.MultiBlockItem;
import eu.pb4.factorytools.api.block.MultiBlock;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.block.data.AbstractCableBlock;
import eu.pb4.polyfactory.block.fluids.PortableFluidTankBlock;
import eu.pb4.polyfactory.block.fluids.PortableFluidTankBlockEntity;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.item.block.*;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.item.tool.*;
import eu.pb4.polyfactory.item.util.*;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.configuration.WrenchItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static eu.pb4.polyfactory.ModInit.id;

public class FactoryItems {
    // Util
    public static final Item FLUID_MODEL = register("fluid_model", FluidModelItem::new);
    // Actual items
    public static final WrenchItem WRENCH = register("wrench", settings -> new WrenchItem(settings.stacksTo(1)));
    public static final Item CLIPBOARD = register("clipboard", settings -> new ClipboardItem(settings.stacksTo(1)));
    public static final Item CONVEYOR = register(FactoryBlocks.CONVEYOR);
    public static final Item STICKY_CONVEYOR = register(FactoryBlocks.STICKY_CONVEYOR);
    public static final Item FUNNEL = register(FactoryBlocks.FUNNEL);
    public static final Item SLOT_AWARE_FUNNEL = register(FactoryBlocks.SLOT_AWARE_FUNNEL);
    public static final Item SPLITTER = register(FactoryBlocks.SPLITTER);
    public static final Item FAN = register(FactoryBlocks.FAN);
    public static final Item EJECTOR = register(FactoryBlocks.EJECTOR);
    public static final Item HAND_CRANK = register(FactoryBlocks.HAND_CRANK);
    public static final Item STEAM_ENGINE = register(FactoryBlocks.STEAM_ENGINE);
    public static final Item SMELTERY_CORE = register(FactoryBlocks.SMELTERY_CORE);
    public static final Item SMELTERY = register(FactoryBlocks.SMELTERY);
    public static final Item PRIMITIVE_SMELTERY = register(FactoryBlocks.PRIMITIVE_SMELTERY);
    public static final Item CASTING_TABLE = register(FactoryBlocks.CASTING_TABLE);
    public static final Item SMELTERY_FAUCED = register(FactoryBlocks.FAUCED);
    public static final Item GRINDER = register(FactoryBlocks.GRINDER);
    public static final Item PRESS = register(FactoryBlocks.PRESS);
    public static final Item CRAFTER = register(FactoryBlocks.CRAFTER);
    public static final Item MIXER = register(FactoryBlocks.MIXER);
    public static final Item MINER = register(FactoryBlocks.MINER);
    public static final Item PLACER = register(FactoryBlocks.PLACER);
    public static final Item PLANTER = register(FactoryBlocks.PLANTER);
    public static final FactoryBlockItem AXLE = register(FactoryBlocks.AXLE);
    public static final FactoryBlockItem CHAIN_DRIVE = register(FactoryBlocks.CHAIN_DRIVE);
    public static final Item TURNTABLE = register(FactoryBlocks.TURNTABLE);
    public static final Item GEARBOX = register(FactoryBlocks.GEARBOX);
    public static final Item CLUTCH = register(FactoryBlocks.CLUTCH);
    public static final Item CONTAINER = register( FactoryBlocks.CONTAINER);
    public static final Item NIXIE_TUBE = register(FactoryBlocks.NIXIE_TUBE);
    public static final WindmillSailItem WINDMILL_SAIL = register("windmill_sail", WindmillSailItem::new);
    public static final Item METAL_GRID = register(FactoryBlocks.METAL_GRID);
    public static final Item STRING_MESH = register("string_mesh", SimplePolymerItem::new);
    public static final Item SAW_DUST = register("saw_dust", SimplePolymerItem::new);
    public static final Item COAL_DUST = register("coal_dust", SimplePolymerItem::new);
    public static final Item NETHERRACK_DUST = register("netherrack_dust", SimplePolymerItem::new);
    public static final Item ENDER_DUST = register("ender_dust", SimplePolymerItem::new);
    public static final Item ENDER_INFUSED_AMETHYST_SHARD = register("ender_infused_amethyst_shard", SimplePolymerItem::new);
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", SimplePolymerItem::new);
    public static final Item STEEL_INGOT = register("steel_ingot", SimplePolymerItem::new);
    public static final Item STEEL_NUGGET = register("steel_nugget", SimplePolymerItem::new);
    public static final Item STEEL_BLOCK = register(FactoryBlocks.STEEL_BLOCK);
    public static final Item STEEL_PLATE = register("steel_plate", SimplePolymerItem::new);
    public static final Item COPPER_PLATE = register("copper_plate", SimplePolymerItem::new);
    public static final Item BRITTLE_GLASS_BOTTLE = register("brittle_glass_bottle", SimplePolymerItem::new);
    public static final Item BRITTLE_POTION = register("brittle_potion", settings -> new BrittlePotionItem(settings.component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).component(DataComponents.CONSUMABLE, Consumables.defaultDrink().soundAfterConsume(SoundEvents.OMINOUS_BOTTLE_DISPOSE).build()).stacksTo(1)));
    public static final Item THROWABLE_GLASS_BOTTLE = register("throwable_glass_bottle", SimplePolymerItem::new);
    public static final Item LINGERING_THROWABLE_GLASS_BOTTLE = register("lingering_throwable_glass_bottle", SimplePolymerItem::new);
    public static final Item STEEL_GEAR = register("steel_gear", (settings) -> new GearItem(FactoryBlocks.AXLE_WITH_GEAR, settings));
    public static final Item LARGE_STEEL_GEAR = register("large_steel_gear", (settings) -> new GearItem(FactoryBlocks.AXLE_WITH_LARGE_GEAR, settings));
    public static final Item STEEL_MACHINE_GEARBOX = register("generic_machine_part", SimplePolymerItem::new);
    public static final Item WOODEN_PLATE = register("wooden_plate", SimplePolymerItem::new);
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", SimplePolymerItem::new);
    public static final Item INTEGRATED_CIRCUIT = register("integrated_circuit", SimplePolymerItem::new);
    public static final Item REDSTONE_CHIP = register("redstone_chip", SimplePolymerItem::new);
    public static final Item CHAIN_LIFT = register("chain_lift", s -> new SimplePolymerItem(s.stacksTo(1)));

    public static final Item ITEM_FILTER = register("item_filter", ImprovedFilterItem::new);

    public static final Item CREATIVE_MOTOR = register(FactoryBlocks.CREATIVE_MOTOR);
    public static final Item CREATIVE_CONTAINER = register(FactoryBlocks.CREATIVE_CONTAINER);
    public static final Item TACHOMETER = register(FactoryBlocks.TACHOMETER);
    public static final Item STRESSOMETER = register(FactoryBlocks.STRESSOMETER);
    public static final Item ITEM_COUNTER = register(FactoryBlocks.ITEM_COUNTER);
    public static final Item REDSTONE_INPUT = register(FactoryBlocks.REDSTONE_INPUT);
    public static final Item REDSTONE_OUTPUT = register(FactoryBlocks.REDSTONE_OUTPUT);
    public static final Item SPEAKER = register(FactoryBlocks.SPEAKER);
    public static final Item RECORD_PLAYER = register(FactoryBlocks.RECORD_PLAYER);
    public static final Item ITEM_READER = register(FactoryBlocks.ITEM_READER);
    public static final Item BLOCK_OBSERVER = register(FactoryBlocks.BLOCK_OBSERVER);
    public static final Item TEXT_INPUT = register(FactoryBlocks.TEXT_INPUT);
    public static final Item DIGITAL_CLOCK = register(FactoryBlocks.DIGITAL_CLOCK);
    public static final Item ARITHMETIC_OPERATOR = register(FactoryBlocks.ARITHMETIC_OPERATOR);
    public static final Item DATA_COMPARATOR = register(FactoryBlocks.DATA_COMPARATOR);
    public static final Item DATA_EXTRACTOR = register(FactoryBlocks.DATA_EXTRACTOR);
    public static final Item PROGRAMMABLE_DATA_EXTRACTOR = register(FactoryBlocks.PROGRAMMABLE_DATA_EXTRACTOR);
    public static final Item DATA_MEMORY = register("data_memory", (settings) -> new DataMemoryBlockItem(FactoryBlocks.DATA_MEMORY, settings.useBlockDescriptionPrefix()));
    public static final Item NIXIE_TUBE_CONTROLLER = register(FactoryBlocks.NIXIE_TUBE_CONTROLLER);
    public static final Item GAUGE = register(FactoryBlocks.GAUGE);
    public static final Item HOLOGRAM_PROJECTOR = register(FactoryBlocks.HOLOGRAM_PROJECTOR);
    public static final Item WIRELESS_REDSTONE_RECEIVER = register(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
    public static final Item WIRELESS_REDSTONE_TRANSMITTER = register(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
    public static final Item PORTABLE_REDSTONE_TRANSMITTER = register("portable_redstone_transmitter",
            settings -> new WirelessRedstoneTransmitterItem(settings.stacksTo(1)
                    .component(FactoryDataComponents.REMOTE_KEYS, new Pair<>(ItemStack.EMPTY, ItemStack.EMPTY))));

    public static final PunchCardItem PUNCH_CARD = register("punch_card", PunchCardItem::new);

    public static final CableItem CABLE = register("cable", (settings) -> new CableItem(FactoryBlocks.CABLE, AbstractCableBlock.DEFAULT_COLOR, settings.useBlockDescriptionPrefix()));
    public static final Item GATED_CABLE = register(FactoryBlocks.GATED_CABLE);
    public static final ColoredDownsampledBlockItem LAMP = registerColored(FactoryBlocks.LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_LAMP = registerColored(FactoryBlocks.INVERTED_LAMP, -1);
    public static final ColoredDownsampledBlockItem CAGED_LAMP = registerColored(FactoryBlocks.CAGED_LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_CAGED_LAMP = registerColored(FactoryBlocks.INVERTED_CAGED_LAMP, -1);
    public static final ColoredDownsampledBlockItem FIXTURE_LAMP = registerColored(FactoryBlocks.FIXTURE_LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_FIXTURE_LAMP = registerColored(FactoryBlocks.INVERTED_FIXTURE_LAMP, -1);
    public static final Item STEEL_BUTTON = register(FactoryBlocks.STEEL_BUTTON);
    public static final Item ELECTRIC_MOTOR = register(FactoryBlocks.ELECTRIC_MOTOR);
    public static final Item ELECTRIC_GENERATOR = register(FactoryBlocks.ELECTRIC_GENERATOR);
    public static final Item WORKBENCH = register(FactoryBlocks.WORKBENCH);
    public static final Item BLUEPRINT_WORKBENCH = register(FactoryBlocks.BLUEPRINT_WORKBENCH);
    public static final Item MOLDMAKING_TABLE = register(FactoryBlocks.MOLDMAKING_TABLE);
    public static final Item ARTIFICIAL_DYE = register("artificial_dye", ArtificialDyeItem::new);
    public static final Item DYNAMITE = register("dynamite", settings -> new DynamiteItem(settings.stacksTo(16)));
    public static final Item STICKY_DYNAMITE = register("sticky_dynamite", settings -> new DynamiteItem(settings.stacksTo(16)));
    public static final Item INVERTED_REDSTONE_LAMP = register(FactoryBlocks.INVERTED_REDSTONE_LAMP);
    public static final Item TINY_POTATO_SPRING = register(FactoryBlocks.TINY_POTATO_SPRING, settings -> settings.equippableUnswappable(EquipmentSlot.HEAD));
    public static final Item GOLDEN_TINY_POTATO_SPRING = register(FactoryBlocks.GOLDEN_TINY_POTATO_SPRING, settings -> settings.equippableUnswappable(EquipmentSlot.HEAD));
    public static final Item EXPERIENCE_BUCKET = register("experience_bucket", settings -> new SimplePolymerItem(settings.stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final Item SLIME_BUCKET = register("slime_bucket", settings -> new SimplePolymerItem(settings.stacksTo(1).craftRemainder(Items.BUCKET)));
    public static final Item HONEY_BUCKET = register("honey_bucket", settings -> new SimplePolymerItem(settings.craftRemainder(Items.BUCKET)
            .usingConvertsTo(Items.BUCKET)
            .food(new FoodProperties.Builder().nutrition(18).saturationModifier(0.2F).build(),
                    Consumables.defaultDrink().consumeSeconds(8.0F).sound(SoundEvents.HONEY_DRINK)
                            .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.POISON))
                            .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.WITHER))
                            .onConsume(new RemoveStatusEffectsConsumeEffect(MobEffects.HUNGER))
                            .build()
            ).stacksTo(1)));

    public static final Item CRISPY_HONEY = register("crispy_honey", settings -> new SimplePolymerItem(settings
            .food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build(), Consumables.defaultFood().consumeSeconds(0.8F).build())));
    public static final Item HONEYED_APPLE = register("honeyed_apple", settings -> new SimplePolymerItem(settings
            .food(new FoodProperties.Builder().nutrition(7).saturationModifier(1.5f).build())));
    public static final Item CRUSHED_RAW_IRON = register("crushed_raw_iron", SimplePolymerItem::new);
    public static final Item CRUSHED_RAW_COPPER = register("crushed_raw_copper", SimplePolymerItem::new);
    public static final Item CRUSHED_RAW_GOLD = register("crushed_raw_gold", SimplePolymerItem::new);
    public static final Item SPRAY_CAN = register("spray_can", settings -> new DyeSprayItem(settings.stacksTo(1)));
    public static final Item PIPE = register("pipe", settings -> new PipeItem(FactoryBlocks.PIPE, settings.useBlockDescriptionPrefix()));
    public static final Item FILTERED_PIPE = register(FactoryBlocks.FILTERED_PIPE);
    public static final Item REDSTONE_VALVE_PIPE = register(FactoryBlocks.REDSTONE_VALVE_PIPE);
    public static final Item PUMP = register(FactoryBlocks.PUMP);
    public static final Item NOZZLE = register(FactoryBlocks.NOZZLE);
    public static final Item DRAIN = register(FactoryBlocks.DRAIN);
    public static final Item MECHANICAL_DRAIN = register(FactoryBlocks.MECHANICAL_DRAIN);
    public static final Item MECHANICAL_SPOUT = register(FactoryBlocks.MECHANICAL_SPOUT);
    public static final Item CREATIVE_DRAIN = register(FactoryBlocks.CREATIVE_DRAIN);
    public static final Item FLUID_TANK = register(FactoryBlocks.FLUID_TANK);
    public static final Item PORTABLE_FLUID_TANK = register(FactoryBlocks.PORTABLE_FLUID_TANK,
            (s) -> s.stacksTo(1).component(FactoryDataComponents.FLUID, FluidComponent.empty(PortableFluidTankBlockEntity.CAPACITY)));

    public static final PressureFluidGun PRESSURE_FLUID_GUN = register("pressure_fluid_gun", settings -> new PressureFluidGun(
            settings.stacksTo(1).enchantable(5).repairable(COPPER_PLATE).durability(800)));

    public static final Item ITEM_PACKER = register(FactoryBlocks.ITEM_PACKER);

    public static final SpoutMolds INGOT_MOLD = SpoutMolds.create("ingot");
    public static final SpoutMolds NUGGET_MOLD = SpoutMolds.create("nugget");
    public static final SpoutMolds PIPE_MOLD = SpoutMolds.create("pipe");
    public static final SpoutMolds BOTTLE_MOLD = SpoutMolds.create("bottle");
    public static final SpoutMolds THROWABLE_BOTTLE_MOLD = SpoutMolds.create("throwable_bottle");
    public static final SpoutMolds BRITTLE_BOTTLE_MOLD = SpoutMolds.create("brittle_bottle");
    public static final SpoutMolds CHAIN_MOLD = SpoutMolds.create("chain");


    public static final List<SpoutMolds> MOLDS = List.of(INGOT_MOLD, NUGGET_MOLD, PIPE_MOLD, BOTTLE_MOLD, THROWABLE_BOTTLE_MOLD, BRITTLE_BOTTLE_MOLD, CHAIN_MOLD);

    public static void register() {
        FuelRegistryEvents.BUILD.register(((builder, context) -> {
            builder.add(SAW_DUST, (int) (context.baseSmeltTime() * 0.3));
            builder.add(WOODEN_PLATE, (int) (context.baseSmeltTime() * 0.6));
            builder.add(COAL_DUST, (int) (context.baseSmeltTime() * 0.8));
        }));

        BuiltInRegistries.ITEM.addAlias(id("copper_nugget"), Identifier.parse("copper_nugget"));


        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.fromNamespaceAndPath(ModInit.ID, "a_group"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, -1)
                .icon(WINDMILL_SAIL::getDefaultInstance)
                .title(Component.translatable("itemgroup." + ModInit.ID))
                .displayItems(((context, entries) -> {
                    entries.accept(WRENCH);

                    // Rotational machines (tier 1)

                    // Rotation transmission
                    entries.accept(AXLE);
                    entries.accept(GEARBOX);
                    entries.accept(CLUTCH);
                    entries.accept(CHAIN_DRIVE);
                    entries.accept(Items.IRON_CHAIN);
                    entries.accept(STEEL_GEAR);
                    entries.accept(LARGE_STEEL_GEAR);

                    // Rotation Generation
                    entries.accept(HAND_CRANK);
                    entries.accept(WINDMILL_SAIL);
                    entries.accept(STEAM_ENGINE);

                    // Item Movement/Storage
                    entries.accept(CONVEYOR);
                    entries.accept(STICKY_CONVEYOR);
                    entries.accept(FAN);
                    entries.accept(METAL_GRID);
                    entries.accept(FUNNEL);
                    entries.accept(SLOT_AWARE_FUNNEL);
                    entries.accept(SPLITTER);
                    entries.accept(CONTAINER);
                    entries.accept(ITEM_PACKER);
                    entries.accept(ITEM_FILTER);

                    // Rotation other?
                    entries.accept(EJECTOR);
                    entries.accept(TURNTABLE);

                    // Crafting/Machines
                    entries.accept(WORKBENCH);
                    entries.accept(BLUEPRINT_WORKBENCH);
                    entries.accept(GRINDER);
                    entries.accept(PRESS);
                    entries.accept(MIXER);
                    entries.accept(CRAFTER);
                    entries.accept(MINER);
                    entries.accept(PLACER);
                    entries.accept(PLANTER);

                    // Fluids
                    entries.accept(PIPE);
                    entries.accept(FILTERED_PIPE);
                    entries.accept(REDSTONE_VALVE_PIPE);
                    entries.accept(PUMP);
                    entries.accept(DRAIN);
                    entries.accept(MECHANICAL_DRAIN);
                    entries.accept(MECHANICAL_SPOUT);
                    entries.accept(FLUID_TANK);
                    entries.accept(PORTABLE_FLUID_TANK);
                    entries.accept(NOZZLE);

                    entries.accept(PRIMITIVE_SMELTERY);
                    entries.accept(SMELTERY_CORE);
                    entries.accept(CASTING_TABLE);
                    entries.accept(MOLDMAKING_TABLE);
                    entries.accept(SMELTERY_FAUCED);

                    // Data
                    entries.accept(CABLE);
                    entries.accept(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.RED)), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    entries.accept(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.GREEN)), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    entries.accept(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.BLUE)), CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    entries.accept(GATED_CABLE);
                    entries.accept(REDSTONE_OUTPUT);
                    entries.accept(REDSTONE_INPUT);
                    entries.accept(TACHOMETER);
                    entries.accept(STRESSOMETER);
                    entries.accept(ITEM_COUNTER);
                    entries.accept(ITEM_READER);
                    entries.accept(BLOCK_OBSERVER);
                    entries.accept(TEXT_INPUT);
                    entries.accept(DIGITAL_CLOCK);
                    entries.accept(RECORD_PLAYER);
                    entries.accept(GAUGE);
                    entries.accept(NIXIE_TUBE_CONTROLLER);
                    entries.accept(NIXIE_TUBE);
                    entries.accept(HOLOGRAM_PROJECTOR);
                    entries.accept(SPEAKER);
                    entries.accept(DATA_MEMORY);
                    entries.accept(DATA_EXTRACTOR);
                    entries.accept(PROGRAMMABLE_DATA_EXTRACTOR);
                    entries.accept(DATA_COMPARATOR);
                    entries.accept(ARITHMETIC_OPERATOR);

                    // Redstone?
                    entries.accept(WIRELESS_REDSTONE_RECEIVER);
                    entries.accept(WIRELESS_REDSTONE_TRANSMITTER);
                    entries.accept(PORTABLE_REDSTONE_TRANSMITTER);

                    // Electrical machines (tier 2)


                    // Rest
                    entries.accept(INVERTED_REDSTONE_LAMP);
                    entries.accept(ColoredItem.stack(LAMP, 1, DyeColor.WHITE));
                    entries.accept(ColoredItem.stack(INVERTED_LAMP, 1, DyeColor.WHITE));
                    entries.accept(ColoredItem.stack(CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.accept(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.accept(ColoredItem.stack(FIXTURE_LAMP, 1, DyeColor.WHITE));
                    entries.accept(ColoredItem.stack(INVERTED_FIXTURE_LAMP, 1, DyeColor.WHITE));
                    entries.accept(STEEL_BUTTON);
                    entries.accept(TINY_POTATO_SPRING);
                    entries.accept(GOLDEN_TINY_POTATO_SPRING);

                    // Tools
                    entries.accept(DYNAMITE);
                    entries.accept(STICKY_DYNAMITE);
                    entries.accept(PRESSURE_FLUID_GUN);
                    entries.accept(CHAIN_LIFT);
                    entries.accept(SPRAY_CAN);

                    // Food
                    entries.accept(CRISPY_HONEY);
                    entries.accept(HONEYED_APPLE);

                    // Other Items
                    entries.accept(HONEY_BUCKET);
                    entries.accept(SLIME_BUCKET);
                    entries.accept(EXPERIENCE_BUCKET);
                    entries.accept(THROWABLE_GLASS_BOTTLE);
                    entries.accept(LINGERING_THROWABLE_GLASS_BOTTLE);
                    entries.accept(BRITTLE_GLASS_BOTTLE);

                    // Generic Materials
                    entries.accept(SAW_DUST);
                    entries.accept(COAL_DUST);
                    entries.accept(NETHERRACK_DUST);
                    entries.accept(ENDER_DUST);
                    entries.accept(CRUSHED_RAW_IRON);
                    entries.accept(CRUSHED_RAW_COPPER);
                    entries.accept(CRUSHED_RAW_GOLD);
                    entries.accept(STEEL_ALLOY_MIXTURE);
                    entries.accept(STEEL_INGOT);
                    entries.accept(STEEL_NUGGET);
                    entries.accept(STEEL_BLOCK);
                    entries.accept(STEEL_PLATE);
                    entries.accept(COPPER_PLATE);
                    entries.accept(WOODEN_PLATE);
                    entries.accept(TREATED_DRIED_KELP);
                    entries.accept(STRING_MESH);
                    entries.accept(ENDER_INFUSED_AMETHYST_SHARD);
                    entries.accept(STEEL_MACHINE_GEARBOX);
                    entries.accept(REDSTONE_CHIP);
                    entries.accept(INTEGRATED_CIRCUIT);

                    // Mold stuff
                    for (var mold : MOLDS) entries.accept(mold.clay());
                    for (var mold : MOLDS) entries.accept(mold.hardened());
                    for (var mold : MOLDS) entries.accept(mold.mold());


                    // Fancy dyes
                    entries.accept(ArtificialDyeItem.of(0xFF0000));
                    entries.accept(ArtificialDyeItem.of(0xFFFF00));
                    entries.accept(ArtificialDyeItem.of(0x00FF00));
                    entries.accept(ArtificialDyeItem.of(0x00FFFF));
                    entries.accept(ArtificialDyeItem.of(0x0000FF));
                    entries.accept(ArtificialDyeItem.of(0xFF00FF));

                    // Enchantments
                    addEnchantment(context, entries, FactoryEnchantments.IGNORE_MOVEMENT);

                    //entries.add();
                    //entries.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(, 1)));

                    // Creative
                    entries.accept(CREATIVE_MOTOR);
                    entries.accept(CREATIVE_CONTAINER);
                    entries.accept(CREATIVE_DRAIN);
                })).build()
        );

        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.fromNamespaceAndPath(ModInit.ID, "variants"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, -1)
                .icon(() -> ColoredItem.stack(CABLE, 1, DyeColor.RED))
                .title(Component.translatable("itemgroup." + ModInit.ID + ".variants"))
                .displayItems(((context, entries) -> {

                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        var stack = WINDMILL_SAIL.getDefaultInstance();
                        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(DyeColorExtra.getColor(dye)));
                        entries.accept(stack);
                    }

                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(INVERTED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(FIXTURE_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        entries.accept(ColoredItem.stack(INVERTED_FIXTURE_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var fluid : FactoryRegistries.FLUID_TYPES) {
                        if (fluid.defaultData() == Unit.INSTANCE) {
                            var stack = PORTABLE_FLUID_TANK.getDefaultInstance();
                            stack.update(FactoryDataComponents.FLUID, FluidComponent.DEFAULT, x -> x.with(fluid.defaultInstance(), x.capacity()));
                            entries.accept(stack);
                        }
                    }

                    for (var potion : BuiltInRegistries.POTION.asHolderIdMap()) {
                        if (potion != Potions.WATER) {
                            var stack = PORTABLE_FLUID_TANK.getDefaultInstance();
                            stack.update(FactoryDataComponents.FLUID, FluidComponent.DEFAULT, x -> x.with(FactoryFluids.getPotion(potion), x.capacity()));
                            entries.accept(stack);
                        }
                    }

                    for (var potion : BuiltInRegistries.POTION.asHolderIdMap()) {
                        var stack = BRITTLE_POTION.getDefaultInstance();
                        stack.update(DataComponents.POTION_CONTENTS, PotionContents.EMPTY, x -> x.withPotion(potion));
                        entries.accept(stack);
                    }

                    for (var dye : FactoryUtil.COLORS_CREATIVE) {
                        var x = ColoredItem.stack(SPRAY_CAN, 1, DyeColorExtra.getColor(dye));
                        x.set(FactoryDataComponents.USES_LEFT, 128);
                        entries.accept(x);
                    }

                })).build()
        );

        if (ModInit.DEV_MODE) {
            PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.fromNamespaceAndPath(ModInit.ID, "experimental"), CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, -1)
                    .icon(FactoryDebugItems.DEBUG_PIPE_FLOW::getDefaultInstance)
                    .title(Component.translatable("itemgroup." + ModInit.ID + ".experimental"))
                    .displayItems(((context, entries) -> {
                        FactoryDebugItems.addItemGroup(context, entries);
                        entries.accept(ELECTRIC_GENERATOR, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        entries.accept(ELECTRIC_MOTOR, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        entries.accept(CLIPBOARD, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        entries.accept(SMELTERY, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    })).build()
            );
        }

        AttackBlockCallback.EVENT.register(WRENCH::handleBlockAttack);
        AttackEntityCallback.EVENT.register(WRENCH::handleEntityAttack);
        UseEntityCallback.EVENT.register(WRENCH::handleEntityUse);

        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(PortableFluidTankBlockItem::createItemAsset);
    }

    private static void addEnchantment(CreativeModeTab.ItemDisplayParameters context, CreativeModeTab.Output entries, ResourceKey<Enchantment> ignoreMovement) {
        context.holders().lookupOrThrow(Registries.ENCHANTMENT).get(FactoryEnchantments.IGNORE_MOVEMENT).ifPresent(x -> {
            var item = Items.ENCHANTED_BOOK.getDefaultInstance();
            var b = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            b.upgrade(x, 1);
            item.set(DataComponents.STORED_ENCHANTMENTS, b.toImmutable());
            entries.accept(item);
        });
    }

    public static <T extends Item> T register(Identifier id, Function<Item.Properties, T> function) {
        var item = function.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    public static <T extends Item> T register(String path, Function<Item.Properties, T> function) {
        return register(Identifier.fromNamespaceAndPath(ModInit.ID, path), function);
    }

    public static <E extends Block & PolymerBlock> ColoredDownsampledBlockItem registerColored(E block, int color) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        var item = new ColoredDownsampledBlockItem(block, color, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    public static <E extends Block & PolymerBlock> FactoryBlockItem register(E block) {
        return register(block, (s) -> {});
    }
    public static <E extends Block & PolymerBlock> FactoryBlockItem register(E block, Consumer<Item.Properties> settingsConsumer) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        FactoryBlockItem item;
        var settings = new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix();
        settingsConsumer.accept(settings);

        if (block instanceof MultiBlock multiBlock) {
            item = new MultiBlockItem(multiBlock, settings);
        } else if (block instanceof AbstractCableBlock cableBlock) {
            item = new CabledBlockItem((AbstractCableBlock & PolymerBlock) cableBlock, settings);
        } else if (block instanceof PortableFluidTankBlock) {
            item = new PortableFluidTankBlockItem(block, settings);
        } else if (block instanceof AxleBlock axleBlock && axleBlock.placesLikeAxle()) {
            item = new AxleItem(axleBlock, settings);
        } else if (block instanceof BlockWithTooltip) {
            item = new TooltippedBlockItem(block, settings);
        } else {
            item = new FactoryBlockItem(block, settings);
        }
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    static {
        FactoryDebugItems.DEBUG_PIPE_FLOW.getName();
    }
}
