package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.function.BiConsumer;

public class DebugData {
    public static boolean enabled = false;
    private static int tick;
    private static Object2ObjectMap<Class<?>, Object2IntMap<Class<?>>> CURRENT_CALL_MAP = new Object2ObjectOpenCustomHashMap<>(Util.identityHashStrategy());
    private static Object2ObjectMap<Class<?>, Object2IntMap<Class<?>>> PREVIOUS_CALL_MAP = new Object2ObjectOpenCustomHashMap<>(Util.identityHashStrategy());

    public static void register() {
        enabled = ModInit.DEV_ENV;
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

    public static void addPacketCall(Object source, Object packet) {
        if (enabled) {
            var classBound = CURRENT_CALL_MAP.get(source.getClass());
            if (classBound == null) {
                classBound = new Object2IntOpenCustomHashMap<>(Util.identityHashStrategy());
                CURRENT_CALL_MAP.put(source.getClass(), classBound);
            }
            classBound.put(packet.getClass(), classBound.getInt(packet.getClass()) + 1);
        }
    }

    public static void printPacketCalls(BiConsumer<Class<?>, Collection<Object2IntMap.Entry<Class<?>>>> consumer) {
        if (enabled) {
            PREVIOUS_CALL_MAP.forEach((a, b) -> consumer.accept(a, b.object2IntEntrySet()));
        }
    }
}
