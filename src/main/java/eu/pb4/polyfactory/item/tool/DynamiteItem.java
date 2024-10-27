package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import eu.pb4.polyfactory.entity.DynamiteEntity;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ProjectileItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DynamiteItem extends SimplePolymerItem implements ProjectileItem {

    public DynamiteItem(Item.Settings settings) {
        super(settings);
        DispenserBlock.registerBehavior(this, new ProjectileDispenserBehavior(this));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);

        DynamiteEntity.spawn(user.getRotationVector(), user.getEyePos(), world, stack.copyWithCount(1));

        world.playSound(null, user.getX(), user.getEyeY(), user.getZ(), FactorySoundEvents.ENTITY_DYNAMITE_THROW, user.getSoundCategory(), 0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 1F));


        if (!user.isCreative()) {
            stack.decrement(1);
        }

        return ActionResult.SUCCESS_SERVER;
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        return DynamiteEntity.create(Vec3d.of(direction.getVector()), pos, world, stack.copyWithCount(1));
    }
}
