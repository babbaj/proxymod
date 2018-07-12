package net.futureclient.proxymod;

import com.mojang.authlib.exceptions.AuthenticationException;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.CryptManager;
import net.minecraftforge.fml.client.FMLClientHandler;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ProtocolHandler {

    private static final Pattern CHANEL_PATTERN = Pattern.compile("PROXY\\|(?<name>.+)");

    private static boolean awaitingProxyJoin = false;
    public static boolean isConnectedToProxy = false;

    // client wants to tell the proxy to join a server
    public static void onProxyConnect(String ip) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeString(ip);
        CPacketCustomPayload packet = new CPacketCustomPayload("PROXY|JoinRequest", buffer);
        sendPacket(packet);
    }

    public static void onPacketReceived(Packet<?> packet) {
        Optional.of(packet)
                .filter(SPacketCustomPayload.class::isInstance)
                .map(SPacketCustomPayload.class::cast)
                .filter(payload -> payload.getChannelName().startsWith("PROXY|"))
                .ifPresent(payloadPacket -> {
                    Optional<Consumer<PacketBuffer>> optionalFunc =
                            Optional.of(payloadPacket)
                            .flatMap(payload -> {
                                final String channelName = CHANEL_PATTERN.matcher(payload.getChannelName()).group("name");
                                return ProxyProtocol.getResponseHandler(channelName);
                            });
                    if (!optionalFunc.isPresent()) {
                        // TODO: handle this better
                        throw new IllegalStateException("Unknown proxy protocol: " + payloadPacket.getChannelName());
                    }
                    optionalFunc.get().accept(payloadPacket.getBufferData());
                });
    }


    private static void onAuthRequest(PacketBuffer payload) {
        final PublicKey publicKey = CryptManager.decodePublicKey(payload.readByteArray());
        final SecretKey secretKey = EncyptionUtil.decodeSecretKey(payload.readByteArray());

        final String hash = (new BigInteger(CryptManager.getServerIdHash("", publicKey, secretKey))).toString(16);

        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        if (authenticate(hash)) {
            // success
            buffer.writeBoolean(true);
        } else {
            // fail
            buffer.writeBoolean(false);
        }
        CPacketCustomPayload packet = new CPacketCustomPayload("PROXY|ConfirmAuth", buffer);
        sendPacket(packet);
        // TODO: enable encryption with secretKey

    }

    private static boolean authenticate(String hash) {
        final Minecraft mc = getMinecraft();
        try {
            mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), hash);
            return true;
        } catch (AuthenticationException ex) {
            System.err.println("Failed to autheticate! " + ex.getMessage());
            return false;
        }
    }

    private static void sendPacket(Packet<?> packetOut) {
        FMLClientHandler.instance().getClientToServerNetworkManager().sendPacket(packetOut);
    }

    private enum ProxyProtocol {
        AUTH_REQUEST("AuthRequest", ProtocolHandler::onAuthRequest);

        private final String channel;
        private final Consumer<PacketBuffer> handler;

        ProxyProtocol(String channel, Consumer<PacketBuffer> handler) {
            this.channel = channel;
            this.handler = handler;
        }


        public static Optional<Consumer<PacketBuffer>> getResponseHandler(String channelIn) {
            return Stream.of(values())
                    .filter(handler -> handler.channel.equals(channelIn))
                    .findFirst()
                    .map(protocol -> protocol.handler);
        }
    }

}
