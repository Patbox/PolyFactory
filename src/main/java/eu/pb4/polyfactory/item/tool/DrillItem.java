package eu.pb4.polyfactory.item.tool;

import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.CompositeItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.ConditionItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.SelectItemModel;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.bool.CustomModelDataFlagProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.item.property.select.CustomModelDataStringProperty;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static eu.pb4.polyfactory.ModInit.id;

public class DrillItem extends Item implements PolymerItem {
    public DrillItem(Properties properties) {
        super(properties);
        var model = properties.effectiveModel();

        PolymerResourcePackUtils.RESOURCE_PACK_AFTER_INITIAL_CREATION_EVENT.register(builder -> setupModel(builder, model));
    }

    @Override
    public Component getName(ItemStack self) {
        if (self.has(FactoryDataComponents.DRILL_HEAD) && (self.get(FactoryDataComponents.DRILL_HEAD).get(FactoryDataComponents.MATERIAL_NAME) != null)) {
            return Component.translatable(this.descriptionId + ".title", Objects.requireNonNull(Objects.requireNonNull(self.get(FactoryDataComponents.DRILL_HEAD)).get(FactoryDataComponents.MATERIAL_NAME)));
        }

        return super.getName(self);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem) {
        if (clickAction == ClickAction.PRIMARY) {
            return super.overrideOtherStackedOnMe(self, other, slot, clickAction, player, carriedItem);
        }

        if (other.isEmpty() && self.has(FactoryDataComponents.DRILL_HEAD)) {
            carriedItem.set(self.get(FactoryDataComponents.DRILL_HEAD).create());
            self.remove(FactoryDataComponents.DRILL_HEAD);
            self.remove(DataComponents.TOOL);
            self.remove(DataComponents.ENCHANTMENTS);
            return true;
        }

        if (other.has(FactoryDataComponents.DRILL_HEAD_TOOL)) {
            if (self.has(FactoryDataComponents.DRILL_HEAD)) {
                carriedItem.set(self.get(FactoryDataComponents.DRILL_HEAD).create());
            } else {
                carriedItem.set(ItemStack.EMPTY);
            }
            self.set(FactoryDataComponents.DRILL_HEAD, ItemStackTemplate.fromNonEmptyStack(other));
            self.set(DataComponents.TOOL, other.get(FactoryDataComponents.DRILL_HEAD_TOOL));
            self.set(DataComponents.ENCHANTMENTS, other.get(DataComponents.ENCHANTMENTS));
            return true;
        }

        return false;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        if (display.shows(FactoryDataComponents.DRILL_HEAD) && itemStack.has(FactoryDataComponents.DRILL_HEAD)) {
            var head = Objects.requireNonNull(itemStack.get(FactoryDataComponents.DRILL_HEAD));
            builder.accept(Component.literal(" ")
                    .append(head.getOrDefault(DataComponents.CUSTOM_NAME, head.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY)))
                    .withStyle(ChatFormatting.GRAY)
            );

            //head.addDetailsToTooltip(context, head.getOrDefault(DataComponents.TOOLTIP_DISPLAY, display), null, tooltipFlag,
            //        text -> builder.accept(Component.literal(" ").append(text)));
        }
    }

    public void handleDamage(ItemStack self, int amount, ServerLevel level, @Nullable ServerPlayer player) {
        if (!self.has(FactoryDataComponents.DRILL_HEAD)) {
            return;
        }

        var head = self.get(FactoryDataComponents.DRILL_HEAD).create();
        head.hurtAndBreak(amount, level, player, _ -> {});
        if (head.isEmpty()) {
            self.remove(FactoryDataComponents.DRILL_HEAD);
            self.remove(DataComponents.TOOL);
        } else {
            self.set(FactoryDataComponents.DRILL_HEAD, ItemStackTemplate.fromNonEmptyStack(head));
        }
    }

    @Override
    public void modifyBasePolymerItemStack(ItemStack out, ItemStack stack, PacketContext context, HolderLookup.Provider lookup) {
        PolymerItem.super.modifyBasePolymerItemStack(out, stack, context, lookup);
        var head = stack.get(FactoryDataComponents.DRILL_HEAD);
        String id = "";
        if (head != null) {
            id = "" + head.components().get(head.item().components(), DataComponents.ITEM_MODEL);

            out.set(DataComponents.MAX_DAMAGE, head.get(DataComponents.MAX_DAMAGE));
            out.set(DataComponents.DAMAGE, head.get(DataComponents.DAMAGE));
            //out.set(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.NONE, 20));
        }
        out.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(!id.isEmpty()), List.of(id), List.of()));
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext packetContext) {
        return Items.TRIAL_KEY;
    }

    private static void setupModel(ResourcePackBuilder builder, Identifier identifier) {
        // Todo: replace this with something dynamic
        var head = List.of(
                FactoryItems.COPPER_DRILL_HEAD.builtInRegistryHolder().key().identifier(),
                FactoryItems.IRON_DRILL_HEAD.builtInRegistryHolder().key().identifier(),
                FactoryItems.GOLDEN_DRILL_HEAD.builtInRegistryHolder().key().identifier(),
                FactoryItems.DIAMOND_DRILL_HEAD.builtInRegistryHolder().key().identifier(),
                FactoryItems.NETHERITE_DRILL_HEAD.builtInRegistryHolder().key().identifier()
        );

        var headModel = SelectItemModel.builder(new CustomModelDataStringProperty(0));
        for (var type : head) {
            var modelId = type.withPrefix("item/").withSuffix("_held");
            headModel.withCase(type.toString(), new BasicItemModel(modelId));

            builder.addData("assets/" + modelId.getNamespace() + "/models/" + modelId.getPath() + ".json",
                    new ModelAsset(id("item/handheld_drill"), Map.of("layer0", new ModelAsset.TextureValue(type.withPrefix("item/"), false))));
        }
        headModel.fallback(new BasicItemModel(id("item/fallback_drill_head_held")));
        headModel.transformation(new Matrix4f().translate(-2 / 16f, 2 / 16f, 0).scale(1, 1, 0.999f));

        builder.addData("assets/" + identifier.getNamespace() + "/items/" + identifier.getPath() + ".json", new ItemAsset(
                new ConditionItemModel(new CustomModelDataFlagProperty(0),
                        new CompositeItemModel(List.of(
                                new BasicItemModel(identifier.withSuffix("_body").withPrefix("item/")),
                                headModel.build()
                        )),
                        new BasicItemModel(identifier.withSuffix("_body_empty").withPrefix("item/"))
                ), new ItemAsset.Properties(false, false)
        ));
    }
}
