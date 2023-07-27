package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.util.FactoryEntityTags;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class SelectivePassthroughBlock extends Block implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public SelectivePassthroughBlock(Settings settings) {
        super(settings.dynamicBounds());
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext entityShapeContext && entityShapeContext.getEntity() != null && entityShapeContext.getEntity().getType().isIn(FactoryEntityTags.GRID_PASSABLE)) {
            return VoxelShapes.empty();
        }

        return super.getCollisionShape(state, world, pos, context);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        var model = new BaseModel();
        var element = LodItemDisplayElement.createSimple(this.asItem().getDefaultStack());
        element.setScale(new Vector3f(2));
        model.addElement(element);
        return model;
    }
}
