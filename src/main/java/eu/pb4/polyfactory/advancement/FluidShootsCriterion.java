package eu.pb4.polyfactory.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import eu.pb4.factorytools.api.advancement.FactoryAdvancementCriteria;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.fluid.FluidInstance;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.ContextAwarePredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class FluidShootsCriterion extends SimpleCriterionTrigger<FluidShootsCriterion.Condition> {
    public static Criterion<?> ofNozzle(FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.NOZZLE_SHOOTS.createCriterion(new Condition(Optional.empty(), List.of(fluids)));
    }

    public static Criterion<?> ofFluidLauncher(FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.createCriterion(new Condition(Optional.empty(), List.of(fluids)));
    }

    public static Criterion<?> ofFluidLauncher(ItemPredicate.Builder builder, FluidInstance<?>... fluids) {
        return PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.createCriterion(new Condition(Optional.of(builder.build()), List.of(fluids)));
    }

    public static void triggerNozzle(ServerPlayer player, FluidInstance<?> instance) {
        PolyFactoryAdvancementCriteria.NOZZLE_SHOOTS.trigger(player, condition -> condition.fluids.isEmpty() || condition.fluids.contains(instance));
    }

    public static void triggerFluidLauncher(ServerPlayer player, ItemStack stack, FluidInstance<?> instance) {
        PolyFactoryAdvancementCriteria.FLUID_LAUNCHER_SHOOTS.trigger(player, condition ->
                (condition.fluids.isEmpty() || condition.fluids.contains(instance))
                        && (condition.itemPredicate.isEmpty() || condition.itemPredicate.get().test(stack)));
    }

    @Override
    public Codec<FluidShootsCriterion.Condition> codec() {
        return Condition.CODEC;
    }

    public record Condition(Optional<ItemPredicate> itemPredicate, List<FluidInstance<?>> fluids) implements SimpleInstance {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(Condition::itemPredicate),
                FluidInstance.CODEC.listOf().fieldOf("fluid").forGetter(Condition::fluids)
        ).apply(instance, Condition::new));
        @Override
        public Optional<ContextAwarePredicate> player() {
            return Optional.empty();
        }
    }
}
