package net.futureclient.proxymod.mixin.mixins;

import io.netty.buffer.Unpooled;
import net.futureclient.proxymod.ProxyMod;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.login.server.SPacketEncryptionRequest;
import net.minecraft.network.play.client.CPacketCustomPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.crypto.SecretKey;

/**
 * @author Babbaj
 */
@Mixin(NetHandlerLoginClient.class)
public class NetHandlerLoginClientPatch {

    @Shadow private NetworkManager networkManager;

    // nigger api
    // TODO: do this in asmlib
    @Inject(method = "handleEncryptionRequest",
            at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/util/CryptManager;createNewSharedKey()Ljavax/crypto/SecretKey;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void handleEncryptionRequest(SPacketEncryptionRequest packetIn, CallbackInfo cb, SecretKey secretKey)
    {
        if (ProxyMod.enabled) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
            buffer.writeBytes(secretKey.getEncoded());
            CPacketCustomPayload toProxy = new CPacketCustomPayload("SM_PROXY|SharedKey", buffer);

            networkManager.sendPacket(toProxy);
        }
    }

}
