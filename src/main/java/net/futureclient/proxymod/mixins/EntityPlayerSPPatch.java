package net.futureclient.proxymod.mixins;

import net.futureclient.proxymod.ProtocolHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(EntityPlayerSP.class)
public class EntityPlayerSPPatch {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("/proxy connect\\s+(.+)");

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void sendChatMessage(String message, CallbackInfo cb) {
        Matcher matcher;
        if ((matcher = COMMAND_PATTERN.matcher(message)).matches()) { //TODO: make work with uppercase?
            final String ip = matcher.group(1).trim();
            ProtocolHandler.onProxyJoinRequest(ip);
        }
    }
}
