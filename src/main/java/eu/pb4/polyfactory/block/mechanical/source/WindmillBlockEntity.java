package eu.pb4.polyfactory.block.mechanical.source;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polyfactory.other.FactoryBiomeTags;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class WindmillBlockEntity extends BlockEntity {
    private static final double LOG_BASE = Math.log(2);
    private final NonNullList<ItemStack> sails = NonNullList.create();
    private int sample = Integer.MIN_VALUE;

    public WindmillBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WINDMILL, pos, state);
        for (int i = 0; i < state.getValue(WindmillBlock.SAIL_COUNT); i++) {
            sails.add(new ItemStack(FactoryItems.WINDMILL_SAIL));
        }
    }


    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        var list = view.list("Sails", ItemStack.OPTIONAL_CODEC);
        for (var sail : this.sails) {
            list.add(sail);
        }
    }


    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.sails.clear();
        for (var sail : view.listOrEmpty("Sails", ItemStack.OPTIONAL_CODEC)) {
            this.sails.add(sail);
        }
    }

    public boolean addSail(int i, ItemStack stack) {
        if (stack.is(FactoryItems.WINDMILL_SAIL)) {
            if (i < this.sails.size()) {
                this.sails.set(i, stack.copyWithCount(1));
            } else {
                this.sails.add(stack.copyWithCount(1));
            }
            stack.shrink(1);
            this.setChanged();

            var model = BlockBoundAttachment.get(this.level, this.worldPosition);

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

            if (sail.has(DataComponents.DYED_COLOR)) {
                //noinspection DataFlowIssue
                return sail.get(DataComponents.DYED_COLOR).rgb();
            }
        }

        return 0xFFFFFF;
    }

    public NonNullList<ItemStack> getSails() {
        return this.sails;
    }

    // https://www.desmos.com/calculator/u7cstq97vr
    public void updateRotationalData(RotationData.State modifier, BlockState state, ServerLevel serverWorld, BlockPos pos) {
        if (state.getValue(WindmillBlock.WATERLOGGED)) {
            return;
        }

        var baseHeight = this.sample;
        if (baseHeight == Integer.MIN_VALUE) {
            baseHeight = serverWorld.getChunkSource().getGenerator()
                    .getFirstFreeHeight(pos.getX(), pos.getZ(), Heightmap.Types.MOTION_BLOCKING, serverWorld, serverWorld.getChunkSource().randomState());
            this.sample = baseHeight;
        }
        var sails = state.getValue(WindmillBlock.SAIL_COUNT);

        double x;

        if (serverWorld.dimension().equals(Level.NETHER)) {
            x = 32 - Math.abs(pos.getY() - 64) / 2d;
        } else if (serverWorld.dimension().equals(Level.END)) {
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
        if (biome.is(FactoryBiomeTags.WINDMILL_HIGH_SPEED_BONUS)) {
            speed *= 1.35;
        } else if (biome.is(FactoryBiomeTags.WINDMILL_MIDDLE_SPEED_BONUS)) {
            speed *= 1.2;
        } else if (biome.is(FactoryBiomeTags.WINDMILL_LOW_SPEED_BONUS)) {
            speed *= 1.08;
        }

        if (serverWorld.isRaining()) {
            speed *= 1.1;
        }

        modifier.provide(speed, Mth.clamp(speed * 0.15 * sails * 1.2, 0.5, 18), false);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        super.preRemoveSideEffects(pos, oldState);
        if (this.level != null) {
            Containers.dropContents(level, pos, this.getSails());
        }
    }
}
