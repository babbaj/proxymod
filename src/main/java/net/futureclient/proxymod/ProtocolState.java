package net.futureclient.proxymod;

public enum ProtocolState {
    IDLE, // Not doing anything. If we receive an AuthRequest it is not authorised and will be ignored
    AWAITING_AUTH, // Sent a JoinRequest packet and expecting an AuthRequest packet back
    AWAITING_CONNECTION, // Sent an AuthConfirm packet and waiting for the proxy to handle it
    CONNECTED // Connected to a server through the proxy
}
