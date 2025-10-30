package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity {

    // I have no clue why this isn't working. At least it's of minor importance.

    /*@Redirect(
            method = "positionRider",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/actors/seat/SeatEntity;getCustomEntitySeatOffset(Lnet/minecraft/world/entity/Entity;)D"
            )
    )
    private static double CAL$getCustomEntitySeatOffset(@Nullable Entity entity) {
        return CustomSeatEntity.getCustomEntitySeatOffset(entity);
    }*/

}
