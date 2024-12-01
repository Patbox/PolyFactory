package eu.pb4.polyfactory.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModel;

public class DirectionConnectingModel {
    public static final int SIZE = (int) Math.pow(2, 6);
    private final Identifier baseModel;
    private final ItemStack[] models = new ItemStack[SIZE];
    private final boolean colored;

    public DirectionConnectingModel(Identifier baseModel, boolean colored) {
        this.baseModel = baseModel;
        this.colored = colored;

        for (var i = 0; i < SIZE; i++) {
            this.models[i] = Items.PAPER.getDefaultStack();
            this.models[i].set(DataComponentTypes.ITEM_MODEL, bridgeModel(baseModel.withSuffixedPath("/" + i)));
        }
    }

    public void generateModels(BiConsumer<String, byte[]> dataWriter) {
        var model = ResourceUtils.getElementResolvedModel(this.baseModel);

        for (int i = 0; i < SIZE; i++) {
            var base = new JsonObject();
            base.asMap().putAll(model.asMap());
            var elements = new JsonArray();

            for (var element : model.getAsJsonArray("elements")) {
                var name = element.getAsJsonObject().get("name");

                if (name == null || Direction.byName(name.getAsString()) == null || hasDirection(i, Direction.byName(name.getAsString()))) {
                    elements.add(element);
                }
            }
            base.add("elements", elements);

            dataWriter.accept(AssetPaths.model(this.baseModel.withSuffixedPath("/" + i + ".json")), base.toString().getBytes(StandardCharsets.UTF_8));
            if (this.colored) {
                dataWriter.accept(AssetPaths.itemAsset(bridgeModel(this.baseModel.withSuffixedPath("/" + i))),
                        new ItemAsset(
                                new BasicItemModel(
                                        this.baseModel.withSuffixedPath("/" + i),
                                        List.of(new CustomModelDataTintSource(0, 0xFFFFFF))
                                ),
                                ItemAsset.Properties.DEFAULT).toJson().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public ItemStack get(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
        return get(getModelId(state, directionPredicate));
    }

    public ItemStack get(int i) {
        return this.models[i];
    }
    public static boolean hasDirection(int i, Direction direction) {
        if (direction == null) {
            return false;
        }

        return (i & (1 << direction.ordinal())) != 0;
    }

    public static int getModelId(BlockState state, BiPredicate<BlockState, Direction> directionPredicate) {
        int i = 0;

        for(int j = 0; j < Direction.values().length; ++j) {
            var direction = Direction.values()[j];
            if (directionPredicate.test(state, direction)) {
                i |= 1 << j;
            }
        }

        return i;
    }
}
