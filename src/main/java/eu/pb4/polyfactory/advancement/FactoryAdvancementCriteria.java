package eu.pb4.polyfactory.advancement;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.loottable.CopyColorLootFunction;
import net.minecraft.advancement.AdvancementCriterion;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.advancement.criterion.TravelCriterion;
import net.minecraft.loot.provider.number.LootNumberProviderType;

public class FactoryAdvancementCriteria {
    public static final TriggerCriterion TRIGGER = register("trigger", new TriggerCriterion());
    public static void register() {

    }


    public static <E extends CriterionConditions, T extends Criterion<E>> T register(String path, T item) {
        Criteria.register(ModInit.ID + ":" + path, item);
        return item;
    }
}
