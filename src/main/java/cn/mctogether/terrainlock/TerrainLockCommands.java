package cn.mctogether.terrainlock;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TerrainLockCommands {
    private TerrainLockCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(literal("terrainlock").requires(s -> s.hasPermission(2))
            .executes(c -> status(c.getSource()))
            .then(literal("status").executes(c -> status(c.getSource())))
            .then(literal("audit").executes(c -> { say(c.getSource(),"TerrainLock injected-hooks="+TerrainLockAudit.verify()+"/13"); return 1; }))
            .then(literal("test")
                .then(argument("oldX",integer()).then(argument("oldZ",integer())
                    .then(argument("newX",integer()).then(argument("newZ",integer())
                        .executes(c -> test(c.getSource(),getInteger(c,"oldX"),getInteger(c,"oldZ"),getInteger(c,"newX"),getInteger(c,"newZ")))))))));
    }
    private static void say(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message),false);
    }
    private static int status(CommandSourceStack source) {
        say(source,"TerrainLock version=" + TerrainLock.VERSION + " mode=single-mod blocked=" + TerrainLock.BLOCKED.get()
            + " unexpectedNewChunks=" + TerrainLock.UNEXPECTED_NEW_CHUNKS.get());
        for (ServerLevel world : source.getServer().getAllLevels()) {
            say(source,"dimension=" + world.dimension().location() + " locked=" + TerrainLock.locked(world));
        }
        return 1;
    }
    private static void check(CommandSourceStack source, String label, boolean passed) {
        say(source,"TEST " + label + "=" + passed);
        if (!passed) throw new IllegalStateException(label);
    }
    private static int test(CommandSourceStack source, int x, int z, int nx, int nz) {
        ServerLevel world = source.getServer().overworld();
        if (!TerrainLock.existing(world,x,z) || world.getChunkSource().getChunkAtIfLoadedImmediately(x,z) != null || TerrainLock.existing(world,nx,nz)) {
            source.sendFailure(Component.literal("Need an existing UNLOADED chunk and an absent chunk"));
            return 0;
        }
        try {
            var scheduler = ((ChunkSystemServerLevel)world).moonrise$getChunkTaskScheduler();
            var priority = PrioritisedExecutor.Priority.NORMAL;
            CompletableFuture<ChunkAccess> oldLoad = new CompletableFuture<>();
            scheduler.scheduleChunkLoad(x,z,false,ChunkStatus.FULL,true,priority,oldLoad::complete);
            oldLoad.orTimeout(15,TimeUnit.SECONDS).whenComplete((chunk,error) -> world.getServer().execute(() -> {
                try {
                    check(source,"existing-unloaded-chunk-load",error == null && chunk instanceof LevelChunk);
                    check(source,"new-sync-readonly-empty",world.getChunkSource().getChunk(nx,nz,ChunkStatus.FULL,true) instanceof EmptyLevelChunk);
                    check(source,"new-sync-still-unloaded",world.getChunkSource().getChunkAtIfLoadedImmediately(nx,nz) == null);
                    check(source,"new-plugin-load-denied",!world.getWorld().loadChunk(nx,nz,true));
                    var pos = new ChunkPos(nx,nz);
                    boolean added = scheduler.chunkHolderManager.addTicketAtLevel(TicketType.PLUGIN,pos,33,Unit.INSTANCE);
                    if (added) scheduler.chunkHolderManager.removeTicketAtLevel(TicketType.PLUGIN,pos,33,Unit.INSTANCE);
                    check(source,"direct-generation-ticket-denied",!added);
                    check(source,"movement-to-new-terrain-denied",TerrainLock.deniedMove(world,x*16.0,z*16.0,nx*16.0,nz*16.0));
                    check(source,"teleport-to-new-terrain-denied",TerrainLock.deniedPosition(world,nx*16.0,nz*16.0));
                    check(source,"movement-to-existing-terrain-allowed",!TerrainLock.deniedMove(world,0,0,x*16.0,z*16.0));
                    for (ServerLevel other : source.getServer().getAllLevels()) {
                        if (!other.dimension().equals(world.dimension())) check(source,"other-dimension-unlocked-"+other.dimension().location(),!TerrainLock.locked(other));
                    }
                    CompletableFuture<ChunkAccess> absent = new CompletableFuture<>();
                    scheduler.scheduleChunkLoad(nx,nz,true,ChunkStatus.FULL,true,priority,absent::complete);
                    absent.orTimeout(15,TimeUnit.SECONDS).whenComplete((fresh,failure) -> world.getServer().execute(() -> {
                        try { check(source,"new-async-generate-true-denied",failure == null && fresh == null); }
                        catch (Exception e) { source.sendFailure(Component.literal("TEST FAILED: "+e)); }
                    }));
                } catch (Exception e) { source.sendFailure(Component.literal("TEST FAILED: "+e)); }
            }));
            return 1;
        } catch (Exception e) { source.sendFailure(Component.literal("TEST FAILED: "+e)); return 0; }
    }
}
