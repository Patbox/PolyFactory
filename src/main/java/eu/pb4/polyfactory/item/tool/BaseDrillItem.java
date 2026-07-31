package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItemIds;
import eu.pb4.polyfactory.item.component.MiningMode;
import eu.pb4.polyfactory.item.util.CustomItemBrokenHandler;
import eu.pb4.polyfactory.ui.GuiModels;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.ServerPlayerGameModeExt;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.*;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.CustomModelDataFlagProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.CustomModelDataStringProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static eu.pb4.polyfactory.ModInit.id;

public class BaseDrillItem extends Item implements PolymerItem, CustomItemBrokenHandler {
    public static final List<Identifier> HEAD_ATTACHMENT_IDS = new ArrayList<>(List.of(
            FactoryItemIds.COPPER_DRILL_HEAD.identifier(),
            FactoryItemIds.IRON_DRILL_HEAD.identifier(),
            FactoryItemIds.GOLDEN_DRILL_HEAD.identifier(),
            FactoryItemIds.DIAMOND_DRILL_HEAD.identifier(),
            FactoryItemIds.NETHERITE_DRILL_HEAD.identifier()
    ));

    public BaseDrillItem(Properties properties) {
        super(properties);
        var model = properties.effectiveModel();

        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> setupModel(builder, model));
    }

    @Override
    public Component getName(ItemStack self) {
        if (self.has(FactoryDataComponents.DRILL_ATTACHMENT) && (self.get(FactoryDataComponents.DRILL_ATTACHMENT).get(FactoryDataComponents.MATERIAL_NAME) != null)) {
            return Component.translatable(this.descriptionId + ".title", Objects.requireNonNull(Objects.requireNonNull(self.get(FactoryDataComponents.DRILL_ATTACHMENT)).get(FactoryDataComponents.MATERIAL_NAME)));
        }

        return super.getName(self);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        if (clickAction == ClickAction.PRIMARY) {
            return super.overrideOtherStackedOnMe(self, other, slot, clickAction, player, carriedItem);
        }

        if (other.isEmpty() && self.has(FactoryDataComponents.DRILL_ATTACHMENT)) {
            carriedItem.set(takeItemFrom(self));
            return true;
        }

        if (other.has(FactoryDataComponents.DRILL_HEAD_TOOL)) {
            carriedItem.set(putItemIn(self, other));
            return true;
        }

        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getMainHandItem();

        var modes = stack.get(FactoryDataComponents.MINING_MODES);
        if (modes == null) {
            return InteractionResult.PASS;
        }

        var mode = stack.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE);

        stack.set(FactoryDataComponents.SELECTED_MINING_MODE, player.isSecondaryUseActive() ? Util.findPreviousInIterable(modes, mode) : Util.findNextInIterable(modes, mode));

        FactoryUtil.playSoundToPlayer(player, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.2f);

        player.sendOverlayMessage(Component.empty().append(player.getMainHandItem().getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE).tooltip()));

        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        if (display.shows(FactoryDataComponents.DRILL_ATTACHMENT)) {
            if (itemStack.has(FactoryDataComponents.DRILL_ATTACHMENT)) {
                var head = Objects.requireNonNull(itemStack.get(FactoryDataComponents.DRILL_ATTACHMENT));
                builder.accept(Component.translatable("text.polyfactory.attached", head.getOrDefault(DataComponents.CUSTOM_NAME, head.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY)))
                        .withStyle(ChatFormatting.GRAY)
                );
            } else {
                builder.accept(Component.translatable("item.polyfactory.portable_drill.tooltip.1").withColor(TextColor.GRAY));
                builder.accept(Component.translatable("item.polyfactory.portable_drill.tooltip.2").withColor(TextColor.GRAY));
            }
        }
    }

    @Override
    public void onDestroyed(ItemEntity entity) {
        super.onDestroyed(entity);
        var stack = takeItemFrom(entity.getItem());
        if (!stack.isEmpty()) {
            ItemUtils.onContainerDestroyed(entity, Stream.of(stack));
        }
    }

    @Override
    public boolean canDestroyBlock(ItemStack itemStack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        return itemStack.has(FactoryDataComponents.DRILL_ATTACHMENT) && this.hasFuel(user, itemStack);
    }

    @Override
    public boolean mineBlock(ItemStack itemStack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        var result = super.mineBlock(itemStack, level, state, pos, owner);
        if (!hasFuel(owner, itemStack)) {
            return result;
        }

        var takePower = !(owner instanceof ServerPlayer player) || !(((ServerPlayerGameModeExt) player.gameMode).polyfactory$currentlyDestroyed() instanceof BlockPos currentPos) || currentPos.equals(pos);

        if (takePower) {
            this.drainFuel(owner, itemStack);
            return true;
        }


        return false;
    }

    protected boolean hasFuel(LivingEntity user, ItemStack itemStack) {
        return true;
    }

    protected long getUsesFromFuel(LivingEntity user, ItemStack itemStack) {
        return Long.MAX_VALUE;
    }

    protected void drainFuel(LivingEntity owner, ItemStack itemStack) {
    }

    @Override
    public boolean onItemBreakingDamageApplied(ItemStack itemStack, @Nullable ServerPlayer player, Consumer<Item> onBreak) {
        var head = takeItemFrom(itemStack);
        onBreak.accept(head.getItem());
        return true;
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        PolymerItem.super.modifyBasePolymerItemStack(out, stack, context, lookup);
        var head = stack.get(FactoryDataComponents.DRILL_ATTACHMENT);
        String id = "";
        String mode = stack.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE).getSerializedName();
        if (head != null) {
            id = "" + head.components().get(head.item().components(), DataComponents.ITEM_MODEL);
        }

        if (stack.getOrDefault(FactoryDataComponents.SELECTED_MINING_MODE, MiningMode.SINGLE) != MiningMode.SINGLE) {
            out.set(DataComponents.TOOL, new Tool(List.of(), 0, 1, true));
        }

        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                List.of(),
                List.of(!id.isEmpty()),
                List.of(id, mode),
                List.of()
        ));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.TRIAL_KEY;
    }

    private static ItemStack putItemIn(ItemStack drill, ItemStack head) {
        var out = takeItemFrom(drill);

        drill.set(FactoryDataComponents.DRILL_ATTACHMENT, ItemStackTemplate.fromNonEmptyStack(head));
        drill.set(FactoryDataComponents.MINING_MODES, head.get(FactoryDataComponents.MINING_MODES));
        var modes = head.get(FactoryDataComponents.MINING_MODES);

        drill.set(FactoryDataComponents.SELECTED_MINING_MODE, modes != null && !modes.isEmpty() ? modes.getFirst() : null);
        drill.set(DataComponents.TOOL, head.get(FactoryDataComponents.DRILL_HEAD_TOOL));
        drill.set(DataComponents.ATTRIBUTE_MODIFIERS, head.get(FactoryDataComponents.DRILL_HEAD_ATTRIBUTE_MODIFIERS));
        drill.set(DataComponents.ENCHANTMENTS, head.get(DataComponents.ENCHANTMENTS));
        drill.set(DataComponents.MAX_DAMAGE, head.get(DataComponents.MAX_DAMAGE));
        drill.set(DataComponents.DAMAGE, head.get(DataComponents.DAMAGE));
        drill.set(DataComponents.UNBREAKABLE, head.get(DataComponents.UNBREAKABLE));
        drill.set(DataComponents.WEAPON, head.get(DataComponents.WEAPON));

        return out;
    }

    private static ItemStack takeItemFrom(ItemStack drill) {
        if (!drill.has(FactoryDataComponents.DRILL_ATTACHMENT)) {
            return ItemStack.EMPTY;
        } else {
            var head = drill.get(FactoryDataComponents.DRILL_ATTACHMENT).create();
            head.set(DataComponents.MAX_DAMAGE, drill.get(DataComponents.MAX_DAMAGE));
            head.set(DataComponents.DAMAGE, drill.get(DataComponents.DAMAGE));

            drill.remove(FactoryDataComponents.DRILL_ATTACHMENT);
            drill.remove(FactoryDataComponents.MINING_MODES);
            drill.remove(FactoryDataComponents.SELECTED_MINING_MODE);
            drill.remove(DataComponents.TOOL);
            drill.remove(DataComponents.ATTRIBUTE_MODIFIERS);
            drill.remove(DataComponents.ENCHANTMENTS);
            drill.remove(DataComponents.MAX_DAMAGE);
            drill.remove(DataComponents.DAMAGE);
            drill.remove(DataComponents.UNBREAKABLE);
            drill.remove(DataComponents.WEAPON);

            return head;
        }
    }

    private static void setupModel(ResourcePackBuilder builder, Identifier identifier) {
        var headModel = SelectItemModel.builder(new CustomModelDataStringProperty(0));
        for (var type : HEAD_ATTACHMENT_IDS) {
            var modelId = type.withPrefix("item/").withSuffix("_held");
            headModel.withCase(type.toString(), new BasicItemModel(modelId));

            builder.addData("assets/" + modelId.getNamespace() + "/models/" + modelId.getPath() + ".json",
                    new ModelAsset(id("item/handheld_drill"), Map.of("layer0", new ModelAsset.TextureValue(type.withPrefix("item/"), false))));
        }
        headModel.fallback(new BasicItemModel(id("item/fallback_drill_head_held")));
        headModel.transformation(new Matrix4f().translate(-2 / 16f, 2 / 16f, 0).scale(1, 1, 0.999f));

        var iconModel = SelectItemModel.builder(new CustomModelDataStringProperty(1));
        iconModel.fallback(EmptyItemModel.INSTANCE);
        for (var mode : MiningMode.values()) {
            if (mode == MiningMode.SINGLE) {
                continue;
            }

            iconModel.withCase(mode.getSerializedName(), new BasicItemModel(id("sgui/elements/drill_icon_" + mode.getSerializedName())));
        }

        builder.addData("assets/" + identifier.getNamespace() + "/items/" + identifier.getPath() + ".json", new ItemAsset(
                new CompositeItemModel(List.of(
                        new ConditionItemModel(new CustomModelDataFlagProperty(0),
                                new CompositeItemModel(List.of(
                                        new BasicItemModel(identifier.withSuffix("_body").withPrefix("item/")),
                                        headModel.build(),
                                        GuiModels.createGuiOnly(iconModel.build())
                                )),
                                new BasicItemModel(identifier.withSuffix("_body_empty").withPrefix("item/"))
                        )
                )), new ItemAsset.Properties(true, false)
        ));
    }
}
