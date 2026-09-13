package eu.pb4.polyfactory.util;

import eu.pb4.polyfactory.ModInit;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class WoodUtil {
    public static List<WoodType> VANILLA = List.of(
            WoodType.OAK, WoodType.SPRUCE, WoodType.BIRCH, WoodType.ACACIA,
            WoodType.CHERRY, WoodType.JUNGLE, WoodType.DARK_OAK, WoodType.PALE_OAK, WoodType.POPLAR,
            WoodType.CRIMSON, WoodType.WARPED, WoodType.MANGROVE,
            WoodType.BAMBOO);

    public static List<WoodType> MODDED = new ArrayList<>();

    private static Set<WoodType> IS_HYPNAE = new ReferenceArraySet<>(List.of(WoodType.CRIMSON,  WoodType.WARPED));
    private static Set<WoodType> IS_BLOCK = new ReferenceArraySet<>(List.of(WoodType.BAMBOO));
    private static Set<WoodType> HAS_LOG = new ReferenceArraySet<>(VANILLA);
    private static Set<WoodType> HAS_STRIPPED = new ReferenceArraySet<>(VANILLA);
    private static final List<Consumer<WoodType>> CONSUMERS = new ArrayList<>();

    public static boolean isWood(WoodType type) {
        return !isHyphae(type) && !isBlock(type);
    }

    public static boolean isHyphae(WoodType type) {
        return IS_HYPNAE.contains(type);
    }

    public static boolean isBlock(WoodType type) {
        return IS_BLOCK.contains(type);
    }

    public static boolean hasLog(WoodType type) {
        return HAS_LOG.contains(type);
    }

    public static boolean hasStripped(WoodType type) {
        return HAS_STRIPPED.contains(type);
    }

    public static boolean addModded(WoodType type) {
        var id = Identifier.tryParse(type.name());
        if (id == null) {
            ModInit.LOGGER.warn("Invalid wood type name '" + type.name() + "'! Skipping...");
            return false;
        }
        if (id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
            if (!VANILLA.contains(type)) {
                ModInit.LOGGER.warn("Missing vanilla or misnamed modded wood type name '" + type.name() + "'! Skipping...");
            }
            return false;
        }

        if (!BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_planks"))) {
            ModInit.LOGGER.warn("Missing planks of wood type '" + type.name() + "'! Skipping...");
            return false;
        }

        if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_log"))) {
            HAS_LOG.add(type);
            if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_log").withPrefix("stripped_"))) {
                HAS_STRIPPED.add(type);
            }
        } else if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_stem"))) {
            HAS_LOG.add(type);
            IS_HYPNAE.add(type);
            if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_stem").withPrefix("stripped_"))) {
                HAS_STRIPPED.add(type);
            }
        } else if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_block"))) {
            HAS_LOG.add(type);
            IS_BLOCK.add(type);
            if (BuiltInRegistries.BLOCK.containsKey(id.withSuffix("_block").withPrefix("stripped_"))) {
                HAS_STRIPPED.add(type);
            }
        }

        MODDED.add(type);
        CONSUMERS.forEach(x -> x.accept(type));
        return true;
    }

    public static String getLogSuffix(WoodType type) {
        if (!hasLog(type)) {
            return "planks";
        }

        if (isBlock(type)) {
            return "block";
        }

        if (isHyphae(type)) {
            return "stem";
        }

        return "log";
    }

    public static <T> void forEach(Map<WoodType, T> map, Consumer<T> consumer) {
        WoodType.values().forEach(x -> {
            var y = map.get(x);
            if (y != null) {
                consumer.accept(y);
            }
        });
    }

    public static <T> void forEach(List<Map<WoodType, ?>> list, Consumer<T> consumer) {
        WoodType.values().forEach(x -> {
            for (var map : list) {
                var y = map.get(x);
                if (y != null) {
                    consumer.accept((T) y);
                }
            }
        });
    }

    public static <T> void forEachByItem(List<Map<WoodType, ?>> list, Consumer<T> consumer) {
        for (var map : list) {
            WoodType.values().forEach(x -> {
                var y = map.get(x);
                if (y != null) {
                    consumer.accept((T) y);
                }
            });
        }
    }

    public static void registerVanillaAndWaitForModded(Consumer<WoodType> consumer) {
        for (var type : VANILLA) {
            consumer.accept(type);
        }
        CONSUMERS.add(consumer);
    }

    public static String asPath(WoodType type) {
        return type.name().replace(':', '/');
    }

    public static Identifier getPlanksId(WoodType type) {
        return Identifier.parse(type.name()).withSuffix("_planks");
    }

    public static Identifier getFenceId(WoodType type) {
        return Identifier.parse(type.name()).withSuffix("_fence");
    }

    public static Identifier getSlabId(WoodType type) {
        return Identifier.parse(type.name()).withSuffix("_slab");
    }

    public static Identifier getLogId(WoodType type) {
        if (hasLog(type)) {
            return Identifier.parse(type.name()).withSuffix("_" + getLogSuffix(type));
        }
        return getPlanksId(type);
    }

    public static Identifier getStrippedLogId(WoodType type) {
        if (type.name().equals("blockus:raw_bamboo")) {
            return Identifier.withDefaultNamespace("bamboo_block");
        }
        if (hasStripped(type)) {
            return getLogId(type).withPrefix("stripped_");
        }
        return getLogId(type);
    }

    public static Identifier getPlanksTexture(WoodType type) {
        return getPlanksId(type).withPrefix("block/");
    }

    public static Identifier getLogTexture(WoodType type) {
        if (type.name().startsWith("pyrite:") && type.name().endsWith("_mushroom")) {
            return Identifier.withDefaultNamespace("block/mushroom_stem");
        }

        if (type.name().equals("blockus:raw_bamboo")) {
            return Identifier.withDefaultNamespace("block/stripped_bamboo_block");
        }

        if (hasLog(type)) {
            return getLogId(type).withPrefix("block/");
        }
        return getPlanksTexture(type);
    }

    public static Identifier getStrippedLogTexture(WoodType type) {
        if (type.name().equals("blockus:raw_bamboo")) {
            return Identifier.withDefaultNamespace("block/bamboo_block");
        }

        if (hasStripped(type)) {
            return getStrippedLogId(type).withPrefix("block/");
        }
        return getLogTexture(type);
    }

    public static Identifier getLogTopTexture(WoodType type) {
        if (type.name().equals("blockus:raw_bamboo")) {
            return Identifier.withDefaultNamespace("block/stripped_bamboo_block_top");
        }

        if (hasLog(type)) {
            return getLogId(type).withPrefix("block/").withSuffix("_top");
        }
        return getPlanksTexture(type);
    }

    public static Identifier getStrippedLogTopTexture(WoodType type) {
        if (type.name().equals("blockus:raw_bamboo")) {
            return Identifier.withDefaultNamespace("block/bamboo_block_top");
        }

        if (hasStripped(type)) {
            return getStrippedLogId(type).withPrefix("block/").withSuffix("_top");
        }
        return getLogTopTexture(type);
    }
}
