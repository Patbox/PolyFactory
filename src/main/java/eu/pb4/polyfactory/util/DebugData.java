package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DebugData {
    public static boolean enabled = false;
    private static int tick;
    private static Object2IntMap<Class<?>> CURRENT_CALL_MAP = new Object2IntOpenCustomHashMap<>(Util.identityHashStrategy());
    private static Object2IntMap<Class<?>> PREVIOUS_CALL_MAP = new Object2IntOpenCustomHashMap<>(Util.identityHashStrategy());

    public static void register() {
        enabled = ModInit.DEV;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (enabled && tick % 20 == 0) {
                var old = PREVIOUS_CALL_MAP;
                old.clear();
                PREVIOUS_CALL_MAP = CURRENT_CALL_MAP;
                CURRENT_CALL_MAP = old;
            }
            tick++;
        });
    }

    public static void addPacketCall(Object source) {
        if (enabled) {
            CURRENT_CALL_MAP.put(source.getClass(), CURRENT_CALL_MAP.getInt(source.getClass()) + 1);
        }
    }

    public static void printPacketCalls(BiConsumer<Class<?>, Integer> consumer) {
        if (enabled) {
            PREVIOUS_CALL_MAP.forEach(consumer);
        }
    }
}
