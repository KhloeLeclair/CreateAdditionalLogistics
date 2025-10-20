package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft;

import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LazyShaftBlock extends AbstractLazyShaftBlock implements ProperWaterloggedBlock {

    public LazyShaftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

}
