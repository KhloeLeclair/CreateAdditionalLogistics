package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.cog;

import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class LazyCogWheelBlock extends AbstractLazyCogWheelBlock implements ProperWaterloggedBlock {

    public LazyCogWheelBlock(boolean large, Properties properties) {
        super(large, properties);
    }

    public static LazyCogWheelBlock small(Properties properties) { return new LazyCogWheelBlock(false, properties); }
    public static LazyCogWheelBlock large(Properties properties) { return new LazyCogWheelBlock(true, properties); }

    @Override
    public FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }
}
