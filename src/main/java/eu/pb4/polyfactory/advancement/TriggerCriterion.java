package eu.pb4.polyfactory.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

public class TriggerCriterion extends AbstractCriterion<TriggerCriterion.Condition> {
    public static AdvancementCriterion<?> of(Identifier powerHandCrank) {
        return FactoryAdvancementCriteria.TRIGGER.create(new Condition(powerHandCrank));
    }

    @Override
    protected Condition conditionsFromJson(JsonObject obj, Optional<LootContextPredicate> predicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        return new Condition(Identifier.tryParse(obj.get("trigger").getAsString()));
    }

    public static void trigger(ServerPlayerEntity player, Identifier identifier) {
        FactoryAdvancementCriteria.TRIGGER.trigger(player, condition -> condition.identifier.equals(identifier));
    }

    public record Condition(Identifier identifier) implements Conditions {
        @Override
        public Optional<LootContextPredicate> getPlayerPredicate() {
            return Optional.empty();
        }

        @Override
        public JsonObject toJson() {
            var obj = new JsonObject();
            obj.addProperty("trigger", this.identifier.toString());
            return obj;
        }
    }
}
