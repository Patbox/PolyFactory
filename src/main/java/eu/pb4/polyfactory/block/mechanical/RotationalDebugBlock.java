package eu.pb4.polyfactory.block.mechanical;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.nodes.generic.AllSideNode;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class RotationalDebugBlock extends RotationalNetworkBlock implements PolymerBlock, BlockWithElementHolder {
    public RotationalDebugBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public Collection<BlockNode> createRotationalNodes(BlockState state, ServerLevel world, BlockPos pos) {
        return List.of(new AllSideNode());
    }

    @Override
    public ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public final class Model extends BlockModel {
        private final TextDisplayElement mainElement;
        private final ServerLevel world;
        private final BlockPos pos;

        private Model(ServerLevel world, BlockPos pos) {
            this.mainElement = new TextDisplayElement();
            this.mainElement.setBillboardMode(Display.BillboardConstraints.CENTER);
            this.mainElement.setBackground(0xFF000000);
            this.mainElement.setTranslation(new Vector3f(0, -0.4f, 0));
            this.addElement(this.mainElement);
            this.world = world;
            this.pos = pos;
        }



        @Override
        protected void onTick() {
            var rotation = RotationUser.getRotation(this.world, this.pos);

            this.mainElement.setText(Component.literal(String.format("""
                    Speed: %.4f (%.4f) 
                    Speed (rad): %.4f
                    Speed (RPM): %.4f
                    Stress: %s / %s
                    Rotation: %s
                    Negative: %s""", rotation.speed(), rotation.directSpeed(), rotation.speedRadians(), rotation.speed() / 360 * 60 * 20, rotation.directStressUsage(), rotation.directStressCapacity(), rotation.rotation(), rotation.isNegative()
                    )).withColor(rotation.isOverstressed() ? 0xFFBBBB : 0xFFFFFF)
            )
            ;
        }
    }
}
