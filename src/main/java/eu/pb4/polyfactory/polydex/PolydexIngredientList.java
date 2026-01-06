package eu.pb4.polyfactory.polydex;

import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;

import java.util.Arrays;
import java.util.List;

public record PolydexIngredientList<T>(PolydexIngredient<T>... entries) implements PolydexIngredient<T> {

    @SafeVarargs
    public PolydexIngredientList {
    }

    @Override
    public List<PolydexStack<T>> asStacks() {
        return Arrays.stream(entries).map(PolydexIngredient::asStacks).flatMap(List::stream).toList();
    }

    @Override
    public float chance() {
        return 1;
    }

    @Override
    public long amount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.entries.length == 0;
    }

    @Override
    public Class<T> getBackingClass() {
        return this.entries[0].getBackingClass();
    }

    @Override
    public boolean matchesDirect(PolydexStack<T> polydexStack, boolean b) {
        for (var x : this.entries) {
            if (x.matchesDirect(polydexStack, b)) {
                return true;
            }
        }

        return false;
    }
}
