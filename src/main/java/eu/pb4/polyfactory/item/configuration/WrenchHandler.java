package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.JavaOps;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.entity.configurable.ConfigurableEntity;
import eu.pb4.polyfactory.entity.configurable.EntityConfig;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ServerPlayNetExt;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WrenchHandler {
    private final Sidebar sidebar = new Sidebar(Sidebar.Priority.HIGH);
    private final Map<Object, String> currentAction = new Reference2ObjectOpenHashMap<>();
    private BlockState state = Blocks.AIR.defaultBlockState();
    @Nullable
    private BlockPos pos;

    @Nullable
    private Entity entity;

    private List<BlockConfig<?>> blockActions = List.of();
    private List<? extends EntityConfig<?, ?>> entityActions = List.of();

    private ItemStack currentStack = ItemStack.EMPTY;

    private ExtraModelCallbacks extraModelCallbacks = ExtraModelCallbacks.NO_OP;

    public WrenchHandler(ServerGamePacketListenerImpl handler) {
        this.sidebar.addPlayer(handler);
        this.sidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);
    }

    public static WrenchHandler of(ServerPlayer player) {
        return ((ServerPlayNetExt) player.connection).polyFactory$getWrenchHandler();
    }

    public void tickDisplay(ServerPlayer player) {
        var stack = ItemStack.EMPTY;
        if (player.getMainHandItem().is(FactoryItems.WRENCH) || player.getMainHandItem().is(FactoryItems.CLIPBOARD)) {
            stack = player.getMainHandItem();
        } else if (player.getOffhandItem().is(FactoryItems.WRENCH) || player.getOffhandItem().is(FactoryItems.CLIPBOARD)) {
            stack = player.getOffhandItem();
        }

        if (stack.isEmpty()) {
            this.entity = null;
            this.state = Blocks.AIR.defaultBlockState();
            this.pos = null;
            this.sidebar.hide();
            this.extraModelCallbacks.stopTargetting(player);
            this.extraModelCallbacks = ExtraModelCallbacks.NO_OP;
            return;
        }
        var isWrench = stack.is(FactoryItems.WRENCH);

        var hitResult = getTarget(player);

        if (hitResult.getType() == HitResult.Type.MISS) {
            this.extraModelCallbacks.stopTargetting(player);
            this.extraModelCallbacks = ExtraModelCallbacks.NO_OP;
            this.entity = null;
            this.state = Blocks.AIR.defaultBlockState();
            this.pos = null;
            this.sidebar.hide();
        } else if (hitResult instanceof BlockHitResult blockHitResult) {
            this.entity = null;
            this.entityActions = List.of();
            var state = player.level().getBlockState(blockHitResult.getBlockPos());
            if (state == this.state && blockHitResult.getBlockPos().equals(this.pos) && ItemStack.isSameItemSameComponents(stack, this.currentStack)) {
                if (this.state.getBlock() instanceof ConfigurableBlock configurableBlock && isWrench) {
                    configurableBlock.wrenchTick(player, blockHitResult, this.state);
                }
                return;
            }
            this.currentStack = stack.copy();

            this.state = state;
            this.pos = blockHitResult.getBlockPos();
            this.extraModelCallbacks.stopTargetting(player);
            this.extraModelCallbacks = BlockAwareAttachment.get(player.level(), this.pos) instanceof HolderAttachment attachment
                    && attachment.holder() instanceof ExtraModelCallbacks callbacks ? callbacks : ExtraModelCallbacks.NO_OP;
            this.extraModelCallbacks.startTargetting(player);

            if (this.state.getBlock() instanceof ConfigurableBlock configurableBlock) {
                if (isWrench) {
                    configurableBlock.wrenchTick(player, blockHitResult, this.state);
                }
                this.blockActions = configurableBlock.getBlockConfiguration(player, blockHitResult.getBlockPos(), blockHitResult.getDirection(), this.state);
                var selected = this.currentAction.get(this.state.getBlock());
                var diffMap = new HashMap<String, Object>();
                if (stack.has(FactoryDataComponents.CONFIGURATION_DATA)) {
                    for (var config : this.blockActions) {
                        for (var entry : stack.getOrDefault(FactoryDataComponents.CONFIGURATION_DATA, ConfigurationData.EMPTY).entries()) {
                            if (!config.id().equals(entry.id())) {
                                continue;
                            }

                            var decoded = config.codec().decode(JavaOps.INSTANCE, entry.value());

                            if (decoded.isSuccess()) {
                                diffMap.put(config.id(), decoded.getOrThrow().getFirst());
                            }
                        }
                    }
                }

                this.sidebar.setTitle(Component.translatable("item.polyfactory.wrench.title",
                        Component.empty()/*.append(this.state.getBlock().getName())
                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(false))*/)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));
                try {
                    this.sidebar.set((b) -> {
                        int size = Math.min(this.blockActions.size(), 15);
                        for (var i = 0; i < size; i++) {
                            var action = this.blockActions.get(i);

                            var t = Component.empty();

                            if (isWrench) {
                                if ((selected == null && i == 0) || action.id().equals(selected)) {
                                    t.append(Component.literal(String.valueOf(GuiTextures.SPACE_1)).setStyle(UiResourceCreator.STYLE));
                                    t.append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                                } else {
                                    t.append("   ");
                                }
                            }

                            t.append(action.name()).append(": ");

                            var value = action.value().getValue(player.level(), blockHitResult.getBlockPos(), blockHitResult.getDirection(), state);
                            //noinspection unchecked
                            var valueFrom = ((BlockValueFormatter<Object>) action.formatter()).getDisplayValue(value, player.level(), blockHitResult.getBlockPos(), blockHitResult.getDirection(), state);

                            if (isWrench) {
                                b.add(t, Component.empty().append(valueFrom).withStyle(ChatFormatting.YELLOW));
                            } else if (diffMap.containsKey(action.id()) && !Objects.equals(diffMap.get(action.id()), value)) {
                                //noinspection unchecked
                                var diff = ((BlockValueFormatter<Object>) action.formatter()).getDisplayValue(diffMap.get(action.id()), player.level(), blockHitResult.getBlockPos(), blockHitResult.getDirection(), state);
                                b.add(t, Component.empty().append(valueFrom).append(Component.literal(" -> ").withStyle(ChatFormatting.GOLD)).append(diff).withStyle(ChatFormatting.YELLOW));
                            } else {
                                b.add(t.withStyle(ChatFormatting.GRAY), Component.empty().append(valueFrom).withColor(ARGB.scaleRGB(ChatFormatting.YELLOW.getColor(), 0.7f)));
                            }
                        }
                    });
                } catch (Throwable e) {
                    ModInit.LOGGER.error("Failed to create wrench display!", e);
                }
                this.sidebar.show();
            } else {
                this.blockActions = List.of();
                this.sidebar.hide();
            }
        } else if (hitResult instanceof EntityHitResult entityHitResult) {
            this.state = Blocks.AIR.defaultBlockState();
            this.pos = null;
            if (this.entity == entityHitResult.getEntity() && ItemStack.isSameItemSameComponents(stack, this.currentStack)) {
                if (this.entity instanceof ConfigurableEntity<?> configurableEntity && isWrench) {
                    configurableEntity.wrenchTick(player, entityHitResult.getLocation());
                }
                return;
            }
            this.currentStack = stack.copy();

            this.entity = entityHitResult.getEntity();
            if (this.entity instanceof ConfigurableEntity<?> configurableEntity) {
                if (isWrench) {
                    configurableEntity.wrenchTick(player, entityHitResult.getLocation());
                }
                this.entityActions = configurableEntity.getEntityConfiguration(player, entityHitResult.getLocation());
                var selected = this.currentAction.get(this.entity.getType());
                var diffMap = new HashMap<String, Object>();
                if (stack.has(FactoryDataComponents.CONFIGURATION_DATA)) {
                    for (var config : this.entityActions) {
                        for (var entry : stack.getOrDefault(FactoryDataComponents.CONFIGURATION_DATA, ConfigurationData.EMPTY).entries()) {
                            if (!config.id().equals(entry.id())) {
                                continue;
                            }

                            var decoded = config.codec().decode(JavaOps.INSTANCE, entry.value());

                            if (decoded.isSuccess()) {
                                diffMap.put(config.id(), decoded.getOrThrow().getFirst());
                            }
                        }
                    }
                }

                this.sidebar.setTitle(Component.translatable("item.polyfactory.wrench.title",
                                Component.empty()/*.append(this.state.getBlock().getName())
                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(false))*/)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)));
                try {
                    this.sidebar.set((b) -> {
                        int size = Math.min(this.entityActions.size(), 15);
                        for (var i = 0; i < size; i++) {
                            var action = (EntityConfig<Object, Entity>) this.entityActions.get(i);

                            var t = Component.empty();

                            if (isWrench) {
                                if ((selected == null && i == 0) || action.id().equals(selected)) {
                                    t.append(Component.literal(String.valueOf(GuiTextures.SPACE_1)).setStyle(UiResourceCreator.STYLE));
                                    t.append(Component.literal("» ").setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
                                } else {
                                    t.append("   ");
                                }
                            }

                            t.append(action.name()).append(": ");

                            var value = action.value().getValue(entity, entityHitResult.getLocation());
                            //noinspection unchecked
                            var valueFrom = action.formatter().getDisplayValue(value, entity, entityHitResult.getLocation());

                            if (isWrench) {
                                b.add(t, Component.empty().append(valueFrom).withStyle(ChatFormatting.YELLOW));
                            } else if (diffMap.containsKey(action.id()) && !Objects.equals(diffMap.get(action.id()), value)) {
                                //noinspection unchecked
                                var diff = action.formatter().getDisplayValue(diffMap.get(action.id()), entity, entityHitResult.getLocation());
                                b.add(t, Component.empty().append(valueFrom).append(Component.literal(" -> ").withStyle(ChatFormatting.GOLD)).append(diff).withStyle(ChatFormatting.YELLOW));
                            } else {
                                b.add(t.withStyle(ChatFormatting.GRAY), Component.empty().append(valueFrom).withColor(ARGB.scaleRGB(ChatFormatting.YELLOW.getColor(), 0.7f)));
                            }
                        }
                    });
                } catch (Throwable e) {
                    ModInit.LOGGER.error("Failed to create wrench display!", e);
                }
                this.sidebar.show();
            } else {
                this.entityActions = List.of();
                this.sidebar.hide();
            }
        }
    }

    public static HitResult getTarget(ServerPlayer camera) {
        return findCrosshairTarget(camera, camera.blockInteractionRange(), camera.entityInteractionRange(), 0);
    }

    private static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        double maxRange = Math.max(blockInteractionRange, entityInteractionRange);
        double sqrMaxRange = Mth.square(maxRange);
        Vec3 vec3d = camera.getEyePosition(tickProgress);
        HitResult hitResult = camera.pick(maxRange, tickProgress, false);
        double squaredDistanceToBlock = hitResult.getLocation().distanceToSqr(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            sqrMaxRange = squaredDistanceToBlock;
            maxRange = Math.sqrt(squaredDistanceToBlock);
        }

        Vec3 rotation = camera.getViewVector(tickProgress);
        Vec3 endPos = vec3d.add(rotation.x * maxRange, rotation.y * maxRange, rotation.z * maxRange);
        AABB cameraBox = camera.getBoundingBox().expandTowards(rotation.scale(maxRange)).inflate(1.0, 1.0, 1.0);
        var entityHitResult = ProjectileUtil.getEntityHitResult(camera, vec3d, endPos, cameraBox, EntitySelector.CAN_BE_PICKED, sqrMaxRange);
        return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(vec3d) < squaredDistanceToBlock ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange) : ensureTargetInRange(hitResult, vec3d, blockInteractionRange);
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3 cameraPos, double interactionRange) {
        var pos = hitResult.getLocation();
        if (!pos.closerThan(cameraPos, interactionRange)) {
            var hitPos = hitResult.getLocation();
            var direction = Direction.getApproximateNearest(hitPos.x - cameraPos.x, hitPos.y - cameraPos.y, hitPos.z - cameraPos.z);
            return BlockHitResult.miss(hitPos, direction, BlockPos.containing(hitPos));
        } else {
            return hitResult;
        }
    }

    public InteractionResult useBlockAction(ServerPlayer player, Level world, BlockPos pos, Direction side, boolean alt) {
        var state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ConfigurableBlock configurableBlock)) {
            return InteractionResult.PASS;
        }
        var actions = configurableBlock.getBlockConfiguration(player, pos, side, this.state);

        if (actions.isEmpty()) {
            return InteractionResult.PASS;
        }
        var current = this.currentAction.get(state.getBlock());
        if (current == null) {
            current = actions.get(0).id();
        }

        for (var act : actions) {
            @SuppressWarnings("unchecked") var action = (BlockConfig<Object>) act;
            if (action.id().equals(current)) {
                var newValue = (alt ? action.alt() : action.action()).modifyValue(action.value().getValue(world, pos, side, state), !player.isShiftKeyDown(), player, world, pos, side, state);
                if (action.value().setValue(newValue, world, pos, side, state)) {
                    this.pos = null;
                    TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
                    FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_USE, SoundSource.PLAYERS, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.95f);
                    return InteractionResult.SUCCESS_SERVER;
                }
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    public void attackBlockAction(ServerPlayer player, Level world, BlockPos pos, Direction side) {
        var state = world.getBlockState(pos);
        VirtualDestroyStage.updateState(player, pos, state, -1);
        if (!(state.getBlock() instanceof ConfigurableBlock configurableBlock)) {
            return;
        }

        var actions = configurableBlock.getBlockConfiguration(player, pos, side, this.state);
        if (actions.isEmpty()) {
            return;
        }

        var current = this.currentAction.get(state.getBlock());

        if (current == null) {
            current = actions.getFirst().id();
        }
        boolean foundCurrent = false;
        String nextAction = actions.getFirst().id();
        String previousAction = actions.getLast().id();
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
            this.currentAction.put(state.getBlock(), player.isShiftKeyDown() ? previousAction : nextAction);
            FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_SWITCH, SoundSource.PLAYERS, 0.3f, 1f);
            this.pos = null;
        }
    }

    public InteractionResult useEntityAction(ServerPlayer player, Entity entity, Vec3 pos, boolean alt) {
        if (!(entity instanceof ConfigurableEntity<?> configurableEntity)) {
            return InteractionResult.PASS;
        }
        var actions = configurableEntity.getEntityConfiguration(player, pos);

        if (actions.isEmpty()) {
            return InteractionResult.PASS;
        }
        var current = this.currentAction.get(entity.getType());
        if (current == null) {
            current = actions.get(0).id();
        }

        for (var act : actions) {
            @SuppressWarnings("unchecked") var action = (EntityConfig<Object, Entity>) act;
            if (action.id().equals(current)) {
                var newValue = (alt ? action.alt() : action.action()).modifyValue(action.value().getValue(entity, pos), !player.isShiftKeyDown(), player, entity, pos);
                if (action.value().setValue(newValue, entity, pos)) {
                    this.entity = null;
                    TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
                    FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_USE, SoundSource.PLAYERS, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.95f);
                    return InteractionResult.SUCCESS_SERVER;
                }
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.FAIL;
    }

    public void attackEntityAction(ServerPlayer player, Entity entity, Vec3 pos) {
        if (!(entity instanceof ConfigurableEntity<?> configurableEntity)) {
            return;
        }

        var actions = configurableEntity.getEntityConfiguration(player, pos);
        if (actions.isEmpty()) {
            return;
        }

        var current = this.currentAction.get(entity.getType());

        if (current == null) {
            current = actions.getFirst().id();
        }
        boolean foundCurrent = false;
        String nextAction = actions.getFirst().id();
        String previousAction = actions.getLast().id();
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
            this.currentAction.put(entity.getType(), player.isShiftKeyDown() ? previousAction : nextAction);
            FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_SWITCH, SoundSource.PLAYERS, 0.3f, 1f);
            this.entity = null;
        }
    }

    public void forceUpdate() {
        this.pos = null;
    }

    public interface ExtraModelCallbacks {
        ExtraModelCallbacks NO_OP = new ExtraModelCallbacks() {};

        default void startTargetting(ServerPlayer player) {}
        default void stopTargetting(ServerPlayer player) {}
    }
}
