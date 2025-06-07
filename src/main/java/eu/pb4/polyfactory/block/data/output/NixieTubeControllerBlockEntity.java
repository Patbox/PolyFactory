package eu.pb4.polyfactory.block.data.output;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

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
    public void readData(ReadView view) {
        super.readData(view);
        this.scrollSpeed = view.getInt("scroll_speed", 0);
        this.scrollLoop = view.getBoolean("scroll_loop", false);
        this.scrollPoint = view.getInt("scroll_point", 0);
        this.text = view.getString("text", "");
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("scroll_speed", this.scrollSpeed);
        view.putInt("scroll_point", this.scrollPoint);
        view.putBoolean("scroll_loop", this.scrollLoop);
        view.putString("text", this.text);
    }

    public boolean receiveData(DataContainer data) {
        this.setCachedData(data);
        var dir = this.getCachedState().get(NixieTubeControllerBlock.FACING);
        if (this.world.getBlockEntity(this.pos.offset(dir)) instanceof NixieTubeBlockEntity nixieTube && canConnect(nixieTube, dir)) {
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
                    string = string.repeat(Math.max(2, MathHelper.ceilDiv(length, string.length())));
                }
            }

            this.scrollPoint = 0;
            this.text = string;
            nixieTube.pushText(string, data.padding());
            if (this.getCachedState().get(NixieTubeControllerBlock.POWERED)) {
                this.world.setBlockState(pos, this.getCachedState().with(NixieTubeControllerBlock.POWERED, false));
            }
            this.markDirty();
        }

        return true;
    }

    private static boolean canConnect(NixieTubeBlockEntity nixieTube, Direction dir) {
        if (dir.getAxis() == Direction.Axis.Y) {
            return (nixieTube.getCachedState().get(NixieTubeBlock.HALF) == BlockHalf.TOP) == (dir.getDirection() == Direction.AxisDirection.NEGATIVE);
        } else {
            return nixieTube.getCachedState().get(NixieTubeBlock.AXIS) == dir.getAxis();
        }
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        var self = (NixieTubeControllerBlockEntity) t;

        if (self.scrollSpeed != 0 && self.tick % self.scrollSpeed == 0) {
            self.tick++;
            var data = self.getCachedData();
            var dir = state.get(NixieTubeControllerBlock.FACING);

            if (data != null && world.getBlockEntity(pos.offset(dir)) instanceof NixieTubeBlockEntity nixieTube && canConnect(nixieTube, dir)) {
                var length = nixieTube.connectionSize() * 2;
                var string = data.asString();
                if ((string.length() < length && !self.scrollLoop) || string.isEmpty()) {
                    if (!state.get(NixieTubeControllerBlock.POWERED)) {
                        world.setBlockState(pos, state.with(NixieTubeControllerBlock.POWERED, true));
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
                    string = string.repeat(Math.max(2, MathHelper.ceilDiv(length, string.length())));
                }

                if ((self.scrollLoop || (string.length() - self.scrollPoint >= length)) && state.get(NixieTubeControllerBlock.POWERED)) {
                    world.setBlockState(pos, state.with(NixieTubeControllerBlock.POWERED, false));
                } else if (!self.scrollLoop && (string.length() - self.scrollPoint < length) && !state.get(NixieTubeControllerBlock.POWERED)) {
                    world.setBlockState(pos, state.with(NixieTubeControllerBlock.POWERED, true));
                }

                self.text = string.substring(Math.min(self.scrollPoint, string.length()), Math.min(self.scrollPoint + length, string.length()));
                nixieTube.pushText(self.text, data.padding());
                self.scrollPoint++;

                self.markDirty();
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
        this.markDirty();
    }

    public void setScrollSpeed(int scrollSpeed) {
        this.scrollSpeed = scrollSpeed;
        this.markDirty();
    }
}
