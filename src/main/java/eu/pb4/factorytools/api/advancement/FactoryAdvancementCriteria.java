package eu.pb4.factorytools.api.advancement;

import eu.pb4.polyfactory.ModInit;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.CriterionConditions;

public class FactoryAdvancementCriteria {
    public static final TriggerCriterion TRIGGER = register("trigger", new TriggerCriterion());
    public static void register() {

    }


    public static <E extends CriterionConditions, T extends Criterion<E>> T register(String path, T item) {
        Criteria.register(ModInit.ID + ":" + path, item);
        return item;
    }
}
