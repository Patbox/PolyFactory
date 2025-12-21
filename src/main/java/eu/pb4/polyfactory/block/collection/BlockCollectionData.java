package eu.pb4.polyfactory.block.collection;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record BlockCollectionData(int sizeX, int sizeY, int sizeZ, BlockState[] states, BlockEntity[] blockEntities) {
    public BlockCollectionData(int x, int y, int z) {
        this(x, y, z, new BlockState[x * y * z], new BlockEntity[x * y * z]);
    }
    public int size() {
        return this.sizeX * this.sizeY * this.sizeZ;
    }

    public int index(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < this.sizeX && y < this.sizeY && z < this.sizeZ ? (x + y * this.sizeX) * this.sizeZ + z : -1;
    }

    public boolean setBlockState(int x, int y, int z, BlockState state) {
        return setBlockState(x, y, z, state, null);
    }

    public boolean setBlockState(int x, int y, int z, BlockState state, @Nullable BlockEntity blockEntity) {
        var i = index(x, y, z);
        if (i == -1) {
            return false;
        }
        if (this.states[i] == state) {
            this.blockEntities[i] = blockEntity;
            return false;
        }
        this.states[i] = state;
        this.blockEntities[i] = blockEntity;
        return true;
    }

    public BlockState getBlockState(int x, int y, int z) {
        var index = index(x, y, z);
        if (index == -1) {
            return Blocks.AIR.defaultBlockState();
        }
        var state = this.states[index];
        if (state == null) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    public static BlockCollectionData createDebug() {
        var data = new BlockCollectionData(9, 9, 9);

        for (int y = 0; y < 4; y++) {
            data.setBlockState(4, y, 4, Blocks.STONE.defaultBlockState(), null);
        }

        for (int x = 0; x <= 8; x++) {
            data.setBlockState(x, 3, 4, Blocks.TNT.defaultBlockState(), null);
            data.setBlockState(4, 3, x, Blocks.TNT.defaultBlockState(), null);
        }

        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                data.setBlockState(x, 3, z, Blocks.GLASS.defaultBlockState(), null);
            }
            data.setBlockState(2, 4, x, Blocks.GLASS.defaultBlockState(), null);
            data.setBlockState(6, 4, x, Blocks.GLASS.defaultBlockState(), null);
            data.setBlockState(x, 4, 2, Blocks.GLASS.defaultBlockState(), null);
            data.setBlockState(x, 4, 6, Blocks.GLASS.defaultBlockState(), null);
        }
        data.setBlockState(4, 5, 4, Blocks.TORCH.defaultBlockState(), null);
        return data;
    }
}
