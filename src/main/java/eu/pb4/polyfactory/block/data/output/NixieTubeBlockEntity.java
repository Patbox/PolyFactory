package eu.pb4.polyfactory.block.data.output;

import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.BlockEntityExtraListener;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putString("Text", this.value);
        view.putInt("PIndex", this.positiveIndex);
        view.putInt("NIndex", this.negativeIndex);
        view.putInt("ConnSize", this.connectionSize);
        view.putInt("Color", this.color);
        view.putInt("Padding", this.padding);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        this.value = view.getStringOr("Text", "");
        this.positiveIndex = view.getIntOr("PIndex", 0);
        this.negativeIndex = view.getIntOr("NIndex", 0);
        this.color = view.getIntOr("Color", 0);
        this.connectionSize = view.getIntOr("ConnSize", 0);
        this.padding = (char) view.getIntOr("Padding", 0);
        this.updateTextDisplay();
    }

    public boolean setIndex(int positive, int negative) {
        if (this.positiveIndex == positive && this.negativeIndex == negative) {
            return false;
        }
        this.positiveIndex = positive;
        this.negativeIndex = negative;
        this.setChanged();
        return true;
    }

    public boolean setText(String value) {
        if (!this.value.equals(value)) {
            this.value = value;
            this.setChanged();
            return true;
        }
        return false;
    }

    @Override
    public void onListenerUpdate(LevelChunk chunk) {
        this.model = BlockBoundAttachment.get(chunk, this.getBlockPos()).holder() instanceof NixieTubeBlock.Model model ? model : null;
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
        var axis = this.getBlockState().getValue(NixieTubeBlock.AXIS);
        var dir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        if (!modifierAndPredicate.test(this)) {
            return;
        }
        this.updateTextDisplay();
        var mut = this.worldPosition.mutable();

        if (this.getBlockState().getValue(NixieTubeBlock.NEGATIVE_CONNECTED)) {
            while (true) {
                assert level != null;
                if (level.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                        && tube.getBlockState().getValue(NixieTubeBlock.AXIS) == axis && tube.getBlockState().getValue(NixieTubeBlock.POSITIVE_CONNECTED)) {
                    if (modifierAndPredicate.test(tube)) {
                        if (tube.getBlockState().getValue(NixieTubeBlock.NEGATIVE_CONNECTED)) {
                            continue;
                        }
                    }
                }
                break;
            }
            mut.set(worldPosition);
        }
        dir = dir.getOpposite();
        if (this.getBlockState().getValue(NixieTubeBlock.POSITIVE_CONNECTED)) {
            while (true) {
                assert level != null;
                if (level.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                        && tube.getBlockState().getValue(NixieTubeBlock.AXIS) == axis && tube.getBlockState().getValue(NixieTubeBlock.NEGATIVE_CONNECTED)) {

                    if (modifierAndPredicate.test(tube)) {
                        if (tube.getBlockState().getValue(NixieTubeBlock.POSITIVE_CONNECTED)) {
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

    public void updatePositions(Level world, BlockPos pos, BlockState newState) {
        var list = new ArrayList<NixieTubeBlockEntity>();
        var mut = pos.mutable();
        list.add(this);
        var axis = newState.getValue(NixieTubeBlock.AXIS);
        var half = newState.getValue(NixieTubeBlock.HALF);
        var dir = Direction.get(Direction.AxisDirection.NEGATIVE, axis);
        int max = 64;

        while (true) {
            if (world.getBlockEntity(mut.move(dir)) instanceof NixieTubeBlockEntity tube
                    && tube.getBlockState().getValue(NixieTubeBlock.AXIS) == axis && tube.getBlockState().getValue(NixieTubeBlock.HALF) == half) {
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
                    && tube.getBlockState().getValue(NixieTubeBlock.AXIS) == axis && tube.getBlockState().getValue(NixieTubeBlock.HALF) == half) {
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
        if (list.size() >= 3 && FactoryUtil.getClosestPlayer(world, pos, 32) instanceof ServerPlayer player) {
            TriggerCriterion.trigger(player, FactoryTriggers.NIXIE_TUBE_CONNECTED_3_OR_MORE);
        }
    }
}
