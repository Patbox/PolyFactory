package eu.pb4.polyfactory.block.data.util;

import eu.pb4.polyfactory.data.DataContainer;
import org.jetbrains.annotations.Nullable;

public interface DataCache {
    @Nullable
    DataContainer getCachedData();

    void setCachedData(DataContainer lastData);
}
