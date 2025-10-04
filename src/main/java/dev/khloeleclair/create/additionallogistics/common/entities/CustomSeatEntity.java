package dev.khloeleclair.create.additionallogistics.common.entities;

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
