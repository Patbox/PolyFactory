package eu.pb4.polyfactory.item.configuration;

import com.mojang.serialization.JavaOps;
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
import eu.pb4.sidebars.api.Sidebar;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WrenchHandler {
    private final Sidebar sidebar = new Sidebar(Sidebar.Priority.HIGH);
    private final Map<Object, String> currentAction = new Reference2ObjectOpenHashMap<>();
    private BlockState state = Blocks.AIR.getDefaultState();
    @Nullable
    private BlockPos pos;

    @Nullable
    private Entity entity;

    private List<BlockConfig<?>> blockActions = List.of();
    private List<? extends EntityConfig<?, ?>> entityActions = List.of();

    private ItemStack currentStack = ItemStack.EMPTY;

    public WrenchHandler(ServerPlayNetworkHandler handler) {
        this.sidebar.addPlayer(handler);
        this.sidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
    }

    public static WrenchHandler of(ServerPlayerEntity player) {
        return ((ServerPlayNetExt) player.networkHandler).polyFactory$getWrenchHandler();
    }

    public void tickDisplay(ServerPlayerEntity player) {
        var stack = ItemStack.EMPTY;
        if (player.getMainHandStack().isOf(FactoryItems.WRENCH) || player.getMainHandStack().isOf(FactoryItems.CLIPBOARD)) {
            stack = player.getMainHandStack();
        } else if (player.getOffHandStack().isOf(FactoryItems.WRENCH) || player.getOffHandStack().isOf(FactoryItems.CLIPBOARD)) {
            stack = player.getOffHandStack();
        }

        if (stack.isEmpty()) {
            this.entity = null;
            this.state = Blocks.AIR.getDefaultState();
            this.pos = null;
            this.sidebar.hide();
            return;
        }
        var isWrench = stack.isOf(FactoryItems.WRENCH);

        var hitResult = getTarget(player);

        if (hitResult.getType() == HitResult.Type.MISS) {
            this.entity = null;
            this.state = Blocks.AIR.getDefaultState();
            this.pos = null;
            this.sidebar.hide();
        } else if (hitResult instanceof BlockHitResult blockHitResult) {
            this.entity = null;
            this.entityActions = List.of();
            var state = player.getEntityWorld().getBlockState(blockHitResult.getBlockPos());
            if (state == this.state && blockHitResult.getBlockPos().equals(this.pos) && ItemStack.areItemsAndComponentsEqual(stack, this.currentStack)) {
                if (this.state.getBlock() instanceof ConfigurableBlock configurableBlock && isWrench) {
                    configurableBlock.wrenchTick(player, blockHitResult, this.state);
                }
                return;
            }
            this.currentStack = stack.copy();

            this.state = state;
            this.pos = blockHitResult.getBlockPos();
            if (this.state.getBlock() instanceof ConfigurableBlock configurableBlock) {
                if (isWrench) {
                    configurableBlock.wrenchTick(player, blockHitResult, this.state);
                }
                this.blockActions = configurableBlock.getBlockConfiguration(player, blockHitResult.getBlockPos(), blockHitResult.getSide(), this.state);
                var selected = this.currentAction.get(this.state.getBlock());
                var diffMap = new HashMap<String, Object>();
                if (stack.contains(FactoryDataComponents.CONFIGURATION_DATA)) {
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

                this.sidebar.setTitle(Text.translatable("item.polyfactory.wrench.title",
                        Text.empty()/*.append(this.state.getBlock().getName())
                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(false))*/)
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
                try {
                    this.sidebar.set((b) -> {
                        int size = Math.min(this.blockActions.size(), 15);
                        for (var i = 0; i < size; i++) {
                            var action = this.blockActions.get(i);

                            var t = Text.empty();

                            if (isWrench) {
                                if ((selected == null && i == 0) || action.id().equals(selected)) {
                                    t.append(Text.literal(String.valueOf(GuiTextures.SPACE_1)).setStyle(UiResourceCreator.STYLE));
                                    t.append(Text.literal("» ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                                } else {
                                    t.append("   ");
                                }
                            }

                            t.append(action.name()).append(": ");

                            var value = action.value().getValue(player.getEntityWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state);
                            //noinspection unchecked
                            var valueFrom = ((BlockValueFormatter<Object>) action.formatter()).getDisplayValue(value, player.getEntityWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state);

                            if (isWrench) {
                                b.add(t, Text.empty().append(valueFrom).formatted(Formatting.YELLOW));
                            } else if (diffMap.containsKey(action.id()) && !Objects.equals(diffMap.get(action.id()), value)) {
                                //noinspection unchecked
                                var diff = ((BlockValueFormatter<Object>) action.formatter()).getDisplayValue(diffMap.get(action.id()), player.getEntityWorld(), blockHitResult.getBlockPos(), blockHitResult.getSide(), state);
                                b.add(t, Text.empty().append(valueFrom).append(Text.literal(" -> ").formatted(Formatting.GOLD)).append(diff).formatted(Formatting.YELLOW));
                            } else {
                                b.add(t.formatted(Formatting.GRAY), Text.empty().append(valueFrom).withColor(ColorHelper.scaleRgb(Formatting.YELLOW.getColorValue(), 0.7f)));
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
            this.state = Blocks.AIR.getDefaultState();
            this.pos = null;
            if (this.entity == entityHitResult.getEntity() && ItemStack.areItemsAndComponentsEqual(stack, this.currentStack)) {
                if (this.entity instanceof ConfigurableEntity<?> configurableEntity && isWrench) {
                    configurableEntity.wrenchTick(player, entityHitResult.getPos());
                }
                return;
            }
            this.currentStack = stack.copy();

            this.entity = entityHitResult.getEntity();
            if (this.entity instanceof ConfigurableEntity<?> configurableEntity) {
                if (isWrench) {
                    configurableEntity.wrenchTick(player, entityHitResult.getPos());
                }
                this.entityActions = configurableEntity.getEntityConfiguration(player, entityHitResult.getPos());
                var selected = this.currentAction.get(this.entity.getType());
                var diffMap = new HashMap<String, Object>();
                if (stack.contains(FactoryDataComponents.CONFIGURATION_DATA)) {
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

                this.sidebar.setTitle(Text.translatable("item.polyfactory.wrench.title",
                                Text.empty()/*.append(this.state.getBlock().getName())
                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW).withBold(false))*/)
                        .setStyle(Style.EMPTY.withColor(Formatting.GOLD).withBold(true)));
                try {
                    this.sidebar.set((b) -> {
                        int size = Math.min(this.entityActions.size(), 15);
                        for (var i = 0; i < size; i++) {
                            var action = (EntityConfig<Object, Entity>) this.entityActions.get(i);

                            var t = Text.empty();

                            if (isWrench) {
                                if ((selected == null && i == 0) || action.id().equals(selected)) {
                                    t.append(Text.literal(String.valueOf(GuiTextures.SPACE_1)).setStyle(UiResourceCreator.STYLE));
                                    t.append(Text.literal("» ").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
                                } else {
                                    t.append("   ");
                                }
                            }

                            t.append(action.name()).append(": ");

                            var value = action.value().getValue(entity, entityHitResult.getPos());
                            //noinspection unchecked
                            var valueFrom = action.formatter().getDisplayValue(value, entity, entityHitResult.getPos());

                            if (isWrench) {
                                b.add(t, Text.empty().append(valueFrom).formatted(Formatting.YELLOW));
                            } else if (diffMap.containsKey(action.id()) && !Objects.equals(diffMap.get(action.id()), value)) {
                                //noinspection unchecked
                                var diff = action.formatter().getDisplayValue(diffMap.get(action.id()), entity, entityHitResult.getPos());
                                b.add(t, Text.empty().append(valueFrom).append(Text.literal(" -> ").formatted(Formatting.GOLD)).append(diff).formatted(Formatting.YELLOW));
                            } else {
                                b.add(t.formatted(Formatting.GRAY), Text.empty().append(valueFrom).withColor(ColorHelper.scaleRgb(Formatting.YELLOW.getColorValue(), 0.7f)));
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

    public static HitResult getTarget(ServerPlayerEntity camera) {
        return findCrosshairTarget(camera, camera.getBlockInteractionRange(), camera.getEntityInteractionRange(), 0);
    }

    private static HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        double maxRange = Math.max(blockInteractionRange, entityInteractionRange);
        double sqrMaxRange = MathHelper.square(maxRange);
        Vec3d vec3d = camera.getCameraPosVec(tickProgress);
        HitResult hitResult = camera.raycast(maxRange, tickProgress, false);
        double squaredDistanceToBlock = hitResult.getPos().squaredDistanceTo(vec3d);
        if (hitResult.getType() != HitResult.Type.MISS) {
            sqrMaxRange = squaredDistanceToBlock;
            maxRange = Math.sqrt(squaredDistanceToBlock);
        }

        Vec3d rotation = camera.getRotationVec(tickProgress);
        Vec3d endPos = vec3d.add(rotation.x * maxRange, rotation.y * maxRange, rotation.z * maxRange);
        Box cameraBox = camera.getBoundingBox().stretch(rotation.multiply(maxRange)).expand(1.0, 1.0, 1.0);
        var entityHitResult = ProjectileUtil.raycast(camera, vec3d, endPos, cameraBox, EntityPredicates.CAN_HIT, sqrMaxRange);
        return entityHitResult != null && entityHitResult.getPos().squaredDistanceTo(vec3d) < squaredDistanceToBlock ? ensureTargetInRange(entityHitResult, vec3d, entityInteractionRange) : ensureTargetInRange(hitResult, vec3d, blockInteractionRange);
    }

    private static HitResult ensureTargetInRange(HitResult hitResult, Vec3d cameraPos, double interactionRange) {
        var pos = hitResult.getPos();
        if (!pos.isInRange(cameraPos, interactionRange)) {
            var hitPos = hitResult.getPos();
            var direction = Direction.getFacing(hitPos.x - cameraPos.x, hitPos.y - cameraPos.y, hitPos.z - cameraPos.z);
            return BlockHitResult.createMissed(hitPos, direction, BlockPos.ofFloored(hitPos));
        } else {
            return hitResult;
        }
    }

    public ActionResult useBlockAction(ServerPlayerEntity player, World world, BlockPos pos, Direction side, boolean alt) {
        var state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ConfigurableBlock configurableBlock)) {
            return ActionResult.PASS;
        }
        var actions = configurableBlock.getBlockConfiguration(player, pos, side, this.state);

        if (actions.isEmpty()) {
            return ActionResult.PASS;
        }
        var current = this.currentAction.get(state.getBlock());
        if (current == null) {
            current = actions.get(0).id();
        }

        for (var act : actions) {
            @SuppressWarnings("unchecked") var action = (BlockConfig<Object>) act;
            if (action.id().equals(current)) {
                var newValue = (alt ? action.alt() : action.action()).modifyValue(action.value().getValue(world, pos, side, state), !player.isSneaking(), player, world, pos, side, state);
                if (action.value().setValue(newValue, world, pos, side, state)) {
                    this.pos = null;
                    TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
                    FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_USE, SoundCategory.PLAYERS, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.95f);
                    return ActionResult.SUCCESS_SERVER;
                }
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.PASS;
    }

    public void attackBlockAction(ServerPlayerEntity player, World world, BlockPos pos, Direction side) {
        var state = world.getBlockState(pos);
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
            this.currentAction.put(state.getBlock(), player.isSneaking() ? previousAction : nextAction);
            FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_SWITCH, SoundCategory.PLAYERS, 0.3f, 1f);
            this.pos = null;
        }
    }

    public ActionResult useEntityAction(ServerPlayerEntity player, Entity entity, Vec3d pos, boolean alt) {
        if (!(entity instanceof ConfigurableEntity<?> configurableEntity)) {
            return ActionResult.PASS;
        }
        var actions = configurableEntity.getEntityConfiguration(player, pos);

        if (actions.isEmpty()) {
            return ActionResult.PASS;
        }
        var current = this.currentAction.get(entity.getType());
        if (current == null) {
            current = actions.get(0).id();
        }

        for (var act : actions) {
            @SuppressWarnings("unchecked") var action = (EntityConfig<Object, Entity>) act;
            if (action.id().equals(current)) {
                var newValue = (alt ? action.alt() : action.action()).modifyValue(action.value().getValue(entity, pos), !player.isSneaking(), player, entity, pos);
                if (action.value().setValue(newValue, entity, pos)) {
                    this.entity = null;
                    TriggerCriterion.trigger(player, FactoryTriggers.WRENCH);
                    FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_USE, SoundCategory.PLAYERS, 0.3f, player.getRandom().nextFloat() * 0.1f + 0.95f);
                    return ActionResult.SUCCESS_SERVER;
                }
                return ActionResult.FAIL;
            }
        }

        return ActionResult.FAIL;
    }

    public void attackEntityAction(ServerPlayerEntity player, Entity entity, Vec3d pos) {
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
            this.currentAction.put(entity.getType(), player.isSneaking() ? previousAction : nextAction);
            FactoryUtil.playSoundToPlayer(player,FactorySoundEvents.ITEM_WRENCH_SWITCH, SoundCategory.PLAYERS, 0.3f, 1f);
            this.entity = null;
        }
    }
}
