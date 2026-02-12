package eu.pb4.polyfactory.polydex;

import eu.pb4.sgui.api.elements.GuiElement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeType;

import static eu.pb4.polyfactory.ModInit.LOGGER;

public class PolydexCompat {
    public static final boolean IS_PRESENT = FabricLoader.getInstance().isModLoaded("polydex2");


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

    public static void openUsagePage(ServerPlayer player, Identifier entry, Runnable runnable) {
        if (IS_PRESENT) {
            PolydexCompatImpl.openUsagePage(player, entry, runnable);
        } else {
            runnable.run();
        }
    }

    public static void openResultPage(ServerPlayer player, Identifier entry, Runnable runnable) {
        if (IS_PRESENT) {
            PolydexCompatImpl.openResultPage(player, entry, runnable);
        } else {
            runnable.run();
        }
    }
}
