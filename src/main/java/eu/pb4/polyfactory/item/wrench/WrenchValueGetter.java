package eu.pb4.polyfactory.item.wrench;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface WrenchValueGetter {
    String getDisplayValue(World world, BlockPos pos, Direction side, BlockState state);


    @SuppressWarnings({"unchecked", "rawtypes"})
    static WrenchValueGetter ofProperty(Property<?> property) {
        return (world, pos, side, state) -> state.getOrEmpty(property).map((x) -> ((Property) property).name(x)).orElse("<*>");
    }
}
