# Alfajor
A ViaVersion hack to intercept and override packet mappers of specific protocols.

## Example code
```java
public class DemoBukkitPlugin extends JavaPlugin {
    
    @Override
    public void onLoad() {
        // Shortcut of Via.getManager().getProtocolManager().getProtocol();
        Protocol1_9To1_8 proto = ViaAlfajor.getProtocol(Protocol1_9To1_8.class);
        // Class to start the dirty tricks
        ProtocolHandle<ClientboundPackets1_8, ServerboundPackets1_9> handle = ProtocolHandle.wrap(protocol);
        // Inject a PacketRemapper into spawn_mob remappers
        // It wouldn't override any registered remapper
        handle.injectClientbound(ClientboundPackets1_8.SPAWN_MOB,
            remapper -> {
                // Do whatever you want here
            });
        
        // Inject a remapper for serverbound packets
        handle.injectServerbound(ServerboundPackets1_9.VEHICLE_MOVE,
            remapper -> {
                //...
            }
        );
        
        // Inject a remapper overriding all the spawn_mob remappers.
        handle.injectOverrideClientbound(ClientboundPackets1_8.SPAWN_MOB,
            remapper -> {
                // ...
            });
    }
}
```
  
There is a [plugin example](https://github.com/OcZi/Alfajor/tree/master/plugin-example) of coloured beds for clients that support it with a 1.8.8 server base.
![](https://media.discordapp.net/attachments/516845390079983618/1031656619144130661/unknown.png?width=883&height=452)
## Build
The entire project is built with JDK 8 using ViaVersion 4.4.2 as the target.

The API is licensed under [The MIT License](LICENSE) and the plugin example is under [GPL v3 License](plugin-example/LICENSE).
