package eu.pb4.polyfactory.entity;

import eu.pb4.polyfactory.fluid.FluidType;

public interface FluidDataOwner<T> {
    FluidType<T> fluidType();
    void setFluidData(T data);
    T getFluidData();
}
