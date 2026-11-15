package cn.mctogether.terrainlock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.util.Mth;

@Mod("mctg_terrainlock")
public final class TerrainLock {
    private static final ConcurrentHashMap<Long, Boolean> EXISTING = new ConcurrentHashMap<>();
    public static final AtomicLong BLOCKED = new AtomicLong();
    public static final AtomicLong UNEXPECTED_NEW_CHUNKS = new AtomicLong();
    public static final String VERSION = "1.1.0";
    public TerrainLock() {
        System.setProperty("mctg.terrainlock.version", VERSION);
        NeoForge.EVENT_BUS.addListener(TerrainLockCommands::register);
        NeoForge.EVENT_BUS.addListener(TerrainLock::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(TerrainLock::onServerStarted);
        System.out.println("[MCTGTerrainLock] Overworld: existing FULL chunks load normally; new terrain generation disabled.");
    }
    private static void onServerStarted(ServerStartedEvent event) {
        int hooks=TerrainLockAudit.verify();
        System.out.println("[MCTGTerrainLock] ACTIVE " + VERSION + ": single server mod; audit=" + hooks + "/13 hooks; commands and movement/teleport guards ready.");
    }
    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.isNewChunk() && event.getLevel() instanceof ServerLevel world && locked(world)) {
            UNEXPECTED_NEW_CHUNKS.incrementAndGet();
            System.err.println("[MCTGTerrainLock] Unexpected new overworld chunk " + event.getChunk().getPos());
        }
    }
    public static boolean deniedPosition(ServerLevel world, double x, double z) {
        // Invalid packets remain the vanilla packet handler's responsibility.
        return Double.isFinite(x) && Double.isFinite(z) && Math.abs(x) <= 3.0E7 && Math.abs(z) <= 3.0E7
            && denied(world, Mth.floor(x) >> 4, Mth.floor(z) >> 4);
    }
    public static boolean deniedMove(ServerLevel world, double fromX, double fromZ, double toX, double toZ) {
        if ((Mth.floor(fromX) >> 4) == (Mth.floor(toX) >> 4) && (Mth.floor(fromZ) >> 4) == (Mth.floor(toZ) >> 4)) return false;
        return deniedPosition(world, toX, toZ);
    }
    public static boolean locked(ServerLevel world) {
        return world.dimension().equals(Level.OVERWORLD);
    }
    public static boolean existing(ServerLevel world, int x, int z) {
        if (!locked(world)) return true;
        LevelChunk loaded = world.getChunkSource().getChunkAtIfLoadedImmediately(x, z);
        if (loaded != null) return true;
        long key = ChunkPos.asLong(x, z);
        Boolean known = EXISTING.get(key);
        if (known != null) return known;
        var region = world.getWorld().getWorldFolder().toPath().resolve("region/r." + (x >> 5) + "." + (z >> 5) + ".mca");
        if (!java.nio.file.Files.isRegularFile(region)) {
            EXISTING.putIfAbsent(key, false);
            return false;
        }
        // Read disk NBT only. Bukkit's isChunkGenerated treats partial chunks as generated on this core.
        boolean full = world.getChunkSource().chunkMap.read(new ChunkPos(x, z)).join()
            .map(tag -> "minecraft:full".equals(tag.getString("Status")) || "full".equals(tag.getString("Status")))
            .orElse(false);
        EXISTING.putIfAbsent(key, full);
        return full;
    }
    public static boolean denied(ServerLevel world, int x, int z) {
        if (!locked(world) || existing(world, x, z)) return false;
        BLOCKED.incrementAndGet();
        return true;
    }
}
