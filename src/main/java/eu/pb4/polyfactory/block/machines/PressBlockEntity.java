package eu.pb4.polyfactory.block.machines;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationalSource;
import eu.pb4.polyfactory.block.mechanical.conveyor.ConveyorBlockEntity;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.display.LodItemDisplayElement;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.recipe.PressRecipe;
import eu.pb4.polyfactory.util.inventory.MinimalSidedInventory;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.elements.BlockDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class PressBlockEntity extends BlockEntity implements MinimalSidedInventory {
    public static final int INPUT_SLOT = 0;
    public static final int TEMPLATE_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int[] INPUT_SLOTS = { INPUT_SLOT };
    private static final int[] TEMPLATE_SLOTS = { TEMPLATE_SLOT };
    private static final int[] OUTPUT_SLOTS = { OUTPUT_SLOT };
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private Animation animator;
    protected double process = 0;
    @Nullable
    protected PressRecipe currentRecipe = null;
    @Nullable
    protected Item currentItem = null;
    protected int currentItemCount = -1;

    public PressBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.PRESS, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        Inventories.writeNbt(nbt, stacks);
        nbt.putDouble("Progress", this.process);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        Inventories.readNbt(nbt, this.stacks);
        this.process = nbt.getDouble("Progress");
    }

    @Override
    public DefaultedList<ItemStack> getStacks() {
        return this.stacks;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        return facing == side ? INPUT_SLOTS : (facing.getOpposite() == side ? OUTPUT_SLOTS : new int[0]);
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return slot == INPUT_SLOT && (dir == null || this.getCachedState().get(PressBlock.INPUT_FACING) == dir);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        var facing = this.getCachedState().get(PressBlock.INPUT_FACING);
        return (slot == INPUT_SLOT && facing == dir) || (slot != INPUT_SLOT && facing.getOpposite() == dir);
    }

    public void openGui(ServerPlayerEntity player) {
        new Gui(player);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (PressBlockEntity) t;

        if (self.animator == null) {
            self.animator = Animation.create(self);
        }
        var stack = self.getStack(0);
        self.animator.setItem(stack);
        if (stack.isEmpty()) {
            if (self.process != 0) {
                self.animator.updatePiston(0);
            }
            self.process = 0;
            self.animator.tick();
            return;
        }

        if (self.currentRecipe == null && self.currentItem != null && stack.isOf(self.currentItem) && stack.getCount() == self.currentItemCount) {
            if (self.process != 0) {
                self.animator.updatePiston(0);
            }
            self.process = 0;
            self.animator.tick();
            return;
        }

        if (self.currentRecipe == null || !self.currentRecipe.matches(self, world)) {
            if (self.process != 0) {
                self.animator.updatePiston(0);
            }
            self.process = 0;
            self.currentItem = stack.getItem();
            self.currentItemCount = stack.getCount();
            self.currentRecipe = world.getRecipeManager().getFirstMatch(FactoryRecipeTypes.PRESS, self, world).orElse(null);

            if (self.currentRecipe == null) {
                self.animator.tick();
                return;
            }
        }

        if (self.process >= 1) {
            if (self.getStack(OUTPUT_SLOT).isEmpty()) {
                self.process = -0.2;
                stack.decrement(self.currentRecipe.inputCount());
                var out = self.currentRecipe.craft(self, self.world.getRegistryManager());

                if (state.get(PressBlock.HAS_CONVEYOR) && world.getBlockEntity(pos.offset(state.get(PressBlock.INPUT_FACING).getOpposite())) instanceof ConveyorBlockEntity be) {
                    if (be.tryAdding(out) && !out.isEmpty()) {
                        self.setStack(OUTPUT_SLOT, out);
                    }
                } else {
                    self.setStack(OUTPUT_SLOT, out);
                }
            }
        } else {
            var speed = Math.max(Math.abs(RotationalSource.getNetworkSpeed((ServerWorld) world, pos.up(2))), 0);

            if (speed >= self.currentRecipe.minimumSpeed() && self.getStack(OUTPUT_SLOT).isEmpty()) {
                /*if (world.getTime() % MathHelper.clamp(Math.round(1 / speed), 2, 5) == 0) {
                    var dir = state.get(PressBlock.INPUT_FACING).getAxis();
                    ((ServerWorld) world).spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, stack.copy()), pos.getX() + 0.5, pos.getY() + 1.15, pos.getZ() + 0.5, 0, (Math.random() - 0.5) * (dir == Direction.Axis.Z ? 2 : 0.2), 0.02, (Math.random() - 0.5) * (dir == Direction.Axis.X ? 2 : 0.2), 2);
                }*/
                self.process += speed / 5;
                if (self.process >= 0 && (self.process - speed / 5) * 1.2 <= 1) {
                    self.animator.updatePiston(self.process * 1.2);
                } else if (self.process < 0) {
                    self.animator.updatePiston(-self.process);
                }
            }
        }
        self.animator.tick();

    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (this.animator != null) {
            this.animator.destroy();
            this.animator = null;
        }
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.GENERIC_3X3, player, false);
            this.setTitle(PressBlockEntity.this.getCachedState().getBlock().getName());
            var x= new GuiElementBuilder(Items.WHITE_STAINED_GLASS_PANE).setName(Text.empty());
            this.setSlot(1, x);
            this.setSlotRedirect(0, new Slot(PressBlockEntity.this, INPUT_SLOT, 0, 0));
            this.setSlotRedirect(2, new Slot(PressBlockEntity.this, TEMPLATE_SLOT, 0, 0));
            this.setSlot(3, x);
            this.setSlot(4, new GuiElementInterface() {
                @Override
                public ItemStack getItemStack() {
                    return new GuiElementBuilder(Items.GOLDEN_PICKAXE)
                            .setName(Text.empty())
                            .hideFlags()
                            .setDamage(PressBlockEntity.this.currentRecipe != null
                                    ? Items.GOLDEN_PICKAXE.getMaxDamage() - (int) (MathHelper.clamp(PressBlockEntity.this.process, 0, 1) * Items.GOLDEN_PICKAXE.getMaxDamage())
                                    : Items.GOLDEN_PICKAXE.getMaxDamage()
                            ).asStack();
                }
            });
            this.setSlot(5, x);
            this.setSlotRedirect(7, new FurnaceOutputSlot(player, PressBlockEntity.this, OUTPUT_SLOT, 1, 0));
            this.setSlot(6, x);
            this.setSlot(8, x);

            this.open();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(PressBlockEntity.this.pos)) > (18*18)) {
                this.close();
            }
            super.onTick();
        }
    }

    private record Animation(PressBlockEntity self, LodElementHolder holder, BlockDisplayElement pushingElement,
                             LodItemDisplayElement itemElement, Matrix4f matrix4f) {
        public static Animation create(PressBlockEntity self) {
            var holder = new LodElementHolder();
            var baseModel = new ItemDisplayElement();
            var pushingElement = new BlockDisplayElement();
            var itemElement = new LodItemDisplayElement();


            baseModel.setDisplayWidth(1);
            baseModel.setDisplayHeight(3);
            pushingElement.setDisplayWidth(1);
            pushingElement.setDisplayHeight(3);
            itemElement.setDisplayWidth(1);
            itemElement.setDisplayHeight(3);

            holder.addElement(baseModel);
            holder.addElement(pushingElement);
            holder.addElement(itemElement);

            itemElement.setTransformation(new Matrix4f().translate(0, 0.6f, 0).rotateX(MathHelper.HALF_PI).scale(0.8f));
            new ChunkAttachment(holder, (WorldChunk) self.getWorld().getChunk(self.pos), Vec3d.ofCenter(self.pos), false);

            var a = new Animation(self, holder, pushingElement, itemElement, new Matrix4f());
            a.updatePiston(self.process);
            a.setItem(self.getStack(0));
            return a;
        }

        public void tick() {
            this.holder.tick();
        }

        public void destroy() {
            this.holder.destroy();
        }

        public void updatePiston(double process) {
            this.pushingElement.setInterpolationDuration(1);
            this.pushingElement.setBlockState(Blocks.PISTON_HEAD.getDefaultState().with(PistonHeadBlock.FACING, Direction.DOWN).with(PistonHeadBlock.SHORT, process < 0.8));
            this.pushingElement.setTransformation(matrix4f.translation( -0.5f, (float) (1.5 - MathHelper.clamp(Math.pow(process, 3), 0, 1)), -0.5f));

            if (this.pushingElement.isDirty()) {
                this.pushingElement.startInterpolation();
            }
        }

        public void setItem(ItemStack stack) {
            this.itemElement.setItem(stack.copyWithCount(1));
        }
    }
}
