package eu.pb4.polyfactory.models.fluid;

import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.fluid.FluidType;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ModelRenderType;
import it.unimi.dsi.fastutil.ints.IntList;
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
    private final Map<FluidType<?>, ItemStack> solidFluid = new IdentityHashMap<>();

    public SpoutFluidModel(Identifier model) {
        super(model, FactoryUtil::requestModelBase, true);
        this.runStuff();
    }

    @Override
    protected void runStuff() {
        if (StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass() == FluidModel.class) return;
        super.runStuff();
    }

    @Override
    protected void handleFluidTexture(Identifier id, FluidType<?> fluidType, Function<ModelRenderType, Item> function) {
        super.handleFluidTexture(id, fluidType, function);
        if (fluidType.solidTexture().isEmpty()) {
            return;
        }
        this.textures.add(new Texture(id.withSuffix("__solid"), fluidType.solidTexture().get(), true));
        var stack = new ItemStack(function.apply(ModelRenderType.COLORED));
        stack.set(DataComponents.ITEM_MODEL, bridgeModelNoItem(this.baseModel.withSuffix("/" + id.getNamespace() + "/" + id.getPath() + "__solid")));
        this.solidFluid.put(fluidType, stack);
    }
    public <T> ItemStack get(@Nullable FluidInstance<T> type, int color, boolean solid) {
        return type != null ? get(type.type(), type.data(), color, solid) : ItemStack.EMPTY;
    }
    public <T> ItemStack get(FluidType<T> fluid, T data, int color, boolean solid) {
        var stack = solid ? this.solidFluid.getOrDefault(fluid, this.model.getOrDefault(fluid, ItemStack.EMPTY))
                : this.model.getOrDefault(fluid, ItemStack.EMPTY);

        stack = stack.copy();
        //noinspection unchecked
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(),
                IntList.of(fluid.color().isEmpty() ? color : ARGB.average(color, (fluid.color().get()).getColor(data)))));
        return stack;
    }
}
