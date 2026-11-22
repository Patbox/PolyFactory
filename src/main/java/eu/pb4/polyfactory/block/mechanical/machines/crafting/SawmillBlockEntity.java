package eu.pb4.polyfactory.block.mechanical.machines.crafting;

import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.mechanical.conveyor.SimpleMovingItemContainerBlockEntity;
import eu.pb4.polyfactory.block.mechanical.source.DieselEngineBlockEntity;
import eu.pb4.polyfactory.block.other.ItemOutputBufferBlock;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.block.other.OutputContainerOwner;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.input.SawmillInput;
import eu.pb4.polyfactory.recipe.sawmill.SawmillRecipe;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.GuiUtils;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.inventory.SubContainer;
import eu.pb4.polyfactory.util.movingitem.SimpleMovingItemContainer;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class SawmillBlockEntity extends LockableBlockEntity implements SimpleMovingItemContainerBlockEntity, MachineInfoProvider, OutputContainerOwner, BlockEntityExtraListener {
    public static final int INPUT_SLOT = 0;
    private static final int[] INPUT_SLOTS = {INPUT_SLOT};
    private static final int[] OUTPUT_SLOTS = {1, 2, 3, 4, 5};
    private final SimpleMovingItemContainer[] containers = new SimpleMovingItemContainer[]{
            new SimpleMovingItemContainer(0, this::addMoving, this::removeMoving),
            new SimpleMovingItemContainer(),
            new SimpleMovingItemContainer(),
            new SimpleMovingItemContainer(),
            new SimpleMovingItemContainer(),
            new SimpleMovingItemContainer(),
    };

    protected double process = 0;
    @Nullable
    protected RecipeHolder<SawmillRecipe> currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    private boolean active;
    private double speedScale;
    private Component state = null;
    private final Container outputContainer = new SubContainer(this, 1);
    private SawmillBlock.Model model = null;
    private double requiredTime = 1;
    boolean canDamage;

    public SawmillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.SAWMILL, pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        this.writeInventoryView(view);
        view.putDouble("progress", this.process);
        view.putDouble("required_time", this.requiredTime);
        super.saveAdditional(view);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.readInventoryView(view);
        this.process = view.getDoubleOr("progress", 0);
        this.requiredTime = view.getDoubleOr("required_time", 1);
        this.currentItem = null;
        super.loadAdditional(view);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        var facing = this.getBlockState().getValue(SawmillBlock.INPUT_FACING);
        return facing.getOpposite() == side || side == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return slot != INPUT_SLOT;
    }

    public void createGui(ServerPlayer player) {
        new Gui(player);
    }

    @Override
    public void setBlockState(BlockState blockState) {
        super.setBlockState(blockState);
        this.currentItem = null;
    }

    public static <T extends BlockEntity> void ticker(Level level, BlockPos pos, BlockState state, T t) {
        var self = (SawmillBlockEntity) t;
        var serverLevel = (ServerLevel) level;

        var stack = self.getItem(0);

        self.state = null;
        if (stack.isEmpty()) {
            self.process = 0;
            self.active = false;
            self.model.setActive(self.canDamage = RotationUser.getRotation(level, pos).speed() > 0);
            self.speedScale = 0;
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.is(self.currentItem)) {
            self.process = 0;
            self.active = false;
            self.model.setActive(self.canDamage = false);
            self.speedScale = 0;
            self.state = INCORRECT_ITEMS_TEXT;
            return;
        }
        var input = new SawmillInput(self.getItem(0).copy(), state.getValue(SawmillBlock.BLADE_ANGLE));
        if (self.currentItem == null || !stack.is(self.currentItem)) {
            self.process = 0;
            self.speedScale = 0;
            self.currentItem = stack.getItem();
            self.currentRecipe = serverLevel.recipeAccess().getRecipeFor(FactoryRecipeTypes.SAWMILL, input, level).orElse(null);

            if (self.currentRecipe == null) {
                self.active = false;
                self.model.setActive(self.canDamage = false);
                self.state = INCORRECT_ITEMS_TEXT;
                return;
            }
        }
        self.active = true;
        assert self.currentRecipe != null;
        self.requiredTime = self.currentRecipe.value().time(input);

        if (self.process >= self.requiredTime) {
            var output = self.getOutputContainer();
            // Check space
            {
                var inv = new SimpleContainer(output.getContainerSize());
                for (int i = 0; i < output.getContainerSize(); i++) {
                    inv.setItem(i, output.getItem(i).copy());
                }

                for (var item : self.currentRecipe.value().output(input, null)) {
                    FactoryUtil.tryInsertingInv(inv, item, null);

                    if (!item.isEmpty()) {
                        self.state = OUTPUT_FULL_TEXT;
                        return;
                    }
                }
            }

            if (FactoryUtil.getClosestPlayer(level, pos, 32) instanceof ServerPlayer player) {
                CriteriaTriggers.RECIPE_CRAFTED.trigger(player, self.currentRecipe.id(), List.of(self.getItem(0)));
            }

            var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getBreakSound() : SoundEvents.STONE_BREAK;
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.6f, 0.5f);
            self.process = 0;
            stack.shrink(1);
            self.updatePosition(0);

            for (var out : self.currentRecipe.value().output(input, level.getRandom())) {
                FactoryUtil.tryInsertingRegular(output, out.copy());
            }

            self.setChanged();
        } else {
            var d = Math.max(self.currentRecipe.value().optimalSpeed(input) - self.currentRecipe.value().minimumSpeed(input), 1);
            var rot = RotationUser.getRotation(level, pos);
            var speed = Math.min(Math.max(Math.abs(rot.speed()) - self.currentRecipe.value().minimumSpeed(input), 0), d) / d / 20;
            self.speedScale = speed;
            if (speed > 0) {
                self.model.setActive(self.canDamage = true);
                if (level.getGameTime() % Mth.clamp(Math.round(1 / speed), 2, 5) == 0) {
                    var container = self.containers[0].getPos(Vec3.ZERO);

                    serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(stack)),
                            container.x, container.y, container.z, 0,
                            (Math.random() - 0.5) * 0.2, 0.02, (Math.random() - 0.5) * 0.2, 2);
                }
                if (level.getGameTime() % 20 == 0) {
                    var sound = stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock().defaultBlockState().getSoundType().getHitSound() : SoundEvents.WOOD_HIT;
                    level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.5f, 0.5f);
                }

                self.process += speed;
                self.updatePosition(0);
                self.setChanged();
                return;
            } else if (level.getGameTime() % 8 == 0) {
                serverLevel.sendParticles(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.75, pos.getZ() + 0.5, 3,
                        0.25, 0.2, 0.25, 0.01, 0.03, 0.01);
            }
            self.model.setActive(self.canDamage = false);

            self.state = rot.getStateTextOrElse(rot.hasNoActiveProviders() ? TOO_SLOW_TEXT : TOO_SLOW_DISCONNECTED_TEXT);
        }
    }

    public double getStress() {
        if (this.active) {
            var input = new SawmillInput(this.getItem(0), this.getBlockState().getValue(SawmillBlock.BLADE_ANGLE));

            return this.currentRecipe != null ?
                    getActiveStress(
                            this.currentRecipe.value().minimumSpeed(input),
                            this.currentRecipe.value().optimalSpeed(input),
                            this.speedScale) : 1;
        }
        return 0;
    }

    public static double getActiveStress(double minimalSpeed, double optimalSpeed, double speedScale) {
        return Mth.clamp(optimalSpeed * speedScale,
                minimalSpeed,
                optimalSpeed
        ) * 0.7;
    }

    @Override
    public @Nullable Component getCurrentState() {
        return this.state;
    }

    @Override
    public Container getOwnOutputContainer() {
        return this.outputContainer;
    }

    @Override
    public Container getOutputContainer() {
        return ItemOutputBufferBlock.getOutputContainer(this.outputContainer, this.level, this.getBlockPos(), this.getBlockState().getValue(SawmillBlock.INPUT_FACING).getOpposite());
    }

    @Override
    public boolean isOutputConnectedTo(Direction dir) {
        return this.getBlockState().getValue(SawmillBlock.INPUT_FACING).getOpposite() == dir;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        if (BlockAwareAttachment.get(chunk, this.getBlockPos()) instanceof BlockAwareAttachment attachment && attachment.holder() instanceof SawmillBlock.Model model) {
            this.model = model;
            this.updatePosition(0);
            this.containers[0].maybeAdd(model);
        }
    }

    @Override
    public @Nullable BlockModel getModel() {
        return this.model;
    }

    @Override
    public void updatePosition(int id) {
        var c = containers[id];

        if (!c.isContainerEmpty()) {
            var base = Vec3.atCenterOf(this.worldPosition).add(0, 0.5, 0);
            base = base.relative(this.getBlockState().getValue(SawmillBlock.INPUT_FACING), Math.clamp(0.5 - this.process / this.requiredTime, -0.4, 0.4));

            assert c.getContainer() != null;
            c.getContainer().overrideInterpolationDuration(this.process == 0 ? 0 : 2);
            c.getContainer().setPos(base);
            c.getContainer().scale(0.5f);
        }
    }

    @Override
    public SimpleMovingItemContainer[] getContainers() {
        return this.containers;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayer player) {
            super(MenuType.GENERIC_9x3, player, false);
            this.setTitle(GuiTextures.GRINDER.apply(SawmillBlockEntity.this.getName()));
            this.setSlot(9, PolydexCompat.getButton(FactoryRecipeTypes.SAWMILL));

            this.setSlot(4, new Slot(SawmillBlockEntity.this, 0, 0, 0));
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            this.setSlot(20, new FurnaceResultSlot(player, SawmillBlockEntity.this, 1, 1, 0));
            this.setSlot(21, new FurnaceResultSlot(player, SawmillBlockEntity.this, 2, 2, 0));
            this.setSlot(22, new FurnaceResultSlot(player, SawmillBlockEntity.this, 3, 3, 0));
            this.setSlot(23, new FurnaceResultSlot(player, SawmillBlockEntity.this, 4, 4, 0));
            this.setSlot(24, new FurnaceResultSlot(player, SawmillBlockEntity.this, 5, 5, 0));


            this.setSlot(9 + 7, GuiUtils.createIteratingButton(
                    () -> getBlockState().getValue(SawmillBlock.BLADE_ANGLE),
                    val -> getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(SawmillBlock.BLADE_ANGLE, val)),
                    SawmillBlock.BladeAngle.values(),
                    SawmillBlock.BladeAngle::getButton
            ));

            this.open();
        }

        private float progress() {
            return SawmillBlockEntity.this.currentRecipe != null
                    ? (float) Mth.clamp(SawmillBlockEntity.this.process / SawmillBlockEntity.this.currentRecipe.value().time(
                            new SawmillInput(SawmillBlockEntity.this.getItem(0), SawmillBlockEntity.this.getBlockState().getValue(SawmillBlock.BLADE_ANGLE))), 0, 1)
                    : 0;
        }

        @Override
        public void onTick() {
            if (player.position().distanceToSqr(Vec3.atCenterOf(SawmillBlockEntity.this.worldPosition)) > (18 * 18)) {
                this.close();
            }
            this.setSlot(13, GuiTextures.PROGRESS_VERTICAL.get(progress()));
            super.onTick();
        }
    }
}
