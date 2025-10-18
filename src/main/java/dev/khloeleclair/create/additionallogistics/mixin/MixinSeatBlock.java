package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import dev.khloeleclair.create.additionallogistics.common.content.contraptions.actors.seats.AbstractSeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SeatBlock.class)
public class MixinSeatBlock {

    @Inject(
            method = "sitDown",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void CAL$onSitDown(Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.getBlockState(pos).getBlock() instanceof AbstractSeatBlock seat) {
            ci.cancel();
            seat.handleSitDown(world, pos, entity);
        }
    }

}
