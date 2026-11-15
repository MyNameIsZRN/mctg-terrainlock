package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import java.util.Set;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=ServerPlayer.class,remap=false)
public abstract class PlayerTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"),cancellable=true,require=1)
    private void mctg$guardCrossWorld(ServerLevel world,double x,double y,double z,Set<RelativeMovement> relative,float yaw,float pitch,CallbackInfoReturnable<Boolean> cir) {
        if (TerrainLock.deniedPosition(world,x,z)) cir.setReturnValue(false);
    }
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"),cancellable=true,require=1)
    private void mctg$guardCrossWorldVoid(ServerLevel world,double x,double y,double z,float yaw,float pitch,CallbackInfo ci) {
        if (TerrainLock.deniedPosition(world,x,z)) ci.cancel();
    }
}
