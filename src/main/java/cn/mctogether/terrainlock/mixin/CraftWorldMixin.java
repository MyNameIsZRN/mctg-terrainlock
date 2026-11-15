package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="org.bukkit.craftbukkit.CraftWorld", remap=false)
public abstract class CraftWorldMixin {
    @Shadow @Final private ServerLevel world;
    @Inject(method="loadChunk(IIZ)Z", at=@At("HEAD"), cancellable=true, require=1)
    private void mctg$denyNewPluginLoad(int x, int z, boolean generate, CallbackInfoReturnable<Boolean> cir) {
        if (TerrainLock.denied(world,x,z)) cir.setReturnValue(false);
    }
}
