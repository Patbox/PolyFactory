package eu.pb4.polyfactory.block.data.io;

import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.InputTransformerBlockEntity;
import eu.pb4.polyfactory.block.data.util.ChanneledDataBlockEntity;
import eu.pb4.polyfactory.data.DataContainer;
import eu.pb4.polyfactory.data.DataType;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

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
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putString("field", this.field);
    }

    @Override
    public void readData(ReadView view) {
        super.readData(view);
        this.field = view.getString("field", "");
    }

    public void setField(String field) {
        this.field = field;
        this.markDirty();
    }

    public String field() {
        return field;
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity, this);
    }

    private static class Gui extends AnvilInputGui {
        private final DataExtractorBlockEntity blockEntity;

        public Gui(ServerPlayerEntity player, DataExtractorBlockEntity blockEntity) {
            super(player, true);
            this.blockEntity = blockEntity;
            this.setTitle(GuiTextures.DATA_EXTRACTOR.apply(blockEntity.getDisplayName()));
            this.setDefaultInputValue(blockEntity.field);
            this.updateDone();
            this.setSlot(2, GuiTextures.BUTTON_CLOSE.get().setName(ScreenTexts.BACK).setCallback(x -> {
                player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.UI, 0.5f, 1);
                this.close();
            }));
            this.open();
        }

        @Override
        public void onInput(String input) {
            super.onInput(input);
            this.updateDone();
            if (this.screenHandler != null) {
                this.screenHandler.setReceivedStack(2, ItemStack.EMPTY);
            }

        }

        private void updateDone() {
            this.setSlot(1, GuiTextures.BUTTON_DONE.get().setName(ScreenTexts.DONE).setCallback(x -> {
                player.playSoundToPlayer(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.UI, 0.5f, 1);
                this.blockEntity.setField(this.getInput());
                var b = ((DataExtractorBlock) this.blockEntity.getCachedState().getBlock());
                b.sendData(this.blockEntity.world, this.blockEntity.getCachedState().get(DataExtractorBlock.FACING_OUTPUT), this.blockEntity.getPos(), this.blockEntity.lastInput());
                this.close();
            }));

            int i = 3;
            {
                var b = new GuiElementBuilder(Items.NETHER_STAR).setName(Text.translatable("data_type.polyfactory.any").formatted(Formatting.ITALIC));
                for (var field : DataContainer.GENERIC_EXTRACTS) {
                    var color = Formatting.GRAY;

                    if (this.getInput().equals(field)) {
                        color = Formatting.YELLOW;
                    }

                    b.addLoreLine(Text.empty()
                            .append(Text.literal(" " + field).setStyle(Style.EMPTY.withItalic(true)))
                            .append(" - ")
                            .append(Text.translatable("data_type_field.polyfactory.any." + field + ".desc"))
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
                    var color = Formatting.GRAY;

                    if (
                            (field.endsWith("*") && this.getInput().startsWith(field.substring(0, field.length() - 1)))
                                    || (!field.endsWith("*") && this.getInput().equals(field))) {
                        color = Formatting.YELLOW;
                    }


                    b.addLoreLine(Text.empty()
                            .append(Text.literal(" " + field).setStyle(Style.EMPTY.withItalic(true)))
                            .append(" - ")
                            .append(Text.translatable("data_type_field.polyfactory." + x.asString() + "." + field + ".desc"))
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
            itemStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(input));
            itemStack.set(DataComponentTypes.TOOLTIP_DISPLAY, new TooltipDisplayComponent(true, ReferenceSortedSets.emptySet()));
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

}
