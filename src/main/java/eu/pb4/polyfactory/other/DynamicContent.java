package eu.pb4.polyfactory.other;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.recipe.OutputStack;
import eu.pb4.polyfactory.block.mechanical.machines.crafting.SawmillBlock;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidStack;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.mixin.ShapedRecipeAccessor;
import eu.pb4.polyfactory.mixin.ShapedRecipePatternAccessor;
import eu.pb4.polyfactory.mixin.ShapelessRecipeAccessor;
import eu.pb4.polyfactory.recipe.drain.SimpleDrainRecipe;
import eu.pb4.polyfactory.recipe.grinding.SimpleGrindingRecipe;
import eu.pb4.polyfactory.recipe.mixing.BrewingMixingRecipe;
import eu.pb4.polyfactory.recipe.sawmill.SawmillRecipe;
import eu.pb4.polyfactory.recipe.sawmill.SimpleSawmillRecipe;
import eu.pb4.polyfactory.recipe.spout.SimpleSpoutRecipe;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.WoodUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.BlockItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
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
            } else if (recipe.getValue() instanceof ShapelessRecipeAccessor shapelessRecipe) {
                var resultId = shapelessRecipe.getResult().item().unwrapKey().get();

                if (resultId.identifier().getPath().endsWith("_planks") && shapelessRecipe.getIngredients().size() == 1) {
                    var out = SimpleSawmillRecipe.of(getShortString(recipe.getKey()), shapelessRecipe.getIngredients().getFirst(),
                            0.2 * shapelessRecipe.getResult().count(), 5, 10,
                            OutputStack.of(shapelessRecipe.getResult().withCount((int) (shapelessRecipe.getResult().count() * 1.5))),
                            OutputStack.of(FactoryItems.SAW_DUST, 0.8f));
                    exported.accept(out.id(), out.value());
                }
            } else if (recipe.getValue() instanceof ShapedRecipeAccessor shapedRecipe && ((ShapedRecipePatternAccessor) (Object) shapedRecipe.getPattern()).getData().isPresent()) {
                var resultId = shapedRecipe.getResult().item().unwrapKey().get();
                var shape = ((ShapedRecipePatternAccessor) (Object) shapedRecipe.getPattern()).getData().orElseThrow();

                if (shape.key().size() == 1 && resultId.identifier().getPath().endsWith("_slab")) {
                    var out = SimpleSawmillRecipe.of(getShortString(recipe.getKey()), shape.key().values().iterator().next(),
                            SawmillBlock.BladeAngle.ZERO,
                            0.5, 6, 12,
                            OutputStack.of(shapedRecipe.getResult().withCount(2)));

                    exported.accept(out.id(), out.value());
                } else if (shape.key().size() == 1 && resultId.identifier().getPath().endsWith("_stairs")) {
                    var out = SimpleSawmillRecipe.of(getShortString(recipe.getKey()), shape.key().values().iterator().next(),
                            SawmillBlock.BladeAngle.FORTY_FIVE,
                            0.5, 6, 12,
                            OutputStack.of(shapedRecipe.getResult().withCount(1)));

                    exported.accept(out.id(), out.value());
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
        return getShortString(entry.unwrapKey().orElseThrow());
    }

    @Unique
    private static String getShortString(ResourceKey<?> entry) {
        //noinspection OptionalGetWithoutIsPresent
        var key = entry.identifier();

        return key.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? key.getPath().replace("/", "_") : key.toDebugFileName();
    }
}
