package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.item.FactoryItems;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WrenchHandler {
    private final Sidebar sidebar = new Sidebar(Sidebar.Priority.HIGH);
    private final Object2ObjectLinkedOpenCustomHashMap<Block, String> currentAction = new Object2ObjectLinkedOpenCustomHashMap<>(Util.identityHashStrategy());
    private BlockState state = Blocks.AIR.getDefaultState();
    @Nullable
    private BlockPos pos;

    private List<WrenchAction> actions = List.of();

    public WrenchHandler(ServerPlayNetworkHandler handler) {
        this.sidebar.addPlayer(handler);
    }

    public static WrenchHandler of(ServerPlayerEntity player) {
        return ((ServerPlayNetExt) player.networkHandler).polyFactory$getWrenchHandler();
    }

    public void tick(ServerPlayerEntity player) {
        if (player.getMainHandStack().isOf(FactoryItems.WRENCH) && player.raycast(7, 0, false) instanceof BlockHitResult blockHitResult) {
            var state = player.getWorld().getBlockState(blockHitResult.getBlockPos());
            if (state == this.state && blockHitResult.getBlockPos().equals(this.pos)) {
                return;
            }

            this.state = state;
            this.pos = blockHitResult.getBlockPos();
            if (this.state.getBlock() instanceof WrenchableBlock wrenchableBlock) {
                this.actions = wrenchableBlock.getWrenchActions();
                var selected = this.currentAction.get(this.state.getBlock());
                this.sidebar.setTitle(Text.translatable("item.polyfactory.wrench.title",
                        Text.empty()/*.append(this.state.getBlock().getName())
                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(false))*/)
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
                this.sidebar.set((b) -> {
                    int size = Math.min(this.actions.size(), 15);
                    for (var i = 0; i < size; i++) {
                        var action = this.actions.get(i);

                        var t = Text.empty();

                        if ((selected == null && i == 0) || action.id().equals(selected)) {
                            t.append(Text.literal("» ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                        } else {
                            t.append("   ");
                        }

                        t.append(action.name()).append(": ").append(
                                Text.literal(action.value().getDisplayValue(player.getWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state)).formatted(Formatting.GRAY));

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
            this.pos = null;
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
                this.pos = null;
                TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
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
                this.pos = null;
                return;
            }
            if (action.id().equals(current)) {
                next = true;
            }
        }

        if (next) {
            this.currentAction.put(state.getBlock(), this.actions.get(0).id());
            this.pos = null;
        }
    }
}
