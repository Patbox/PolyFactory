package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.ModeledItem;
import eu.pb4.polyfactory.entity.DynamiteEntity;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.util.FactorySoundEvents;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.BlockPlacementDispenserBehavior;
import net.minecraft.block.dispenser.DispenserBehavior;
import net.minecraft.block.dispenser.ProjectileDispenserBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public class DynamiteItem extends ModeledItem {

    public DynamiteItem(Settings settings) {
        super(settings);
        DispenserBlock.registerBehavior(this, new ProjectileDispenserBehavior() {
            @Override
            protected ProjectileEntity createProjectile(World world, Position position, ItemStack stack) {
                var dynamite = new DynamiteEntity(FactoryEntities.DYNAMITE, world);
                dynamite.setPosition(position.getX(), position.getY(), position.getZ());
                dynamite.setItemStack(stack.copyWithCount(1));
                return dynamite;
            }
        });
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);

        DynamiteEntity.create(user.getRotationVector(), user.getEyePos(), world, stack.copyWithCount(1));

        world.playSound(null, user.getX(), user.getEyeY(), user.getZ(), FactorySoundEvents.ENTITY_DYNAMITE_THROW, user.getSoundCategory(), 0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 1F));


        if (!user.isCreative()) {
            stack.decrement(1);
        }

        return TypedActionResult.success(stack);
    }
}
