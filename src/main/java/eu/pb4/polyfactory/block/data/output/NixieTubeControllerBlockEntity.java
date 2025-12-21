package eu.pb4.polyfactory.block.data.output;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class NixieTubeControllerBlockEntity extends ChanneledDataBlockEntity {
    private int scrollSpeed = 0;
    private int scrollPoint = 0;
    private boolean scrollLoop = false;
    private String text = "";
    private int tick = 0;

    public NixieTubeControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.NIXIE_TUBE_CONTROLLER, pos, state);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.scrollSpeed = view.getIntOr("scroll_speed", 0);
        this.scrollLoop = view.getBooleanOr("scroll_loop", false);
        this.scrollPoint = view.getIntOr("scroll_point", 0);
        this.text = view.getStringOr("text", "");
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putInt("scroll_speed", this.scrollSpeed);
        view.putInt("scroll_point", this.scrollPoint);
        view.putBoolean("scroll_loop", this.scrollLoop);
        view.putString("text", this.text);
    }

    public boolean receiveData(DataContainer data) {
        this.setCachedData(data);
        var dir = this.getBlockState().getValue(NixieTubeControllerBlock.FACING);
        if (this.level.getBlockEntity(this.worldPosition.relative(dir)) instanceof NixieTubeBlockEntity nixieTube && canConnect(nixieTube, dir)) {
            var length = nixieTube.connectionSize() * 2;
            var string = data.asString();

            if (!string.isEmpty()) {
                if (data.forceRight()) {
                    if (string.length() > length) {
                        string = string.substring(string.length() - length);
                    } else {
                        string = Character.toString(data.padding()).repeat(length - string.length()) + string;
                    }
                } else {
                    string = string.substring(0, Math.min(length, string.length()));
                }
                if (string.length() < length && this.scrollLoop) {
                    string = string.repeat(Math.max(2, Mth.positiveCeilDiv(length, string.length())));
                }
            }

            this.scrollPoint = 0;
            this.text = string;
            nixieTube.pushText(string, data.padding());
            if (this.getBlockState().getValue(NixieTubeControllerBlock.POWERED)) {
                this.level.setBlockAndUpdate(worldPosition, this.getBlockState().setValue(NixieTubeControllerBlock.POWERED, false));
            }
            this.setChanged();
        }

        return true;
    }

    private static boolean canConnect(NixieTubeBlockEntity nixieTube, Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) {
            return (nixieTube.getBlockState().getValue(NixieTubeBlock.HALF) == Half.TOP) == (dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE);
        } else {
            return nixieTube.getBlockState().getValue(NixieTubeBlock.AXIS) == dir.getAxis();
        }
    }

    public static <T extends BlockEntity> void tick(Level world, BlockPos pos, BlockState state, T t) {
        var self = (NixieTubeControllerBlockEntity) t;

        if (self.scrollSpeed != 0 && self.tick % self.scrollSpeed == 0) {
            self.tick++;
            var data = self.getCachedData();
            var dir = state.getValue(NixieTubeControllerBlock.FACING);

            if (data != null && world.getBlockEntity(pos.relative(dir)) instanceof NixieTubeBlockEntity nixieTube && canConnect(nixieTube, dir)) {
                var length = nixieTube.connectionSize() * 2;
                var string = data.asString();
                if ((string.length() < length && !self.scrollLoop) || string.isEmpty()) {
                    if (!state.getValue(NixieTubeControllerBlock.POWERED)) {
                        world.setBlockAndUpdate(pos, state.setValue(NixieTubeControllerBlock.POWERED, true));
                    }

                    return;
                }

                if (data.forceRight()) {
                    if (string.length() > length) {
                        string = string.substring(string.length() - length);
                    } else {
                        string = Character.toString(data.padding()).repeat(length - string.length()) + string;
                    }
                }

                if (self.scrollPoint >= string.length()) {
                    if (self.scrollLoop) {
                        self.scrollPoint = 0;
                    } else {
                        string = "";
                    }
                }

                if (string.length() - self.scrollPoint < length && self.scrollLoop) {
                    string = string.repeat(Math.max(2, Mth.positiveCeilDiv(length, string.length())));
                }

                if ((self.scrollLoop || (string.length() - self.scrollPoint >= length)) && state.getValue(NixieTubeControllerBlock.POWERED)) {
                    world.setBlockAndUpdate(pos, state.setValue(NixieTubeControllerBlock.POWERED, false));
                } else if (!self.scrollLoop && (string.length() - self.scrollPoint < length) && !state.getValue(NixieTubeControllerBlock.POWERED)) {
                    world.setBlockAndUpdate(pos, state.setValue(NixieTubeControllerBlock.POWERED, true));
                }

                self.text = string.substring(Math.min(self.scrollPoint, string.length()), Math.min(self.scrollPoint + length, string.length()));
                nixieTube.pushText(self.text, data.padding());
                self.scrollPoint++;

                self.setChanged();
            }
        } else {
            self.tick++;
        }
    }

    public boolean scrollLoop() {
        return this.scrollLoop;
    }

    public int scrollSpeed() {
        return this.scrollSpeed;
    }

    public void setScrollLoop(boolean scrollLoop) {
        this.scrollLoop = scrollLoop;
        this.setChanged();
    }

    public void setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
        this.setChanged();
    }
}
