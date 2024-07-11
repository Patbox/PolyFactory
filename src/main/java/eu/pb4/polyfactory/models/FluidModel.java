package eu.pb4.polyfactory.models;

import eu.pb4.factorytools.api.resourcepack.BaseItemProvider;
import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;

public class FluidModel {
    private static final String BASE_MODEL = """
            {
              "parent": "|BASE|",
              "textures": {
                "texture": "|ID|"
              }
            }
            """.replace(" ", "").replace("\n", "");

    private static final Item[] COLOR_ITEMS = new Item[] {
            Items.LEATHER_HELMET,
            Items.LEATHER_CHESTPLATE,
            Items.LEATHER_LEGGINGS,
            Items.LEATHER_BOOTS,
            Items.LEATHER_HORSE_ARMOR,
    };
    private final Identifier baseModel;
    private final List<Pair<Identifier, Identifier>> textures = new ArrayList<>();
    private final Map<FluidType, ItemStack> model = new IdentityHashMap<>();


    private int coloredIndex = 0;

    public FluidModel(Identifier model) {
        this.baseModel = model;

        for (var fluid : FactoryRegistries.FLUID_TYPES.getIds()) {
            this.addTextures(fluid, Objects.requireNonNull(FactoryRegistries.FLUID_TYPES.get(fluid)));
        }

        RegistryEntryAddedCallback.event(FactoryRegistries.FLUID_TYPES).register((rawId, id, object) -> {
            this.addTextures(id, object);
        });

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register((b) -> generateAssets(b::addData));
    }

    public ItemStack get(@Nullable FluidType type) {
        if (type == null) {
            return ItemStack.EMPTY;
        }
        var stack = this.model.getOrDefault(type, ItemStack.EMPTY);
        if (type.color().isEmpty()) {
            return stack;
        }
        stack = stack.copy();
        stack.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(type.color().getAsInt(), false));
        return stack;
    }

    private void addTextures(Identifier id, FluidType object) {
        this.textures.add(new Pair<>(id, object.texture()));
        this.model.put(object, BaseItemProvider.requestModel(
                object.color().isEmpty() ? BaseItemProvider.requestModel() : COLOR_ITEMS[(coloredIndex++) % COLOR_ITEMS.length],
                this.baseModel.withSuffixedPath("/" + id.getNamespace() + "/" + id.getPath())));
    }

    private void generateAssets(BiConsumer<String, byte[]> assetWriter) {
        for (var fluid : this.textures) {
            assetWriter.accept("assets/" + baseModel.getNamespace() + "/models/" + this.baseModel.getPath()  + "/" + fluid.getLeft().getNamespace() + "/" + fluid.getLeft().getPath() + ".json",
                    BASE_MODEL.replace("|BASE|", this.baseModel.toString())
                            .replace("|ID|", fluid.getRight().toString())
                            .getBytes(StandardCharsets.UTF_8));
        }
    }
}
