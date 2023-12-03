package eu.pb4.factorytools.impl;

import it.unimi.dsi.fastutil.objects.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Util;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

public class DebugData {
    public static boolean enabled = false;
    private static int tick;
    private static Map<Class<?>, Reference2IntMap<Class<?>>> CURRENT_CALL_MAP = new Reference2ObjectOpenHashMap<>();
    private static Map<Class<?>, Reference2IntMap<Class<?>>> PREVIOUS_CALL_MAP = new Reference2ObjectOpenHashMap<>();

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
                classBound = new Reference2IntOpenHashMap<>();
                CURRENT_CALL_MAP.put(source.getClass(), classBound);
            }
            classBound.put(packet.getClass(), classBound.getInt(packet.getClass()) + 1);
        }
    }

    public static void printPacketCalls(BiConsumer<Class<?>, Collection<Reference2IntMap.Entry<Class<?>>>> consumer) {
        if (enabled) {
            PREVIOUS_CALL_MAP.forEach((a, b) -> consumer.accept(a, b.reference2IntEntrySet()));
        }
    }
}
