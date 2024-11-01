package eu.pb4.polyfactory.models.fluid;

import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils.bridgeModel;

public class FluidModel {
    private static final String BASE_MODEL = """
            {
              "parent": "|BASE|",
              "textures": {
                "texture": "|ID|"
              }
            }
            """.replace(" ", "").replace("\n", "");

    private final Identifier baseModel;
    private final List<Pair<Identifier, Identifier>> textures = new ArrayList<>();
    private final Map<FluidType<?>, ItemStack> model = new IdentityHashMap<>();

    public FluidModel(Identifier model) {
        this(model, FactoryUtil::requestModelBase);
    }
    public FluidModel(Identifier model, Function<ModelRenderType, Item> function) {
        this.baseModel = model;

        for (var fluid : FactoryRegistries.FLUID_TYPES.getIds()) {
            this.addTextures(fluid, Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.get(fluid)), function);
        }

        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            this.addTextures(id, object, function);
        });

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> generateAssets(b::addData));
    }
    public <T> ItemStack get(FluidType<T> fluid, T data) {
        var stack = this.model.getOrDefault(fluid, ItemStack.EMPTY);
        if (fluid.color().isEmpty()) {
            return stack;
        }
        stack = stack.copy();
        //noinspection unchecked
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent((fluid.color().get()).getColor(data), false));
        return stack;
    }

    public <T> ItemStack get(@Nullable FluidInstance<T> type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        return get(type.type(), type.data());
    }

    private void addTextures(Identifier id, FluidType<?> object, Function<ModelRenderType, Item> function) {
        this.textures.add(new Pair<>(id, object.texture()));
        var stack = new ItemStack(function.apply(object.modelRenderType()));
        stack.set(DataComponentTypes.ITEM_MODEL, bridgeModel(this.baseModel.withSuffixedPath("/" + id.getNamespace() + "/" + id.getPath())));
        this.model.put(object, stack);
    }

    private void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        for (var fluid : this.textures) {
            assetWriter.accept("assets/" + baseModel.getNamespace() + "/models/" + this.baseModel.getPath()  + "/" + fluid.getLeft().getNamespace() + "/" + fluid.getLeft().getPath() + ".json",
                    BASE_MODEL.replace("|BASE|", this.baseModel.toString())
                            .replace("|ID|", fluid.getRight().toString())
                            .getBytes(StandardCharsets.UTF_8));
        }
    }

    public ItemStack getRaw(FluidInstance<?> x) {
        return this.model.get(x.type());
    }
}
