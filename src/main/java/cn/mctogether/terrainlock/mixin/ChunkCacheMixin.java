package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import net.minecraft.server.level.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ServerChunkCache.class, remap=false)
public abstract class ChunkCacheMixin {
    @Shadow @Final public ServerLevel level;
    @Inject(method="syncLoad", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$noSynchronousGeneration(int x, int z, ChunkStatus status, CallbackInfoReturnable<ChunkAccess> cir) {
        if (status != ChunkStatus.EMPTY && TerrainLock.denied(level, x, z)) {
            // Vanilla's read-only empty chunk is never added to a holder, ticketed, or saved.
            // Returning it preserves synchronous callers' non-null contract without generating void chunks on disk.
            cir.setReturnValue(new EmptyLevelChunk(level, new ChunkPos(x, z), level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS)));
        }
    }
}
