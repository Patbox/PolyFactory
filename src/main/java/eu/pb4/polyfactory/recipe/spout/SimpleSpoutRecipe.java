package eu.pb4.polyfactory.recipe.spout;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.recipe.CountedIngredient;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.input.SingleItemWithFluid;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.List;

public record SimpleSpoutRecipe(CountedIngredient item, List<FluidStack<?>> fluidInput,
                                ItemStack output, boolean copyComponents, int itemDamage, Holder<SoundEvent> soundEvent,
                                double time, double coolingTime) implements SpoutRecipe {
    public static final MapCodec<SimpleSpoutRecipe> CODEC = RecordCodecBuilder.mapCodec(x -> x.group(
                    CountedIngredient.CODEC.fieldOf("item").forGetter(SimpleSpoutRecipe::item),
                    FluidStack.CODEC.listOf().optionalFieldOf("fluid_input", List.of()).forGetter(SimpleSpoutRecipe::fluidInput),
                    ItemStack.SINGLE_ITEM_CODEC.fieldOf("result").forGetter(SimpleSpoutRecipe::output),
                    Codec.BOOL.optionalFieldOf("copy_components", false).forGetter(SimpleSpoutRecipe::copyComponents),
                    Codec.INT.optionalFieldOf("item_damage", 0).forGetter(SimpleSpoutRecipe::itemDamage),
                    SoundEvent.CODEC.fieldOf("sound").forGetter(SimpleSpoutRecipe::soundEvent),
                    Codec.DOUBLE.fieldOf("time").forGetter(SimpleSpoutRecipe::time),
                    Codec.DOUBLE.fieldOf("cooling_ticks").forGetter(SimpleSpoutRecipe::coolingTime)
            ).apply(x, SimpleSpoutRecipe::new)
    );
    

    public static SimpleSpoutRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultInstance(), false, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), 0);
    }

    public static SimpleSpoutRecipe toItemCopy(Item item, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultInstance(), true, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), 0);
    }

    public static SimpleSpoutRecipe toItem(Item item, FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime ) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultInstance(), false, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    public static SimpleSpoutRecipe toItemCopy(Item item, FluidStack<?> stack, Item out, SoundEvent sound, int coolingTime ) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(1, item), List.of(stack), out.getDefaultInstance(), true, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), coolingTime  );
    }


    public static SimpleSpoutRecipe template(Item template, FluidStack<Unit> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(0, template), List.of(stack), out.getDefaultInstance(), false, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), 0);
    }

    public static SimpleSpoutRecipe template(Item template, FluidStack<Unit> stack, Item out, SoundEvent sound, int coolingTime) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(0, template), List.of(stack), out.getDefaultInstance(), false, 0,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), coolingTime);
    }

    public static SimpleSpoutRecipe templateDamaged(TagKey<Item> template, FluidStack<?> stack, Item out, SoundEvent sound, double coolingTicks) {
        return new SimpleSpoutRecipe(CountedIngredient.fromTag(0, FactoryUtil.fakeTagList(template)), List.of(stack), out.getDefaultInstance(), false, 1,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()),coolingTicks);
    }

    public static SimpleSpoutRecipe templateDamaged(Item template, FluidStack<?> stack, Item out, SoundEvent sound, double coolingTicks) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(0, template), List.of(stack), out.getDefaultInstance(), false, 1,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), coolingTicks);
    }

    public static SimpleSpoutRecipe templateDamaged(TagKey<Item> template, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.fromTag(0, FactoryUtil.fakeTagList(template)), List.of(stack), out.getDefaultInstance(), false, 1,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()),0);
    }

    public static SimpleSpoutRecipe templateDamaged(Item template, FluidStack<?> stack, Item out, SoundEvent sound) {
        return new SimpleSpoutRecipe(CountedIngredient.ofItems(0, template), List.of(stack), out.getDefaultInstance(), false, 1,
                BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SpoutRecipe.getTime(stack.instance(), stack.amount()), 0);
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
    public boolean matches(SingleItemWithFluid input, Level world) {
        if (!item.test(input.stack())) {
            return false;
        }
        for (var fluid : fluidInput) {
            if (input.getFluid(fluid.instance()) < fluid.amount()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(SingleItemWithFluid input, HolderLookup.Provider lookup) {
        var out = output.copy();
        if (this.copyComponents) {
            out.applyComponentsAndValidate(input.stack().getComponentsPatch());
        }

        return out;
    }


    @Override
    public RecipeSerializer<SimpleSpoutRecipe> getSerializer() {
        return FactoryRecipeSerializers.SPOUT_SIMPLE;
    }

    @Override
    public List<FluidStack<?>> fluidInput(SingleItemWithFluid input) {
        return this.fluidInput;
    }

    @Override
    public double time(SingleItemWithFluid input) {
        return this.time;
    }
}
