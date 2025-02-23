package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.RegistryCallbackItem;
import eu.pb4.polyfactory.advancement.FluidShootsCriterion;
import eu.pb4.polyfactory.fluid.FluidContainer;
import eu.pb4.polyfactory.fluid.FluidContainerFromComponent;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.shooting.EntityShooterContext;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.List;

public class PressureFluidGun extends Item implements PolymerItem {
    public PressureFluidGun(Settings settings) {
        super(settings);
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamage() < stack.getMaxDamage() - 1;
    }


    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (entity instanceof LivingEntity livingEntity && livingEntity.getActiveItem() != stack) {
            onStoppedUsing(stack, world, livingEntity, 0);
        }
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        //noinspection unchecked
        var fluid = (FluidInstance<Object>) stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid != null) {
            fluid.shootingBehavior().stopShooting(new EntityShooterContext(user), fluid);
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
        }
        return false;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        //noinspection unchecked
        var fluid = (FluidInstance<Object>) stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid == null) {
            return;
        }
        var ctx = new EntityShooterContext(user);

        var containers = findFluidContainer(user);
        if (containers.isEmpty() || !isUsable(stack)) {
            fluid.shootingBehavior().stopShooting(ctx, fluid);
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
            return;
        }

        for (var container : containers) {
            if (fluid.shootingBehavior().canShoot(ctx, fluid, container)) {
                fluid.shootingBehavior().continueShooting(ctx, fluid, -remainingUseTicks, container);
                if (remainingUseTicks % 20 == 0) {
                    stack.damage(1, user, user.getActiveHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }

                var vec = ctx.rotation().multiply(user.isOnGround() ? -0.002 : -0.05);
                FactoryUtil.addSafeVelocity(user, vec);
                if (user instanceof ServerPlayerEntity player) {
                    FactoryUtil.sendVelocityDelta(player, vec);
                }
                return;
            }
        }
        fluid.shootingBehavior().stopShooting(ctx, fluid);
        stack.remove(FactoryDataComponents.CURRENT_FLUID);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (stack.contains(FactoryDataComponents.CURRENT_FLUID)) {
            return ActionResult.CONSUME;
        }

        var containers = findFluidContainer(user);
        if (containers.isEmpty() || !isUsable(stack)) {
            return ActionResult.FAIL;
        }

        var ctx = new EntityShooterContext(user);

        for (var container : containers) {
            //noinspection unchecked
            for (var f : ((List<FluidInstance<Object>>) (Object) container.fluids())) {
                if (f.shootingBehavior().canShoot(ctx, f, container)) {
                    stack.set(FactoryDataComponents.CURRENT_FLUID, f);
                    user.setCurrentHand(hand);
                    f.shootingBehavior().startShooting(ctx, f, container);
                    var vec = ctx.rotation().multiply(user.isOnGround() ? -0.01 : -0.07);
                    FactoryUtil.addSafeVelocity(user, vec);
                    if (user instanceof ServerPlayerEntity player) {
                        FactoryUtil.sendVelocityDelta(player, vec);
                        FluidShootsCriterion.triggerFluidLauncher(player, stack, f);
                    }
                    return ActionResult.CONSUME;
                }
            }
        }

        return ActionResult.FAIL;
    }

    private List<FluidContainer> findFluidContainer(LivingEntity user) {
        var stacks = new ArrayList<FluidContainer>();
        for (var eq : EquipmentSlot.values()) {
            var stack = user.getEquippedStack(eq);
            var fluid = stack.getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
            if (!fluid.isEmpty()) {
                stacks.add(FluidContainerFromComponent.of(StackReference.of(user, eq)));
            }
        }

        if (user instanceof PlayerEntity player) {
            for (int i = 0; i < player.getInventory().main.size(); i++) {
                var fluid = player.getInventory().getStack(i).getOrDefault(FactoryDataComponents.FLUID, FluidComponent.DEFAULT);
                if (!fluid.isEmpty()) {
                    stacks.add(FluidContainerFromComponent.of(StackReference.of(player.getInventory(), i)));
                }
            }
        }

        return stacks;
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, PacketContext context) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, context);
        if (this.useCrossbowModel(itemStack)) {
            out.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(Items.ARROW.getDefaultStack()));
            /*out.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, out.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                    .with(
                            EntityAttributes.GENERIC_ATTACK_SPEED,
                            new EntityAttributeModifier(id("packet_fluid_launcher"), -1000, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.ANY)
                    .withShowInTooltip(false)
            );*/
        }
        //out.set(DataComponentTypes.CONSUMABLE, new ConsumableComponent(99999f, UseAction.BOW, Registries.SOUND_EVENT.getEntry(SoundEvents.INTENTIONALLY_EMPTY), false, List.of()));
        out.set(DataComponentTypes.DAMAGE, itemStack.get(DataComponentTypes.DAMAGE));
        return out;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        if (this.useCrossbowModel(itemStack)) {
            return Items.CROSSBOW;
        }

        return Items.TRIAL_KEY;
    }

    private boolean useCrossbowModel(ItemStack itemStack) {
        return useActiveModel(itemStack) && PacketContext.get().getEncodedPacket() instanceof EntityEquipmentUpdateS2CPacket;
    }

    private boolean useActiveModel(ItemStack itemStack) {
        return itemStack.contains(FactoryDataComponents.CURRENT_FLUID);
    }
}
