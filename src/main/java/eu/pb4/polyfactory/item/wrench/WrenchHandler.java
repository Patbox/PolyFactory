package eu.pb4.polyfactory.item.wrench;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.FactorySoundEvents;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.advancement.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class WrenchHandler {
    private final Sidebar sidebar = new Sidebar(Sidebar.Priority.HIGH);
    private final Map<Block, String> currentAction = new Reference2ObjectOpenHashMap<>();
    private BlockState state = Blocks.AIR.getDefaultState();
    @Nullable
    private BlockPos pos;

    private List<WrenchAction> actions = List.of();

    public WrenchHandler(ServerPlayNetworkHandler handler) {
        this.sidebar.addPlayer(handler);
        this.sidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
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
                            t.append(Text.literal(String.valueOf(GuiTextures.SPACE_1)).setStyle(UiResourceCreator.STYLE));
                            t.append(Text.literal("» ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                        } else {
                            t.append("   ");
                        }

                        t.append(action.name()).append(": ");

                        b.add(t, Text.empty().append(action.value().getDisplayValue(player.getWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state)).formatted(Formatting.YELLOW));
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

    public ActionResult useAction(ServerPlayerEntity player, World world, BlockPos pos, Direction side, boolean alt) {
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
                if ((alt ? action.alt() : action.action()).applyAction(player, world, pos, side, state, !player.isSneaking())) {
                    this.pos = null;
                    TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
                    player.playSoundToPlayer(FactorySoundEvents.ITEM_WRENCH_USE, SoundCategory.PLAYERS, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.95f);
                    return ActionResult.SUCCESS;
                }
                return ActionResult.FAIL;
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
        boolean foundCurrent = false;
        String nextAction = actions.get(0).id();
        String previousAction = actions.get(actions.size() - 1).id();
        for (var action : actions) {
            if (foundCurrent) {
                nextAction = action.id();
                break;
            }
            if (action.id().equals(current)) {
                foundCurrent = true;
            } else {
                previousAction = action.id();
            }
        }

        if (foundCurrent) {
            this.currentAction.put(state.getBlock(), player.isSneaking() ? previousAction : nextAction);
            player.playSoundToPlayer(FactorySoundEvents.ITEM_WRENCH_SWITCH, SoundCategory.PLAYERS, 0.3f, 1f);
            this.pos = null;
        }
    }
}
