package eu.pb4.polyfactory.block.other;

import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polyfactory.util.ColorProvider;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static eu.pb4.polyfactory.ModInit.id;

public class LampBlock extends RedstoneLampBlock implements FactoryBlock, EntityBlock, BlockStateNameProvider {
    private final boolean inverted;

    public LampBlock(Properties settings, boolean inverted) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(LIT, false));
        this.inverted = inverted;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        var stack = super.getCloneItemStack(world, pos, state, includeData);
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    public boolean setColor(Level world, BlockPos pos, int color) {
        color = FactoryItems.LAMP.downSampleColor(color);
        if (world.getBlockEntity(pos) instanceof ColorProvider provider && provider.getColor() != color) {
            provider.setColor(color);
            return true;
        }

        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof ColorableBlockEntity be) {
            be.setColor(FactoryItems.LAMP.getItemColor(itemStack));
        }

        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(pos, initialBlockState, this.inverted);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ColorableBlockEntity(pos, state);
    }

    @Override
    public Component getName(ServerLevel world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof ColorableBlockEntity be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Component.translatable( this.getDescriptionId() + ".colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Component.translatable(this.getDescriptionId() + ".colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    @Override
    public boolean forceLightUpdates(BlockState blockState) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    public BlockState getPolymerBreakEventBlockState(BlockState state, PacketContext context) {
        return Blocks.GLASS.defaultBlockState();
    }

    public static final class Model extends BlockModel implements ColorProvider.Consumer {
        private final ItemDisplayElement main;
        private final boolean inverted;
        private int color = -2;
        private BlockState state;

        private Model(BlockPos pos, BlockState state, boolean inverted) {
            this.main = ItemDisplayElementUtil.createSimple();
            this.main.setScale(new Vector3f(2 + (pos.distManhattan(BlockPos.ZERO) % 2) * 0.001f));
            this.main.setViewRange(0.8f);
            this.state = state;
            this.inverted = inverted;
            this.addElement(this.main);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        private void setState(BlockState blockState) {
            this.state = blockState;
            if (color != -2) {
                updateModel();
            }
        }

        private void updateModel() {
            var stack = new ItemStack(Items.FIREWORK_STAR);
            stack.set(DataComponents.ITEM_MODEL, this.state.getValue(LIT) == this.inverted ? id("colored_lamp") : id("inverted_colored_lamp"));
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of(this.color)));
            this.main.setItem(stack);
            this.tick();
        }

        public void setColor(int color) {
            this.color = color;
            if (color != -2) {
                updateModel();
            }
        }
    }
}
