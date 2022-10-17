package me.oczi.alfajor.api.protocol;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;

/**
 * Class to hack a {@link Protocol} to register (or override)
 * client and server {@link PacketRemapper PacketRemappers.}
 *
 * @param <C> old clientbound type of protocol.
 * @param <S> new serverbound type of protocol
 */
public interface ProtocolHandle<C extends ClientboundPacketType, S extends ServerboundPacketType> {

    /**
     * Create a new instance of ProtocolHandle wrapping an existed protocol.
     * @param protocol Protocol to wrap.
     * @return ProtocolHandle instance.
     *
     * @param <C> old clientbound type of protocol.
     * @param <S> new serverbound type of protocol
     * @throws NoSuchFieldException Reflection exception.
     * @throws IllegalAccessException Reflection exception.
     */
    static <C extends ClientboundPacketType,
        S extends ServerboundPacketType>
    ProtocolHandle<C, S> wrap(Protocol<C, ?, ?, S> protocol) throws NoSuchFieldException, IllegalAccessException {
        return new ProtocolHandleImpl<>(protocol);
    }

    ProtocolHandle<C, S> injectClientbound(C type, RemapperInjector handler);

    ProtocolHandle<C, S> injectOverrideClientbound(C type, RemapperInjector handler);

    default ProtocolHandle<C, S> listenWrapperClientbound(C type, PacketHandler handler) {
        return injectClientbound(type, remapper -> remapper.handler(handler));
    }

    ProtocolHandle<C, S> injectServerbound(S type, RemapperInjector handler);

    ProtocolHandle<C, S> injectOverrideServerbound(S type, RemapperInjector handler);

    default ProtocolHandle<C, S> listenWrapperServerbound(S type, PacketHandler handler) {
        return injectServerbound(type, remapper -> remapper.handler(handler));
    }

    Protocol<C, ?, ?, S> getProtocol();
}
