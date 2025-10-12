package dev.khloeleclair.create.additionallogistics.common.blockentities;

import dev.khloeleclair.create.additionallogistics.common.blocks.AbstractLazyShaftBlock;
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
        if (face.getAxis() == getBlockState().getValue(AbstractLazyShaftBlock.AXIS))
            return 1f;
        return 0f;
    }

}
