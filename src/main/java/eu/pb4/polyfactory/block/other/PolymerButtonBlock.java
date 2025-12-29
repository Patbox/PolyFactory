package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import static eu.pb4.polyfactory.ModInit.id;

public class PolymerButtonBlock extends ButtonBlock implements FactoryBlock {
    private final ItemStack modelNormal;
    private final ItemStack modelPressed;

    public PolymerButtonBlock(String name, BlockSetType blockSetType, int pressTicks, Properties settings) {
        super(blockSetType, pressTicks, settings);
        this.modelNormal = ItemDisplayElementUtil.getSolidModel(id("block/" + name + "_button"));
        this.modelPressed = ItemDisplayElementUtil.getSolidModel(id("block/" + name + "_button_pressed"));
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.STONE_BUTTON.withPropertiesOf(state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }
    
    private class Model extends BlockModel {
        private final ItemDisplayElement base;
        public Model(BlockState initialBlockState) {
            this.base = ItemDisplayElementUtil.createSimple();
            this.base.setScale(new Vector3f(1.01f, 1.01f, 1.01f));
            this.base.setTranslation(new Vector3f(0, 0.0075f, 0));
            this.updateState(initialBlockState);

            this.addElement(this.base);
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.updateState(this.blockState());
            }
            super.notifyUpdate(updateType);
        }

        private void updateState(BlockState blockState) {
            this.base.setItem(blockState.getValue(ButtonBlock.POWERED) ? modelPressed : modelNormal);
            float p = switch (blockState.getValue(ButtonBlock.FACE)) {
                case WALL -> -90;
                case FLOOR -> 0;
                case CEILING -> 180;
            };
            float y = blockState.getValue(FACING).toYRot() + 180;

            this.base.setYaw(y);
            this.base.setPitch(p);
            this.tick();
        }
    }
}
