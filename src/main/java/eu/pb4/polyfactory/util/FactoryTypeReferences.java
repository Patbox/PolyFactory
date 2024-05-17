package eu.pb4.polyfactory.util;

import com.mojang.datafixers.DSL;
import eu.pb4.polyfactory.data.DataContainer;
import net.minecraft.datafixer.TypeReferences;

public interface FactoryTypeReferences {
    DSL.TypeReference DATA_CONTAINER = TypeReferences.create("polyfactory:data_container");
}
