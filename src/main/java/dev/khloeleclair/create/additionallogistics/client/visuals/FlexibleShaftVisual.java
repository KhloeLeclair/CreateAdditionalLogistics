package dev.khloeleclair.create.additionallogistics.client.visuals;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.khloeleclair.create.additionallogistics.common.blockentities.FlexibleShaftBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.Consumer;

public class FlexibleShaftVisual extends KineticBlockEntityVisual<FlexibleShaftBlockEntity> {

    protected final EnumMap<Direction, RotatingInstance> keys = new EnumMap<>(Direction.class);

    public FlexibleShaftVisual(VisualizationContext context, FlexibleShaftBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF));

        final float speed = blockEntity.getSpeed();

        for(Direction dir : Iterate.directions) {
            byte mode = blockEntity.getSide(dir);
            float s = mode == -1 ? -speed : speed;

            var instance = instancer.createInstance();

            instance.setup(blockEntity, dir.getAxis(), s)
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, dir)
                    .setChanged();

            instance.setVisible(mode != 0);
            keys.put(dir, instance);
        }
    }

    @Override
    public void update(float partialTick) {
        super.update(partialTick);

        final float speed = blockEntity.getSpeed();

        for(var entry : keys.entrySet()) {
            final Direction dir = entry.getKey();
            byte mode = blockEntity.getSide(dir);
            float s = mode == -1 ? -speed : speed;

            final var instance = entry.getValue();
            instance
                    .setup(blockEntity, dir.getAxis(), s)
                    .setChanged();

            instance.setVisible(mode != 0);
        }
    }

    @Override
    public void updateLight(float partialTick) {
        relight(keys.values().toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        keys.values().forEach(AbstractInstance::delete);
        keys.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        keys.values().forEach(consumer);
    }

}
