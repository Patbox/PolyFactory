package eu.pb4.polyfactory.block.electric;

import com.mojang.authlib.GameProfile;
import eu.pb4.factorytools.api.util.LegacyNbtHelper;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.entity.ArtificialWitherSkullEntity;
import eu.pb4.polyfactory.nodes.electric.EnergyData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WitherSkullGeneratorBlockEntity extends BlockEntity {
    private float progress = 10;
    protected GameProfile owner = null;

    public WitherSkullGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.WITHER_SKULL_GENERATOR, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        nbt.putFloat("progress", this.progress);
        if (this.owner != null) {
            nbt.put("owner", LegacyNbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
    }


    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        this.progress = nbt.getFloat("progress");
        if (nbt.contains("owner")) {
            this.owner = LegacyNbtHelper.toGameProfile(nbt.getCompound("owner"));
        }
    }

    public void updateEnergyData(EnergyData.State modifier, BlockState state, ServerWorld world, BlockPos pos) {
        modifier.use(1000);
    }

    public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T t) {
        if (!(world instanceof ServerWorld serverWorld) || !(t instanceof WitherSkullGeneratorBlockEntity self)) {
            return;
        }

        var energy = EnergyUser.getEnergy(serverWorld, pos);
        if (!energy.powered()) {
            return;
        }

        if (self.progress >= 1) {
            if (state.get(WitherSkullGeneratorBlock.POWERED)) {
                var owner = self.owner != null ? serverWorld.getPlayerByUuid(self.owner.getId()) : null;
                var skull = ArtificialWitherSkullEntity.create(serverWorld, pos, state.get(WitherSkullGeneratorBlock.FACING), owner);

                serverWorld.spawnEntity(skull);

                self.progress = 0;
                self.markDirty();
            }
        } else {
            self.progress += 1 / 80f;
            self.markDirty();
        }

    }
}
