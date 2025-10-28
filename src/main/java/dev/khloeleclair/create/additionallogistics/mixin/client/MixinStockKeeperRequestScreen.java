package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesHistoryData;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(StockKeeperRequestScreen.class)
public abstract class MixinStockKeeperRequestScreen extends AbstractSimiContainerScreen<StockKeeperRequestMenu> {

    @Shadow
    @Nullable
    private List<List<ClipboardEntry>> clipboardItem;

    public MixinStockKeeperRequestScreen(StockKeeperRequestMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Inject(
            method = "<init>*",
            at = @At("RETURN")
    )
    private void onConstructed(CallbackInfo ci) {
        var stack = menu.player.getMainHandItem();
        if (clipboardItem == null && stack.is(CALItems.SALES_LEDGER.get())) {
            var history = SalesHistoryData.get(stack);
            if (history != null)
                clipboardItem = history.toPurchaseClipboardEntries();
        }
    }

}
