package dev.khloeleclair.create.additionallogistics.common.blockentities;

import dev.khloeleclair.create.additionallogistics.common.blocks.LazyShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class LazyShaftBlockEntity extends AbstractLowEntityKineticBlockEntity {

    public LazyShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (face.getAxis() == getBlockState().getValue(LazyShaftBlock.AXIS))
            return 1;
        return 0;
    }

}
