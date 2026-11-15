package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import java.util.function.Consumer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler", remap=false)
public abstract class SchedulerMixin {
    @Shadow @Final public ServerLevel world;
    @Inject(method="scheduleChunkLoad(IIZLnet/minecraft/world/level/chunk/status/ChunkStatus;ZLca/spottedleaf/concurrentutil/executor/standard/PrioritisedExecutor$Priority;Ljava/util/function/Consumer;)V", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$denyNewAsync(int x, int z, boolean generate, ChunkStatus status, boolean ticket, PrioritisedExecutor.Priority priority, Consumer<ChunkAccess> complete, CallbackInfo ci) {
        if (status != ChunkStatus.EMPTY && TerrainLock.denied(world, x, z)) {
            if (complete != null) complete.accept(null);
            ci.cancel();
        }
    }
    @Inject(method="scheduleChunkLoad(IILnet/minecraft/world/level/chunk/status/ChunkStatus;ZLca/spottedleaf/concurrentutil/executor/standard/PrioritisedExecutor$Priority;Ljava/util/function/Consumer;)V", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$denyNewLoad(int x, int z, ChunkStatus status, boolean ticket, PrioritisedExecutor.Priority priority, Consumer<ChunkAccess> complete, CallbackInfo ci) {
        if (status != ChunkStatus.EMPTY && TerrainLock.denied(world, x, z)) {
            if (complete != null) complete.accept(null);
            ci.cancel();
        }
    }
    @Inject(method="scheduleTickingState", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$denyNewTick(int x, int z, FullChunkStatus status, boolean ticket, PrioritisedExecutor.Priority priority, Consumer<LevelChunk> complete, CallbackInfo ci) {
        if (TerrainLock.denied(world, x, z)) {
            if (complete != null) complete.accept(null);
            ci.cancel();
        }
    }
}
