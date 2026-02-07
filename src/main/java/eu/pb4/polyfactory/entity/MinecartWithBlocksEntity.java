package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.block.collection.BlockCollection;
import eu.pb4.polyfactory.block.collection.BlockCollectionData;
import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MinecartWithBlocksEntity extends AbstractMinecart implements PolymerEntity {
    private final ElementHolder holder;
    private final BlockCollection blocks;

    public MinecartWithBlocksEntity(EntityType<?> entityType, Level world) {
        super(entityType, world);
        this.holder = new ElementHolder();
        this.blocks = new BlockCollection(BlockCollectionData.createDebug());
        this.blocks.setCenter(4, 0, 4);
        this.blocks.setOffset(new Vec3(0, 2f, 0));
        this.holder.addElement(this.blocks);
        EntityAttachment.ofTicking(this.holder, this);
        this.setCustomDisplayBlockState(Optional.of(Blocks.DROPPER.defaultBlockState()));
        this.setDisplayOffset(8);
    }

    @Override
    public void onClientRemoval() {
        super.onClientRemoval();
        this.blocks.setLevel(null);
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        this.blocks.setLevel(null);

    }

    @Override
    public void tick() {
        this.blocks.setLevel((ServerLevel) this.level());
        super.tick();
        this.blocks.setOverridePos(this.blocks.getCurrentPos().lerp(this.holder.getPos().add(0, 2, 0), 0.5));
        //this.blocks.setQuaternion(new Quaternionf().rotateY(this.getYaw() * MathHelper.RADIANS_PER_DEGREE));
    }

    @Override
    public ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    @Override
    protected Item getDropItem() {
        return Items.AIR;
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        return EntityType.FURNACE_MINECART;
    }
}
