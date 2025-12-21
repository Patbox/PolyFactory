package eu.pb4.polyfactory.block.data.io;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.DataType;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;

public class DataExtractorBlockEntity extends InputTransformerBlockEntity {
    private String field;

    public DataExtractorBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, "");
    }

    public DataExtractorBlockEntity(BlockPos pos, BlockState state, String defaultValue) {
        super(FactoryBlockEntities.DATA_EXTRACTOR, pos, state);
        this.field = defaultValue;
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.putString("field", this.field);
    }

    @Override
    public void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        this.field = view.getStringOr("field", "");
    }

    public void setField(String field) {
        this.field = field;
        this.setChanged();
    }

    public String field() {
        return field;
    }

    @Override
    protected void createGui(ServerPlayer playerEntity) {
        new Gui(playerEntity, this);
    }

    private static class Gui extends AnvilInputGui {
        private final DataExtractorBlockEntity blockEntity;

        public Gui(ServerPlayer player, DataExtractorBlockEntity blockEntity) {
            super(player, true);
            this.blockEntity = blockEntity;
            this.setTitle(GuiTextures.DATA_EXTRACTOR.apply(blockEntity.getDisplayName()));
            this.setDefaultInputValue(blockEntity.field);
            this.updateDone();
            this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(CommonComponents.GUI_BACK).setCallback(x -> {
                FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
                this.close();
            }));
            this.open();
        }

        @Override
        public void onInput(String input) {
            super.onInput(input);
            this.updateDone();
            if (this.screenHandler != null) {
                this.screenHandler.setRemoteSlot(2, ItemStack.EMPTY);
            }

        }

        private void updateDone() {
            this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(CommonComponents.GUI_DONE).setCallback(x -> {
                FactoryUtil.playSoundToPlayer(player,SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.UI, 0.5f, 1);
                this.blockEntity.setField(this.getInput());
                var b = ((DataExtractorBlock) this.blockEntity.getBlockState().getBlock());
                b.sendData(this.blockEntity.level, this.blockEntity.getBlockState().getValue(DataExtractorBlock.FACING_OUTPUT), this.blockEntity.getBlockPos(), this.blockEntity.lastInput());
                this.close();
            }));

            int i = 3;
            {
                var b = new GuiElementBuilder(Items.NETHER_STAR).setName(Component.translatable("data_type.polyfactory.any").withStyle(ChatFormatting.ITALIC));
                for (var field : DataContainer.GENERIC_EXTRACTS) {
                    var color = ChatFormatting.GRAY;

                    if (this.getInput().equals(field)) {
                        color = ChatFormatting.YELLOW;
                    }

                    b.addLoreLine(Component.empty()
                            .append(Component.literal(" " + field).setStyle(Style.EMPTY.withItalic(true)))
                            .append(" - ")
                            .append(Component.translatable("data_type_field.polyfactory.any." + field + ".desc"))
                            .setStyle(Style.EMPTY.withColor(color))
                    );
                }
                this.setSlot(i++, b);
            }
            for (var x : DataType.types()) {
                if (x.fields().isEmpty()) {
                    continue;
                }
                var b = new GuiElementBuilder(Items.PAPER).setName(x.name());
                if (x == this.blockEntity.lastInput().type()) {
                    b.glow();
                }
                for (var field : x.fields()) {
                    var color = ChatFormatting.GRAY;

                    if (
                            (field.endsWith("*") && this.getInput().startsWith(field.substring(0, field.length() - 1)))
                                    || (!field.endsWith("*") && this.getInput().equals(field))) {
                        color = ChatFormatting.YELLOW;
                    }


                    b.addLoreLine(Component.empty()
                            .append(Component.literal(" " + field).setStyle(Style.EMPTY.withItalic(true)))
                            .append(" - ")
                            .append(Component.translatable("data_type_field.polyfactory." + x.getSerializedName() + "." + field + ".desc"))
                            .setStyle(Style.EMPTY.withColor(color))
                    );
                }
                this.setSlot(i++, b);
            }
        }

        @Override
        public void setDefaultInputValue(String input) {
            super.setDefaultInputValue(input);
            if (this.blockEntity != null) {
                updateDone();
            }
            var itemStack = GuiTextures.EMPTY.getItemStack().copy();
            itemStack.set(DataComponents.CUSTOM_NAME, Component.literal(input));
            itemStack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
            this.setSlot(0, itemStack, Objects.requireNonNull(this.getSlot(0)).getGuiCallback());
        }

        @Override
        public void onTick() {
            if (this.blockEntity.isRemoved()
                    || player.position().distanceToSqr(Vec3.atCenterOf(this.blockEntity.getBlockPos())) > (18 * 18)) {
                this.close();
                return;
            }
            super.onTick();
        }
    }

}
