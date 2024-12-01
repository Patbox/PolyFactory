package eu.pb4.polyfactory.models.fluid;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.block.property.ConnectablePart;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.models.FactoryModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SidedMultiFluidViewModel {
    private final Map<FluidInstance<?>, SidedMultiFluidViewModel.Layer> fluidLayers = new Object2ObjectOpenHashMap<>();
    private final ElementHolder holder;
    private SidedMultiFluidViewModel.Layer topLayer;
    private SidedMultiFluidViewModel.Layer bottomLayer;
    private FluidInstance<?> fluidAbove;
    private FluidInstance<?> fluidBelow;
    private float position = 0;
    private ConnectablePart xPart;
    private ConnectablePart zPart;

    public SidedMultiFluidViewModel(ElementHolder holder, ConnectablePart xPart, ConnectablePart zPart) {
        this.holder = holder;
        this.xPart = xPart;
        this.zPart = zPart;
    }

    public void update(ConnectablePart xPart, ConnectablePart zPart) {
        if (this.xPart == xPart && this.zPart == zPart) {
            return;
        }
        this.xPart = xPart;
        this.zPart = zPart;
        for (var x : this.fluidLayers.values()) {
            x.update(this);
        }
    }

    public ConnectablePart getX() {
        return this.xPart;
    }
    public ConnectablePart getZ() {
        return this.zPart;
    }

    private static int textureId(float amount) {
        return MathHelper.clamp(Math.round(amount * 15), 0, 15);
    }

    private void setLayer(FluidInstance<?> instance, float amount) {
        var layer = this.fluidLayers.get(instance);

        if (layer == null) {
            var modelTop = FactoryModels.FLUID_FLAT_FULL.get(instance);
            var parts = new EnumMap<Direction, ItemDisplayElement>(Direction.class);
            for (var dir : Direction.values()) {
                var model = ItemDisplayElementUtil.createSimple(modelTop);
                model.setViewRange(0.5f);
                parts.put(dir, model);
                if (dir.getAxis() != Direction.Axis.Y) {
                    model.setPitch(90);
                    model.setYaw(dir.getPositiveHorizontalDegrees());
                }
                instance.brightness().ifPresent(model::setBrightness);
            }

            layer = new SidedMultiFluidViewModel.Layer(instance, parts);
            this.fluidLayers.put(instance, layer);
        }
        layer.setup(this.position, amount);
        this.position += amount + 0.001f;
    }

    public void setFluids(@Nullable FluidInstance<?> topFluid, @Nullable FluidInstance<?> bottomFluid, Consumer<BiConsumer<FluidInstance<?>, Float>> consumer, Predicate<FluidInstance<?>> removePredicate) {
        this.position = 0;
        consumer.accept(this::setLayer);
        for (var key : List.copyOf(this.fluidLayers.keySet())) {
            if (removePredicate.test(key)) {
                this.fluidLayers.remove(key).destroy(this);
            }
        }
        this.topLayer = this.fluidLayers.get(topFluid);
        this.bottomLayer = this.fluidLayers.get(bottomFluid);

        for (var l : this.fluidLayers.values()) {
            l.update(this);
        }

        if (this.topLayer != null) {
            this.topLayer.updateTop(this);
        }
        if (this.bottomLayer != null) {
            this.bottomLayer.updateBottom(this);
        }
    }

    public void setFluidAbove(@Nullable FluidInstance<?> fluidInstance) {
        if ((this.fluidAbove == null && fluidInstance == null) || (this.fluidAbove != null && this.fluidAbove.equals(fluidInstance))) {
            return;
        }
        this.fluidAbove = fluidInstance;
    }

    public void setFluidBelow(@Nullable FluidInstance<?> fluidInstance) {
        if ((this.fluidBelow == null && fluidInstance == null) || (this.fluidBelow != null && this.fluidBelow.equals(fluidInstance))) {
            return;
        }
        this.fluidBelow = fluidInstance;
    }
    private void addElement(VirtualElement element) {
        this.holder.addElement(element);
    }
    private void removeElement(VirtualElement element) {
        this.holder.removeElement(element);
    }

    private record Layer(FluidInstance<?> instance, EnumMap<Direction, ItemDisplayElement> parts) {
        public void update(SidedMultiFluidViewModel model) {
            if (model.bottomLayer == this && instance.equals(model.fluidAbove)) {
                model.removeElement(parts.get(Direction.UP));
            } else {
                model.addElement(parts.get(Direction.UP));
            }
            if (model.topLayer == this && instance.equals(model.fluidBelow)) {
                model.removeElement(parts.get(Direction.DOWN));
            } else {
                model.addElement(parts.get(Direction.DOWN));
            }
            var x = model.getX();
            var z = model.getZ();

            if (!z.middle()) {
                var val = z.axisDirection();
                if (val == null) {
                    model.addElement(parts.get(Direction.NORTH));
                    model.addElement(parts.get(Direction.SOUTH));
                } else {
                    var dir = Direction.from(Direction.Axis.Z, val);
                    model.addElement(parts.get(dir));
                    model.removeElement(parts.get(dir.getOpposite()));
                }
            } else {
                model.removeElement(parts.get(Direction.NORTH));
                model.removeElement(parts.get(Direction.SOUTH));
            }

            if (!x.middle()) {
                var val = x.axisDirection();
                if (val == null) {
                    model.addElement(parts.get(Direction.EAST));
                    model.addElement(parts.get(Direction.WEST));
                } else {
                    var dir = Direction.from(Direction.Axis.X, val);
                    model.addElement(parts.get(dir));
                    model.removeElement(parts.get(dir.getOpposite()));
                }
            } else {
                model.removeElement(parts.get(Direction.EAST));
                model.removeElement(parts.get(Direction.WEST));
            }
        }

        public void updateTop(SidedMultiFluidViewModel model) {
            if (instance.equals(model.fluidAbove)) {
                model.removeElement(parts.get(Direction.UP));
            } else {
                model.addElement(parts.get(Direction.UP));
            }
        }

        public void updateBottom(SidedMultiFluidViewModel model) {
            if (instance.equals(model.fluidBelow)) {
                model.removeElement(parts.get(Direction.DOWN));
            } else {
                model.addElement(parts.get(Direction.DOWN));
            }
        }

        public void destroy(SidedMultiFluidViewModel model) {
            for (var part : parts.values()) {
                model.removeElement(part);
            }
        }

        public void setup(float position, float amount) {
            this.parts.get(Direction.DOWN).setTranslation(new Vector3f(0, -8f / 16f + (position) * 15.9f / 16f + 0.001f, 0));
            this.parts.get(Direction.UP).setTranslation(new Vector3f(0, -8f / 16f + (position + amount) * 15.9f / 16f + 0.001f, 0));
            var side = FactoryModels.FLUID_FLAT_SCALED[SidedMultiFluidViewModel.textureId(amount)].get(instance);
            for (var dir : FactoryUtil.HORIZONTAL_DIRECTIONS) {
                var part = this.parts.get(dir);
                part.setTranslation(new Vector3f(0, 0.499f, -(-8f / 16f + (position + amount / 2) * 15.9f / 16f + 0.001f)));
                part.setScale(new Vector3f(1, 1, amount));
                part.setItem(side);

            }
        }
    }
}
