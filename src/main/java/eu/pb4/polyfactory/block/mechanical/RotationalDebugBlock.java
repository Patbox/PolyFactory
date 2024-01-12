package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.nodes.generic.AllSideNode;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;

public class RotationalDebugBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder {
    public RotationalDebugBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.DAYLIGHT_DETECTOR;
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new AllSideNode());
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos);
    }

    @Override
    public boolean tickElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends BlockModel {
        private final TextDisplayElement mainElement;
        private final ServerWorld world;
        private final BlockPos pos;

        private Model(ServerWorld world, BlockPos pos) {
            this.mainElement = new TextDisplayElement();
            this.mainElement.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            this.addElement(this.mainElement);
            this.world = world;
            this.pos = pos;
        }



        @Override
        protected void onTick() {
            var rotation = RotationUser.getRotation(this.world, this.pos);

            this.mainElement.setText(Text.translatable("""
                    Speed: %s (%s)
                    Stress: %s / %s
                    Rotation: %s
                    Negative: %s""", rotation.speed(), rotation.directSpeed(), rotation.directStressUsage(), rotation.directStressCapacity(), rotation.rotation(), rotation.isNegative()
                    ).withColor(rotation.isOverstressed() ? 0xFFBBBB : 0xFFFFFF)
            )
            ;
        }
    }
}
