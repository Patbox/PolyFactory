package eu.pb4.polyfactory.recipe.spout;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SpoutInput;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.List;

public record RepairSpoutRecipe() implements SpoutRecipe {
    public static final MapCodec<RepairSpoutRecipe> CODEC = MapCodec.unit(RepairSpoutRecipe::new);
    @Override
    public boolean matches(SpoutInput input, World world) {
        return EnchantmentHelper.hasAnyEnchantmentsWith(input.stack(), EnchantmentEffectComponentTypes.REPAIR_WITH_XP) && input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) >= FluidBehaviours.EXPERIENCE_ORB_TO_FLUID;
    }

    @Override
    public ItemStack craft(SpoutInput input, RegistryWrapper.WrapperLookup lookup) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.getRepairWithExperience(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        int j = Math.min(i, stack.getDamage());
        stack.setDamage(stack.getDamage() - j);
        return stack;
    }

    @Override
    public RecipeSerializer<RepairSpoutRecipe> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_EXPERIENCE_REPAIR;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SpoutInput input) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.getRepairWithExperience(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        int j = Math.min(i, stack.getDamage());
        return List.of(FactoryFluids.EXPERIENCE.of(j * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
    }

    @Override
    public RegistryEntry<SoundEvent> soundEvent() {
        return Registries.SOUND_EVENT.getEntry(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP);
    }

    @Override
    public double time(SpoutInput input) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.getRepairWithExperience(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        return Math.min(i, stack.getDamage()) / 10f;
    }
}
