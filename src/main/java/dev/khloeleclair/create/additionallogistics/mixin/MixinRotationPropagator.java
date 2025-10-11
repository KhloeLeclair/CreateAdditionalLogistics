package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Mixin(RotationPropagator.class)
public class MixinRotationPropagator {

    @Invoker
    private static List<KineticBlockEntity> callGetConnectedNeighbours(KineticBlockEntity be) {
        throw new AssertionError();
    }

    @Invoker
    private static void callPropagateNewSource(KineticBlockEntity currentTE) {
        throw new AssertionError();
    }


    @Overwrite
    private static void propagateMissingSource(KineticBlockEntity updateTE) {
        final Level world = updateTE.getLevel();

        Set<KineticBlockEntity> potentialNewSources = new ObjectOpenHashSet<>();
        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        List<BlockPos> frontier = new LinkedList<>();
        frontier.add(updateTE.getBlockPos());
        visited.add(updateTE.getBlockPos());
        BlockPos missingSource = updateTE.hasSource() ? updateTE.source : null;

        while (!frontier.isEmpty()) {
            final BlockPos pos = frontier.remove(0);
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (!(blockEntity instanceof KineticBlockEntity currentBE))
                continue;

            currentBE.removeSource();
            currentBE.sendData();

            for (KineticBlockEntity neighbourBE : callGetConnectedNeighbours(currentBE)) {
                if (neighbourBE.getBlockPos()
                        .equals(missingSource))
                    continue;
                if (!neighbourBE.hasSource())
                    continue;

                if (!neighbourBE.source.equals(pos)) {
                    potentialNewSources.add(neighbourBE);
                    continue;
                }

                if (neighbourBE.isSource())
                    potentialNewSources.add(neighbourBE);

                if (!visited.add(neighbourBE.getBlockPos()))
                    continue;

                frontier.add(neighbourBE.getBlockPos());
            }
        }

        for (KineticBlockEntity newSource : potentialNewSources) {
            if (newSource.hasSource() || newSource.isSource()) {
                callPropagateNewSource(newSource);
                return;
            }
        }
    }

}
