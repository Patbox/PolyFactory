package eu.pb4.polyfactory;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.mixin.util.MappedRegistryMixin;
import eu.pb4.polyfactory.recipe.drain.SimpleDrainRecipe;
import eu.pb4.polyfactory.recipe.grinding.SimpleGrindingRecipe;
import eu.pb4.polyfactory.recipe.mixing.BrewingMixingRecipe;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.PolyFactoryConfig;
import eu.pb4.polyfactory.util.WoodUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PotionIngredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static eu.pb4.polyfactory.util.FactoryUtil.recipeKey;

public class DynamicContent {
    public static final List<Pair<FluidInstance<?>, Item>> GENERATE_FLUID_BUCKET_RECIPES = new ArrayList<>();

    public static void setup() {

    }

    public static void registerDynamicEntries(Registry<?> registry) {
        if (registry.key().equals(Registries.RECIPE)) {
            //noinspection unchecked
            registerDynamicRecipes((Registry<Recipe<?>>) registry);
        }
    }

    private static void registerDynamicRecipes(Registry<Recipe<?>> registry) {
        BiConsumer<ResourceKey<Recipe<?>>, Recipe<?>> exported = (key, recipe) -> {
            if (!registry.containsKey(key.identifier())) {
                Registry.register(registry, key.identifier(), recipe);
            }
        };

        for (var wood : WoodUtil.MODDED) {
            var path = WoodUtil.asPath(wood);
            if (!WoodUtil.hasLog(wood)) {
                continue;
            }

            var log = BuiltInRegistries.ITEM.getValue(WoodUtil.getLogId(wood));
            var strippedLog = BuiltInRegistries.ITEM.getValue(WoodUtil.getStrippedLogId(wood));

            if (log != strippedLog && strippedLog != Items.AIR && log != Items.AIR) {
                var recipe = SimpleGrindingRecipe.of("wood_stripping/" + path, "wood_stripping", Ingredient.of(log), 0.5, 4, 10,
                        OutputStack.of(strippedLog),
                        OutputStack.of(FactoryItems.SAW_DUST, 0.3f, 2));

                exported.accept(recipe.id(), recipe.value());
            }
        }

        for (var recipe : List.copyOf(registry.entrySet())) {
            if (recipe.getValue() instanceof BrewingRecipe brewingRecipe) {
                if (!brewingRecipe.getOutput().is(Items.POTION) || !brewingRecipe.getInput().ingredient().acceptsItem(Items.POTION.builtInRegistryHolder())) {
                    continue;
                }

                exported.accept(ResourceKey.create(Registries.RECIPE, recipe.getKey().identifier().withSuffix("_brittle")),
                        new BrewingRecipe(
                                new PotionIngredient(Ingredient.of(FactoryItems.BRITTLE_POTION), brewingRecipe.getInput().potions()),
                                brewingRecipe.getReagent(),
                                new ItemStackTemplate(FactoryItems.BRITTLE_POTION.builtInRegistryHolder(), brewingRecipe.getOutput().count(), brewingRecipe.getOutput().components())
                        )
                );


                var result = brewingRecipe.getOutput().components().get(DataComponentMap.EMPTY, DataComponents.POTION_CONTENTS);

                if (brewingRecipe.getInput().potions().isEmpty() || brewingRecipe.getInput().potions().orElseThrow().potions().isEmpty()
                        || brewingRecipe.getReagent().potions().isPresent() || result == null || result.potion().isEmpty()
                ) {
                    continue;
                }


                for (var potionFrom :  brewingRecipe.getInput().potions().orElseThrow().potions().orElseThrow()) {
                    var from = FactoryFluids.getPotion(potionFrom);
                    var to = FactoryFluids.getPotion(result.potion().orElseThrow());
                    var b = new StringBuilder("mixing/brewing/");
                    b.append(getShortString(potionFrom));
                    b.append("_with_");
                    try {
                        for (var stack : brewingRecipe.getReagent().ingredient().items().toList()) {
                            //noinspection deprecation
                            b.append(getShortString(stack));
                            b.append("_");
                        }
                    } catch (Throwable e) {
                        b.append("unknown_stuff_");
                    }

                    b.append("to_");
                    b.append(getShortString(result.potion().orElseThrow()));

                    var key = FactoryUtil.recipeKey(b.toString());

                    exported.accept(key,
                            new BrewingMixingRecipe(getShortString(result.potion().orElseThrow()).replace("long_", "").replace("strong_", ""),
                                    brewingRecipe.getReagent().ingredient(), from, to, FluidConstants.BOTTLE, FluidConstants.BOTTLE * 6,
                                    20, 15, 30, 0.7f, 2f)
                    );
                }
            }
        }

        for (var recipe : DynamicContent.GENERATE_FLUID_BUCKET_RECIPES) {
            fluidBase(exported, recipe.getSecond(), Items.BUCKET, recipe.getFirst().ofBucket(), recipe.getFirst().type().insertSoundEvent(), recipe.getFirst().type().extractSoundEvent());
        }
    }

    private static void fluidBase(BiConsumer<ResourceKey<Recipe<?>>, Recipe<?>> exporter, Item withFluid, Item emptyContainer, FluidStack<?> fluid, SoundEvent fillSound, SoundEvent emptySound) {
        var base = BuiltInRegistries.ITEM.getKey(withFluid).getPath();

        exporter.accept(recipeKey("drain/from_" + base), SimpleDrainRecipe.fromItem(withFluid, fluid, emptyContainer, emptySound));
        exporter.accept(recipeKey("drain/to_" + base), SimpleDrainRecipe.toItem(emptyContainer, fluid, withFluid, fillSound));
        exporter.accept(recipeKey("spout/to_" + base), SimpleSpoutRecipe.toItem(emptyContainer, fluid, withFluid, fillSound));
    }


    @Unique
    private static String getShortString(Holder<?> entry) {
        //noinspection OptionalGetWithoutIsPresent
        var key = entry.unwrapKey().get().identifier();

        return key.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? key.getPath().replace("/", "_") : key.toDebugFileName();
    }
}
