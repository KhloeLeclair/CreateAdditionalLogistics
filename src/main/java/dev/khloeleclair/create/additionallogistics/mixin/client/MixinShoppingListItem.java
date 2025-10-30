package dev.khloeleclair.create.additionallogistics.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlock;
import dev.khloeleclair.create.additionallogistics.common.utilities.CurrencyUtilities;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
                    shift = At.Shift.AFTER,
                    remap = false
            ),
            cancellable = true
    )
    private void CAL$appendHoverText(ItemStack pStack, Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced, CallbackInfo ci, @Nullable @Local Couple<InventorySummary> lists) {
        if (lists == null || ! CurrencyUtilities.isConversionEnabled(CashRegisterBlock.getCashRegisterPos(pStack) != null))
            return;

        var items = lists.getSecond();
        // Check for currency items to handle.
        for(var item : items.getStacks()) {
            if (CurrencyUtilities.getForItem(item.stack.getItem()) != null) {
                ci.cancel();
                CurrencyUtilities.createShoppingListTooltip(Minecraft.getInstance().player, pStack, pTooltipComponents, pIsAdvanced, lists);
                return;
            }
        }
    }

}
