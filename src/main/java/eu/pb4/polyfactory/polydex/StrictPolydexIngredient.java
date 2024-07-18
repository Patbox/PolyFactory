package eu.pb4.polyfactory.polydex;

import eu.pb4.polydex.api.v1.recipe.PolydexIngredient;
import eu.pb4.polydex.api.v1.recipe.PolydexStack;

import java.util.List;

public record StrictPolydexIngredient<T>(PolydexStack<T> stack) implements PolydexIngredient<T> {
    @Override
    public List<PolydexStack<T>> asStacks() {
        return List.of(stack);
    }

    @Override
    public float chance() {
        return stack.chance();
    }

    @Override
    public long amount() {
        return stack.amount();
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    @Override
    public Class<T> getBackingClass() {
        return stack.getBackingClass();
    }

    @Override
    public boolean matchesDirect(PolydexStack<T> polydexStack, boolean strict) {
        return this.stack.matchesDirect(polydexStack, true);
    }
}
