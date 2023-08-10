package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.CreativeMotorBlock;
import eu.pb4.polyfactory.block.creative.ItemGeneratorBlock;
import eu.pb4.polyfactory.block.electric.CablePlateBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.GrinderBlock;
import eu.pb4.polyfactory.block.mechanical.machines.MinerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.MixerBlock;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.PressBlock;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlock;
import eu.pb4.polyfactory.block.mechanical.source.HandCrankBlock;
import eu.pb4.polyfactory.block.mechanical.source.SteamEngineBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.multiblock.MultiBlock;
import eu.pb4.polyfactory.block.other.ContainerBlock;
import eu.pb4.polyfactory.block.other.GreenScreenBlock;
import eu.pb4.polyfactory.block.other.SelectivePassthroughBlock;
import eu.pb4.polyfactory.block.other.NixieTubeBlock;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.Instrument;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class FactoryBlocks {
    public static final ConveyorBlock CONVEYOR = register("conveyor", new ConveyorBlock(Block.Settings.create().hardness(3).nonOpaque()));
    public static final ConveyorBlock STICKY_CONVEYOR = register("sticky_conveyor", new ConveyorBlock(Block.Settings.create().hardness(3).nonOpaque()));
    public static final ElectricMotorBlock MOTOR = register("electric_motor", new ElectricMotorBlock(Block.Settings.create().hardness(2)));
    public static final FunnelBlock FUNNEL = register("funnel", new FunnelBlock(Block.Settings.copy(Blocks.SPRUCE_TRAPDOOR).nonOpaque()));
    public static final SplitterBlock SPLITTER = register("splitter", new SplitterBlock(Block.Settings.create().mapColor(MapColor.STONE_GRAY).instrument(Instrument.BASEDRUM).requiresTool().strength(3.3F).nonOpaque()));
    public static final FanBlock FAN = register("fan", new FanBlock(Block.Settings.create().nonOpaque().hardness(1)));
    public static final SelectivePassthroughBlock METAL_GRID = register("metal_grid", new SelectivePassthroughBlock(Block.Settings.copy(Blocks.IRON_BLOCK).strength(4.0F, 3.0F).nonOpaque()));
    public static final HandCrankBlock HAND_CRANK = register("hand_crank", new HandCrankBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final SteamEngineBlock STEAM_ENGINE = register("steam_engine", new SteamEngineBlock(Block.Settings.copy(SPLITTER).strength(4F).nonOpaque()));
    public static final GrinderBlock GRINDER = register("grinder", new GrinderBlock(Block.Settings.copy(SPLITTER)));
    public static final PressBlock PRESS = register("press", new PressBlock(Block.Settings.copy(SPLITTER)));
    public static final MixerBlock MIXER = register("mixer", new MixerBlock(Block.Settings.copy(SPLITTER)));
    public static final MinerBlock MINER = register("miner", new MinerBlock(Block.Settings.copy(SPLITTER)));
    public static final AxleBlock AXLE = register("axle", new AxleBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final GearboxBlock GEARBOX = register("gearbox", new GearboxBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final WindmillBlock WINDMILL = register("windmill", new WindmillBlock(Block.Settings.copy(Blocks.STRIPPED_OAK_WOOD).strength(2.5F).nonOpaque()));
    public static final ContainerBlock CONTAINER = register("wooden_container", new ContainerBlock(Block.Settings.copy(Blocks.CHEST).nonOpaque()));
    public static final NixieTubeBlock NIXIE_TUBE = register("nixie_tube", new NixieTubeBlock(Block.Settings.copy(Blocks.GLASS).nonOpaque()));


    public static final CreativeMotorBlock CREATIVE_MOTOR = register("creative_motor", new CreativeMotorBlock(AbstractBlock.Settings.create().strength(-1, -1).nonOpaque()));
    public static final ItemGeneratorBlock ITEM_GENERATOR = register("item_generator", new ItemGeneratorBlock(AbstractBlock.Settings.create().strength(-1, -1)));
    public static final RotationalDebugBlock ROTATION_DEBUG = register("rot_debug", new RotationalDebugBlock(AbstractBlock.Settings.create().strength(-1, -1)));
    public static final GreenScreenBlock GREEN_SCREEN = register("green_screen", new GreenScreenBlock(AbstractBlock.Settings.copy(Blocks.GREEN_WOOL)));


    public static void register() {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var state = world.getBlockState(pos);

            if (state.getBlock() instanceof AttackableBlock attackableBlock && hand == Hand.MAIN_HAND) {
                return attackableBlock.onPlayerAttack(state, player, world, pos, direction);
            }

            return ActionResult.PASS;
        });
    }


    public static <T extends Block> T register(String path, T item) {
        return Registry.register(Registries.BLOCK, new Identifier(ModInit.ID, path), item);
    }
}
