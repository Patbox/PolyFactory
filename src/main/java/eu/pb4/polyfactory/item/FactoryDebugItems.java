package eu.pb4.polyfactory.item;

import com.kneelawk.graphlib.api.GraphLib;
import com.kneelawk.graphlib.impl.GraphLibImpl;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.debug.BaseDebugItem;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polyfactory.nodes.data.DataStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.stream.Collectors;

import static eu.pb4.polyfactory.item.FactoryItems.register;

public class FactoryDebugItems {
    public static final Item DEBUG_PIPE_FLOW = register("debug/pipe_flow", (settings) -> BaseDebugItem.onBlockInteract(settings, "Pipe Flow", 0xff8800, (ctx) -> {
        var player = ctx.getPlayer();
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        assert player != null;
        player.displayClientMessage(Component.literal("# Push: ").withStyle(ChatFormatting.YELLOW), false);
        NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPushFlows(pos, () -> true, (direction, strength) -> {
            player.displayClientMessage(Component.literal(direction.getSerializedName() + "=" + strength), false);
        });
        player.displayClientMessage(Component.literal("["
                + Arrays.stream(NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).getWeightedMaxFlow(pos, true, 1000)).mapToObj(String::valueOf).collect(Collectors.joining(", "))
                + "]"), false);
        player.displayClientMessage(Component.literal("# Pull: ").withStyle(ChatFormatting.YELLOW), false);
        NetworkComponent.Pipe.getLogic((ServerLevel) world, pos).runPullFlows(pos, () -> true, (direction, strength) -> {
            player.displayClientMessage(Component.literal(direction.getSerializedName() + "=" + strength), false);
        });
    }));

    public static final Item DEBUG_NODE_INFO = register("debug/node_info", (settings) -> BaseDebugItem.onBlockInteract(settings, "Node Info", 0x0088ff, (ctx) -> {
        var player = ctx.getPlayer();
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        assert player != null;
        GraphLibImpl.UNIVERSE.forEach((id, universe) -> {
            player.displayClientMessage(Component.literal("# " + id + ": ").withStyle(ChatFormatting.YELLOW), false);
            universe.getGraphWorld((ServerLevel) world).getNodesAt(pos).forEach((holder) -> {
                player.displayClientMessage(Component.literal("G: " + holder.getGraphId() + " | " + holder.getNode()), false);
                if (universe == FactoryNodes.DATA) {
                    var data = holder.getGraph().getGraphEntity(DataStorage.TYPE);
                    player.displayClientMessage(Component.literal("  > DataStorage: " + System.identityHashCode(data)), false);
                }
            });
        });
    }));

    public static final Item DEBUG_CABLE_NETWORK = register("debug/cable_network", (settings) -> BaseDebugItem.onBlockInteract(settings, "Cable Network", 0xff00ff, (ctx) -> {
        var player = ctx.getPlayer();
        var world = ctx.getLevel();
        var pos = ctx.getClickedPos();
        assert player != null;
        FactoryNodes.DATA.getGraphWorld((ServerLevel) world).getNodesAt(pos).forEach((holder) -> {
            player.displayClientMessage(Component.literal("G: " + holder.getGraphId() + " (" + holder.getNode() + ")"), false);
            var data = holder.getGraph().getGraphEntity(DataStorage.TYPE);
            player.displayClientMessage(Component.literal("> Receivers: "), false);
            for (var rec : data.receivers().int2ObjectEntrySet()) {
                player.displayClientMessage(Component.literal(">> Channel: " + rec.getIntKey()), false);

                for (var node : rec.getValue()) {
                    player.displayClientMessage(Component.literal("   " + node.pos().toShortString() + " | " + node.node().toString()), false);
                }
            }
            player.displayClientMessage(Component.literal("> Providers: "), false);
            for (var rec : data.providers().int2ObjectEntrySet()) {
                player.displayClientMessage(Component.literal(">> Channel: " + rec.getIntKey()), false);

                for (var node : rec.getValue()) {
                    player.displayClientMessage(Component.literal("   " + node.pos().toShortString() + " | " + node.node().toString()), false);
                }
            }

        });
    }));

    public static final Item ROTATION_DEBUG = register(FactoryBlocks.ROTATION_DEBUG);
    public static final Item TPS_PROVIDER = register(FactoryBlocks.TPS_PROVIDER);

    public static void addItemGroup(CreativeModeTab.ItemDisplayParameters context, CreativeModeTab.Output entries) {
        entries.accept(DEBUG_PIPE_FLOW, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        entries.accept(DEBUG_NODE_INFO, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        entries.accept(DEBUG_CABLE_NETWORK, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        entries.accept(ROTATION_DEBUG, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        entries.accept(TPS_PROVIDER, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
    }
}
