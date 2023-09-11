package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;

public class WrenchHandler {
    private static final Text TITLE = Text.translatable("item.polyfactory.wrench.title").setStyle(Style.EMPTY.withColor(Formatting.GOLD));
    private final Sidebar sidebar = new Sidebar(TITLE, Sidebar.Priority.HIGH);
    private BlockState state = Blocks.AIR.getDefaultState();
    private final Object2ObjectLinkedOpenCustomHashMap<Block, String> currentAction = new Object2ObjectLinkedOpenCustomHashMap<>(Util.identityHashStrategy());
    private List<WrenchAction> actions = List.of();

    public WrenchHandler(ServerPlayNetworkHandler handler) {
        this.sidebar.addPlayer(handler);
    }

    public static WrenchHandler of(ServerPlayerEntity player) {
        return ((ServerPlayNetExt) player.networkHandler).polyFactory$getWrenchHandler();
    }

    public void tick(ServerPlayerEntity player) {
        var hit = player.raycast(7, 0,false);

        if (hit instanceof BlockHitResult blockHitResult) {
            var state = player.getWorld().getBlockState(blockHitResult.getBlockPos());
            if (state == this.state) {
                return;
            }

            this.state = state;
            if (this.state.getBlock() instanceof WrenchableBlock wrenchableBlock) {
                this.actions = wrenchableBlock.getWrenchActions();
                var selected = this.currentAction.get(this.state.getBlock());
                this.sidebar.set((b) -> {
                    int size = Math.min(this.actions.size(), 15);
                    for (var i = 0; i < size; i++) {
                        var action = this.actions.get(i);

                        var t = Text.empty();

                        if ((selected == null && i == 0) || action.id().equals(selected)) {
                            t.append(Text.literal("> ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                        } else {
                            t.append("  ");
                        }

                        t.append(action.name()).append(": ").append(action.value().getDisplayValue(player.getWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state));

                        b.add(t);
                    }
                });
                this.sidebar.show();
            } else {
                this.actions = List.of();
                this.sidebar.hide();
            }
        } else {
            this.state = Blocks.AIR.getDefaultState();
            this.sidebar.hide();
        }
    }

    public ActionResult useAction(ServerPlayerEntity player, World world, BlockPos pos, Direction side) {
        var state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WrenchableBlock wrenchableBlock)) {
            return ActionResult.PASS;
        }
        var actions = wrenchableBlock.getWrenchActions();

        if (actions.isEmpty()) {
            return ActionResult.PASS;
        }
        var current = this.currentAction.get(state.getBlock());
        if (current == null) {
            current = actions.get(0).id();
        }

        for (var action : actions) {
            if (action.id().equals(current)) {
                action.action().applyAction(world, pos, side, state, !player.isSneaking());
                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.PASS;
    }

    public void attackAction(ServerPlayerEntity player, World world, BlockPos pos, Direction side) {
        var state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof WrenchableBlock wrenchableBlock)) {
            return;
        }

        var actions = wrenchableBlock.getWrenchActions();
        if (actions.isEmpty()) {
            return;
        }

        var current = this.currentAction.get(state.getBlock());

        if (current == null) {
            current = actions.get(0).id();
        }
        boolean next = false;
        for (var action : actions) {
            if (next) {
                this.currentAction.put(state.getBlock(), action.id());
                this.state = Blocks.CAVE_AIR.getDefaultState();
                return;
            }
            if (action.id().equals(current)) {
                next = true;
            }
        }

        if (next) {
            this.currentAction.put(state.getBlock(), this.actions.get(0).id());
            this.state = Blocks.CAVE_AIR.getDefaultState();
        }
    }
}
