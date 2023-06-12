package eu.pb4.polyfactory.nodes;

import com.kneelawk.graphlib.api.graph.user.LinkKey;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static eu.pb4.polyfactory.ModInit.id;

public record GenericLinkKey() implements LinkKey {
    public static final Identifier ID = id("generic");
    @Override
    public @NotNull Identifier getTypeId() {
        return ID;
    }

    @Override
    public @Nullable NbtElement toTag() {
        return null;
    }
}
