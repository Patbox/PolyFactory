package eu.pb4.polyfactory.block.collection;

import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

import java.util.Collection;
import java.util.function.Consumer;

public interface BlockCollectionView {
    void polyfactory$provideCollision(Box box, Consumer<VoxelShape> consumer);
    void polyfactory$addCollision(BlockCollection collection);
    void polyfactory$removeCollision(BlockCollection collection);
}
