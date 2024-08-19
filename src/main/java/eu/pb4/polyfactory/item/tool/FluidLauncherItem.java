package eu.pb4.polyfactory.item.tool;

import eu.pb4.factorytools.api.item.RegistryCallbackItem;
import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.entity.splash.SplashEntity;
import eu.pb4.polyfactory.fluid.*;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.component.FluidComponent;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactorySoundEvents;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerModelData;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class FluidLauncherItem extends Item implements PolymerItem, RegistryCallbackItem {
    public Map<FluidType<?>, FluidShootingBehavior> BEHAVIOUR = new IdentityHashMap<>();
    private PolymerModelData baseModel = FactoryModels.PLACEHOLDER;
    private PolymerModelData activeOthersModel = FactoryModels.PLACEHOLDER;
    private PolymerModelData activeSelfModel = FactoryModels.PLACEHOLDER;

    public FluidLauncherItem(Settings settings) {
        super(settings);
        BEHAVIOUR.put(FactoryFluids.WATER, new ShootSplashed(FactoryEntities.WATER_SPLASH, 300, FactorySoundEvents.ITEM_FLUID_LAUNCHER_SHOOT_WATER));
        BEHAVIOUR.put(FactoryFluids.LAVA, new ShootSplashed(FactoryEntities.LAVA_SPLASH, 500, FactorySoundEvents.ITEM_FLUID_LAUNCHER_SHOOT_LAVA));
        BEHAVIOUR.put(FactoryFluids.SLIME, new SnowballTestShooting());
        BEHAVIOUR.put(FactoryFluids.EXPERIENCE, new SnowballTestShooting());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        if (!selected && entity instanceof LivingEntity livingEntity) {
            onStoppedUsing(stack, world, livingEntity, 0);
        }
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        var fluid = stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid != null) {
            BEHAVIOUR.get(fluid.type()).stopShooting((ServerWorld) world, user, stack, fluid, user.getActiveHand());
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
        }
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        var fluid = stack.get(FactoryDataComponents.CURRENT_FLUID);
        if (fluid == null) {
            return;
        }

        var containers = findFluidContainer(user);
        if (containers.isEmpty()) {
            BEHAVIOUR.get(fluid.type()).stopShooting((ServerWorld) world, user, stack, fluid, user.getActiveHand());
            stack.remove(FactoryDataComponents.CURRENT_FLUID);
            return;
        }

        for (var container : containers) {
            for (var f : container.fluids()) {
                var behaviour = BEHAVIOUR.get(f.type());
                if (behaviour.canShoot((ServerWorld) world, user, stack, f, user.getActiveHand(), container)) {
                    behaviour.continueShooting((ServerWorld) world, user, stack, f, user.getActiveHand(), -remainingUseTicks, container);
                    return;
                }
            }
        }
        BEHAVIOUR.get(fluid.type()).stopShooting((ServerWorld) world, user, stack, fluid, user.getActiveHand());
        stack.remove(FactoryDataComponents.CURRENT_FLUID);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var containers = findFluidContainer(user);
        if (containers.isEmpty()) {
            return TypedActionResult.fail(user.getStackInHand(hand));
        }

        var stack = user.getStackInHand(hand);
        for (var container : containers) {
            for (var f : container.fluids()) {
                var behaviour = BEHAVIOUR.get(f.type());
                if (behaviour.canShoot((ServerWorld) world, user, stack, f, hand, container)) {
                    stack.set(FactoryDataComponents.CURRENT_FLUID, f);
                    user.setCurrentHand(hand);
                    behaviour.startShooting((ServerWorld) world, user, stack, f, hand, container);
                    return TypedActionResult.consume(stack);
                }
            }
        }

        return TypedActionResult.fail(stack);
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
    public Item getPolymerItem(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return getModel(itemStack).item();
    }

    @Override
    public int getPolymerCustomModelData(ItemStack itemStack, @Nullable ServerPlayerEntity player) {
        return getModel(itemStack).value();
    }

    @Override
    public ItemStack getPolymerItemStack(ItemStack itemStack, TooltipType tooltipType, RegistryWrapper.WrapperLookup lookup, @Nullable ServerPlayerEntity player) {
        var out = PolymerItem.super.getPolymerItemStack(itemStack, tooltipType, lookup, player);
        if (this.useCrossbowModel(itemStack)) {
            out.set(DataComponentTypes.CHARGED_PROJECTILES, ChargedProjectilesComponent.of(Items.STONE.getDefaultStack()));
            /*out.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, out.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                    .with(
                            EntityAttributes.GENERIC_ATTACK_SPEED,
                            new EntityAttributeModifier(id("packet_fluid_launcher"), -1000, EntityAttributeModifier.Operation.ADD_VALUE),
                            AttributeModifierSlot.ANY)
                    .withShowInTooltip(false)
            );*/
        }
        return out;
    }

    private PolymerModelData getModel(ItemStack itemStack) {
        if (this.useCrossbowModel(itemStack)) {
            return this.activeOthersModel;
        }

        return this.useActiveModel(itemStack) ? this.activeSelfModel : this.baseModel;
    }

    private boolean useCrossbowModel(ItemStack itemStack) {
        return useActiveModel(itemStack) && PacketContext.get().getEncodedPacket() instanceof EntityEquipmentUpdateS2CPacket;
    }

    private boolean useActiveModel(ItemStack itemStack) {
        return itemStack.contains(FactoryDataComponents.CURRENT_FLUID);
    }

    @Override
    public void onRegistered(Identifier selfId) {
        var item = Identifier.of(selfId.getNamespace(), "item/" + selfId.getPath());
        this.baseModel = PolymerResourcePackUtils.requestModel(BaseItemProvider.requestItem(), item);
        this.activeSelfModel = PolymerResourcePackUtils.requestModel(this.baseModel.item(), item.withSuffixedPath("_active"));
        this.activeOthersModel = PolymerResourcePackUtils.requestModel(Items.CROSSBOW, item.withSuffixedPath("_active"));
    }

    public interface FluidShootingBehavior {
        boolean canShoot(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container);
        void startShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container);
        void continueShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, int tick, FluidContainer container);
        void stopShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand);
    }

    public record ShootSplashed(EntityType<? extends SplashEntity<?>> entityType, long amount, RegistryEntry<SoundEvent> soundEvent) implements FluidShootingBehavior {

        @Override
        public boolean canShoot(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container) {
            return container.get(fluidInstance) >= amount;
        }

        @Override
        public void startShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container) {
            shoot(world, user, 0.1f, fluidInstance, container);
        }

        @Override
        public void continueShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, int tick, FluidContainer container) {
            shoot(world, user, Math.min(0.1f + (tick) / 100f, 0.3f), fluidInstance, container);
        }

        private void shoot(ServerWorld world, LivingEntity entity, float divergence, FluidInstance<?> fluidInstance, FluidContainer container) {
            container.extract(fluidInstance, amount, false);
            var baseVector = entity.getRotationVecClient();
            var vec = new Vector3f();
            var pos = new Vec3d(entity.getX() + baseVector.x, entity.getEyeY() - 0.1f + baseVector.y,entity.getZ() + baseVector.z);
            for (int i = 0; i < 10; i++) {
                var projectile = entityType.create(world);
                assert projectile != null;
                projectile.setOwner(entity);
                projectile.setPosition(pos);
                vec.set(baseVector.x, baseVector.y, baseVector.z);
                vec.add((float) entity.getRandom().nextTriangular(0, divergence),
                        (float) entity.getRandom().nextTriangular(0, divergence),
                        (float) entity.getRandom().nextTriangular(0, divergence));
                vec.normalize();
                vec.mul(2.0f + entity.getRandom().nextFloat() * 0.5f);

                projectile.setVelocity(vec.x, vec.y, vec.z);
                world.spawnEntity(projectile);
            }
            world.playSound(null, pos.x, pos.y, pos.z, this.soundEvent, entity.getSoundCategory(), 1, (float) entity.getRandom().nextTriangular(1, 0.1));
        }

        @Override
        public void stopShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand) {}
    }

    public record SnowballTestShooting() implements FluidShootingBehavior {
        @Override
        public boolean canShoot(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container) {
            return container.get(fluidInstance) > 300;
        }

        @Override
        public void startShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, FluidContainer container) {
            var pos = user.getEyePos().add(0, -0.2, 0).add(user.getHandPosOffset(stack.getItem()));
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, 0f);
            world.spawnParticles(fluidInstance.particle(), pos.x, pos.y, pos.z, 5, 0.1, 0.1, 0.1, 0.1);
            var snowball = new SnowballEntity(world, user);
            snowball.setItem(FactoryModels.FLUID_FILTERED_PIPE.get(fluidInstance).copy());
            snowball.setVelocity(user, user.getPitch(), user.getYaw(), 0, 0.5f, 0);
            container.extract(fluidInstance, 250, false);
            world.spawnEntity(snowball);
        }

        @Override
        public void continueShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand, int tick, FluidContainer container) {
            var pos = user.getEyePos().add(0, -0.2, 0).add(user.getHandPosOffset(stack.getItem()));
            world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5f, tick / 10f);
            var snowball = new SnowballEntity(world, user);
            snowball.setItem(FactoryModels.FLUID_FILTERED_PIPE.get(fluidInstance).copy());
            snowball.setVelocity(user, user.getPitch(), user.getYaw(), 0, 1, tick / 50f);
            container.extract(fluidInstance, 300, false);
            world.spawnEntity(snowball);
        }

        @Override
        public void stopShooting(ServerWorld world, LivingEntity user, ItemStack stack, FluidInstance<?> fluidInstance, Hand hand) {
            var pos = user.getEyePos().add(0, -0.2, 0).add(user.getHandPosOffset(stack.getItem()));
            world.spawnParticles(fluidInstance.particle(), pos.x, pos.y, pos.z, 15, 0.1, 0.1, 0.1, 1);
        }
    }
}
