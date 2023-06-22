package eu.pb4.polyfactory.block;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.creative.ItemGeneratorBlock;
import eu.pb4.polyfactory.block.electric.CablePlateBlock;
import eu.pb4.polyfactory.block.machines.GrinderBlock;
import eu.pb4.polyfactory.block.machines.MinerBlock;
import eu.pb4.polyfactory.block.machines.PressBlock;
import eu.pb4.polyfactory.block.mechanical.*;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlock;
import eu.pb4.polyfactory.block.electric.ElectricMotorBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.FunnelBlock;
import eu.pb4.polyfactory.block.mechanical.conveyor.SplitterBlock;
import eu.pb4.polyfactory.block.storage.ContainerBlock;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class FactoryBlocks {
    public static final ConveyorBlock CONVEYOR = register("conveyor", new ConveyorBlock(Block.Settings.create().hardness(2).nonOpaque()));
    public static final ConveyorBlock STICKY_CONVEYOR = register("sticky_conveyor", new ConveyorBlock(Block.Settings.create().hardness(2).nonOpaque()));
    public static final ElectricMotorBlock MOTOR = register("electric_motor", new ElectricMotorBlock(Block.Settings.create().hardness(2)));
    public static final FunnelBlock FUNNEL = register("funnel", new FunnelBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final SplitterBlock SPLITTER = register("splitter", new SplitterBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final FanBlock FAN = register("fan", new FanBlock(Block.Settings.create().hardness(1)));
    public static final CablePlateBlock CABLE_PLATE = register("cable_plate", new CablePlateBlock(Block.Settings.create().hardness(1)));
    public static final HandCrankBlock HAND_CRANK = register("hand_crank", new HandCrankBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final GrinderBlock GRINDER = register("grinder", new GrinderBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final PressBlock PRESS = register("press", new PressBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final MinerBlock MINER = register("miner", new MinerBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final AxleBlock AXLE = register("axle", new AxleBlock(Block.Settings.create().nonOpaque().hardness(1)));
    public static final GearboxBlock GEARBOX = register("gearbox", new GearboxBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final WindmillBlock WINDMILL = register("windmill", new WindmillBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final ContainerBlock CONTAINER = register("wooden_container", new ContainerBlock(Block.Settings.create().hardness(1).nonOpaque()));
    public static final ItemGeneratorBlock ITEM_GENERATOR = register("item_generator", new ItemGeneratorBlock(AbstractBlock.Settings.create().strength(-1, -1)));
    public static final RotationalDebugBlock ROTATION_DEBUG = register("rot_debug", new RotationalDebugBlock(AbstractBlock.Settings.create().strength(-1, -1)));


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
