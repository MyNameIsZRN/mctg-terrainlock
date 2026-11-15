package cn.mctogether.terrainlock;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Force-load only our eight server targets, including packet handlers before the first login. */
public final class TerrainLockAudit {
    private TerrainLockAudit() {}
    public static int verify() {
        Map<String,String[]> targets=new LinkedHashMap<>();
        targets.put("ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData",new String[]{"mctg$noGeneration"});
        targets.put("ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler",new String[]{"mctg$denyNewAsync","mctg$denyNewLoad","mctg$denyNewTick"});
        targets.put("net.minecraft.server.level.ServerChunkCache",new String[]{"mctg$noSynchronousGeneration"});
        targets.put("org.bukkit.craftbukkit.CraftWorld",new String[]{"mctg$denyNewPluginLoad"});
        targets.put("ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager",new String[]{"mctg$denyGenerationTicket"});
        targets.put("net.minecraft.server.network.ServerGamePacketListenerImpl",new String[]{"mctg$guardPlayer","mctg$guardVehicle","mctg$guardTeleport"});
        targets.put("net.minecraft.server.level.ServerPlayer",new String[]{"mctg$guardCrossWorld","mctg$guardCrossWorldVoid"});
        targets.put("net.minecraft.world.entity.Entity",new String[]{"mctg$guardEntity"});
        int count=0;
        try {
            for (var entry:targets.entrySet()) {
                Class<?> target=Class.forName(entry.getKey(),false,TerrainLock.class.getClassLoader());
                var methods=Arrays.stream(target.getDeclaredMethods()).map(m -> m.getName()).toList();
                for (String hook:entry.getValue()) {
                    if (methods.stream().noneMatch(name -> name.contains(hook))) throw new IllegalStateException("Missing terrain lock hook: "+entry.getKey()+"#"+hook);
                    count++;
                }
            }
            return count;
        } catch (ClassNotFoundException error) { throw new IllegalStateException("Incompatible server core",error); }
    }
}
