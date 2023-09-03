package eu.pb4.polyfactory.block.multiblock;

import eu.pb4.polyfactory.mixin.player.ItemUsageContextAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MultiBlock extends Block implements PolymerBlock {
    private static IntProperty[] currentProperties;
    @Nullable
    public final IntProperty partX, partY, partZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final int centerBlockX;
    private final int centerBlockY;
    private final int centerBlockZ;

    public MultiBlock(int x, int y, int z, Settings settings) {
        this(x - 1, y - 1, z - 1, hackPass(x - 1, y - 1, z - 1), settings.pistonBehavior(PistonBehavior.BLOCK));
    }

    private MultiBlock(int x, int y, int z, IntProperty[] hackPass, Settings settings) {
        super(settings);
        partX = hackPass[0];
        partY = hackPass[1];
        partZ = hackPass[2];
        this.maxX = Math.max(x, 0);
        this.maxY = Math.max(y, 0);
        this.maxZ = Math.max(z, 0);

        this.centerBlockX = this.maxX / 2;
        this.centerBlockY = this.maxY / 2;
        this.centerBlockZ = this.maxZ / 2;
    }

    private static IntProperty[] hackPass(int x, int y, int z) {
        var a = new IntProperty[3];
        if (x > 0) {
            a[0] = IntProperty.of("x", 0, x);
        }

        if (y > 0) {
            a[1] = IntProperty.of("y", 0, y);
        }

        if (z > 0) {
            a[2] = IntProperty.of("z", 0, z);
        }
        currentProperties = a;
        return a;
    }

    protected boolean isValid(BlockState state, int x, int y, int z) {
        return true;
    }

    protected int getCenterBlockX(BlockState state) {
        return this.centerBlockX;
    }

    protected int getCenterBlockY(BlockState state) {
        return this.centerBlockY;
    }

    protected int getCenterBlockZ(BlockState state) {
        return this.centerBlockZ;
    }

    protected int getMaxX(BlockState state) {
        return this.maxX;
    }

    protected int getMaxY(BlockState state) {
        return this.maxY;
    }

    protected int getMaxZ(BlockState state) {
        return this.maxZ;
    }

    public boolean place(ItemPlacementContext context, BlockState state) {
        var startPlane = context.getSide();
        var hit = ((ItemUsageContextAccessor) context).callGetHitResult();
        var vec3d = hit.getPos().subtract(hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
        var mut = context.getBlockPos().mutableCopy();

        if (startPlane.getDirection() == Direction.AxisDirection.NEGATIVE) {
            mut.move(startPlane, this.getMax(state, startPlane));
        }
        mut.move(-this.getCenterBlockX(state) * startPlane.getAxis().choose(0, 1, 1),
                -this.getCenterBlockY(state) * startPlane.getAxis().choose(1, 0, 1),
                -this.getCenterBlockZ(state) * startPlane.getAxis().choose(1, 1, 0))
        ;


        var maxX = this.getMaxX(state);
        var maxY = this.getMaxY(state);
        var maxZ = this.getMaxZ(state);

        if (maxX % 2 == 1 && startPlane.getAxis() != Direction.Axis.X) {
            mut.move(vec3d.x < 0.5 ? -1 : 0, 0, 0);
        }

        if (maxY % 2 == 1 && startPlane.getAxis() != Direction.Axis.Y) {
            mut.move(0, vec3d.y < 0.5 ? -1 : 0, 0);
        }

        if (maxZ % 2 == 1 && startPlane.getAxis() != Direction.Axis.Z) {
            mut.move(0, 0, vec3d.z < 0.5 ? -1 : 0);
        }

        var corner = mut.toImmutable();

        var world = context.getWorld();

        PlayerEntity playerEntity = context.getPlayer();
        var shapeContext = playerEntity == null ? ShapeContext.absent() : ShapeContext.of(playerEntity);

        for (int x = 0; x <= maxX; x++) {
            for (int y = 0; y <= maxY; y++) {
                for (int z = 0; z <= maxX; z++) {
                    if (!this.isValid(state, x, y, z)) {
                        continue;
                    }
                    mut.set(corner).move(x, y, z);
                    var targetState = world.getBlockState(mut);
                    if (!targetState.isReplaceable() || !state.canPlaceAt(world, mut) || !context.getWorld().canPlace(state, mut, shapeContext)) {
                        return false;
                    }
                }
            }
        }

        for (int x = 0; x <= maxX; x++) {
            var posState = partX != null ? state.with(partX, x) : state;
            for (int y = 0; y <= maxY; y++) {
                posState = partY != null ? posState.with(partY, y) : posState;
                for (int z = 0; z <= maxZ; z++) {
                    if (!this.isValid(state, x, y, z)) {
                        continue;
                    }
                    posState = partZ != null ? posState.with(partZ, z) : posState;
                    context.getWorld().setBlockState(mut.set(corner).move(x, y, z), posState);
                    this.onPlacedMultiBlock(world, mut, posState, context.getPlayer(), context.getStack());
                }
            }
        }

        return true;
    }

    @Override
    public List<ItemStack> getDroppedStacks(BlockState state, LootContextParameterSet.Builder builder) {
        if (this.canDropStackFrom(state)) {
            return super.getDroppedStacks(state, builder);
        }
        return List.of();
    }

    protected boolean canDropStackFrom(BlockState state) {
        return isCenter(state);
    }

    protected void onPlacedMultiBlock(World world, BlockPos pos, BlockState state, PlayerEntity player, ItemStack stack) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        var property = getForDirection(direction);
        if (property == null) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }
        var value = state.get(property);
        var expectedSideValue = value + direction.getDirection().offset();

        if (expectedSideValue < 0 || expectedSideValue > getMax(state, direction)) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }

        var x = this.getX(state);
        var y = this.getY(state);
        var z = this.getZ(state);

        if (!this.isValid(state, x + direction.getOffsetX(), y + direction.getOffsetY(), z + direction.getOffsetZ())
                || (neighborState.isOf(this) && neighborState.get(property) == expectedSideValue)) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }

        return Blocks.AIR.getDefaultState();
    }

    protected int getX(BlockState state) {
        if (this.partX != null) {
            return state.get(this.partX);
        }
        return 0;
    }

    protected int getY(BlockState state) {
        if (this.partY != null) {
            return state.get(this.partY);
        }
        return 0;
    }

    protected int getZ(BlockState state) {
        if (this.partZ != null) {
            return state.get(this.partZ);
        }
        return 0;
    }

    @Nullable
    protected IntProperty getForDirection(Direction direction) {
        return switch (direction.getAxis()) {
            case X -> partX;
            case Y -> partY;
            case Z -> partZ;
        };
    }

    @Nullable
    protected int getMax(BlockState state, Direction direction) {
        return switch (direction.getAxis()) {
            case X -> this.getMaxX(state);
            case Y -> this.getMaxY(state);
            case Z -> this.getMaxZ(state);
        };
    }

    public BlockPos getCenter(BlockState state, BlockPos pos) {
        var x = partX != null ? state.get(partX) : 0;
        var y = partY != null ? state.get(partY) : 0;
        var z = partZ != null ? state.get(partZ) : 0;

        return pos.add(this.getCenterBlockX(state) - x, this.getCenterBlockY(state) - y, this.getCenterBlockZ(state) - z);
    }

    public boolean isCenter(BlockState state) {
        return getX(state) == getCenterBlockX(state) && getY(state) == getCenterBlockY(state) && getZ(state) == getCenterBlockZ(state);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        for (var currentProperty : currentProperties) {
            if (currentProperty != null) {
                builder.add(currentProperty);
            }
        }
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.STONE;
    }
}
