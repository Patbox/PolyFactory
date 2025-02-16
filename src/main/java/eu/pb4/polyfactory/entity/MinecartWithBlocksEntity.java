package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.block.collection.BlockCollectionData;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import xyz.nucleoid.packettweaker.PacketContext;

public class MinecartWithBlocksEntity extends AbstractMinecartEntity implements PolymerEntity {
    private final ElementHolder holder;
    private final BlockCollection blocks;

    public MinecartWithBlocksEntity(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.holder = new ElementHolder();
        this.blocks = new BlockCollection(BlockCollectionData.createDebug());
        this.blocks.setCenter(4, 0, 4);
        this.blocks.setOffset(new Vec3d(0, 2f, 0));
        this.holder.addElement(this.blocks);
        EntityAttachment.ofTicking(this.holder, this);
        this.setCustomBlock(Blocks.DROPPER.getDefaultState());
        this.setCustomBlockOffset(8);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        this.blocks.setWorld(null);
    }

    @Override
    public void onRemove(RemovalReason reason) {
        super.onRemove(reason);
        this.blocks.setWorld(null);

    }

    @Override
    public void tick() {
        this.blocks.setWorld((ServerWorld) this.getWorld());
        super.tick();
        this.blocks.setOverridePos(this.blocks.getCurrentPos().lerp(this.holder.getPos().add(0, 2, 0), 0.25));
        //this.blocks.setQuaternion(new Quaternionf().rotateY(this.getYaw() * MathHelper.RADIANS_PER_DEGREE));
    }

    @Override
    public ItemStack getPickBlockStack() {
        return ItemStack.EMPTY;
    }

    @Override
    protected Item asItem() {
        return Items.AIR;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityType.FURNACE_MINECART;
    }
}
