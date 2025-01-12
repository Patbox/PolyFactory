package eu.pb4.polyfactory.mixin.block;

import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.block.collection.BlockCollectionView;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(ServerWorld.class)
public class ServerWorldMixin implements BlockCollectionView {
    @Unique
    private final List<BlockCollection> collections = new ArrayList<>();

    @Override
    public void polyfactory$provideCollision(Box box, Consumer<VoxelShape> consumer) {
        for (var x : this.collections) {
            x.provideCollisions(box, consumer);
        }
    }

    @Override
    public void polyfactory$addCollision(BlockCollection collection) {
        this.collections.add(collection);
    }

    @Override
    public void polyfactory$removeCollision(BlockCollection collection) {
        this.collections.remove(collection);
    }
}
