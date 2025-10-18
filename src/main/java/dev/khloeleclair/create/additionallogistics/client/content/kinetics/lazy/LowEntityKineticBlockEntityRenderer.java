package dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.EncasedFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LowEntityKineticBlockEntityRenderer extends KineticBlockEntityRenderer<AbstractLowEntityKineticBlockEntity> {

    public static boolean overrideVisualization;

    public LowEntityKineticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected void renderOpenings(AbstractLowEntityKineticBlockEntity be, PoseStack ms, MultiBufferSource buffer, int light) {
        boolean encased = be.getBlockState().getBlock() instanceof EncasedFlexibleShaftBlock;
        final var level = be.getLevel();
        final var pos = be.getBlockPos();

        for(Direction dir : Iterate.directions) {
            float modifier = be.getRotationSpeedModifier(dir);
            if (modifier == 0f)
                continue;

            int l = encased
                    ? LevelRenderer.getLightColor(level, pos.relative(dir))
                    : light;

            SuperByteBuffer opening = CachedBuffers.partialFacing(modifier == 1f
                        ? (encased ? CALPartialModels.ENCASED_SHAFT_OPENING : CALPartialModels.SHAFT_OPENING)
                        : (encased ? CALPartialModels.ENCASED_SHAFT_OPENING_REVERSED : CALPartialModels.SHAFT_OPENING_REVERSED),
                    be.getBlockState(), dir.getOpposite());
            opening.light(l).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
        }
    }

    @Override
    protected void renderSafe(AbstractLowEntityKineticBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) {
            renderOpenings(be, ms, buffer, light);
            return;
        }

        final var level = be.getLevel();
        final var pos = be.getBlockPos();
        final float time = AnimationTickHolder.getRenderTime(be.getLevel());
        final float baseAngle = (time * be.getSpeed() * 3f / 10) % 360;

        final var state = be.getBlockState();
        final boolean encased = state.getBlock() instanceof EncasedFlexibleShaftBlock;

        if (state.getBlock() instanceof ICogWheel cog && state.hasProperty(BlockStateProperties.AXIS)) {
            final var axis = state.getValue(BlockStateProperties.AXIS);
            final boolean large = cog.isLargeCog();

            SuperByteBuffer cogmodel = CachedBuffers.partialFacingVertical(large
                    ? AllPartialModels.SHAFTLESS_LARGE_COGWHEEL
                    : AllPartialModels.SHAFTLESS_COGWHEEL, state, Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));

            renderRotatingBuffer(be, cogmodel, ms, buffer.getBuffer(RenderType.solid()), light);

            /*float angle = large ? 0 //BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft(be, axis)
                    : getAngleForBe(be, pos, axis);

            kineticRotationTransform(cogmodel, be, cog.getRotationAxis(state), angle, light);
            cogmodel.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));*/
        }

        for(Direction dir : Iterate.directions) {
            float modifier = be.getRotationSpeedModifier(dir);
            if (modifier == 0f)
                continue;

            final var axis = dir.getAxis();
            float offset = getRotationOffsetForPosition(be, pos, axis);
            float angle = baseAngle * modifier;

            angle += offset;
            angle = angle / 180 * (float) Math.PI;

            int l = encased
                ? LevelRenderer.getLightColor(level, pos.relative(dir))
                : light;

            SuperByteBuffer opening = CachedBuffers.partialFacing(modifier == 1f
                        ? (encased ? CALPartialModels.ENCASED_SHAFT_OPENING : CALPartialModels.SHAFT_OPENING)
                        : (encased ? CALPartialModels.ENCASED_SHAFT_OPENING_REVERSED : CALPartialModels.SHAFT_OPENING_REVERSED),
                    state, dir.getOpposite());
            opening.light(l).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));

            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), dir);
            kineticRotationTransform(shaft, be, axis, angle, l);
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }
}
