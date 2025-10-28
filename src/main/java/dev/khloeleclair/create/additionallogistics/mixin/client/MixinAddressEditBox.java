package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.logistics.AddressEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AddressEditBox.class)
public class MixinAddressEditBox extends EditBox {

    public MixinAddressEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Inject(
            method = "<init>*",
            at = @At("RETURN")
    )
    private void CAL$onInit(CallbackInfo ci) {
        setMaxLength(100);
    }

}
