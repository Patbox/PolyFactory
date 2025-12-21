package eu.pb4.polyfactory.block.mechanical.machines;

import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.block.other.MachineInfoProvider;
import eu.pb4.polyfactory.util.movingitem.SimpleMovingItemContainerProvider;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class TallItemMachineBlockEntity extends LockableBlockEntity implements MachineInfoProvider, SimpleMovingItemContainerProvider, WorldlyContainer {
    protected Component state;
    public TallItemMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public abstract BlockModel getModel();

    public abstract double getStress();

    protected abstract void updatePosition(int id);

    protected void addMoving(int i, MovingItem x, boolean newlyAdded) {
        var model = this.getModel();
        if (model != null) {
            if (newlyAdded) {
                updatePosition(i);
                model.addElement(x);
            } else {
                model.addElementWithoutUpdates(x);
                updatePosition(i);
            }
        }
        this.setChanged();
    }

    protected void removeMoving(MovingItem movingItem, boolean fullRemove) {
        var model = this.getModel();

        if (model != null) {
            if (fullRemove) {
                model.removeElement(movingItem);
            } else {
                model.removeElementWithoutUpdates(movingItem);
            }
        }
        this.setChanged();
    }

    @Override
    public @Nullable Component getCurrentState() {
        return this.state;
    }

    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        this.openGui((ServerPlayer) player);
        return InteractionResult.SUCCESS_SERVER;
    }
}
