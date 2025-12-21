package eu.pb4.polyfactory.polydex;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.sgui.api.elements.GuiElement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.crafting.RecipeType;

import static eu.pb4.polyfactory.ModInit.LOGGER;

public class PolydexCompat {
    private static final boolean IS_PRESENT = FabricLoader.getInstance().isModLoaded("polydex2");


    public static void register() {
        if (IS_PRESENT) {
            PolydexCompatImpl.register();
        } else {
            LOGGER.warn("[PolyFactory] Polydex not found! It's highly suggested to install it!");
        }
    }


    public static GuiElement getButton(RecipeType<?> type) {
        if (IS_PRESENT) {
            return PolydexCompatImpl.getButton(type);
        }
        return GuiElement.EMPTY;
    }
}
