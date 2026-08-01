package eu.pb4.polyfactory.recipe.fluid.effects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import eu.pb4.polyfactory.recipe.input.FluidContainerInput;
import eu.pb4.polymer.common.impl.LazyIdMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

import static eu.pb4.polyfactory.util.FactoryUtil.id;

public interface FluidInteractionEffect {
    Codec<FluidInteractionEffect> CODEC = Codec.lazyInitialized(() -> FluidInteractionEffect.TYPES.codec(Identifier.CODEC).dispatch(FluidInteractionEffect::codec, Function.identity()));
    ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends FluidInteractionEffect>> TYPES = new LazyIdMapper<>(m -> {
        m.put(id("explode"), ExplodeFluidInteractionEffect.CODEC);
    });

    void apply(ServerLevel serverLevel, FluidContainerInput input, int repetition, @Nullable Entity holderEntity,  Vec3 position);

    MapCodec<? extends FluidInteractionEffect> codec();
}
