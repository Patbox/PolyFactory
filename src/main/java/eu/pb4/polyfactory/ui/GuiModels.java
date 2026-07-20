package eu.pb4.polyfactory.ui;

import com.mojang.math.Transformation;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.numeric.CustomModelDataFloatProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.numeric.NumericProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.ContextEntityTypeProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.DisplayContextProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.CustomModelDataTintSource;
import eu.pb4.polymer.resourcepack.extras.api.format.item.tint.ItemTintSource;
import net.minecraft.world.entity.EntityTypeIds;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

import static eu.pb4.polyfactory.ModInit.id;

public class GuiModels {
    public static ItemModel createUpperGenericBar(Matrix4f transform, NumericProperty progress, ItemTintSource tintSource) {
        var builder = RangeDispatchItemModel.builder(progress).scale(15);
        builder.fallback(new EmptyItemModel());

        for (int a = 1; a <= 14; a++) {
            builder.entry(a, new BasicItemModel(id("sgui/elements/gen/generic_bar_" + a),
                    List.of(tintSource)));
        }

        return new CompositeItemModel(List.of(
                new BasicItemModel(id("sgui/elements/generic_bar_background")),
                builder.build()), Optional.of(new Transformation(transform))
        );
    }

    public static ItemModel createSideGenericBar(Matrix4f transform, NumericProperty progress, ItemTintSource tintSource) {
        var builder = RangeDispatchItemModel.builder(progress).scale(11);
        builder.fallback(new EmptyItemModel());

        for (int a = 2; a <= 11; a++) {
            builder.entry(a - 1, new BasicItemModel(id("sgui/elements/gen/generic_bar_side_" + a),
                    List.of(tintSource)));
        }

        return new CompositeItemModel(List.of(
                new BasicItemModel(id("sgui/elements/generic_bar_side_background")),
                builder.build()), Optional.of(new Transformation(transform))
        );
    }

    public static ItemModel createGuiOnly(ItemModel model) {
        return SelectItemModel.builder(new DisplayContextProperty()).fallback(EmptyItemModel.INSTANCE)
                .withCase(ItemDisplayContext.GUI,
                        SelectItemModel.builder(new ContextEntityTypeProperty()).fallback(EmptyItemModel.INSTANCE)
                                .withCase(EntityTypeIds.PLAYER, model).build()).build();
    }
}
