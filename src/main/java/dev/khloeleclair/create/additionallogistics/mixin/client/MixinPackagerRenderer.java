package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackagerRenderer.class)
public class MixinPackagerRenderer {

    @Inject(
            method = "getTrayModel",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void CAL$getTrayModel(BlockState blockState, CallbackInfoReturnable<PartialModel> ci) {
        if (blockState.is(CALBlocks.PACKAGE_EDITOR.get()))
            ci.setReturnValue(AllPartialModels.PACKAGER_TRAY_REGULAR);
    }

}
