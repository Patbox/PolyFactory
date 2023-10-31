package eu.pb4.polyfactory.block.other;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.polyfactory.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.base.FactoryBlock;
import eu.pb4.polyfactory.block.data.CableConnectable;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.models.BaseItemProvider;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.CableModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.StateNameProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.*;

public class LampBlock extends RedstoneLampBlock implements FactoryBlock, BlockEntityProvider, StateNameProvider {
    private final boolean inverted;

    public LampBlock(Settings settings, boolean inverted) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(LIT, false));
        this.inverted = inverted;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        if (world.getBlockEntity(pos) instanceof LampBlockEntity be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof LampBlockEntity be) {
            be.setColor(FactoryItems.LAMP.getItemColor(itemStack));
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.BARRIER;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState, this.inverted);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof LampBlockEntity be && !be.isDefaultColor()) {
            if (DyeColorExtra.BY_COLOR.get(be.getColor()) == null) {
                return Text.translatable( this.getTranslationKey() + ".colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable(this.getTranslationKey() + ".colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    public final class Model extends BaseModel {
        private final ItemDisplayElement main;
        private final boolean inverted;
        private int color = -2;
        private BlockState state;

        private Model(BlockState state, boolean inverted) {
            this.main = LodItemDisplayElement.createSimple();
            this.main.setScale(new Vector3f(2));
            this.main.setViewRange(0.5f);
            this.state = state;
            this.inverted = inverted;
            this.addElement(this.main);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                this.setState(BlockBoundAttachment.get(this).getBlockState());
            }
        }

        private void setState(BlockState blockState) {
            this.state = blockState;
            if (color != -2) {
                updateModel();
            }
        }

        private void updateModel() {
            var stack = LodItemDisplayElement.getModel(this.state.get(LIT) == this.inverted ? FactoryItems.LAMP : FactoryItems.INVERTED_LAMP).copy();
            var ex = new NbtCompound();
            var c = new NbtIntArray(new int[] { this.color });
            ex.put("Colors", c);
            stack.getOrCreateNbt().put("Explosion", ex);
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
