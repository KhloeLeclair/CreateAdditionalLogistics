package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.content.logistics.tableCloth.TableClothOverlayRenderer;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.utilities.CurrencyUtilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TableClothOverlayRenderer.class)
public class MixinTableClothOverlayRenderer {

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/equipment/blueprint/BlueprintOverlayRenderer;displayShoppingList(Lnet/createmod/catnip/data/Couple;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            remap = false
    )
    private static void CAL$onTick(CallbackInfo ci, @Local StockTickerBlockEntity tickerBE, @Local ShoppingListItem.ShoppingList list) {
        if (CurrencyUtilities.isConversionEnabled(tickerBE instanceof CashRegisterBlockEntity)) {
            ci.cancel();
            CurrencyUtilities.renderShoppingList(list);
        }
    }

}
