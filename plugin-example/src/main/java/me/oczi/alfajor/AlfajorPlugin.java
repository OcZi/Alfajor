package me.oczi.alfajor;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import me.oczi.alfajor.api.protocol.ProtocolHandle;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class AlfajorPlugin extends JavaPlugin {
    // I literally spent 2 weeks trying to found it on internet
    // without any result.
    // Seeing its id on TileEntityBed.getUpdatePacket() (1.12 API) was much easier.
    public static final short BED_ENTITY_ID = 11;
    private ProtocolHandle<ClientboundPackets1_9_3, ServerboundPackets1_12> handle;

    @Override
    public void onEnable() {
        Protocol1_12To1_11_1 protocol = ViaAlfajor.getProtocol(Protocol1_12To1_11_1.class);
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
                                                    CompoundTag bedTag = randomColorTag(
                                                        x + (chunk.getX() << 4),
                                                        y + (i << 4),
                                                        z + (chunk.getZ() << 4));
                                                    chunk.getBlockEntities().add(bedTag);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        ))
                .injectClientbound(ClientboundPackets1_9_3.MULTI_BLOCK_CHANGE,
                    remapper -> {
                        remapper.map(Type.INT); // 0 - Chunk X
                        remapper.map(Type.INT); // 1 - Chunk Z
                        remapper.map(Type.BLOCK_CHANGE_RECORD_ARRAY); // 2 - Records
                        remapper
                            .handler(wrapper -> {
                                int chunkX = wrapper.get(Type.INT, 0);
                                int chunkZ = wrapper.get(Type.INT, 1);
                                BlockChangeRecord[] records = wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0);
                                for (BlockChangeRecord record : records) {
                                    int id = record.getBlockId();
                                    if (isBed(id)) {
                                        byte sectionX = record.getSectionX();
                                        byte sectionZ = record.getSectionZ();
                                        Position pos = new Position(
                                            sectionX + (chunkX << 4),
                                            record.getY(),
                                            sectionZ + (chunkZ << 4)
                                        );
                                        scheduleBedColorChange(wrapper.user(), pos);
                                    }
                                }
                            });
                    })
                // Not a necessary packet interception but it's funny to change their color with clicks ;)
                // also it produces a double packet call
                .injectClientbound(ClientboundPackets1_9_3.BLOCK_CHANGE,
                    remapper -> {
                        remapper.map(Type.POSITION);
                        remapper.map(Type.VAR_INT);
                        remapper.handler(wrapper -> {
                                int id = wrapper.get(Type.VAR_INT, 0);
                                if (isBed(id)) {
                                    scheduleBedColorChange(wrapper.user(),
                                        wrapper.get(Type.POSITION, 0));
                                }
                            }
                        );
                    });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isBed(int id) {
        return (id >= 416 && id <= 419)  // Bed foot
            || (id >= 424 && id <= 431); // Bed head
    }

    private CompoundTag randomColorTag(Position pos) {
        return randomColorTag(pos.x(), pos.y(), pos.z());
    }

    // Set bed with a random color
    // THIS IS A DEMO. DON'T USE IT IN PRODUCTION.
    private CompoundTag randomColorTag(int x, int y, int z) {
        CompoundTag tag = new CompoundTag();
        int randomColor = new Random().nextInt(15);
        tag.put("color", new IntTag(randomColor));
        tag.put("x", new IntTag(x));
        tag.put("y", new IntTag(y));
        tag.put("z", new IntTag(z));
        tag.put("id", new StringTag("minecraft:bed"));
        return tag;
    }

    private void scheduleBedColorChange(UserConnection connection,
                                        Position pos) throws Exception {
        PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA,
            null,
            connection);
        wrapper.write(Type.POSITION, pos);
        wrapper.write(Type.UNSIGNED_BYTE, BED_ENTITY_ID);
        wrapper.write(Type.NBT, randomColorTag(pos));
        wrapper.scheduleSend(Protocol1_12To1_11_1.class, false);
    }

    @Override
    public void onDisable() {
        handle = null;
    }
}
