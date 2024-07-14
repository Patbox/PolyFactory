package eu.pb4.polyfactory.block.fluids;

public interface FluidInputOutput extends FluidOutput, FluidInput {
    interface ContainerBased extends FluidOutput.ContainerBased, FluidInput.ContainerBased {

    }
}
