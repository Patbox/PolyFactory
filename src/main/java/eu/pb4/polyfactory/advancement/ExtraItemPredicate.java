package eu.pb4.polyfactory.advancement;

import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ExtraItemPredicate {
    static ItemPredicate.Builder withStatic(ItemPredicate.Builder builder, Identifier id) {
        ((ExtraItemPredicate) builder).polyfactory$setStaticPredicate(id);


        return builder;
    }

    void polyfactory$setStaticPredicate(Identifier identifier);
    @Nullable
    Identifier polyfactory$getStaticPredicate();
}
