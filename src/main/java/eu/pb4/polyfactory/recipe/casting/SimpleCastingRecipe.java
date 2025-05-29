package eu.pb4.polyfactory.recipe.casting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.other.FactorySoundEvents;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public record SimpleCastingRecipe(CountedIngredient item, FluidStack<?> fluidInput,
                                  ItemStack output, boolean copyComponents, int itemDamage, RegistryEntry<SoundEvent> soundEvent,
                                  double time, double coolingTime) implements CastingRecipe {
    public static final MapCodec<SimpleCastingRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    CountedIngredient.CODEC.fieldOf("item").forGetter(SimpleCastingRecipe::item),
                    FluidStack.CODEC.fieldOf("fluid_input").forGetter(SimpleCastingRecipe::fluidInput),
                    ItemStack.UNCOUNTED_CODEC.fieldOf("result").forGetter(SimpleCastingRecipe::output),
                    Codec.BOOL.optionalFieldOf("copy_components", false).forGetter(SimpleCastingRecipe::copyComponents),
                    Codec.INT.optionalFieldOf("item_damage", 0).forGetter(SimpleCastingRecipe::itemDamage),
                    SoundEvent.ENTRY_CODEC.optionalFieldOf("sound", FactorySoundEvents.BLOCK_SPOUT_METAL_COOLED).forGetter(SimpleCastingRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleCastingRecipe::time),
                    Codec.DOUBLE.fieldOf("cooling_ticks").forGetter(SimpleCastingRecipe::coolingTime)
            ).apply(x, SimpleCastingRecipe::new)
    );
    

    public static SimpleCastingRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime) {
        return new SimpleCastingRecipe(CountedIngredient.ofItems(1, item), stack, out.getDefaultStack(), false, 0,
                Registries.SOUND_EVENT.getEntry(sound), CastingRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    public static SimpleCastingRecipe toItem(TagKey<Item> item, FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime) {
        return new SimpleCastingRecipe(CountedIngredient.fromTag(1, FactoryUtil.fakeTagList(item)), stack, out.getDefaultStack(), false, 0,
                Registries.SOUND_EVENT.getEntry(sound), CastingRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    public static SimpleCastingRecipe templateDamaged(TagKey<Item> template, FluidStack<?> stack, Item out, SoundEvent sound, double coolingTicks) {
        return new SimpleCastingRecipe(CountedIngredient.fromTag(0, FactoryUtil.fakeTagList(template)), stack, out.getDefaultStack(), false, 1,
                Registries.SOUND_EVENT.getEntry(sound), CastingRecipe.getTime(stack.instance(), stack.amount()),coolingTicks);
    }

    public static SimpleCastingRecipe templateDamaged(Item template, FluidStack<?> stack, Item out, SoundEvent sound, double coolingTicks) {
        return new SimpleCastingRecipe(CountedIngredient.ofItems(0, template), stack, out.getDefaultStack(), false, 1,
                Registries.SOUND_EVENT.getEntry(sound), CastingRecipe.getTime(stack.instance(), stack.amount()), coolingTicks);
    }

    @Override
    public int decreasedInputItemAmount(SingleItemWithFluid input) {
        return this.item.count();
    }

    @Override
    public int damageInputItemAmount(SingleItemWithFluid input) {
        return this.itemDamage;
    }

    @Override
    public double coolingTime(SingleItemWithFluid input) {
        return this.coolingTime;
    }

    @Override
    public boolean matches(SingleItemWithFluid input, World world) {
        if (!item.test(input.stack())) {
            return false;
        }

        if (input.getFluid(this.fluidInput.instance()) < this.fluidInput.amount()) {
            return false;
        }


        return true;
    }

    @Override
    public ItemStack craft(SingleItemWithFluid input, RegistryWrapper.WrapperLookup lookup) {
        var out = output.copy();
        if (this.copyComponents) {
            out.applyChanges(input.stack().getComponentChanges());
        }

        return out;
    }


    @Override
    public RecipeSerializer<SimpleCastingRecipe> getSerializer() {
        return FactoryRecipeSerializers.CASTING_SIMPLE;
    }

    @Override
    public FluidStack<?> fluidInput(SingleItemWithFluid input) {
        return this.fluidInput;
    }

    @Override
    public double time(SingleItemWithFluid input) {
        return this.time;
    }
}
