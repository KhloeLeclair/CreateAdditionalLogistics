package dev.khloeleclair.create.additionallogistics.common.content.contraptions.actors.seats;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALEntityTypes;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class CustomSeatEntity extends SeatEntity {

    public CustomSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public CustomSeatEntity(Level world, BlockPos pos) {
        super(CALEntityTypes.CUSTOM_SEAT.get(), world);
        noPhysics = true;
    }

    @Override
    public void setPos(double x, double y, double z) {
        this.setPosRaw(x, y, z);
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected void positionRider(Entity pEntity, MoveFunction pCallback) {
        double heightOffset = (getY() - pEntity.getVehicleAttachmentPoint(this).y);

        pCallback.accept(pEntity, getX(), heightOffset + getCustomEntitySeatOffset(pEntity), getZ());
    }

    public static double getCustomEntitySeatOffset(Entity passenger) {
        double result = SeatEntity.getCustomEntitySeatOffset(passenger);

        if (!(passenger.getVehicle() instanceof AbstractContraptionEntity contraptionEntity))
            return result;

        var contraption = contraptionEntity.getContraption();
        if (!contraptionEntity.hasPassenger(passenger))
            return result;

        var seatPos = contraption.getSeatOf(passenger.getUUID());
        var state = contraption.getBlocks().get(seatPos).state();

        if (state.getBlock() instanceof AbstractSeatBlock seat) {
            var offset = seat.getSeatPosition(null, BlockPos.ZERO);
            result += offset.y - 0.5;
        }

        return result;
    }


    @OnlyIn(Dist.CLIENT)
    public static class Render extends EntityRenderer<CustomSeatEntity> {

        public Render(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(CustomSeatEntity p_225626_1_, Frustum p_225626_2_, double p_225626_3_, double p_225626_5_,
                                    double p_225626_7_) {
            return false;
        }

        @Override
        public ResourceLocation getTextureLocation(CustomSeatEntity p_110775_1_) {
            return null;
        }
    }
}
