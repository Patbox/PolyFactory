package eu.pb4.polyfactory.models.fluid;

import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;
import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModelNoItem;

public class FluidModel {
    private static final String BASE_MODEL = """
            {
              "parent": "|BASE|",
              "textures": {
                "texture": "|ID|"
              }
            }
            """.replace(" ", "").replace("\n", "");

    protected final Identifier baseModel;
    protected final List<Texture> textures = new ArrayList<>();
    protected final Map<FluidType<?>, ItemStack> model = new IdentityHashMap<>();
    private final Function<ModelRenderType, Item> function;
    protected boolean alwaysColored;

    public FluidModel(Identifier model) {
        this(model, FactoryUtil::requestModelBase);
    }
    public FluidModel(Identifier model, Function<ModelRenderType, Item> function) {
        this(model, function, false);
    }
    public FluidModel(Identifier model, Function<ModelRenderType, Item> function, boolean alwaysColored) {
        this.baseModel = model;
        this.alwaysColored = alwaysColored;
        this.function = function;

        this.runStuff();
    }

    protected void runStuff() {
        for (var fluid : FactoryRegistries.FLUID_TYPES.keySet()) {
            this.handleFluidTexture(fluid, Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.getValue(fluid)), function);
        }

        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            this.handleFluidTexture(id, object, function);
        });

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> generateAssets(b::addData));
    }

    protected void handleFluidTexture(Identifier id, FluidType<?> fluidType, Function<ModelRenderType, Item> function) {
        this.addTextures(id, fluidType, function);
    }

    public <T> ItemStack get(FluidType<T> fluid, T data) {
        var stack = this.model.getOrDefault(fluid, ItemStack.EMPTY);
        if (fluid.color().isEmpty() && !this.alwaysColored) {
            return stack;
        }
        stack = stack.copy();
        //noinspection unchecked
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), IntList.of((fluid.color().get()).getColor(data))));
        return stack;
    }

    public <T> ItemStack get(@Nullable FluidInstance<T> type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        return get(type.type(), type.data());
    }

    protected void addTextures(Identifier id, FluidType<?> object, Function<ModelRenderType, Item> function) {
        this.textures.add(new Texture(id, object.texture(), object.color().isPresent() || this.alwaysColored));
        var stack = new ItemStack(function.apply(object.modelRenderType()));
        stack.set(DataComponents.ITEM_MODEL, bridgeModelNoItem(this.baseModel.withSuffix("/" + id.getNamespace() + "/" + id.getPath())));
        this.model.put(object, stack);
    }

    protected void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        for (var fluid : this.textures) {
            var modelId = bridgeModelNoItem(baseModel.withSuffix("/" + fluid.id().getNamespace() + "/" + fluid.id().getPath()));
            assetWriter.accept(AssetPaths.model(modelId) + ".json",
                    BASE_MODEL.replace("|BASE|", this.baseModel.toString())
                            .replace("|ID|", fluid.texture().toString())
                            .getBytes(StandardCharsets.UTF_8));

            if (fluid.colored || this.alwaysColored) {
                assetWriter.accept(AssetPaths.itemAsset(modelId),
                        new ItemAsset(new BasicItemModel(modelId, List.of(new CustomModelDataTintSource(0, -1))),
                                ItemAsset.Properties.DEFAULT).toJson().getBytes(StandardCharsets.UTF_8));
            } else {
                assetWriter.accept(AssetPaths.itemAsset(modelId),
                        new ItemAsset(new BasicItemModel(modelId),
                                ItemAsset.Properties.DEFAULT).toJson().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public ItemStack getRaw(FluidInstance<?> x) {
        return this.model.get(x.type());
    }

    public Identifier getModelId(Identifier type) {
        return bridgeModelNoItem(baseModel.withSuffix("/" + type.getNamespace() + "/" + type.getPath()));
    }

    protected record Texture(Identifier id, Identifier texture, boolean colored) {}
}
