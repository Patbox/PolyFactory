package eu.pb4.polyfactory.block.other;

import com.mojang.authlib.GameProfile;

public interface OwnedBlockEntity {
    GameProfile getOwner();
    void  setOwner(GameProfile profile);
}
