package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkHolderManager", remap=false)
public abstract class TicketMixin {
    @Shadow @Final private ServerLevel world;
    @Inject(method="addTicketAtLevel(Lnet/minecraft/server/level/TicketType;JILjava/lang/Object;Z)Z", at=@At("HEAD"), cancellable=true, require=1)
    private <T> void mctg$denyGenerationTicket(TicketType<T> type, long pos, int level, T id, boolean lock, CallbackInfoReturnable<Boolean> cir) {
        if (level < ChunkTaskScheduler.getTicketLevel(ChunkStatus.EMPTY) && TerrainLock.denied(world,ChunkPos.getX(pos),ChunkPos.getZ(pos))) cir.setReturnValue(false);
    }
}
