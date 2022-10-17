package me.oczi.alfajor.api.protocol;

import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;

import java.lang.reflect.Field;
import java.util.Map;

public class ProtocolHandleImpl<C extends ClientboundPacketType, S extends ServerboundPacketType>
    implements ProtocolHandle<C, S> {
    private final Protocol<C, ?, ?, S> baseProtocol;

    private final Map<AbstractProtocol.Packet, AbstractProtocol.ProtocolPacket> serverbound;
    private final Map<AbstractProtocol.Packet, AbstractProtocol.ProtocolPacket> clientbound;

    public ProtocolHandleImpl(Protocol<C, ?, ?, S> protocol) throws NoSuchFieldException, IllegalAccessException {
        this.baseProtocol = protocol;

        Class<AbstractProtocol<?, ?, ?, ?>> abstractProtocol = getAbstractProtocol(protocol.getClass());
        serverbound = getFieldValue(protocol, abstractProtocol, "serverbound");
        clientbound = getFieldValue(protocol, abstractProtocol, "clientbound");
    }

    @SuppressWarnings("unchecked")
    private Class<AbstractProtocol<?, ?, ?, ?>> getAbstractProtocol(Class<?> clazz) {
        Class<?> superClass = clazz.getSuperclass();
        return superClass.equals(AbstractProtocol.class)
            ? (Class<AbstractProtocol<?, ?, ?, ?>>) superClass
            : getAbstractProtocol(superClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T getFieldValue(Object instance, Class<?> clazz, String name)
        throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(instance);
    }

    @Override
    public ProtocolHandle<C, S> injectClientbound(C type, RemapperInjector injector) {
        AbstractProtocol.Packet packet = new AbstractProtocol.Packet(type.state(), type.getId());
        AbstractProtocol.ProtocolPacket protocolPacket = clientbound.get(packet);
        PacketRemapper remapper = protocolPacket.getRemapper();
        if (remapper == null) {
            remapper = new PacketRemapper() {
                @Override
                public void registerMap() {
                    injector.inject(this);
                }
            };
            protocolPacket = new AbstractProtocol.ProtocolPacket(protocolPacket.getState(),
                protocolPacket.getUnmappedPacketType(),
                protocolPacket.getMappedPacketType(),
                remapper);
            clientbound.put(packet, protocolPacket);
        } else {
            injector.inject(remapper);
        }
        return this;
    }

    @Override
    public ProtocolHandle<C, S> injectOverrideClientbound(C type, RemapperInjector handler) {
        AbstractProtocol.Packet packet = new AbstractProtocol.Packet(type.state(), type.getId());
        AbstractProtocol.ProtocolPacket protocolPacket = clientbound.get(packet);
        PacketRemapper remapper = new PacketRemapper() {
            @Override
            public void registerMap() {
                handler.inject(this);
            }
        };
        protocolPacket = new AbstractProtocol.ProtocolPacket(protocolPacket.getState(),
            protocolPacket.getUnmappedPacketType(),
            protocolPacket.getMappedPacketType(),
            remapper);
        clientbound.put(packet, protocolPacket);
        return this;
    }

    @Override
    public ProtocolHandle<C, S> injectServerbound(S type, RemapperInjector injector) {
        AbstractProtocol.Packet packet = new AbstractProtocol.Packet(type.state(), type.getId());
        AbstractProtocol.ProtocolPacket protocolPacket = serverbound.get(packet);
        PacketRemapper remapper = protocolPacket.getRemapper();
        if (remapper == null) {
            remapper = new PacketRemapper() {
                @Override
                public void registerMap() {
                    injector.inject(this);
                }
            };
            protocolPacket = new AbstractProtocol.ProtocolPacket(protocolPacket.getState(),
                protocolPacket.getUnmappedPacketType(),
                protocolPacket.getMappedPacketType(),
                remapper);
            serverbound.put(packet, protocolPacket);
        } else {
            injector.inject(remapper);
        }
        return this;
    }

    @Override
    public ProtocolHandle<C, S> injectOverrideServerbound(S type, RemapperInjector injector) {
        AbstractProtocol.Packet packet = new AbstractProtocol.Packet(type.state(), type.getId());
        AbstractProtocol.ProtocolPacket protocolPacket = serverbound.get(packet);
        PacketRemapper remapper = new PacketRemapper() {
            @Override
            public void registerMap() {
                injector.inject(this);
            }
        };
        protocolPacket = new AbstractProtocol.ProtocolPacket(protocolPacket.getState(),
            protocolPacket.getUnmappedPacketType(),
            protocolPacket.getMappedPacketType(),
            remapper);
        serverbound.put(packet, protocolPacket);
        return this;
    }

    @Override
    public Protocol<C, ?, ?, S> getProtocol() {
        return baseProtocol;
    }
}
