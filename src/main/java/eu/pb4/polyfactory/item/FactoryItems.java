package eu.pb4.polyfactory.item;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.item.FactoryBlockItem;
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
import eu.pb4.polyfactory.item.wrench.WrenchItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.item.consume.RemoveEffectsConsumeEffect;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;

import java.util.function.Consumer;
import java.util.function.Function;

public class FactoryItems {
    // Util
    public static final Item MOD_ICON = register("mod_icon", SimplePolymerItem::new);
    public static final Item FLUID_MODEL = register("fluid_model", FluidModelItem::new);
    // Actual items
    public static final WrenchItem WRENCH = register("wrench", settings -> new WrenchItem(settings.maxCount(1)));
    public static final Item CONVEYOR = register(FactoryBlocks.CONVEYOR);
    public static final Item STICKY_CONVEYOR = register(FactoryBlocks.STICKY_CONVEYOR);
    public static final Item FUNNEL = register(FactoryBlocks.FUNNEL);
    public static final Item SPLITTER = register(FactoryBlocks.SPLITTER);
    public static final Item FAN = register(FactoryBlocks.FAN);
    public static final Item HAND_CRANK = register(FactoryBlocks.HAND_CRANK);
    public static final Item STEAM_ENGINE = register(FactoryBlocks.STEAM_ENGINE);
    public static final Item GRINDER = register(FactoryBlocks.GRINDER);
    public static final Item PRESS = register(FactoryBlocks.PRESS);
    public static final Item CRAFTER = register(FactoryBlocks.CRAFTER);
    public static final Item MIXER = register(FactoryBlocks.MIXER);
    public static final Item MINER = register(FactoryBlocks.MINER);
    public static final Item PLACER = register(FactoryBlocks.PLACER);
    public static final Item PLANTER = register(FactoryBlocks.PLANTER);
    public static final FactoryBlockItem AXLE = register(FactoryBlocks.AXLE);
    public static final Item TURNTABLE = register(FactoryBlocks.TURNTABLE);
    public static final Item GEARBOX = register(FactoryBlocks.GEARBOX);
    public static final Item CLUTCH = register(FactoryBlocks.CLUTCH);
    public static final Item CONTAINER = register( FactoryBlocks.CONTAINER);
    public static final Item NIXIE_TUBE = register(FactoryBlocks.NIXIE_TUBE);
    public static final WindmillSailItem WINDMILL_SAIL = register("windmill_sail", WindmillSailItem::new);
    public static final Item METAL_GRID = register(FactoryBlocks.METAL_GRID);
    public static final Item SAW_DUST = register("saw_dust", SimplePolymerItem::new);
    public static final Item COAL_DUST = register("coal_dust", SimplePolymerItem::new);
    public static final Item NETHERRACK_DUST = register("netherrack_dust", SimplePolymerItem::new);
    public static final Item ENDER_DUST = register("ender_dust", SimplePolymerItem::new);
    public static final Item ENDER_INFUSED_AMETHYST_SHARD = register("ender_infused_amethyst_shard", SimplePolymerItem::new);
    public static final Item STEEL_ALLOY_MIXTURE = register("steel_alloy_mixture", SimplePolymerItem::new);
    public static final Item STEEL_INGOT = register("steel_ingot", SimplePolymerItem::new);
    public static final Item STEEL_PLATE = register("steel_plate", SimplePolymerItem::new);
    public static final Item COPPER_PLATE = register("copper_plate", SimplePolymerItem::new);
    public static final Item BRITTLE_GLASS_BOTTLE = register("brittle_glass_bottle", SimplePolymerItem::new);
    public static final Item BRITTLE_POTION = register("brittle_potion", settings -> new BrittlePotionItem(settings.component(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).component(DataComponentTypes.CONSUMABLE, ConsumableComponents.drink().finishSound(SoundEvents.ITEM_OMINOUS_BOTTLE_DISPOSE).build()).maxCount(1)));
    public static final Item THROWABLE_GLASS_BOTTLE = register("throwable_glass_bottle", SimplePolymerItem::new);
    public static final Item LINGERING_THROWABLE_GLASS_BOTTLE = register("lingering_throwable_glass_bottle", SimplePolymerItem::new);
    public static final Item STEEL_GEAR = register("steel_gear", (settings) -> new GearItem(FactoryBlocks.AXLE_WITH_GEAR, settings));
    public static final Item LARGE_STEEL_GEAR = register("large_steel_gear", (settings) -> new GearItem(FactoryBlocks.AXLE_WITH_LARGE_GEAR, settings));
    public static final Item GENERIC_MACHINE_PART = register("generic_machine_part", SimplePolymerItem::new);
    public static final Item WOODEN_PLATE = register("wooden_plate", SimplePolymerItem::new);
    public static final Item TREATED_DRIED_KELP = register("treated_dried_kelp", SimplePolymerItem::new);
    public static final Item INTEGRATED_CIRCUIT = register("integrated_circuit", SimplePolymerItem::new);
    public static final Item REDSTONE_CHIP = register("redstone_chip", SimplePolymerItem::new);

    public static final Item ITEM_FILTER = register("item_filter", FilterItem::new);

    public static final Item CREATIVE_MOTOR = register(FactoryBlocks.CREATIVE_MOTOR);
    public static final Item CREATIVE_CONTAINER = register(FactoryBlocks.CREATIVE_CONTAINER);
    public static final Item TACHOMETER = register(FactoryBlocks.TACHOMETER);
    public static final Item STRESSOMETER = register(FactoryBlocks.STRESSOMETER);
    public static final Item ITEM_COUNTER = register(FactoryBlocks.ITEM_COUNTER);
    public static final Item REDSTONE_INPUT = register(FactoryBlocks.REDSTONE_INPUT);
    public static final Item REDSTONE_OUTPUT = register(FactoryBlocks.REDSTONE_OUTPUT);
    public static final Item ITEM_READER = register(FactoryBlocks.ITEM_READER);
    public static final Item BLOCK_OBSERVER = register(FactoryBlocks.BLOCK_OBSERVER);
    public static final Item ARITHMETIC_OPERATOR = register(FactoryBlocks.ARITHMETIC_OPERATOR);
    public static final Item DATA_MEMORY = register("data_memory", (settings) -> new DataMemoryBlockItem(FactoryBlocks.DATA_MEMORY, settings.useBlockPrefixedTranslationKey()));
    public static final Item NIXIE_TUBE_CONTROLLER = register(FactoryBlocks.NIXIE_TUBE_CONTROLLER);
    public static final Item HOLOGRAM_PROJECTOR = register(FactoryBlocks.HOLOGRAM_PROJECTOR);
    public static final Item WIRELESS_REDSTONE_RECEIVER = register(FactoryBlocks.WIRELESS_REDSTONE_RECEIVER);
    public static final Item WIRELESS_REDSTONE_TRANSMITTER = register(FactoryBlocks.WIRELESS_REDSTONE_TRANSMITTER);
    public static final Item PORTABLE_REDSTONE_TRANSMITTER = register("portable_redstone_transmitter",
            settings -> new WirelessRedstoneTransmitterItem(settings.maxCount(1)
                    .component(FactoryDataComponents.REMOTE_KEYS, new Pair<>(ItemStack.EMPTY, ItemStack.EMPTY))));

    public static final CableItem CABLE = register("cable", (settings) -> new CableItem(FactoryBlocks.CABLE, AbstractCableBlock.DEFAULT_COLOR, settings.useBlockPrefixedTranslationKey()));
    public static final ColoredDownsampledBlockItem LAMP = registerColored(FactoryBlocks.LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_LAMP = registerColored(FactoryBlocks.INVERTED_LAMP, -1);
    public static final ColoredDownsampledBlockItem CAGED_LAMP = registerColored(FactoryBlocks.CAGED_LAMP, -1);
    public static final ColoredDownsampledBlockItem INVERTED_CAGED_LAMP = registerColored(FactoryBlocks.INVERTED_CAGED_LAMP, -1);
    public static final Item STEEL_BUTTON = register(FactoryBlocks.STEEL_BUTTON);
    public static final Item ELECTRIC_MOTOR = register(FactoryBlocks.ELECTRIC_MOTOR);
    public static final Item ELECTRIC_GENERATOR = register(FactoryBlocks.ELECTRIC_GENERATOR);
    public static final Item WORKBENCH = register(FactoryBlocks.WORKBENCH);
    public static final Item ARTIFICIAL_DYE = register("artificial_dye", ArtificialDyeItem::new);
    public static final Item DYNAMITE = register("dynamite", settings -> new DynamiteItem(settings.maxCount(16)));
    public static final Item STICKY_DYNAMITE = register("sticky_dynamite", settings -> new DynamiteItem(settings.maxCount(16)));
    public static final Item INVERTED_REDSTONE_LAMP = register(FactoryBlocks.INVERTED_REDSTONE_LAMP);
    public static final Item TINY_POTATO_SPRING = register(FactoryBlocks.TINY_POTATO_SPRING, settings -> settings.equippableUnswappable(EquipmentSlot.HEAD));
    public static final Item EXPERIENCE_BUCKET = register("experience_bucket", settings -> new SimplePolymerItem(settings.maxCount(1).recipeRemainder(Items.BUCKET)));
    public static final Item SLIME_BUCKET = register("slime_bucket", settings -> new SimplePolymerItem(settings.maxCount(1).recipeRemainder(Items.BUCKET)));
    public static final Item HONEY_BUCKET = register("honey_bucket", settings -> new SimplePolymerItem(settings.recipeRemainder(Items.BUCKET)
            .useRemainder(Items.BUCKET)
            .food(new FoodComponent.Builder().nutrition(18).saturationModifier(0.2F).build(),
                    ConsumableComponents.drink().consumeSeconds(8.0F).sound(SoundEvents.ITEM_HONEY_BOTTLE_DRINK)
                            .consumeEffect(new RemoveEffectsConsumeEffect(StatusEffects.POISON))
                            .consumeEffect(new RemoveEffectsConsumeEffect(StatusEffects.WITHER))
                            .consumeEffect(new RemoveEffectsConsumeEffect(StatusEffects.HUNGER))
                            .build()
            ).maxCount(1)));

    public static final Item CRISPY_HONEY = register("crispy_honey", settings -> new SimplePolymerItem(settings
            .food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.6F).build(), ConsumableComponents.food().consumeSeconds(0.8F).build())));
    public static final Item HONEYED_APPLE = register("honeyed_apple", settings -> new SimplePolymerItem(settings
            .food(new FoodComponent.Builder().nutrition(7).saturationModifier(1.5f).build())));
    public static final Item CRUSHED_RAW_IRON = register("crushed_raw_iron", SimplePolymerItem::new);
    public static final Item CRUSHED_RAW_COPPER = register("crushed_raw_copper", SimplePolymerItem::new);
    public static final Item CRUSHED_RAW_GOLD = register("crushed_raw_gold", SimplePolymerItem::new);
    public static final Item TEMPLATE_BALL = register("template_ball", SimplePolymerItem::new);
    public static final Item SPRAY_CAN = register("spray_can", settings -> new DyeSprayItem(settings.maxCount(1)));
    public static final Item PIPE = register("pipe", settings -> new PipeItem(FactoryBlocks.PIPE, settings.useBlockPrefixedTranslationKey()));
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
            (s) -> s.maxCount(1).component(FactoryDataComponents.FLUID, FluidComponent.empty(PortableFluidTankBlockEntity.CAPACITY)));

    public static final PressureFluidGun PRESSURE_FLUID_GUN = register("pressure_fluid_gun", settings -> new PressureFluidGun(
            settings.maxCount(1).enchantable(5).repairable(COPPER_PLATE).maxDamage(800)));

    public static final Item ITEM_PACKER = register(FactoryBlocks.ITEM_PACKER);

    public static void register() {
        FuelRegistryEvents.BUILD.register(((builder, context) -> {
            builder.add(SAW_DUST, (int) (context.baseSmeltTime() * 0.3));
            builder.add(WOODEN_PLATE, (int) (context.baseSmeltTime() * 0.6));
            builder.add(COAL_DUST, (int) (context.baseSmeltTime() * 0.8));
        }));


        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "a_group"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(WINDMILL_SAIL::getDefaultStack)
                .displayName(Text.translatable("itemgroup." + ModInit.ID))
                .entries(((context, entries) -> {
                    entries.add(WRENCH);

                    // Rotational machines (tier 1)

                    // Rotation transmission
                    entries.add(AXLE);
                    entries.add(GEARBOX);
                    entries.add(CLUTCH);
                    entries.add(STEEL_GEAR);
                    entries.add(LARGE_STEEL_GEAR);

                    // Rotation Generation
                    entries.add(HAND_CRANK);
                    entries.add(WINDMILL_SAIL);
                    entries.add(STEAM_ENGINE);

                    // Item Movement/Storage
                    entries.add(CONVEYOR);
                    entries.add(STICKY_CONVEYOR);
                    entries.add(FAN);
                    entries.add(METAL_GRID);
                    entries.add(FUNNEL);
                    entries.add(SPLITTER);
                    entries.add(CONTAINER);
                    entries.add(ITEM_FILTER);
                    entries.add(ITEM_PACKER);

                    // Rotation other?
                    entries.add(TURNTABLE);

                    // Crafting/Machines
                    entries.add(WORKBENCH);
                    entries.add(GRINDER);
                    entries.add(PRESS);
                    entries.add(MIXER);
                    entries.add(CRAFTER);
                    entries.add(MINER);
                    entries.add(PLACER);
                    entries.add(PLANTER);

                    // Fluids
                    entries.add(PIPE);
                    entries.add(FILTERED_PIPE);
                    entries.add(REDSTONE_VALVE_PIPE);
                    entries.add(PUMP);
                    entries.add(DRAIN);
                    entries.add(MECHANICAL_DRAIN);
                    entries.add(MECHANICAL_SPOUT);
                    entries.add(FLUID_TANK);
                    entries.add(PORTABLE_FLUID_TANK);
                    entries.add(NOZZLE);

                    // Data
                    entries.add(CABLE);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.RED)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.GREEN)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(DyeColor.BLUE)), ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    entries.add(TACHOMETER);
                    entries.add(STRESSOMETER);
                    entries.add(REDSTONE_OUTPUT);
                    entries.add(REDSTONE_INPUT);
                    entries.add(ITEM_COUNTER);
                    entries.add(ITEM_READER);
                    entries.add(BLOCK_OBSERVER);
                    entries.add(NIXIE_TUBE_CONTROLLER);
                    entries.add(NIXIE_TUBE);
                    entries.add(HOLOGRAM_PROJECTOR);
                    entries.add(DATA_MEMORY);
                    entries.add(ARITHMETIC_OPERATOR);

                    // Redstone?
                    entries.add(WIRELESS_REDSTONE_RECEIVER);
                    entries.add(WIRELESS_REDSTONE_TRANSMITTER);
                    entries.add(PORTABLE_REDSTONE_TRANSMITTER);

                    // Electrical machines (tier 2)


                    // Rest
                    entries.add(INVERTED_REDSTONE_LAMP);
                    entries.add(ColoredItem.stack(LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(INVERTED_LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.add(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColor.WHITE));
                    entries.add(STEEL_BUTTON);
                    entries.add(TINY_POTATO_SPRING);

                    // Other items
                    entries.add(DYNAMITE);
                    entries.add(STICKY_DYNAMITE);
                    entries.add(PRESSURE_FLUID_GUN);
                    entries.add(SPRAY_CAN);
                    entries.add(HONEY_BUCKET);
                    entries.add(SLIME_BUCKET);
                    entries.add(EXPERIENCE_BUCKET);
                    entries.add(THROWABLE_GLASS_BOTTLE);
                    entries.add(LINGERING_THROWABLE_GLASS_BOTTLE);
                    entries.add(BRITTLE_GLASS_BOTTLE);
                    entries.add(CRISPY_HONEY);
                    entries.add(HONEYED_APPLE);

                    // Generic Materials
                    entries.add(SAW_DUST);
                    entries.add(COAL_DUST);
                    entries.add(NETHERRACK_DUST);
                    entries.add(ENDER_DUST);
                    entries.add(CRUSHED_RAW_IRON);
                    entries.add(CRUSHED_RAW_COPPER);
                    entries.add(CRUSHED_RAW_GOLD);
                    entries.add(STEEL_ALLOY_MIXTURE);
                    entries.add(STEEL_INGOT);
                    entries.add(STEEL_PLATE);
                    entries.add(COPPER_PLATE);
                    entries.add(WOODEN_PLATE);
                    entries.add(TREATED_DRIED_KELP);
                    entries.add(ENDER_INFUSED_AMETHYST_SHARD);
                    entries.add(GENERIC_MACHINE_PART);
                    entries.add(REDSTONE_CHIP);
                    entries.add(INTEGRATED_CIRCUIT);

                    // Fancy dyes
                    entries.add(ArtificialDyeItem.of(0xFF0000));
                    entries.add(ArtificialDyeItem.of(0xFFFF00));
                    entries.add(ArtificialDyeItem.of(0x00FF00));
                    entries.add(ArtificialDyeItem.of(0x00FFFF));
                    entries.add(ArtificialDyeItem.of(0x0000FF));
                    entries.add(ArtificialDyeItem.of(0xFF00FF));

                    // Enchantments
                    //entries.add(EnchantedBookItem.forEnchantment(new EnchantmentLevelEntry(, 1)));

                    // Creative
                    entries.add(CREATIVE_MOTOR);
                    entries.add(CREATIVE_CONTAINER);
                    entries.add(CREATIVE_DRAIN);
                })).build()
        );

        PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "variants"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                .icon(() -> ColoredItem.stack(CABLE, 1, DyeColor.RED))
                .displayName(Text.translatable("itemgroup." + ModInit.ID + ".variants"))
                .entries(((context, entries) -> {

                    for (var dye : DyeColor.values()) {
                        var stack = WINDMILL_SAIL.getDefaultStack();
                        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(DyeColorExtra.getColor(dye), true));
                        entries.add(stack);
                    }

                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(CABLE, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(INVERTED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }
                    for (var dye : DyeColor.values()) {
                        entries.add(ColoredItem.stack(INVERTED_CAGED_LAMP, 1, DyeColorExtra.getColor(dye)));
                    }

                    for (var fluid : FactoryRegistries.FLUID_TYPES) {
                        if (fluid.defaultData() == Unit.INSTANCE) {
                            var stack = PORTABLE_FLUID_TANK.getDefaultStack();
                            stack.apply(FactoryDataComponents.FLUID, FluidComponent.DEFAULT, x -> x.with(fluid.defaultInstance(), x.capacity()));
                            entries.add(stack);
                        }
                    }

                    for (var potion : Registries.POTION.getIndexedEntries()) {
                        if (potion != Potions.WATER) {
                            var stack = PORTABLE_FLUID_TANK.getDefaultStack();
                            stack.apply(FactoryDataComponents.FLUID, FluidComponent.DEFAULT, x -> x.with(FactoryFluids.getPotion(potion), x.capacity()));
                            entries.add(stack);
                        }
                    }

                    for (var potion : Registries.POTION.getIndexedEntries()) {
                        var stack = BRITTLE_POTION.getDefaultStack();
                        stack.apply(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT, x -> x.with(potion));
                        entries.add(stack);
                    }

                    for (var dye : DyeColor.values()) {
                        var x = ColoredItem.stack(SPRAY_CAN, 1, DyeColorExtra.getColor(dye));
                        x.set(FactoryDataComponents.USES_LEFT, 128);
                        entries.add(x);
                    }

                })).build()
        );

        if (ModInit.DEV_MODE) {
            PolymerItemGroupUtils.registerPolymerItemGroup(Identifier.of(ModInit.ID, "experimental"), ItemGroup.create(ItemGroup.Row.BOTTOM, -1)
                    .icon(FactoryDebugItems.DEBUG_PIPE_FLOW::getDefaultStack)
                    .displayName(Text.translatable("itemgroup." + ModInit.ID + ".experimental"))
                    .entries(((context, entries) -> {
                        FactoryDebugItems.addItemGroup(context, entries);
                        entries.add(ELECTRIC_GENERATOR, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                        entries.add(ELECTRIC_MOTOR, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
                    })).build()
            );
        }

        AttackBlockCallback.EVENT.register(WRENCH::handleBlockAttack);

        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(PortableFluidTankBlockItem::createItemAsset);
    }


    public static <T extends Item> T register(String path, Function<Item.Settings, T> function) {
        var id = Identifier.of(ModInit.ID, path);
        var item = function.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        Registry.register(Registries.ITEM, id, item);
        return item;
    }

    public static <E extends Block & PolymerBlock> ColoredDownsampledBlockItem registerColored(E block, int color) {
        var id = Registries.BLOCK.getId(block);
        var item = new ColoredDownsampledBlockItem(block, color, new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, id, item);
        return item;
    }

    public static <E extends Block & PolymerBlock> FactoryBlockItem register(E block) {
        return register(block, (s) -> {});
    }
    public static <E extends Block & PolymerBlock> FactoryBlockItem register(E block, Consumer<Item.Settings> settingsConsumer) {
        var id = Registries.BLOCK.getId(block);
        FactoryBlockItem item;
        var settings = new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)).useBlockPrefixedTranslationKey();
        settingsConsumer.accept(settings);

        if (block instanceof MultiBlock multiBlock) {
            item = new MultiBlockItem(multiBlock, settings);
        } else if (block instanceof AbstractCableBlock cableBlock) {
            item = new CabledBlockItem((AbstractCableBlock & PolymerBlock) cableBlock, settings);
        } else if (block instanceof PortableFluidTankBlock) {
            item = new PortableFluidTankBlockItem(block, settings);
        } else {
            item = new FactoryBlockItem(block, settings);
        }
        Registry.register(Registries.ITEM, id, item);
        return item;
    }

    static {
        FactoryDebugItems.DEBUG_PIPE_FLOW.getName();
    }
}
