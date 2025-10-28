package dev.khloeleclair.create.additionallogistics.client.content.trains.networkMonitor;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.track.ITrackBlock;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitor;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class NetworkMonitorVisual extends AbstractBlockEntityVisual<NetworkMonitorBlockEntity> implements SimpleTickableVisual {

    private final TransformedInstance overlay;

    public NetworkMonitorVisual(VisualizationContext ctx, NetworkMonitorBlockEntity blockEntity, float partialTick) {
        super(ctx, blockEntity, partialTick);

        overlay = ctx.instancerProvider()
                .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TRACK_OBSERVER_OVERLAY))
                .createInstance();

    }

    @Override
    public void tick(Context context) {
        TrackTargetingBehaviour<NetworkMonitor> target = blockEntity.edgePoint;
        BlockPos targetPosition = target.getGlobalPosition();
        Level level = blockEntity.getLevel();
        BlockState trackState = level.getBlockState(targetPosition);
        Block block = trackState.getBlock();

        if (!(block instanceof ITrackBlock trackBlock)) {
            overlay.setZeroTransform()
                    .setChanged();
            return;
        }

        overlay.setIdentityTransform()
                .translate(targetPosition);

        TrackTargetingBehaviour.RenderedTrackOverlayType type = TrackTargetingBehaviour.RenderedTrackOverlayType.OBSERVER;
        trackBlock.prepareTrackOverlay(overlay, level, targetPosition, trackState, target.getTargetBezier(),
                target.getTargetDirection(), type);

        overlay.setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(overlay);
    }

    @Override
    protected void _delete() {
        overlay.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(overlay);
    }

}
