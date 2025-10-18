package dev.khloeleclair.create.additionallogistics.mixin.client;

import dev.engine_room.flywheel.api.internal.FlwApiLink;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.LowEntityKineticBlockEntityRenderer;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VisualizationManager.class)
public interface MixinVisualizationManager {

    @Overwrite
    static boolean supportsVisualization(@Nullable LevelAccessor level) {
        if (LowEntityKineticBlockEntityRenderer.overrideVisualization)
            return false;
        return FlwApiLink.INSTANCE.supportsVisualization(level);
    }

}
