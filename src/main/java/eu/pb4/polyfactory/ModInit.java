package eu.pb4.polyfactory;

import eu.pb4.polyfactory.advancement.PolyFactoryAdvancementCriteria;
import eu.pb4.polyfactory.advancement.FactoryItemPredicates;
import eu.pb4.polyfactory.block.FactoryPoi;
import eu.pb4.polyfactory.effects.FactoryEffects;
import eu.pb4.polyfactory.entity.FactoryEntities;
import eu.pb4.polyfactory.fluid.FactoryFluids;
import eu.pb4.polyfactory.item.FactoryDataComponents;
import eu.pb4.polyfactory.item.FactoryEnchantmentEffectComponents;
import eu.pb4.polyfactory.loottable.FactoryLootTables;
import eu.pb4.polyfactory.models.*;
import eu.pb4.polyfactory.other.FactoryRegistries;
import eu.pb4.polyfactory.polydex.PolydexCompat;
import eu.pb4.polyfactory.recipe.FactoryRecipeSerializers;
import eu.pb4.polyfactory.recipe.FactoryRecipeTypes;
import eu.pb4.polyfactory.ui.FluidTextures;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.UiResourceCreator;
import eu.pb4.polyfactory.util.*;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.polyfactory.nodes.FactoryNodes;
import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.polymer.resourcepack.extras.api.format.item.ItemAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.item.model.BasicItemModel;
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
		return Identifier.of(ID, path);
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
		FactoryRegistries.setup();
		FactoryBlocks.register();
		FactoryPoi.register();
		FactoryBlockEntities.register();
		FactoryEnchantmentEffectComponents.register();
		FactoryDataComponents.register();
		FactoryItems.register();
		FactoryFluids.register();
		FactoryEntities.register();
		FactoryEffects.register();
		FactoryNodes.register();
		FactoryRecipeTypes.register();
		FactoryRecipeSerializers.register();
		FactoryLootTables.register();
		FactoryCommands.register();
		PolyFactoryAdvancementCriteria.register();
		FactoryUtil.register();
		FactoryItemPredicates.register();
		PotatoWisdom.load();
		FluidTextures.setup();
		FactoryModels.load();

		ConveyorModels.registerAssetsEvents();
		UiResourceCreator.setup();
		GuiTextures.register();
		PolydexCompat.register();
		PolymerResourcePackUtils.addModAssets(ID);
		ResourcePackExtras.forDefault().addBridgedModelsFolder(id("block"), id("particle"), id("entity"));
		ResourcePackExtras.forDefault().addBridgedModelsFolder(id("sgui"), ((identifier, builder) -> {
			return  new ItemAsset(new BasicItemModel(identifier),  new ItemAsset.Properties(false, true));
		}));
		PolymerResourcePackUtils.markAsRequired();

		FluidTextures.setup();

		ServerPlayConnectionEvents.JOIN.register(FactorySecrets::onJoin);

		//for (var block : Registries.BLOCK) {
		//	SoundPatcher.convertIntoServerSound(block.getDefaultState().getSoundGroup());
		//}
	}
}
