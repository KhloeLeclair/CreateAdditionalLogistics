package dev.khloeleclair.create.additionallogistics.client.content.trains.networkMonitor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitor;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkMonitorRenderer  extends SmartBlockEntityRenderer<NetworkMonitorBlockEntity> {

    public NetworkMonitorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(NetworkMonitorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

        if (VisualizationManager.supportsVisualization(be.getLevel()))
            return;

        BlockPos pos = be.getBlockPos();

        TrackTargetingBehaviour<NetworkMonitor> target = be.edgePoint;
        BlockPos targetPosition = target.getGlobalPosition();
        Level level = be.getLevel();
        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();

        if (!(block instanceof ITrackBlock))
            return;

        ms.pushPose();
        TransformStack.of(ms)
                .translate(targetPosition.subtract(pos));
        TrackTargetingBehaviour.RenderedTrackOverlayType type = TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER;
        TrackTargetingBehaviour.render(level, targetPosition, target.getTargetDirection(), target.getTargetBezier(), ms,
                buffer, light, overlay, type, 1);
        ms.popPose();

    }

}
