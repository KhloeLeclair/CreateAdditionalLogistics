package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.khloeleclair.create.additionallogistics.client.registries.CALGuiTextures;
import dev.khloeleclair.create.additionallogistics.common.CALLang;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.IPromiseLimit;
import dev.khloeleclair.create.additionallogistics.common.network.CustomPackets;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FactoryPanelScreen.class)
public abstract class MixinFactoryPanelScreen extends AbstractSimiScreen {

    @Shadow
    private boolean restocker;

    @Shadow
    private FactoryPanelBehaviour behaviour;

    @Shadow
    @Nullable
    private BigItemStack outputConfig;

    @Nullable
    private ScrollInput promiseLimit;

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void CAL$onInit(CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit ipl) || !Config.Common.enablePromiseLimits.get())
            return;

        int x = guiLeft;
        int y = guiTop;

        promiseLimit = new ScrollInput(x + 68, y + windowHeight + 1, 56, 16)
                .withRange(-1, restocker ? (64 * 100 * 20) : 1000);

        if (restocker)
            promiseLimit = promiseLimit.withShiftStep(behaviour.getFilter().getMaxStackSize());
        else
            promiseLimit = promiseLimit.withShiftStep(10);

        promiseLimit.setState(ipl.getPromiseLimit());
        updatePromiseLimitLabel();

        addRenderableWidget(promiseLimit);
    }

    private void updatePromiseLimitLabel() {
        if (promiseLimit == null)
            return;

        String key = "gauge.promise_limit";
        if (promiseLimit.getState() == -1)
            key = key + ".none";

        promiseLimit.titled(CALLang.translate(key).component());
    }

    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void CAL$onTick(CallbackInfo ci) {
        updatePromiseLimitLabel();
    }

    @Inject(
            method = "renderWindow",
            at = @At("RETURN")
    )
    private void CAL$onRenderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit) || promiseLimit == null)
            return;

        // BG
        graphics.blit(CALGuiTextures.PROMISE_LIMIT_BG.getLocation(), promiseLimit.getX() - 8, promiseLimit.getY() - 4, 0, 0, 72, 28, 128, 32);

        // Label
        int limit = promiseLimit.getState();
        if (limit >= 0 && ! restocker && outputConfig != null)
            limit *= outputConfig.count;

        graphics.drawString(font, CreateLang.text(limit == -1 ? " ---" : " " + limit).component(), promiseLimit.getX() + 3, promiseLimit.getY() + 4, 0xffeeeeee, true);
    }


    @Inject(
            method = "sendIt",
            at = @At("RETURN")
    )
    private void CAL$onSendIt(CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit) || promiseLimit == null)
            return;

        int limit = promiseLimit.getState();
        new CustomPackets.UpdateGaugePromiseLimit(behaviour.getPanelPosition(), limit).send();
    }


}
