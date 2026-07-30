package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.fluids.FluidContainerOwner;
import eu.pb4.polyfactory.block.fluids.FluidInput;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.fluid.FactoryFluidTags;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerImpl;
import eu.pb4.polyfactory.fluid.FluidContainerUtil;
import eu.pb4.polyfactory.fluid.world.FluidWorldPullInteraction;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.RedstoneActivationType;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class DieselEngineBlockEntity extends LockableBlockEntity implements FluidInput.ContainerBased {
    private final FluidContainerImpl container = FluidContainerImpl.onlyInTag((long) (FluidConstants.BUCKET * 1.2f), FactoryFluidTags.DIESEL_ENGINE_FUEL, this::setChanged);
    private final FluidWorldPullInteraction fluidPull = new FluidWorldPullInteraction(container, () -> (ServerLevel) getLevel(), this::getBlockPos);
    private float state = 0;
    private RedstoneActivationType activationType = RedstoneActivationType.ALWAYS;
    private int gear = 3;

    public DieselEngineBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.DIESEL_ENGINE, pos, state);
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        this.container.writeData(view, "fluids");
        view.putFloat("state", this.state);
        view.store("redstone_activation", RedstoneActivationType.CODEC, this.activationType);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.container.readData(view, "fluids");
        this.state = view.getFloatOr("state", 0);
        this.activationType = view.read("redstone_activation", RedstoneActivationType.CODEC).orElse(RedstoneActivationType.ALWAYS);
    }

    public RedstoneActivationType getRedstoneActivationType() {
        return this.activationType;
    }

    public void setRedstoneActivationType(RedstoneActivationType activationType) {
        this.activationType = activationType;
        this.setChanged();
    }

    public int getGear() {
        return this.gear;
    }

    public void setGear(int gear) {
        this.state = this.state * this.gear / gear;
        this.gear = gear;
        this.setChanged();
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T t) {
        if (!(t instanceof DieselEngineBlockEntity self) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int consume = Mth.lerpInt(Math.min(Math.abs(self.state - 1), 1), 10, 20) * self.gear / 3;
        self.fluidPull.lowerProgress(0.01);
        if (self.activationType.isActive(state.getValue(DieselEngineBlock.POWERED))) {
            while (consume > 0 && self.container.isNotEmpty()) {
                var fluid = self.container.bottomFluid();
                consume -= (int) self.container.extract(fluid, consume, false);
            }

            var strength = Math.max(10 * self.state * self.gear / 60 / 20, 1f / 60 / 20);
            NetworkComponent.Pipe.getLogic(serverLevel, pos).setSourceStrength(pos, strength);
            self.fluidPull.pullFluid(state.getValue(DieselEngineBlock.FACING).getOpposite(), strength);
        }

        if (consume != 0 && self.state != 0) {
            self.state = (float) Math.max(self.state - 0.03, 0);
            self.setChanged();
            return;
        } else if (consume != 0) {
            return;
        }

        if ((level.getGameTime() + pos.getX() * 3L + pos.getY() * 7L + pos.getZ() * 5L) % Mth.clamp(Math.round(2 / self.state), 1, 6) == 0) {
            var facing = state.getValue(DieselEngineBlock.FACING);
            var center = Vec3.atCenterOf(pos).add(0, facing.getAxis() == Direction.Axis.Y ? 3.5f / 16 : 2.5f / 16, 0);

            var sideAxis = facing.getAxis() == Direction.Axis.Y ? Direction.Axis.X : facing.getClockWise().getAxis();
            var deltaAxis = facing.getAxis() == Direction.Axis.Y ? Direction.Axis.Z : facing.getAxis();

            for (int i = 0; i < self.gear; i++) {
                var particlePos = center
                        .relative(Direction.fromAxisAndDirection(sideAxis, level.getRandom().nextBoolean()
                                        ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE), 9 / 16f)
                        .relative(Direction.fromAxisAndDirection(deltaAxis, level.getRandom().nextBoolean()
                                ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE), 2 / 16f);
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        particlePos.x,
                        particlePos.y,
                        particlePos.z,
                        0, 0, 1, 0, 0.05);
            }
        }

        if (self.state < 1) {
            self.state = (float) Math.min(self.state + 0.01, 1);
        } else if (self.state > 1) {
            self.state = (float) Math.max(self.state * 0.96, 1);
        }
    }

    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel level, BlockPos pos) {
        if (this.state > 0) {
            var m = this.state > 1 ? 1 - Math.min(this.state - 1, 0.9f) : this.state;
            modifier.provide(10 * this.state * this.gear, 110 * m, state.getValue(DieselEngineBlock.FACING).getAxisDirection());
        }
    }

    @Override
    public @Nullable FluidContainer getFluidContainer(Direction direction) {
        return this.getBlockState().getValue(DieselEngineBlock.FACING).getOpposite() == direction ? container : null;
    }

    @Override
    public @Nullable FluidContainer getMainFluidContainer() {
        return this.container;
    }

    private class Gui extends SimpleGui {
        private int lastFluidUpdate = -1;
        private int delayTick = -1;

        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);

            var fluid = FluidContainerUtil.guiElement(container, true);
            for (int x = 0; x < 2; x++) {
                for (int y = 0; y < 3; y++) {
                    this.setSlot(9 * y + 5 + x, fluid);
                }
            }

            this.updateTitleAndFluid();
            this.updateIcons();

            this.setSlot(9 + 1, GuiUtils.createIteratingButton(
                    DieselEngineBlockEntity.this::getRedstoneActivationType,
                    DieselEngineBlockEntity.this::setRedstoneActivationType,
                    RedstoneActivationType.values(),
                    RedstoneActivationType::createButton
            ));

            this.setSlot(9 + 3, GuiUtils.createIteratingButton(
                    DieselEngineBlockEntity.this::getGear,
                    DieselEngineBlockEntity.this::setGear,
                    IntStream.rangeClosed(1, 5).boxed().toArray(Integer[]::new),
                    gear -> GuiTextures.BUTTON_SPEED_GEAR[gear].get().setName(Component.translatable("item.polyfactory.wrench.action.speed_gear").append(": " + gear + " / 5"))
            ));

            this.open();
        }

        private void updateIcons() {
            var m = state > 1 ? 1 - Math.min(state - 1, 0.9f) : state;

            this.setSlot(9 + 4, GuiTextures.DIESEL_ENGINE_POWER.getCeil(m));
        }

        private void updateTitleAndFluid() {
            var text = GuiTextures.DIESEL_ENGINE.apply(
                    Component.empty()
                            .append(Component.literal(GuiTextures.SMELTERY_FLUID_OFFSET + "").setStyle(UiResourceCreator.STYLE))
                            .append(FluidTextures.DIESEL_ENGINE.render(DieselEngineBlockEntity.this.container::provideRender))
                            .append(Component.literal(GuiTextures.SMELTERY_FLUID_OFFSET_N + "").setStyle(UiResourceCreator.STYLE))
                            .append(DieselEngineBlockEntity.this.getBlockState().getBlock().getName())
            );


            if (!text.equals(this.getTitle())) {
                this.setTitle(text);
            }


            this.lastFluidUpdate = DieselEngineBlockEntity.this.container.updateId();
        }


        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(DieselEngineBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }

            if (DieselEngineBlockEntity.this.container.updateId() != this.lastFluidUpdate && delayTick < 0) {
                delayTick = 3;
            }
            if (this.delayTick-- == 0) {
                this.updateTitleAndFluid();
            }
            this.updateIcons();

            super.onTick();
        }
    }
}
