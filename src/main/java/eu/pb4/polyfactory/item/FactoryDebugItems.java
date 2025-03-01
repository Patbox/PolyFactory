package eu.pb4.polyfactory.item;

import com.kneelawk.graphlib.api.GraphLib;
import com.kneelawk.graphlib.impl.GraphLibImpl;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.debug.BaseDebugItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static eu.pb4.polyfactory.item.FactoryItems.register;

public class FactoryDebugItems {
    public static final Item DEBUG_PIPE_FLOW = register("debug/pipe_flow", (settings) -> BaseDebugItem.onBlockInteract(settings, "Pipe Flow", 0xff8800, (ctx) -> {
        var player = ctx.getPlayer();
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        assert player != null;
        player.sendMessage(Text.literal("# Push: ").formatted(Formatting.YELLOW), false);
        NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPushFlows(pos, () -> true, (direction, strength) -> {
            player.sendMessage(Text.literal(direction.asString() + "=" + strength), false);
        });
        player.sendMessage(Text.literal("# Pull: ").formatted(Formatting.YELLOW), false);
        NetworkComponent.Pipe.getLogic((ServerWorld) world, pos).runPullFlows(pos, () -> true, (direction, strength) -> {
            player.sendMessage(Text.literal(direction.asString() + "=" + strength), false);
        });
    }));

    public static final Item DEBUG_NODE_INFO = register("debug/node_info", (settings) -> BaseDebugItem.onBlockInteract(settings, "Node Info", 0x0088ff, (ctx) -> {
        var player = ctx.getPlayer();
        var world = ctx.getWorld();
        var pos = ctx.getBlockPos();
        assert player != null;
        GraphLibImpl.UNIVERSE.forEach((id, universe) -> {
            player.sendMessage(Text.literal("# " + id + ": ").formatted(Formatting.YELLOW), false);
            universe.getGraphWorld((ServerWorld) world).getNodesAt(pos).forEach((holder) -> {
                player.sendMessage(Text.literal("G: " + holder.getGraphId() + " | " + holder.getNode()), false);
            });
        });
    }));
    public static final Item ROTATION_DEBUG = register(FactoryBlocks.ROTATION_DEBUG);
    public static final Item TPS_PROVIDER = register(FactoryBlocks.TPS_PROVIDER);

    public static void addItemGroup(ItemGroup.DisplayContext context, ItemGroup.Entries entries) {
        entries.add(DEBUG_PIPE_FLOW, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
        entries.add(DEBUG_NODE_INFO, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
        entries.add(ROTATION_DEBUG, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
        entries.add(TPS_PROVIDER, ItemGroup.StackVisibility.PARENT_TAB_ONLY);
    }
}
