package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RotationPropagator.class)
public interface RotationPropagatorInvoker {

    @Invoker(remap = false)
    public static float callGetConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to) {
        throw new AssertionError();
    };

}
