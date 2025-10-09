package dev.khloeleclair.create.additionallogistics.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.khloeleclair.create.additionallogistics.common.blockentities.FlexibleShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

public class FlexibleShaftBlockEntityRenderer extends KineticBlockEntityRenderer<FlexibleShaftBlockEntity> {

    public FlexibleShaftBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected void renderOpenings(FlexibleShaftBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        for(Direction dir : Iterate.directions) {
            float modifier = be.getRotationSpeedModifier(dir);
            if (modifier == 0f)
                continue;

            SuperByteBuffer opening = CachedBuffers.partialFacing(CALPartialModels.SHAFT_OPENING, be.getBlockState(), dir.getOpposite());
            opening.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected void renderSafe(FlexibleShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            renderOpenings(be, ms, buffer, light);
            return;
        }

        //final var state = be.getBlockState();
        final var pos = be.getBlockPos();
        final float time = AnimationTickHolder.getRenderTime(be.getLevel());
        final float baseAngle = (time * be.getSpeed() * 3f / 10) % 360;

        for(Direction dir : Iterate.directions) {
            float modifier = be.getRotationSpeedModifier(dir);
            if (modifier == 0f)
                continue;

            final var axis = dir.getAxis();
            float offset = getRotationOffsetForPosition(be, pos, axis);
            float angle = baseAngle * modifier;

            angle += offset;
            angle = angle / 180 * (float) Math.PI;

            SuperByteBuffer opening = CachedBuffers.partialFacing(CALPartialModels.SHAFT_OPENING, be.getBlockState(), dir.getOpposite());
            opening.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), dir);
            kineticRotationTransform(shaft, be, axis, angle, light);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
