package eu.pb4.polyfactory.entity.configurable;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record EntityConfig<T, E extends Entity>(String id, Component name, Codec<T> codec, EntityConfigValue<T, E> value, EntityValueFormatter<T, E> formatter,
                                                WrenchModifyEntityValue<T, E> action, WrenchModifyEntityValue<T, E> alt) {
    public static EntityConfig<Float, ?> PITCH = of("pitch", Codec.FLOAT,
            EntityConfigValue.of(Entity::getXRot, Entity::setXRot),
            EntityValueFormatter.str(x -> String.valueOf((int) Mth.triangleWave(x, 90))),
            WrenchModifyEntityValue.simple((a, b) -> (float) Mth.wrapDegrees(Math.round(a) + (b ? 1 : -1)))
    ).withAlt((value1, next, player, entity, targetPos) -> player.getXRot());

    public static EntityConfig<Float, ?> YAW = of("yaw", Codec.FLOAT,
            EntityConfigValue.of(Entity::getYRot, Entity::setYRot),
            EntityValueFormatter.str(x -> String.valueOf((int) Mth.wrapDegrees(x))),
            WrenchModifyEntityValue.simple((a, b) -> (float) Mth.wrapDegrees(Math.round(a) + (b ? 1 : -1)))
    ).withAlt((value1, next, player, entity, targetPos) -> player.getYRot());

    public static EntityConfig<?, ?> DISMOUNT = of("dismount", Unit.CODEC,
            EntityConfigValue.of(x -> x.isVehicle() ? Unit.INSTANCE : null, (a, b) -> a.ejectPassengers()),
            (unused, entity, pos) -> {
                var passenger = entity.getFirstPassenger();
                if (passenger == null) {
                    return Component.translatable("text.polyfactory.none_wrapped").withStyle(ChatFormatting.GRAY);
                }
                return passenger.getName();
            },
            WrenchModifyEntityValue.simple((a, b) -> a)
    );

    public static <T, E extends Entity> EntityConfig<T, E> of(String id, Codec<T> codec, EntityConfigValue<T, E> value, EntityValueFormatter<T, E> formatter,
                                                              WrenchModifyEntityValue<T, E> action) {
        return new EntityConfig<T, E>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                codec,
                value,
                formatter,
                action, action);
    }

    public static <T, E extends Entity> EntityConfig<T, E> of(String id, Codec<T> codec, EntityConfigValue<T, E> value,
                                                              WrenchModifyEntityValue<T, E> action) {
        return new EntityConfig<T, E>(id, Component.translatable("item.polyfactory.wrench.action." + id),
                codec,
                value,
                EntityValueFormatter.getDefault(),
                action, action);
    }


    public EntityConfig<T, E> withAlt(WrenchModifyEntityValue<T, E> alt) {
        return new EntityConfig<>(this.id, this.name, this.codec, this.value, this.formatter, this.action, alt);
    }

    public Component getDisplayValue(E entity, Vec3 pos) {
        return this.formatter.getDisplayValue(this.value.getValue(entity, pos), entity, pos);
    }

    public <A extends Entity> EntityConfig<T, A> cast() {
        //noinspection unchecked
        return (EntityConfig<T, A>) this;
    }
}
