package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.BasicDataType;
import eu.pb4.polyfactory.data.StringData;
import eu.pb4.polyfactory.item.wrench.WrenchAction;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;

public class TextInputBlock extends CabledDataProviderBlock {
    public static final EnumProperty<BasicDataType> MODE = EnumProperty.of("mode", BasicDataType.class);

    public TextInputBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(MODE, BasicDataType.STRING));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(MODE);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        var x = super.onUse(state, world, pos, player, hit);

        if (x == ActionResult.PASS && !player.isSneaking() && state.get(FACING).getOpposite() != hit.getSide()
                && player instanceof ServerPlayerEntity serverPlayer && world.getBlockEntity(pos) instanceof ChanneledDataBlockEntity be) {
            if (be.checkUnlocked(player)) {
                new Gui(serverPlayer, be);
            }
            return ActionResult.SUCCESS_SERVER;
        }

        return x;
    }
    private class Gui extends AnvilInputGui {
        private final ChanneledDataBlockEntity blockEntity;

        public Gui(ServerPlayerEntity player, ChanneledDataBlockEntity blockEntity) {
            super(player, false);
            this.blockEntity = blockEntity;
            this.setTitle(GuiTextures.TEXT_INPUT.apply(blockEntity.getDisplayName()));
            this.updateDone();
            this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(ScreenTexts.BACK).setCallback(x -> {
                player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                this.close();
            }));
            this.open();
        }

        @Override
        public void onInput(String input) {
            super.onInput(input);
            this.updateDone();
            if (this.screenHandler != null) {
                this.screenHandler.setPreviousTrackedSlot(2, ItemStack.EMPTY);
            }
        }

        private void updateDone() {
            var data = this.blockEntity.getCachedState().get(MODE).parse(this.getInput());
            if (data != null) {
                this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(ScreenTexts.DONE).setCallback(x -> {
                    player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.MASTER, 0.5f, 1);
                    sendData(blockEntity.getWorld(), blockEntity.getPos(), data);
                    this.close();
                }));
            } else {
                this.setSlot(1, GuiTextures.BUTTON_DONE_BLOCKED.get().setName(Text.empty().append(ScreenTexts.DONE).formatted(Formatting.GRAY)));
            }
        }

        @Override
        public void setDefaultInputValue(String input)  {
            super.setDefaultInputValue(input);
            if (this.blockEntity != null) {
                updateDone();
            }
            var itemStack = GuiTextures.EMPTY.getItemStack().copy();
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(input));
            itemStack.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
            this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
        }

        @Override
        public void onTick() {
            if (this.blockEntity.isRemoved()
                    || player.getPos().squaredDistanceTo(Vec3d.ofCenter(this.blockEntity.getPos())) > (18 * 18)) {
                this.close();
                return;
            }
            super.onTick();
        }
    }

    @Override
    public List<WrenchAction> getWrenchActions() {
        return List.of(
                WrenchAction.CHANNEL,
                this.facingAction,
                WrenchAction.of("mode", MODE, BasicDataType::text)
        );
    }
}
