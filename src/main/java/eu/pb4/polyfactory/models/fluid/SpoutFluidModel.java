package eu.pb4.polyfactory.models.fluid;

import eu.pb4.factorytools.api.util.LazyItemStack;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import static eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras.bridgeModelNoItem;

public class SpoutFluidModel extends FluidModel {
    private final Map<FluidType<?>, ItemStackTemplate> solidFluid = new IdentityHashMap<>();

    public SpoutFluidModel(Identifier model) {
        super(model, true);
        this.runStuff();
    }

    @Override
    protected void runStuff() {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass() == FluidModel.class) return;
        super.runStuff();
    }

    @Override
    protected void handleFluidTexture(Identifier id, FluidType<?> fluidType) {
        super.handleFluidTexture(id, fluidType);
        if (fluidType.solidTexture().isEmpty()) {
            return;
        }
        this.textures.add(new Texture(id.withSuffix("__solid"), fluidType.solidTexture().get(), true));
        var stack = new ItemStackTemplate(Items.STONE, DataComponentPatch.builder()
                .set(DataComponents.ITEM_MODEL, bridgeModelNoItem(this.baseModel.withSuffix("/" + id.getNamespace() + "/" + id.getPath() + "__solid")))
                .build());
        this.solidFluid.put(fluidType, stack);
    }
    public <T> ItemStack get(@Nullable FluidInstance<T> type, int color, boolean solid) {
        return type != null ? get(type.type(), type.data(), color, solid) : ItemStack.EMPTY;
    }
    public <T> ItemStack get(FluidType<T> fluid, T data, int color, boolean solid) {
        var stackt = solid ? this.solidFluid.getOrDefault(fluid, this.template.get(fluid))
                : this.template.get(fluid);

        var stack = stackt.create();
        //noinspection unchecked
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(),
                IntList.of(fluid.color().isEmpty() ? color : ARGB.average(color, (fluid.color().get()).getColor(data)))));
        return stack;
    }
}
