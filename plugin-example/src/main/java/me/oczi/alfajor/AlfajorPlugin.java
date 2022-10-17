package me.oczi.alfajor;

import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_8.ClientboundPackets1_8;
import com.viaversion.viaversion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;
import me.oczi.alfajor.api.protocol.ProtocolHandle;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class AlfajorPlugin extends JavaPlugin {
    private ProtocolHandle<ClientboundPackets1_9_3, ServerboundPackets1_12> handle;

    @Override
    public void onEnable() {
        Protocol1_12To1_11_1 protocol = ViaAlfajor.getProtocol(Protocol1_12To1_11_1.class);
        try {
            ProtocolHandle<ClientboundPackets1_8, ServerboundPackets1_9> wrap = ProtocolHandle.wrap(ViaAlfajor.getProtocol(Protocol1_9To1_8.class));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            handle = ProtocolHandle.wrap(protocol);

            // We can't add a new remapper because it needs the chunk to read
            // (And chunk1_9 reference is unreachable).
            // So, override them with the same logic.
            handle.injectOverrideClientbound(ClientboundPackets1_9_3.CHUNK_DATA,
                remapper -> remapper
                    .handler(wrapper -> {
                            // Code from Protocol1_12To1_11_1 class
                            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                            Chunk1_9_3_4Type type = new Chunk1_9_3_4Type(clientWorld);
                            Chunk chunk = wrapper.passthrough(type);

                            for (int i = 0; i < chunk.getSections().length; i++) {
                                ChunkSection section = chunk.getSections()[i];
                                if (section == null)
                                    continue;

                                for (int y = 0; y < 16; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        for (int x = 0; x < 16; x++) {
                                            int block = section.getBlockWithoutData(x, y, z);
                                            if (block == 26) {
                                                CompoundTag tag = new CompoundTag();
                                                // Set bed with a random color
                                                // THIS IS A DEMO. DON'T USE IT IN PRODUCTION.
                                                int randomColor = new Random().nextInt(15);
                                                tag.put("color", new IntTag(randomColor));
                                                tag.put("x", new IntTag(x + (chunk.getX() << 4)));
                                                tag.put("y", new IntTag(y + (i << 4)));
                                                tag.put("z", new IntTag(z + (chunk.getZ() << 4)));
                                                tag.put("id", new StringTag("minecraft:bed"));

                                                chunk.getBlockEntities().add(tag);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    ));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        handle = null;
    }
}
