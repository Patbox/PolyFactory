package eu.pb4.polyfactory.entity.configurable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public record EntityConfig<T, E extends Entity>(String id, Text name, Codec<T> codec, EntityConfigValue<T, E> value, EntityValueFormatter<T, E> formatter,
                                                WrenchModifyEntityValue<T, E> action, WrenchModifyEntityValue<T, E> alt) {
    public static EntityConfig<Float, ?> PITCH = of("pitch", Codec.FLOAT,
            EntityConfigValue.of(Entity::getPitch, Entity::setPitch),
            EntityValueFormatter.str(x -> String.valueOf((int) MathHelper.wrap(x, 90))),
            WrenchModifyEntityValue.simple((a, b) -> (float) MathHelper.wrapDegrees(Math.round(a) + (b ? 1 : -1)))
    ).withAlt((value1, next, player, entity, targetPos) -> player.getPitch());

    public static EntityConfig<Float, ?> YAW = of("yaw", Codec.FLOAT,
            EntityConfigValue.of(Entity::getYaw, Entity::setYaw),
            EntityValueFormatter.str(x -> String.valueOf((int) MathHelper.wrapDegrees(x))),
            WrenchModifyEntityValue.simple((a, b) -> (float) MathHelper.wrapDegrees(Math.round(a) + (b ? 1 : -1)))
    ).withAlt((value1, next, player, entity, targetPos) -> player.getYaw());

    public static EntityConfig<?, ?> DISMOUNT = of("dismount", Unit.CODEC,
            EntityConfigValue.of(x -> x.hasPassengers() ? Unit.INSTANCE : null, (a, b) -> a.removeAllPassengers()),
            (unused, entity, pos) -> {
                var passenger = entity.getFirstPassenger();
                if (passenger == null) {
                    return Text.translatable("text.polyfactory.none_wrapped").formatted(Formatting.GRAY);
                }
                return passenger.getName();
            },
            WrenchModifyEntityValue.simple((a, b) -> a)
    );

    public static <T, E extends Entity> EntityConfig<T, E> of(String id, Codec<T> codec, EntityConfigValue<T, E> value, EntityValueFormatter<T, E> formatter,
                                                              WrenchModifyEntityValue<T, E> action) {
        return new EntityConfig<T, E>(id, Text.translatable("item.polyfactory.wrench.action." + id),
                codec,
                value,
                formatter,
                action, action);
    }

    public static <T, E extends Entity> EntityConfig<T, E> of(String id, Codec<T> codec, EntityConfigValue<T, E> value,
                                                              WrenchModifyEntityValue<T, E> action) {
        return new EntityConfig<T, E>(id, Text.translatable("item.polyfactory.wrench.action." + id),
                codec,
                value,
                EntityValueFormatter.getDefault(),
                action, action);
    }


    public EntityConfig<T, E> withAlt(WrenchModifyEntityValue<T, E> alt) {
        return new EntityConfig<>(this.id, this.name, this.codec, this.value, this.formatter, this.action, alt);
    }

    public Text getDisplayValue(E entity, Vec3d pos) {
        return this.formatter.getDisplayValue(this.value.getValue(entity, pos), entity, pos);
    }

    public <A extends Entity> EntityConfig<T, A> cast() {
        //noinspection unchecked
        return (EntityConfig<T, A>) this;
    }
}
