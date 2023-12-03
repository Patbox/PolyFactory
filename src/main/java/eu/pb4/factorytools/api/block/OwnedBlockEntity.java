package eu.pb4.factorytools.api.block;

import com.mojang.authlib.GameProfile;

public interface OwnedBlockEntity {
    GameProfile getOwner();
    void setOwner(GameProfile profile);
}
