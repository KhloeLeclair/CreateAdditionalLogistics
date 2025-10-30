package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.khloeleclair.create.additionallogistics.common.content.contraptions.actors.seats.CustomSeatEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity {

    // I have no clue why this isn't working. At least it's of minor importance.

    @Redirect(
            method = "positionRider",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/actors/seat/SeatEntity;getCustomEntitySeatOffset(Lnet/minecraft/world/entity/Entity;)D",
                    remap = false
            ),
            remap = false
    )
    private double CAL$getCustomEntitySeatOffset(@Nullable Entity entity) {
        return CustomSeatEntity.getCustomEntitySeatOffset(entity);
    }

}
