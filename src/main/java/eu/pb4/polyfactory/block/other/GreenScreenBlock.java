package eu.pb4.polyfactory.block.other;

import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.decoration.Brightness;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class GreenScreenBlock extends Block implements PolymerBlock, BlockWithElementHolder, VirtualDestroyStage.Marker {
    public GreenScreenBlock(Settings settings) {
        super(settings);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }


    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, pos, initialBlockState);
    }


    public final class Model extends ElementHolder {
        private final ItemDisplayElement mainElement;

        private Model(ServerWorld world, BlockPos pos, BlockState state) {
            this.mainElement = new ItemDisplayElement(FactoryItems.GREEN_SCREEN);
            this.mainElement.setDisplaySize(1, 1);
            this.mainElement.setModelTransformation(ModelTransformationMode.FIXED);
            this.mainElement.setScale(new Vector3f(2));
            this.mainElement.setInvisible(true);
            this.mainElement.setBrightness(new Brightness(15, 15));

            this.addElement(this.mainElement);

        }
    }
}
