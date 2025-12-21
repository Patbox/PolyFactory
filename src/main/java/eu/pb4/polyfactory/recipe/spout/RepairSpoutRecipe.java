package eu.pb4.polyfactory.recipe.spout;

import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public record RepairSpoutRecipe() implements SpoutRecipe {
    public static final MapCodec<RepairSpoutRecipe> CODEC = MapCodec.unit(RepairSpoutRecipe::new);
    @Override
    public boolean matches(SingleItemWithFluid input, Level world) {
        return EnchantmentHelper.has(input.stack(), EnchantmentEffectComponents.REPAIR_WITH_XP) && input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) >= FluidBehaviours.EXPERIENCE_ORB_TO_FLUID;
    }

    @Override
    public ItemStack assemble(SingleItemWithFluid input, HolderLookup.Provider lookup) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        int j = Math.min(i, stack.getDamageValue());
        stack.setDamageValue(stack.getDamageValue() - j);
        return stack;
    }

    @Override
    public RecipeSerializer<RepairSpoutRecipe> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_EXPERIENCE_REPAIR;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SingleItemWithFluid input) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        int j = Math.min(i, stack.getDamageValue());
        return List.of(FactoryFluids.EXPERIENCE.of(j * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
    }

    @Override
    public Holder<SoundEvent> soundEvent() {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EXPERIENCE_ORB_PICKUP);
    }

    @Override
    public double time(SingleItemWithFluid input) {
        var stack = input.stack().copy();
        int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(input.world(), stack, (int) (input.getFluid(FactoryFluids.EXPERIENCE.defaultInstance()) / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID));
        return Math.min(i, stack.getDamageValue()) / 10f;
    }
}
