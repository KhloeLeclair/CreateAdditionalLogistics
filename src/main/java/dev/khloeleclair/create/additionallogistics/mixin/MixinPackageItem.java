package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.logistics.box.PackageItem;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.SafeRegex;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackageItem.class)
public class MixinPackageItem extends Item {

    public MixinPackageItem(Properties properties) {
        super(properties);
    }

    @Inject(
            method = "matchAddress(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void CAL$onMatchAddress(String boxAddress, String address, CallbackInfoReturnable<Boolean> ci) {
        if (Config.Common.globOptimize.get())
            ci.setReturnValue(SafeRegex.matchAddress(boxAddress, address));
    }

}
