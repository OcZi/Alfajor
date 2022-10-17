package me.oczi.alfajor.api.protocol;

import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;

public interface RemapperInjector {

    void inject(PacketRemapper remapper);
}
