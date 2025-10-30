package dev.khloeleclair.create.additionallogistics.mixin;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.utilities.ISetLazyTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SmartBlockEntity.class)
public abstract class MixinSmartBlockEntity implements ISetLazyTickCounter {

    @Shadow(remap = false)
    private int lazyTickCounter;

    @Override
    public int getLazyTickCounter() {
        return lazyTickCounter;
    }

    @Override
    public void setLazyTickCounter(int lazyTickCounter) {
        this.lazyTickCounter = lazyTickCounter;
    }

}
