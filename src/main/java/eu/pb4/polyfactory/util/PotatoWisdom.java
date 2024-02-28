package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.random.Random;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PotatoWisdom {
    public static final List<String> RANDOM = new ArrayList<>();
    public static String get(Random random) {
        if (random.nextFloat() > 0.8 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
            return "It Is Wednesday My Dudes";
        }

        if (RANDOM.isEmpty()) {
            return "missingno";
        }

        return RANDOM.get(random.nextInt(RANDOM.size()));
    }

    public static void load() {
        try {
            var file = FabricLoader.getInstance().getModContainer(ModInit.ID).get().findPath("potato.txt");
            if (file.isPresent()) {
                RANDOM.addAll(Files.readAllLines(file.get()));
            }

            var splashPath = PolymerCommonUtils.getClientJarRoot().resolve("assets/minecraft/texts/splashes.txt");
            if (Files.exists(splashPath)) {
                RANDOM.addAll(Files.readAllLines(splashPath));
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
