package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.other.FactoryBiomeTags;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class WindmillBlockEntity extends BlockEntity {
    private static final double LOG_BASE = Math.log(2);
    private final DefaultedList<ItemStack> sails = DefaultedList.of();
    private int sample = Integer.MIN_VALUE;

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WINDMILL, pos, state);
        for (int i = 0; i < state.get(WindmillBlock.SAIL_COUNT); i++) {
            sails.add(new ItemStack(FactoryItems.WINDMILL_SAIL));
        }
    }


    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        var list = new NbtList();
        for (var sail : this.sails) {
            if (!sail.isEmpty()) {
                list.add(sail.toNbt(lookup));
            } else {
                list.add(new NbtCompound());
            }
        }
        nbt.put("Sails", list);
    }


    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        this.sails.clear();
        for (var sail : nbt.getListOrEmpty("Sails")) {
            this.sails.add(FactoryUtil.fromNbtStack(lookup, sail));
        }
    }

    public boolean addSail(int i, ItemStack stack) {
        if (stack.isOf(FactoryItems.WINDMILL_SAIL)) {
            if (i < this.sails.size()) {
                this.sails.set(i, stack.copyWithCount(1));
            } else {
                this.sails.add(stack.copyWithCount(1));
            }
            stack.decrement(1);
            this.markDirty();

            var model = BlockBoundAttachment.get(this.world, this.pos);

            if (model != null) {
                ((WindmillBlock.Model) model.holder()).updateSailsBe();
            }

            return true;
        }
        return false;
    }

    public int getSailColor(int i) {
        if (i < this.sails.size()) {
            var sail = this.sails.get(i);

            if (sail.contains(DataComponentTypes.DYED_COLOR)) {
                //noinspection DataFlowIssue
                return sail.get(DataComponentTypes.DYED_COLOR).rgb();
            }
        }

        return 0xFFFFFF;
    }

    public DefaultedList<ItemStack> getSails() {
        return this.sails;
    }

    // https://www.desmos.com/calculator/u7cstq97vr
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerWorld serverWorld, BlockPos pos) {
        if (state.get(WindmillBlock.WATERLOGGED)) {
            return;
        }

        var baseHeight = this.sample;
        if (baseHeight == Integer.MIN_VALUE) {
            baseHeight = serverWorld.getChunkManager().getChunkGenerator()
                    .getHeightOnGround(pos.getX(), pos.getZ(), Heightmap.Type.MOTION_BLOCKING, serverWorld, serverWorld.getChunkManager().getNoiseConfig());
            this.sample = baseHeight;
        }
        var sails = state.get(WindmillBlock.SAIL_COUNT);

        double x;

        if (serverWorld.getRegistryKey().equals(World.NETHER)) {
            x = 32 - Math.abs(pos.getY() - 64) / 2d;
        } else if (serverWorld.getRegistryKey().equals(World.END)) {
            x = 5;
        } else {
            x = pos.getY() - baseHeight - 2;
        }

        if (x <= 0 || sails < 2) {
            modifier.stress(0.15);
            return;
        }

        var speed = Math.min(Math.log(x) / LOG_BASE * 2.1, 10.5);
        if (speed <= 0) {
            return;
        }

        var biome = serverWorld.getBiome(pos);
        if (biome.isIn(FactoryBiomeTags.WINDMILL_HIGH_SPEED_BONUS)) {
            speed *= 1.35;
        } else if (biome.isIn(FactoryBiomeTags.WINDMILL_MIDDLE_SPEED_BONUS)) {
            speed *= 1.2;
        } else if (biome.isIn(FactoryBiomeTags.WINDMILL_LOW_SPEED_BONUS)) {
            speed *= 1.08;
        }

        if (serverWorld.isRaining()) {
            speed *= 1.1;
        }

        modifier.provide(speed, MathHelper.clamp(speed * 0.15 * sails * 1.2, 0.5, 18), false);
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        super.onBlockReplaced(pos, oldState);
        if (this.world != null) {
            ItemScatterer.spawn(world, pos, this.getSails());
        }
    }
}
