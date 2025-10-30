package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlock;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TableClothBlockEntity.class)
public abstract class MixinTableClothBlockEntity extends SmartBlockEntity {

    @Shadow(remap = false)
    public AutoRequestData requestData;

    public MixinTableClothBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(
            method = "useShop",
            at = @At("RETURN"),
            remap = false
    )
    private void CAL$onUseShop(Player player, CallbackInfoReturnable<InteractionResult> ci) {

        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (! itemInHand.is(AllItems.SHOPPING_LIST.get()) || ci.getReturnValue() != InteractionResult.SUCCESS)
            return;

        BlockPos tickerPos = requestData.targetOffset.offset(worldPosition);
        if (level != null && level.getBlockEntity(tickerPos) instanceof CashRegisterBlockEntity) {
            CashRegisterBlock.setCashRegisterPos(itemInHand, tickerPos);
            player.setItemInHand(InteractionHand.MAIN_HAND, itemInHand);
        }

    }

}
