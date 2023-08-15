package eu.pb4.polyfactory.block.creative;

import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.polyfactory.block.network.RotationalNetworkBlock;
import eu.pb4.polyfactory.block.other.ContainerBlockEntity;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.models.BaseModel;
import eu.pb4.polyfactory.models.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.FunctionalDirectionNode;
import eu.pb4.polyfactory.nodes.mechanical.RotationData;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.ResourceAmount;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CreativeContainerBlockEntity extends ContainerBlockEntity {
    static {
        ItemStorage.SIDED.registerForBlockEntity((self, dir) -> self.storage, FactoryBlockEntities.CREATIVE_CONTAINER);
    }

    public CreativeContainerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.CREATIVE_CONTAINER, pos, state);
    }

    @Override
    protected SingleItemStorage createStorage() {
        return new SingleItemStorage() {
            @Override
            protected long getCapacity(ItemVariant variant) {
                return variant.getItem().getMaxCount();
            }

            @Override
            public long extract(ItemVariant extractedVariant, long maxAmount, TransactionContext transaction) {
                return Math.min(this.amount, maxAmount);
            }

            @Override
            public long insert(ItemVariant insertedVariant, long maxAmount, TransactionContext transaction) {
                return Math.min(variant.getItem().getMaxCount() - this.amount, maxAmount);
            }

            @Override
            protected void readSnapshot(ResourceAmount<ItemVariant> snapshot) {
            }

            @Override
            protected void onFinalCommit() {
            }
        };
    }

    @Override
    public ItemStack extract(int amount) {
        return this.getItemStack().copyWithCount((int) Math.min(amount, this.storage.amount));
    }

    @Override
    public void setItemStack(ItemStack stack) {
        this.storage.amount = stack.getCount();
        super.setItemStack(stack);
    }

}
