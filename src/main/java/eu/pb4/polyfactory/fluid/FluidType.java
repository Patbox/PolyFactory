package eu.pb4.polyfactory.fluid;

import com.mojang.serialization.Codec;
import eu.pb4.polyfactory.FactoryRegistries;
import eu.pb4.polyfactory.util.FactoryUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public record FluidType() {
    public static final long BLOCK_AMOUNT = FluidConstants.BLOCK;
    public static final Codec<FluidType> CODEC = FactoryRegistries.FLUID_TYPES.getCodec();

    public Text getName() {
        return Text.translatable(Util.createTranslationKey("fluid_type", FactoryRegistries.FLUID_TYPES.getId(this)));
    }

    public MutableText toLabeledAmount(long amount) {
        return Text.empty().append(getName()).append(": ").append(FactoryUtil.fluidText(amount));
    }
}
