package eu.pb4.polyfactory.fluid;

public interface FluidExchangeHandler extends FluidHolder {
    boolean canInsert(FluidInstance<?> type, long amount, boolean exact);

    long insert(FluidInstance<?> type, long amount, boolean exact);

    boolean canExtract(FluidInstance<?> type, long amount, boolean exact);

    /// Returns amount extracted
    long extract(FluidInstance<?> type, long amount, boolean exact);

    default boolean canInsert(FluidStack<?> stack, boolean strict) {
        return canInsert(stack.instance(), stack.amount(), strict);
    }

    default boolean canExtract(FluidStack<?> stack, boolean strict) {
        return canExtract(stack.instance(), stack.amount(), strict);
    }

    default void insertExact(FluidInstance<?> instance, long amount) {
        insert(instance, amount, true);
    }

    default long insert(FluidStack<?> stack, boolean strict) {
        return insert(stack.instance(), stack.amount(), strict);
    }

    default long extract(FluidStack<?> stack, boolean strict) {
        return extract(stack.instance(), stack.amount(), strict);
    }
}
