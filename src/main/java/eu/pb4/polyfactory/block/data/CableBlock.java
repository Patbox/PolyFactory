package eu.pb4.polyfactory.block.data;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.data.util.GenericCabledDataBlock;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.block.CabledBlockItem;
import eu.pb4.polyfactory.item.block.FrameItem;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class CableBlock extends AbstractCableBlock implements BlockStateNameProvider {
    public static final BooleanProperty FRAMED = FrameItem.PROPERTY;

    public CableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(FRAMED, false));
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return (context.getStack().getItem() instanceof CabledBlockItem) || super.canReplace(state, context);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (state.getBlock() instanceof GenericCabledDataBlock && !state.get(GenericCabledDataBlock.HAS_CABLE)) {
            return state.with(GenericCabledDataBlock.HAS_CABLE, true);
        }

        return super.getPlacementState(ctx);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FRAMED);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        return Blocks.STRUCTURE_VOID.getDefaultState();
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, ServerPlayerEntity player) {
        return Blocks.BARRIER.getDefaultState();
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (this == FactoryBlocks.CABLE && blockEntity instanceof ColorProvider be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Text.translatable("block.polyfactory.cable.colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable("block.polyfactory.cable.colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }


    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }


    public static final class Model extends BaseCableModel {
        private final ItemDisplayElement frame;

        private Model(BlockState state) {
            super(state, true);
            this.frame = ItemDisplayElementUtil.createSimple(FactoryItems.FRAME);
            this.frame.setScale(new Vector3f(2));
            this.frame.setViewRange(0.8f);
            if (state.get(FRAMED)) {
                this.addElement(this.frame);
            }
        }

        @Override
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            if (blockState.get(FRAMED)) {
                this.addElement(this.frame);
            } else {
                this.removeElement(this.frame);
            }
        }
    }
}
