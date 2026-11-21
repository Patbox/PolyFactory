package eu.pb4.polyfactory.mixin.machines;

import eu.pb4.polyfactory.block.fluids.FluidInput;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.fluid.FluidBehaviours;
import eu.pb4.polyfactory.fluid.FluidInstance;
import eu.pb4.polyfactory.mixin.CatalystListenerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SculkCatalystBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkCatalystBlockEntity.class)
public abstract class SculkCatalystBlockEntityMixin implements FluidInput {
    @Shadow
    public abstract SculkCatalystBlockEntity.CatalystListener getListener();

    @Unique
    private long injectedExperience = 0;

    @Unique
    private int injectionTimeout = 40;

    @Inject(method = "serverTick", at = @At("TAIL"))
    private static void tick(Level level, BlockPos pos, BlockState state, SculkCatalystBlockEntity entity, CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        var self = (SculkCatalystBlockEntityMixin) (Object) entity;

        if (self.injectedExperience >= FluidBehaviours.EXPERIENCE_ORB_TO_FLUID && --self.injectionTimeout == 0) {
            var listener = (CatalystListenerAccessor) self.getListener();
            var experienceWouldDrop = Math.toIntExact(self.injectedExperience / FluidBehaviours.EXPERIENCE_ORB_TO_FLUID);

            for (var p : BlockPos.withinManhattan(pos, 2)) {
                if (level.getBlockState(p).canBeReplaced() && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    self.injectedExperience -= experienceWouldDrop * FluidBehaviours.EXPERIENCE_ORB_TO_FLUID;
                    listener.getSculkSpreader().addCursors(p, experienceWouldDrop);
                    listener.callBloom(serverLevel, pos, state, level.getRandom());
                    break;
                }
            }

        } else if (self.injectedExperience <= 0) {
            self.injectionTimeout = -1;
        } else if (self.injectionTimeout < 0) {
            self.injectionTimeout = 40;
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void saveData(ValueOutput output, CallbackInfo ci) {
        output.putLong("polyfactory:injected_experience", this.injectedExperience);
        output.putInt("polyfactory:injection_timeout", this.injectionTimeout);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void loadData(ValueInput input, CallbackInfo ci) {
        this.injectedExperience = input.getLongOr("polyfactory:injected_experience", 0);
        this.injectionTimeout = input.getIntOr("polyfactory:injection_timeout", 40);
    }


    @Override
    public long insertFluid(FluidInstance<?> type, long amount, Direction direction) {
        if (!type.is(FactoryFluids.EXPERIENCE)) {
            return amount;
        }

        this.injectedExperience += amount;

        return 0;
    }
}
