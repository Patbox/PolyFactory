package eu.pb4.polyfactory.entity.configurable;

import com.mojang.serialization.JavaOps;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.configuration.ConfigurationData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public interface ConfigurableEntity<E extends Entity> {
    List<EntityConfig<?, E>> getEntityConfiguration(@Nullable ServerPlayer player, @Nullable Vec3 targetPos);

    default void wrenchTick(ServerPlayer player, Vec3 targetPos) {}

    default void writeMinimalConfigurationToStack(Consumer<EntityConfig<?, E>> consumer) {}

    static void applyConfiguration(ConfigurableEntity<?> configurableEntity, ConfigurationData data) {
        var entity = (Entity) configurableEntity;
        //noinspection unchecked
        var configs = (List<EntityConfig<Object, Entity>>) (Object) configurableEntity.getEntityConfiguration(null, null);

        var byId = data.byId();

        for (var config : configs) {
            var entry = byId.get(config.id());
            if (entry == null) {
                continue;
            }

            var decoded = config.codec().decode(JavaOps.INSTANCE, entry.value());

            if (decoded.isSuccess()) {
                config.value().setValue(decoded.getOrThrow().getFirst(), entity, null);
            }
        }
    }

    static ConfigurationData extractConfiguration(ConfigurableEntity<?> configurableEntity, boolean copyAll) {
        var entity = (Entity) configurableEntity;
        //noinspection unchecked
        var conf = (ConfigurableEntity<Entity>) configurableEntity;

        var entries = new ArrayList<ConfigurationData.Entry>();

        Consumer<EntityConfig<?, Entity>> consumer = c -> {
            //noinspection unchecked
            var config = (EntityConfig<Object, Entity>) c;
            if (c.codec() == Unit.CODEC) {
                return;
            }

            var val = config.value().getValue(entity, Vec3.ZERO);
            entries.add(new ConfigurationData.Entry(
                    config.name(),
                    config.formatter().getDisplayValue(val, entity, Vec3.ZERO),
                    config.id(),
                    config.codec().encodeStart(JavaOps.INSTANCE, val).getOrThrow()
            ));
        };

        if (copyAll) {
            conf.getEntityConfiguration(null, null).forEach(consumer);
        } else {
            conf.writeMinimalConfigurationToStack(consumer);
        }

        return new ConfigurationData(entries);
    }
}
