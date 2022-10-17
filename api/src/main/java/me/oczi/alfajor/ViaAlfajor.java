package me.oczi.alfajor;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaManager;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;


public interface ViaAlfajor {

    static ViaManager viaManager() {
        return Via.getManager();
    }

    static ProtocolManager protocolManager() {
        return viaManager().getProtocolManager();
    }

    static <T extends Protocol<?, ?, ?, ?>> T getProtocol(Class<T> clazz) {
        return protocolManager().getProtocol(clazz);
    }

    static Protocol<?, ?, ?, ?> getProtocol(ProtocolVersion client, ProtocolVersion server) {
        return protocolManager().getProtocol(client, server);
    }
}
