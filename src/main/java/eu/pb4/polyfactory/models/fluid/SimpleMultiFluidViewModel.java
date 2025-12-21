package eu.pb4.polyfactory.models.fluid;

import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.util.Mth;

public class SimpleMultiFluidViewModel {
    private final Map<FluidInstance<?>, ItemDisplayElement> fluidLayers = new Object2ObjectOpenHashMap<>();
    private final ElementHolder holder;
    private float height;
    private float position = 0;
    private FluidModel[] models;
    private float pitch;
    private float yaw;
    private final List<Layer> layers = new ArrayList<>();

    public SimpleMultiFluidViewModel(ElementHolder holder, FluidModel[] models, float height) {
        this.holder = holder;
        this.models = models;
        this.height = height;
    }

    public void setModels(FluidModel[] models, float height) {
        if (this.models == models && this.height == height) {
            return;
        }
        this.models = models;
        this.height = height;

        this.position = 0;
        for (var x : layers) {
            var y = this.fluidLayers.get(x.fluid);
            if (y != null) {
                setValues(y, x.fluid, x.amount);
            }
        }
    }

    private int textureId(float amount) {
        return Mth.clamp(Math.round(amount * this.models.length), 0,  this.models.length - 1);
    }

    private void setLayer(FluidInstance<?> instance, float amount) {
        this.layers.add(new Layer(instance, amount));
        var layer = this.fluidLayers.get(instance);
        if (layer == null) {
            layer = ItemDisplayElementUtil.createSimple();
            layer.setViewRange(0.5f);
            layer.setYaw(yaw);
            layer.setPitch(pitch);
            instance.brightness().ifPresent(layer::setBrightness);
            this.fluidLayers.put(instance, layer);
        }
        this.setValues(layer, instance, amount);
        this.addElement(layer);
    }

    private void setValues(ItemDisplayElement layer, FluidInstance<?> instance, float amount) {
        var scale = this.height / 16f;
        layer.setTranslation(new Vector3f(0, ((-8 / 16f + (position + amount / 2) * 15.9f / 16f) + 0.003f) * scale, 0));
        layer.setScale(new Vector3f(1, amount, 1));
        layer.setItem(this.models[textureId(amount)].get(instance));
        this.position += amount + 0.001f;
    }

    public void setFluids(Consumer<BiConsumer<FluidInstance<?>, Float>> consumer, Predicate<FluidInstance<?>> removePredicate) {
        this.position = 0;
        this.layers.clear();
        consumer.accept(this::setLayer);
        for (var key : List.copyOf(this.fluidLayers.keySet())) {
            if (removePredicate.test(key)) {
                this.removeElement(this.fluidLayers.remove(key));
            }
        }
    }

    private void addElement(VirtualElement element) {
        this.holder.addElement(element);
    }
    private void removeElement(VirtualElement element) {
        this.holder.removeElement(element);
    }

    public void setRotation(float pitch, float yaw) {
        if (this.pitch != pitch || this.yaw != yaw) {
            this.pitch = pitch;
            this.yaw = yaw;
            for (var x : this.fluidLayers.values()) {
                x.setPitch(pitch);
                x.setYaw(yaw);
            }
        }
    }

    private record Layer(FluidInstance<?> fluid, float amount) {};
}
