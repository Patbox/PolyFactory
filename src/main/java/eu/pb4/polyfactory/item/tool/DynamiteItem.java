package eu.pb4.polyfactory.item.tool;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import eu.pb4.polyfactory.entity.DynamiteEntity;
import eu.pb4.polyfactory.other.FactorySoundEvents;

public class DynamiteItem extends SimplePolymerItem implements ProjectileItem {

    public DynamiteItem(Item.Properties settings) {
        super(settings);
        DispenserBlock.registerBehavior(this, new ProjectileDispenseBehavior(this));
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        var stack = user.getItemInHand(hand);

        DynamiteEntity.spawn(user.getLookAngle(), user.getEyePosition(), world, stack.copyWithCount(1), user);

        world.playSound(null, user.getX(), user.getEyeY(), user.getZ(), FactorySoundEvents.ENTITY_DYNAMITE_THROW, user.getSoundSource(), 0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 1F));


        if (!user.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public Projectile asProjectile(Level world, Position pos, ItemStack stack, Direction direction) {
        return DynamiteEntity.create(Vec3.atLowerCornerOf(direction.getUnitVec3i()), pos, world, stack.copyWithCount(1), null);
    }
}
