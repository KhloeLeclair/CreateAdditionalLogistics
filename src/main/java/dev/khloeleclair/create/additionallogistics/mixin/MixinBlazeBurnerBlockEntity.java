package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlazeBurnerBlockEntity.class)
public class MixinBlazeBurnerBlockEntity {

    @Inject(
            method = "getStockTicker",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private static void onGetStockTicker(LevelAccessor level, BlockPos pos, CallbackInfoReturnable<StockTickerBlockEntity> ci) {
        if (ci.getReturnValue() == null) {
            if (level instanceof Level l && !l.isLoaded(pos))
                return;

            for(Direction dir : Iterate.horizontalDirections) {
                var dpos = pos.relative(dir);
                BlockState state = level.getBlockState(dpos);
                if (state.is(CALBlocks.CASH_REGISTER.get()) && level.getBlockEntity(dpos) instanceof StockTickerBlockEntity stbe) {
                    ci.setReturnValue(stbe);
                    return;
                }
            }
        }
    }

}
