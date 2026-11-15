package cn.mctogether.terrainlock.mixin;

import cn.mctogether.terrainlock.TerrainLock;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.RelativeMovement;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ServerGamePacketListenerImpl.class,remap=false)
public abstract class MovementMixin {
    @Shadow public ServerPlayer player;
    @Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
    @Unique private long mctg$lastWarning;
    @Unique private void mctg$warn() {
        long now=System.currentTimeMillis();
        if (now-mctg$lastWarning>3000) {
            player.sendSystemMessage(Component.literal("§e主世界已关闭新地形生成，请在已生成区域内活动。"));
            mctg$lastWarning=now;
        }
    }
    @Inject(method="handleMovePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true,require=1)
    private void mctg$guardPlayer(ServerboundMovePlayerPacket packet,CallbackInfo ci) {
        if (awaitingPositionFromClient!=null || !Double.isFinite(packet.getY(player.getY()))) return;
        if (TerrainLock.deniedMove(player.serverLevel(),player.getX(),player.getZ(),packet.getX(player.getX()),packet.getZ(player.getZ()))) {
            player.connection.internalTeleport(player.getX(),player.getY(),player.getZ(),player.getYRot(),player.getXRot(),Set.of());
            mctg$warn(); ci.cancel();
        }
    }
    @Inject(method="handleMoveVehicle",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true,require=1)
    private void mctg$guardVehicle(ServerboundMoveVehiclePacket packet,CallbackInfo ci) {
        if (!Double.isFinite(packet.getY())) return;
        var vehicle=player.getRootVehicle();
        if (vehicle!=player && vehicle.getControllingPassenger()==player
            && TerrainLock.deniedMove(player.serverLevel(),vehicle.getX(),vehicle.getZ(),packet.getX(),packet.getZ())) {
            player.connection.send(new ClientboundMoveVehiclePacket(vehicle));
            mctg$warn(); ci.cancel();
        }
    }
    @Inject(method="internalTeleport",at=@At("HEAD"),cancellable=true,require=1)
    private void mctg$guardTeleport(double x,double y,double z,float yaw,float pitch,Set<RelativeMovement> relative,CallbackInfo ci) {
        // This core passes absolute server coordinates; relative flags affect only the outgoing packet.
        if (TerrainLock.deniedPosition(player.serverLevel(),x,z)) { mctg$warn(); ci.cancel(); }
    }
}
