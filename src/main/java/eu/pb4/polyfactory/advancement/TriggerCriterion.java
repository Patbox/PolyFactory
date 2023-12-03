package eu.pb4.polyfactory.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class TriggerCriterion extends AbstractCriterion<TriggerCriterion.Condition> {
    public static AdvancementCriterion<?> of(Identifier powerHandCrank) {
        return FactoryAdvancementCriteria.TRIGGER.create(new Condition(powerHandCrank));
    }

    public static void trigger(ServerPlayerEntity player, Identifier identifier) {
        FactoryAdvancementCriteria.TRIGGER.trigger(player, condition -> condition.identifier.equals(identifier));
    }

    @Override
    public Codec<Condition> getConditionsCodec() {
        return Condition.CODEC;
    }

    public record Condition(Identifier identifier) implements Conditions {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.fieldOf("trigger").forGetter(Condition::identifier)
        ).apply(instance, Condition::new));
        @Override
        public Optional<LootContextPredicate> player() {
            return Optional.empty();
        }
    }
}
