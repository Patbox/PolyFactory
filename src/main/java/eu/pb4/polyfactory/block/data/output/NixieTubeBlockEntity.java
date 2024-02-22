package eu.pb4.polyfactory.block.data.output;

import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.function.Predicate;

public class NixieTubeBlockEntity extends BlockEntity implements BlockEntityExtraListener {
    private String value = "";
    private char padding = ' ';
    private int positiveIndex = 0;
    private int negativeIndex = 0;
    private int connectionSize = 1;
    private int color = 0xff6e19;
    private NixieTubeBlock.Model model;
    public NixieTubeBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.NIXIE_TUBE, pos, state);
    }

    public int connectionSize() {
        return connectionSize;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Text", this.value);
        nbt.putInt("PIndex", this.positiveIndex);
        nbt.putInt("NIndex", this.negativeIndex);
        nbt.putInt("ConnSize", this.connectionSize);
        nbt.putInt("Color", this.color);
        nbt.putInt("Padding", this.padding);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.value = nbt.getString("Text");
        this.positiveIndex = nbt.getInt("PIndex");
        this.negativeIndex = nbt.getInt("NIndex");
        this.color = nbt.getInt("Color");
        this.connectionSize = nbt.getInt("ConnSize");
        this.padding = (char) nbt.getInt("Padding");
        this.updateTextDisplay();
    }

    public boolean setIndex(int positive, int negative) {
        if (this.positiveIndex == positive && this.negativeIndex == negative) {
            return false;
        }
        this.positiveIndex = positive;
        this.negativeIndex = negative;
        this.markDirty();
        return true;
    }

    public boolean setText(String value) {
        if (!this.value.equals(value)) {
            this.value = value;
            this.markDirty();
            return true;
        }
        return false;
    }

    @Override
    public void onListenerUpdate(WorldChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.getPos()).holder() instanceof NixieTubeBlock.Model model ? model : null;
        this.updateTextDisplay();
    }

    public void pushText(String string, char padding) {
        String finalString = string;
        pushUpdate((tube) -> {
            var b = tube.setText(finalString);
            if (b) {
                tube.padding = padding;
                tube.updateTextDisplay();
            }
            return b;
        });
    }
    public void pushUpdate(Predicate<NixieTubeBlockEntity> modifierAndPredicate) {
        var axis = this.getCachedState().get(NixieTubeBlock.AXIS);
        var dir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        if (!modifierAndPredicate.test(this)) {
            return;
        }
        this.updateTextDisplay();
        var mut = this.pos.mutableCopy();

        if (this.getCachedState().get(NixieTubeBlock.NEGATIVE_CONNECTED)) {
            while (true) {
                assert world != null;
                if (world.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                        && tube.getCachedState().get(NixieTubeBlock.AXIS) == axis && tube.getCachedState().get(NixieTubeBlock.POSITIVE_CONNECTED)) {
                    if (modifierAndPredicate.test(tube)) {
                        if (tube.getCachedState().get(NixieTubeBlock.NEGATIVE_CONNECTED)) {
                            continue;
                        }
                    }
                }
                break;
            }
            mut.set(pos);
        }
        dir = dir.getOpposite();
        if (this.getCachedState().get(NixieTubeBlock.POSITIVE_CONNECTED)) {
            while (true) {
                assert world != null;
                if (world.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                        && tube.getCachedState().get(NixieTubeBlock.AXIS) == axis && tube.getCachedState().get(NixieTubeBlock.NEGATIVE_CONNECTED)) {

                    if (modifierAndPredicate.test(tube)) {
                        if (tube.getCachedState().get(NixieTubeBlock.POSITIVE_CONNECTED)) {
                            continue;
                        }
                    }
                }
                break;
            }
        }
    }

    public void updateTextDisplay() {
        if (this.model == null) {
            return;
        }
        this.model.setColor(this.color);
        this.model.setText(getCharSafe(this.positiveIndex * 2), getCharSafe(this.positiveIndex * 2 + 1), getCharSafe(this.negativeIndex * 2), getCharSafe(this.negativeIndex * 2 + 1));
    }

    public boolean setColor(int color) {
        if (this.color == color) {
            return false;
        }
        this.color = color;
        return true;
    }

    private char getCharSafe(int i) {
        return i >= 0 && i < this.value.length() ? this.value.charAt(i) : this.padding;
    }

    public void updatePositions(World world, BlockPos pos, BlockState newState) {
        var list = new ArrayList<NixieTubeBlockEntity>();
        var mut = pos.mutableCopy();
        list.add(this);
        var axis = newState.get(NixieTubeBlock.AXIS);
        var half = newState.get(NixieTubeBlock.HALF);
        var dir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        int max = 64;

        while (true) {
            if (world.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                    && tube.getCachedState().get(NixieTubeBlock.AXIS) == axis && tube.getCachedState().get(NixieTubeBlock.HALF) == half) {
                list.add(0, tube);

                if (max-- == 0) {
                    return;
                }
                continue;
            }
            break;
        }
        mut.set(pos);
        dir = dir.getOpposite();
        max = 64;
        while (true) {
            if (world.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                    && tube.getCachedState().get(NixieTubeBlock.AXIS) == axis && tube.getCachedState().get(NixieTubeBlock.HALF) == half) {
                list.add(tube);
                if (max-- == 0) {
                    return;
                }
                continue;
            }
            break;
        }
        int positive = 0;
        int negative = list.size() - 1;

        for (var entry : list) {
            boolean dirty = false;
            dirty |= entry.setIndex(positive++, negative--);
            dirty |= entry.setText(this.value);
            entry.connectionSize = list.size();
            if (dirty) {
                entry.updateTextDisplay();
            }
        }
        if (list.size() >= 3 && FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayerEntity player) {
            TriggerCriterion.trigger(player, FactoryTriggers.NIXIE_TUBE_CONNECTED_3_OR_MORE);
        }
    }
}
