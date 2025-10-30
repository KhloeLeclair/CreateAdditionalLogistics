package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.logistics.packagePort.PackagePortMenu;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackagePortScreen.class)
public abstract class MixinPackagePortScreen extends AbstractSimiContainerScreen<PackagePortMenu> {

    public MixinPackagePortScreen(PackagePortMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Shadow(remap = false)
    private EditBox addressBox;

    @Inject(
            method = "nameBoxX",
            at = @At("TAIL"),
            remap = false
    )
    private void CPE$onNameBoxX(String s, EditBox nameBox, CallbackInfoReturnable<Integer> ci) {
        // TODO: Adjust the position if the name gets too long.
    }

    @Inject(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;setMaxLength(I)V",
                    shift = At.Shift.AFTER
            )
    )
    private void CPE$onInitAfterSetNameboxLength(CallbackInfo ci) {
        addressBox.setMaxLength(100);
    }

}
