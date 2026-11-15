package cn.mctogether.terrainlock.mixin;
import cn.mctogether.terrainlock.TerrainLock;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Entity.class,remap=false)
public abstract class EntityTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFLorg/bukkit/event/player/PlayerTeleportEvent$TeleportCause;)Z",at=@At("HEAD"),cancellable=true,require=1)
    private void mctg$guardEntity(ServerLevel world,double x,double y,double z,Set<RelativeMovement> relative,float yaw,float pitch,TeleportCause cause,CallbackInfoReturnable<Boolean> cir) {
        if (TerrainLock.deniedPosition(world,x,z)) cir.setReturnValue(false);
    }
}
