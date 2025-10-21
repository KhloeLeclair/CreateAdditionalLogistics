package dev.khloeleclair.create.additionallogistics.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import dev.khloeleclair.create.additionallogistics.api.currency.ICurrency;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CurrencyUtilities;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataComponents;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ShoppingListItem.class)
public class MixinShoppingListItem {

    @Inject(
            method = "appendHoverText",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lcom/simibubi/create/content/logistics/tableCloth/ShoppingListItem$ShoppingList;bakeEntries(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/createmod/catnip/data/Couple;",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void CAL$appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag, CallbackInfo ci, @Nullable @Local Couple<InventorySummary> lists) {
        if (lists == null || ! CurrencyUtilities.isConversionEnabled(stack.get(CALDataComponents.CASH_REGISTER_POS) != null))
            return;

        var items = lists.getSecond();
        // Check for currency items to handle.
        for(var item : items.getStacks()) {
            if (ICurrency.getForItem(item.stack.getItem()) != null) {
                ci.cancel();
                CurrencyUtilities.createShoppingListTooltip(stack, tooltipComponents, tooltipFlag, lists);
                return;
            }
        }
    }

}
