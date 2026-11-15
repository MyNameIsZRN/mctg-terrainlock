package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader$PlayerChunkLoaderData", remap=false)
public abstract class PlayerLoaderMixin {
    @Shadow @Final private ServerLevel world;
    @Inject(method="canPlayerGenerateChunks", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$noGeneration(CallbackInfoReturnable<Boolean> cir) {
        if (TerrainLock.locked(world)) cir.setReturnValue(false);
    }
}
