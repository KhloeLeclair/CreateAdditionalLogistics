package dev.khloeleclair.create.additionallogistics.client.content.logistics.packageAccelerator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageAccelerator.PackageAcceleratorBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class PackageAcceleratorRenderer  extends KineticBlockEntityRenderer<PackageAcceleratorBlockEntity> {

    public PackageAcceleratorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(PackageAcceleratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        final var state = be.getBlockState();
        final var block = state.getBlock();

        Direction.Axis axis = getRotationAxisOf(be);
        final var pos = be.getBlockPos();

        float angle = getAngleForBe(be, pos, axis);

        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), state.getValue(FACING).getOpposite());
        kineticRotationTransform(shaft, be, axis, angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    @Override
    protected SuperByteBuffer getRotatedModel(PackageAcceleratorBlockEntity be, BlockState state) {
        return CachedBuffers.partialFacingVertical(
                AllPartialModels.SHAFTLESS_COGWHEEL, state,
                Direction.fromAxisAndDirection(((IRotate)state.getBlock()).getRotationAxis(state), Direction.AxisDirection.POSITIVE)
        );
    }
}
