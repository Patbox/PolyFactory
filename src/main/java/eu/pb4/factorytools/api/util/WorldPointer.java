package eu.pb4.factorytools.api.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.InventoryProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class WorldPointer {
    private final ServerWorld world;
    private final BlockPos pos;
    private BlockState blockState;
    private BlockEntity blockEntity;
    private Inventory inventory;
    private boolean requireBlockEntityCheck = true;
    private boolean requireInventoryCheck = true;

    public WorldPointer(World world, BlockPos pos) {
        this.world = (ServerWorld) world;
        this.pos = pos;
    }

    public double getX() {
        return this.pos.getX() + 0.5;
    }

    public double getY() {
        return this.pos.getY() + 0.5;
    }

    public double getZ() {
        return this.pos.getZ() + 0.5;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public BlockState getBlockState() {
        if (this.blockState == null) {
            this.blockState = this.world.getBlockState(this.pos);
        }
        return this.blockState;
    }

    public <T extends BlockEntity> T getBlockEntity() {
        if (this.requireBlockEntityCheck) {
            this.blockEntity = this.world.getBlockEntity(this.pos);
            this.requireBlockEntityCheck = false;
        }
        return (T) this.blockEntity;
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public Inventory getInventory() {
        if (this.requireInventoryCheck) {
            if (this.getBlockState().getBlock() instanceof InventoryProvider provider) {
                this.inventory = provider.getInventory(this.blockState, this.world, this.pos);
            } else {
                var be = this.getBlockEntity();

                if (be instanceof Inventory inventory) {
                    this.inventory = inventory;
                }
            }

            this.requireInventoryCheck = false;
        }
        return this.inventory;
    }
}
