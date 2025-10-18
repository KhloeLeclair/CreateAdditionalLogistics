package dev.khloeleclair.create.additionallogistics.client.content.logistics.packageAccelerator;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageAccelerator.PackageAcceleratorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageAccelerator.PackageAcceleratorBlock;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PackageAcceleratorVisual extends KineticBlockEntityVisual<PackageAcceleratorBlockEntity> {

    protected final RotatingInstance shaft;
    protected final RotatingInstance cog;
    final Direction direction;
    private final Direction opposite;

    public PackageAcceleratorVisual(VisualizationContext context, PackageAcceleratorBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);

        direction = blockState.getValue(PackageAcceleratorBlock.FACING);
        opposite = direction.getOpposite();

        cog = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFTLESS_COGWHEEL))
                .createInstance();

        cog.setup(blockEntity)
            .setPosition(getVisualPosition())
            .rotateToFace(rotationAxis())
            .setChanged();

        shaft = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF))
            .createInstance();

        shaft.setup(blockEntity)
            .setPosition(getVisualPosition())
            .rotateToFace(Direction.SOUTH, opposite)
            .setChanged();
    }

    @Override
    public void update(float partialTick) {
        cog.setup(blockEntity).setChanged();
        shaft.setup(blockEntity).setChanged();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(cog, shaft);
    }

    @Override
    protected void _delete() {
        cog.delete();
        shaft.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
        consumer.accept(cog);
        consumer.accept(shaft);
    }

}
