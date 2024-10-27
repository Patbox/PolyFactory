package eu.pb4.polyfactory.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.pb4.polyfactory.util.ResourceUtils;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import static eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils.bridgeModel;

public class DirectionConnectingModel {
    public static final int SIZE = (int) Math.pow(2, 6);
    private final Identifier baseModel;
    private final ItemStack[] models = new ItemStack[SIZE];

    public DirectionConnectingModel(Identifier baseModel, boolean colored) {
        this.baseModel = baseModel;

        for (var i = 0; i < SIZE; i++) {
            this.models[i] = (colored ? Items.LEATHER_HORSE_ARMOR : Items.PAPER).getDefaultStack();
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
