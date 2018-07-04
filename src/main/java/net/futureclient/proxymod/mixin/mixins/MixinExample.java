package net.futureclient.proxymod.mixin.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * @author 086
 */
@Mixin(Minecraft.class)
public class MixinExample {

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    public void displayGuiScreen(GuiScreen guiScreenIn, CallbackInfo info) {
        System.out.println("Mixin: Displaying screen: " + guiScreenIn);
    }

}
