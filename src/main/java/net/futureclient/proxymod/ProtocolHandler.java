package net.futureclient.proxymod;

import com.mojang.authlib.exceptions.AuthenticationException;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.*;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ProtocolHandler {

    private static final Pattern CHANEL_PATTERN = Pattern.compile("PROXY\\|(?<name>.+)");

    // Used to make sure we don't handle (((unauthorized))) auth requests.
    // A bad proxy can mess this up but we're guaranteed to be safe from (((them))).
    // 2 lazy to add events to make sure this is correct
    public static ProtocolState protocolState = ProtocolState.IDLE;

    // unused
    //public static boolean isConnectedToProxy = false;


    // client wants to tell the proxy to join a server
    public static void onProxyJoinRequest(String ip) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeString(ip);
        CPacketCustomPayload packet = new CPacketCustomPayload("PROXY|JoinRequest", buffer);
        sendPacket(packet);
        protocolState = ProtocolState.AWAITING_AUTH;
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
                                final Matcher matcher = CHANEL_PATTERN.matcher(payload.getChannelName());
                                if (!matcher.matches()) throw new IllegalStateException("Failed to find match for \"" + payload.getChannelName() + "\"");
                                final String channelName = matcher.group("name");
                                return ProxyProtocol.getResponseHandler(channelName);
                            });
                    if (!optionalFunc.isPresent()) {
                        //throw new IllegalStateException("Unknown proxy protocol: " + payloadPacket.getChannelName());
                        System.err.println("Unknown proxy protocol: " + payloadPacket.getChannelName());
                    }
                    optionalFunc.get().accept(payloadPacket.getBufferData());
                });
    }


    private static void onAuthRequest(PacketBuffer payload) {
        final PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        final CPacketCustomPayload packet = new CPacketCustomPayload("PROXY|ConfirmAuth", buffer);

        if (protocolState != ProtocolState.AWAITING_AUTH) {
            // don't auth but tell them we failed to be polite :^)
            buffer.writeBoolean(false);
            sendPacket(packet);
            return;
        }

        // hash is computed with the target server's public key and the secret key shared between the client and the proxy
        final String hash = payload.readString(32767);
        if (authenticate(hash)) {
            // success
            buffer.writeBoolean(true);
        } else {
            // fail
            buffer.writeBoolean(false);
        }

        sendPacket(packet);
        protocolState = ProtocolState.AWAITING_CONNECTION;
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

    // sent if joining a server failed for whatever reason
    private static void onProxyFail(PacketBuffer buffer) {
        // just let the server tell us through chat
        //String reason = buffer.readString(32767);
        protocolState = ProtocolState.IDLE;
    }

    private static void sendPacket(Packet<?> packetOut) {
        FMLClientHandler.instance().getClientToServerNetworkManager().sendPacket(packetOut);
    }

    private enum ProxyProtocol {
        AUTH_REQUEST("AuthRequest", ProtocolHandler::onAuthRequest),
        CONFIRM_JOIN("ConfirmJoin", buffer -> protocolState = ProtocolState.CONNECTED),

        FAIL("Fail", ProtocolHandler::onProxyFail);

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
