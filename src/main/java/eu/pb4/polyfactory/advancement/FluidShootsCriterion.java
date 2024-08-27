package eu.pb4.polyfactory.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.advancement.FactoryAdvancementCriteria;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.fluid.FluidInstance;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

public class FluidShootsCriterion extends AbstractCriterion<FluidShootsCriterion.Condition> {
    public static AdvancementCriterion<?> ofNozzle(FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.NOZZLE_SHOOTS.create(new Condition(Optional.empty(), List.of(fluids)));
    }

    public static AdvancementCriterion<?> ofFluidLauncher(FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.create(new Condition(Optional.empty(), List.of(fluids)));
    }

    public static AdvancementCriterion<?> ofFluidLauncher(ItemPredicate.Builder builder, FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.create(new Condition(Optional.of(builder.build()), List.of(fluids)));
    }

    public static void triggerNozzle(ServerPlayerEntity player, FluidInstance<?> instance) {
        PolyFactoryAdvancementCriteria.NOZZLE_SHOOTS.trigger(player, condition -> condition.fluids.isEmpty() || condition.fluids.contains(instance));
    }

    public static void triggerFluidLauncher(ServerPlayerEntity player, ItemStack stack, FluidInstance<?> instance) {
        PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.trigger(player, condition ->
                (condition.fluids.isEmpty() || condition.fluids.contains(instance))
                        && (condition.itemPredicate.isEmpty() || condition.itemPredicate.get().test(stack)));
    }

    @Override
    public Codec<FluidShootsCriterion.Condition> getConditionsCodec() {
        return Condition.CODEC;
    }

    public record Condition(Optional<ItemPredicate> itemPredicate, List<FluidInstance<?>> fluids) implements Conditions {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Condition::itemPredicate),
                FluidInstance.CODEC.listOf().fieldOf("fluid").forGetter(Condition::fluids)
        ).apply(instance, Condition::new));
        @Override
        public Optional<LootContextPredicate> player() {
            return Optional.empty();
        }
    }
}
