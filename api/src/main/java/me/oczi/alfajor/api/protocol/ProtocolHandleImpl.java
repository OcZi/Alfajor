package me.oczi.alfajor.api.protocol;

import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import static com.viaversion.viaversion.api.protocol.AbstractProtocol.*;

public class ProtocolHandleImpl<C extends ClientboundPacketType, S extends ServerboundPacketType>
    implements ProtocolHandle<C, S> {
    private final Protocol<C, ?, ?, S> baseProtocol;

    private final Map<Packet, ProtocolPacket> serverbound;
    private final Map<Packet, ProtocolPacket> clientbound;

    private final Class<C> oldClientbound;
    private final Class<? extends ClientboundPacketType> newClientbound;
    private final Class<? extends ServerboundPacketType> oldServerbound;
    private final Class<S> newServerbound;

    public ProtocolHandleImpl(Protocol<C, ?, ?, S> protocol) throws NoSuchFieldException, IllegalAccessException {
        this.baseProtocol = protocol;

        // Iterate all superclasses to get abstract protocol
        Class<AbstractProtocol<?, ?, ?, ?>> superClass = getAbstractProtocol(protocol.getClass());
        // Here is the trick; obtain clientbound/serverbound maps to interact within
        serverbound = getFieldValue(protocol, superClass, "serverbound");
        clientbound = getFieldValue(protocol, superClass, "clientbound");

        oldClientbound = getFieldValue(protocol, superClass, "oldClientboundPacketEnum");
        newClientbound = getFieldValue(protocol, superClass, "newClientboundPacketEnum");
        oldServerbound = getFieldValue(protocol, superClass, "oldServerboundPacketEnum");
        newServerbound = getFieldValue(protocol, superClass, "newServerboundPacketEnum");
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
        return injectPacket(type, injector, true, false);
    }

    @Override
    public ProtocolHandle<C, S> injectOverrideClientbound(C type, RemapperInjector injector) {
        return injectPacket(type, injector, true, true);
    }

    @Override
    public ProtocolHandle<C, S> injectServerbound(S type, RemapperInjector injector) {
        return injectPacket(type, injector, false, false);
    }

    @Override
    public ProtocolHandle<C, S> injectOverrideServerbound(S type, RemapperInjector injector) {
        return injectPacket(type, injector, false, true);
    }

    private ProtocolHandle<C, S> injectPacket(PacketType packetType,
                                              RemapperInjector injector,
                                              boolean client,
                                              boolean override) {
        Packet packet = new Packet(packetType.state(), packetType.getId());
        Map<Packet, ProtocolPacket> base = client ? clientbound : serverbound;
        ProtocolPacket protocolPacket = base.get(packet);
        PacketRemapper remapper = protocolPacket == null ? null : protocolPacket.getRemapper();
        if (override || remapper == null) {
            remapper = new PacketRemapper() {
                @Override
                public void registerMap() {
                    injector.inject(this);
                }
            };
            if (protocolPacket == null) {
                protocolPacket = new ProtocolPacket(packetType.state(),
                    packetType,
                    findMappedType(packetType, client),
                    remapper);
            } else {
                protocolPacket = new ProtocolPacket(protocolPacket.getState(),
                    Objects.requireNonNull(protocolPacket.getUnmappedPacketType(),
                        "Unmapped packet is null (packet using legacy constructor?)"),
                    protocolPacket.getMappedPacketType(),
                    remapper);
            }
        } else {
            injector.inject(remapper);
        }
        base.put(packet, protocolPacket);
        return this;
    }

    @SuppressWarnings("unchecked")
    private PacketType findMappedType(PacketType type, boolean client) {
        String name = type.getName();
        Class<? extends Enum<? extends PacketType>> enumClass;
        if (client) {
            if (newClientbound == oldClientbound) {
                return type;
            }

            enumClass = (Class<? extends Enum<? extends PacketType>>) newClientbound;
        } else {
            if (newServerbound == oldServerbound) {
                return type;
            }

            enumClass = (Class<? extends Enum<? extends PacketType>>) oldServerbound;
        }

        for (Enum<? extends PacketType> constant : enumClass.getEnumConstants()) {
            String constantName = constant.name();
            if (constantName.equals(name)) {
                return (PacketType) constant;
            }
        }

        throw new IllegalArgumentException(
            "Packet " + name + " in " + baseProtocol.getClass().getSimpleName()
                + " could not be automatically mapped!");
    }

    @Override
    public Protocol<C, ?, ?, S> getProtocol() {
        return baseProtocol;
    }
}
