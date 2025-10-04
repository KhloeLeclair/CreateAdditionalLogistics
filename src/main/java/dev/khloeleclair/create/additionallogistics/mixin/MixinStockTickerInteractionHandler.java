package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import dev.khloeleclair.create.additionallogistics.common.blockentities.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.items.SalesLedgerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StockTickerInteractionHandler.class)
public abstract class MixinStockTickerInteractionHandler {

    @Inject(
            method = "interactWithShop",
            at = @At("RETURN")
    )
    private static void CPE$onPurchase(Player player, Level level, BlockPos targetPos, ItemStack mainHandItem, CallbackInfo ci) {
        // If the main hand id wasn't voided, no transaction happened.
        if (player.getMainHandItem().equals(mainHandItem))
            return;

        CashRegisterBlockEntity.recordSale(level, targetPos, player, mainHandItem);
    }

}
