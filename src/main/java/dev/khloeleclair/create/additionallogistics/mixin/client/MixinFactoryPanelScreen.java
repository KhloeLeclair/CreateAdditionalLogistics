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
import dev.khloeleclair.create.additionallogistics.common.registries.CALPackets;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    @Nullable
    private ScrollInput CAL$promiseLimit;

    @Unique
    @Nullable
    private ScrollInput CAL$requestAdditional;

    @Inject(
            method = "init",
            at = @At("RETURN")
    )
    private void CAL$onInit(CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit ipl))
            return;

        int x = guiLeft;
        int y = guiTop;

        if (Config.Common.enablePromiseLimits.get()) {
            CAL$promiseLimit = new ScrollInput(x + 68, y + windowHeight + 1, 56, 16)
                    .withRange(-1, restocker ? (64 * 100 * 20) : 1000);

            if (restocker)
                CAL$promiseLimit = CAL$promiseLimit.withShiftStep(behaviour.getFilter().getMaxStackSize());
            else
                CAL$promiseLimit = CAL$promiseLimit.withShiftStep(10);

            CAL$promiseLimit.setState(ipl.getCALPromiseLimit());
            CAL$updatePromiseLimitLabel();

            addRenderableWidget(CAL$promiseLimit);
        }

        if (restocker && Config.Common.enableAdditionalStock.get()) {
            int maxSize = behaviour.getFilter().getMaxStackSize();

            CAL$requestAdditional = new ScrollInput(x + 4, y + windowHeight - 24, 47, 16)
                    .withRange(0, 1 + maxSize * 100)
                    .withStepFunction(c -> {
                        if (!c.shift)
                            return 1;

                        int remaining = c.currentValue % maxSize;
                        if (remaining == 0)
                            return maxSize;

                        if (c.forward)
                            return maxSize - remaining;
                        return remaining;
                    })
                    .withShiftStep(maxSize == 1 ? 5 : maxSize);

            CAL$requestAdditional.setState(ipl.getCALAdditionalStock());
            CAL$updateRequestAdditionalLabel();

            addRenderableWidget(CAL$requestAdditional);
        }

    }

    @Unique
    private void CAL$updateRequestAdditionalLabel() {
        if (CAL$requestAdditional == null)
            return;

        String key = "gauge.request_additional";
        if (CAL$requestAdditional.getState() <= 0)
            key = key + ".none";

        CAL$requestAdditional.titled(CALLang.translate(key).component());
    }

    @Unique
    private void CAL$updatePromiseLimitLabel() {
        if (CAL$promiseLimit == null)
            return;

        String key = "gauge.promise_limit";
        if (CAL$promiseLimit.getState() == -1)
            key = key + ".none";

        CAL$promiseLimit.titled(CALLang.translate(key).component());
    }

    @Inject(
            method = "tick",
            at = @At("RETURN")
    )
    private void CAL$onTick(CallbackInfo ci) {
        CAL$updatePromiseLimitLabel();
        CAL$updateRequestAdditionalLabel();
    }

    @Inject(
            method = "renderWindow",
            at = @At("RETURN")
    )
    private void CAL$onRenderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit))
            return;

        if (CAL$promiseLimit != null) {
            // BG
            CALGuiTextures.PROMISE_LIMIT_BG.render(graphics, CAL$promiseLimit.getX() - 8, CAL$promiseLimit.getY() - 4);

            // Label
            int limit = CAL$promiseLimit.getState();
            if (limit >= 0 && !restocker && outputConfig != null)
                limit *= outputConfig.count;

            graphics.drawString(font, CreateLang.text(limit == -1 ? " ---" : " " + limit).component(), CAL$promiseLimit.getX() + 3, CAL$promiseLimit.getY() + 4, 0xffeeeeee, true);
        }

        if (CAL$requestAdditional == null)
            return;

        // BG
        CALGuiTextures.ADDITIONAL_STOCK_BG.render(graphics, CAL$requestAdditional.getX() + 2, CAL$requestAdditional.getY() - 1);

        // Label
        graphics.drawString(font, CreateLang.text(" " + formatAdditional()).component(), CAL$requestAdditional.getX() + 15, CAL$requestAdditional.getY() + 4, 0xffeeeeee, true);
    }

    @Unique
    private String formatAdditional() {
        int additional = CAL$requestAdditional == null ? 0 : CAL$requestAdditional.getState();
        if (additional <= 0)
            return "---";

        int stackSize = behaviour.getFilter().getMaxStackSize();
        int stacks = additional / stackSize;
        int items = additional % stackSize;

        if (stacks == 0)
            return String.valueOf(items);

        else if (items != 0)
            return String.valueOf(additional);

        return stacks + "\u25A4";
    }


    @Inject(
            method = "sendIt",
            at = @At("RETURN")
    )
    private void CAL$onSendIt(CallbackInfo ci) {
        if (!(behaviour instanceof IPromiseLimit) || CAL$promiseLimit == null)
            return;

        int limit = CAL$promiseLimit.getState();
        int additional = CAL$requestAdditional == null ? 0 : CAL$requestAdditional.getState();
        new CALPackets.UpdateGaugePromiseLimit(behaviour.getPanelPosition(), limit, additional).send();
    }


}
