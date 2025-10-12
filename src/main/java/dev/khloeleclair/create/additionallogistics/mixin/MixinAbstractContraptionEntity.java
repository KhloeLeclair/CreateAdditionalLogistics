package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import dev.khloeleclair.create.additionallogistics.common.entities.CustomSeatEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContraptionEntity.class)
public class MixinAbstractContraptionEntity {

    @Redirect(
            method = "positionRider",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/contraptions/actors/seat/SeatEntity;getCustomEntitySeatOffset(Lnet/minecraft/world/entity/Entity;)D"
            )
    )
    private static double CAL$getCustomEntitySeatOffset(Entity entity) {
        return CustomSeatEntity.getCustomEntitySeatOffset(entity);
    }

}
