package eu.pb4.polyfactory;

import eu.pb4.factorytools.impl.DebugData;
import eu.pb4.factorytools.api.advancement.FactoryAdvancementCriteria;
import eu.pb4.polyfactory.advancement.FactoryItemPredicates;
import eu.pb4.polyfactory.block.FactoryPoi;
import eu.pb4.polyfactory.block.data.output.RedstoneOutputBlock;
import eu.pb4.polyfactory.block.mechanical.AxleBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithGearBlock;
import eu.pb4.polyfactory.block.mechanical.AxleWithLargeGearBlock;
import eu.pb4.polyfactory.block.mechanical.machines.PlanterBlock;
import eu.pb4.polyfactory.block.mechanical.source.WindmillBlock;
import eu.pb4.polyfactory.block.data.providers.TinyPotatoSpringBlock;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.item.FactoryEnchantments;
import eu.pb4.polyfactory.loottable.FactoryLootTables;
import eu.pb4.polyfactory.models.CableModel;
import eu.pb4.polyfactory.models.ConveyorModel;
import eu.pb4.polyfactory.models.GenericParts;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.*;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInit implements ModInitializer {
	public static final String ID = "polyfactory";
	public static final String VERSION = FabricLoader.getInstance().getModContainer(ID).get().getMetadata().getVersion().getFriendlyString();
	public static final Logger LOGGER = LoggerFactory.getLogger("PolyFactory");
    public static final boolean DEV_ENV = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final boolean DEV_MODE = VERSION.contains("-dev.") || DEV_ENV;
    @SuppressWarnings("PointlessBooleanExpression")
	public static final boolean DYNAMIC_ASSETS = true && DEV_ENV;

    public static Identifier id(String path) {
		return new Identifier(ID, path);
	}

	@Override
	public void onInitialize() {
		if (VERSION.contains("-dev.")) {
			LOGGER.warn("=====================================================");
			LOGGER.warn("You are using development version of PolyFactory!");
			LOGGER.warn("Support is limited, as features might be unfinished!");
			LOGGER.warn("You are on your own!");
			LOGGER.warn("=====================================================");
		}

		FactoryBlocks.register();
		FactoryPoi.register();
		FactoryBlockEntities.register();
		FactoryEnchantments.register();
		FactoryItems.register();
		FactoryEntities.register();
		FactoryNodes.register();
		FactoryRecipeTypes.register();
		FactoryRecipeSerializers.register();
		FactoryLootTables.register();
		FactoryCommands.register();
		FactoryUtil.register();
		FactoryItemPredicates.register();
		DebugData.register();
		PotatoWisdom.load();

		ConveyorModel.registerAssetsEvents();
		CableModel.registerAssetsEvents();
		initModels();
		UiResourceCreator.setup();
		GuiTextures.register();
		PolydexCompat.register();
		PolymerResourcePackUtils.addModAssets(ID);
		PolymerResourcePackUtils.markAsRequired();

		ServerPlayConnectionEvents.JOIN.register(FactorySecrets::onJoin);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void initModels() {
		AxleBlock.Model.ITEM_MODEL.getItem();
		WindmillBlock.Model.MODEL.getItem();
		AxleWithGearBlock.Model.ITEM_MODEL_1.getItem();
		AxleWithLargeGearBlock.Model.GEAR_MODEL.getItem();
		PlanterBlock.Model.OUTPUT_1.getItem();
		TinyPotatoSpringBlock.Model.BASE_MODEL.getItem();
		RedstoneOutputBlock.Model.OUTPUT_OVERLAY.item();
		GenericParts.SMALL_GEAR.isEmpty();
	}
}
