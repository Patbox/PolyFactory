package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import com.mojang.serialization.Codec;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.util.WorldPointer;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.configurable.BlockConfig;
import eu.pb4.polyfactory.block.configurable.BlockValueFormatter;
import eu.pb4.polyfactory.block.configurable.ConfigurableBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.RotationalNetworkBlock;
import eu.pb4.polyfactory.models.RotationAwareModel;
import eu.pb4.polyfactory.nodes.generic.FunctionalAxisNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polyfactory.util.movingitem.MovingItemContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItemProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class SawmillBlock extends RotationalNetworkBlock implements FactoryBlock, EntityBlock, RotationUser, ConfigurableBlock, MovingItemConsumer, MovingItemProvider {
    public static final EnumProperty<Direction> INPUT_FACING = EnumProperty.create("input_facing", Direction.class, x -> x.getAxis() != Direction.Axis.Y);
    public static final EnumProperty<BladeAngle> BLADE_ANGLE = EnumProperty.create("blade_angle", BladeAngle.class);

    private static final List<BlockConfig<?>> BLOCK_CONFIGS = List.of(
            BlockConfig.ofDirection(INPUT_FACING),
            BlockConfig.of(BLADE_ANGLE, BlockValueFormatter.actionSubTranslations(BLADE_ANGLE.getName()))
    );
    private final Identifier model;
    private final Identifier modelSaw;
    private final Identifier modelSawActive;

    public SawmillBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(BLADE_ANGLE, BladeAngle.ZERO));
        this.model = settings.blockIdOrThrow().identifier().withPrefix("block/");
        this.modelSaw = this.model.withSuffix("_saw");
        this.modelSawActive = this.modelSaw.withSuffix("_active");
    }

    @Override
    public boolean pushItemTo(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        if (self.getBlockState().getValue(INPUT_FACING) == pushDirection) {
            return false;
        }

        var be = (SawmillBlockEntity) self.getBlockEntity();

        var container = be.getContainerHolder(0);

        if (container.isContainerEmpty()) {
            container.pushAndAttach(conveyor.pullAndRemove());
        } else {
            var targetStack = container.getContainer().get();
            var sourceStack = conveyor.getContainer().get();

            if (ItemStack.isSameItemSameComponents(container.getContainer().get(), conveyor.getContainer().get())) {
                var count = Math.min(targetStack.getCount() + sourceStack.getCount(), container.getMaxStackCount(sourceStack));
                if (count != targetStack.getCount()) {
                    var dec = count - targetStack.getCount();
                    targetStack.grow(dec);
                    sourceStack.shrink(dec);
                }

                if (sourceStack.isEmpty()) {
                    conveyor.clearContainer();
                }
            }
        }

        return true;
    }

    @Override
    public void getItemFrom(WorldPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, MovingItemContainerHolder conveyor) {
        var inputDir = self.getBlockState().getValue(INPUT_FACING);
        if (!conveyor.isContainerEmpty() || pushDirection == inputDir || inputDir.getOpposite() != relative) {
            return;
        }

        var be = (SawmillBlockEntity) self.getBlockEntity();

        for (int i = 1; i < 6; i++) {
            var out = be.getContainerHolder(i);

            if (out.isContainerEmpty()) {
                continue;
            }
            var stack = out.getContainer().get();

            var amount = Math.min(stack.getCount(), out.getMaxStackCount(stack));

            if (stack.getCount() == amount) {
                conveyor.pushAndAttach(out.pullAndRemove());
            } else {
                stack.shrink(amount);
                conveyor.setMovementPosition(pushDirection == inputDir.getOpposite() ? 0 : 0.5);
                conveyor.pushNew(stack.copyWithCount(amount));
            }

            return;
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity)) {
            return;
        }

        var be = (SawmillBlockEntity) level.getBlockEntity(pos);

        if (!be.canDamage) {
            return;
        }

        var rot = RotationUser.getRotation(level, pos);

        entity.hurtServer(serverLevel, level.damageSources().stalagmite(), (float) Math.clamp(rot.speed() / 20, 0.1f, 5f));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INPUT_FACING, BLADE_ANGLE);
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new FunctionalAxisNode(state.getValue(INPUT_FACING).getClockWise().getAxis()));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(INPUT_FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() && world.getBlockEntity(pos) instanceof SawmillBlockEntity be) {
            be.openGui((ServerPlayer) player);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.useWithoutItem(state, world, pos, player, hit);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        world.updateNeighbourForOutputSignal(pos, this);
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return FactoryUtil.transform(state, rotation::rotate, INPUT_FACING);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return FactoryUtil.transform(state, mirror::mirror, INPUT_FACING);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SawmillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerLevel && type == FactoryBlockEntities.SAWMILL ? SawmillBlockEntity::ticker : null;
    }

    @Override
    public List<BlockConfig<?>> getBlockConfiguration(@Nullable ServerPlayer player, BlockPos blockPos, Direction side, BlockState state) {
        return BLOCK_CONFIGS;
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.BARREL.defaultBlockState();
    }

    @Override
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof SawmillBlockEntity be) {
            modifier.stress(be.getStress());
        }
    }

    public final class Model extends RotationAwareModel {
        private static final Vec3 SAW_OFFSET = new Vec3(0, 7 / 16f, 0);
        private final ItemDisplayElement main;
        private final ItemDisplayElement saw;
        private boolean active;
        private final float angle = 0;

        private Model(ServerLevel world, BlockState state) {
            this.main = ItemDisplayElementUtil.createSimple(model);
            this.main.setScale(new Vector3f(2));
            this.saw = ItemDisplayElementUtil.createSimple(modelSaw);
            this.saw.setScale(new Vector3f(2));
            this.saw.setOffset(SAW_OFFSET);
            this.updateStatePos(state);
            this.addElement(this.main);
            this.addElement(this.saw);
        }

        private void updateStatePos(BlockState state) {
            var direction = state.getValue(INPUT_FACING);

            this.main.setYaw(direction.toYRot() + 180);
            this.saw.setYaw(direction.toYRot() + 180);
            this.saw.setLeftRotation(new Quaternionf().rotateZ(state.getValue(BLADE_ANGLE).angle * Mth.DEG_TO_RAD));
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                updateStatePos(this.blockState());
            }
        }

        public void setActive(boolean active) {
            if (this.active == active) {
                return;
            }

            this.saw.setItem(ItemDisplayElementUtil.getModel(active ? modelSawActive : modelSaw).get());
            this.active = active;
        }
    }


    public enum BladeAngle implements StringRepresentable {
        ZERO(0),
        TWENTY_TWO(22),
        FORTY_FIVE(45);

        public static final Codec<BladeAngle> CODEC = StringRepresentable.fromEnum(BladeAngle::values);

        private final int angle;

        BladeAngle(int angle) {
            this.angle = angle;
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public GuiElementBuilder getButton() {
            return (switch (this) {
                case ZERO -> GuiTextures.BUTTON_BLADE_ANGLE_ZERO;
                case TWENTY_TWO -> GuiTextures.BUTTON_BLADE_ANGLE_TWENTY_TWO;
                case FORTY_FIVE -> GuiTextures.BUTTON_BLADE_ANGLE_FORTY_FIVE;
            }).get().setName(Component.translatable("item.polyfactory.wrench.action.blade_angle").append(": ")
                    .append(Component.translatable("item.polyfactory.wrench.action.blade_angle." + getSerializedName())));
        }


        public ItemStack getPolydexIcon() {
            return (switch (this) {
                case ZERO -> GuiTextures.BUTTON_BLADE_ANGLE_ZERO;
                case TWENTY_TWO -> GuiTextures.BUTTON_BLADE_ANGLE_TWENTY_TWO;
                case FORTY_FIVE -> GuiTextures.BUTTON_BLADE_ANGLE_FORTY_FIVE;
            }).get().setName(Component.translatable("text.polyfactory.polydex_blade_angle",
                    Component.translatable("item.polyfactory.wrench.action.blade_angle." + getSerializedName()))).asStack();
        }
    }
}
