package net.futureclient.proxymod.mixins;

import io.netty.channel.ChannelHandlerContext;
import net.futureclient.proxymod.ProtocolHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class NetworkManagerPatch {

    @Inject(method = "channelRead0", at = @At(value = "INVOKE", target = "net/minecraft/network/Packet.processPacket(Lnet/minecraft/network/INetHandler;)V"))
    private void channelRead0$processPacket(ChannelHandlerContext p_channelRead0_1_, Packet<?> packetIn, CallbackInfo cb) {
        ProtocolHandler.onPacketReceived(packetIn);
    }
}
