package dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy;

import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazyShaftBlock;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EncasedLazyCogCTBehavior extends EncasedCTBehaviour {

    @Nullable
    private final Couple<CTSpriteShiftEntry> sideShifts;
    private final boolean large;

    public EncasedLazyCogCTBehavior(CTSpriteShiftEntry shift) {
        this(shift, null);
    }

    public EncasedLazyCogCTBehavior(CTSpriteShiftEntry shift, @Nullable Couple<CTSpriteShiftEntry> sideShifts) {
        super(shift);
        large = sideShifts == null;
        this.sideShifts = sideShifts;
    }

    @Override
    public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos,
                              BlockPos otherPos, Direction face) {
        Direction.Axis axis = state.getValue(AbstractLazyShaftBlock.AXIS);
        if (large || axis == face.getAxis())
            return super.connectsTo(state, other, reader, pos, otherPos, face);

        if (other.getBlock() == state.getBlock() && other.getValue(AbstractLazyShaftBlock.AXIS) == state.getValue(AbstractLazyShaftBlock.AXIS))
            return true;

        BlockState blockState = reader.getBlockState(otherPos.relative(face));
        if (!ICogWheel.isLargeCog(blockState))
            return false;

        return ((IRotate) blockState.getBlock()).getRotationAxis(blockState) == axis;
    }

    @Override
    protected boolean reverseUVs(BlockState state, Direction face) {
        return state.getValue(AbstractLazyShaftBlock.AXIS)
                .isHorizontal()
                && face.getAxis()
                .isHorizontal()
                && face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
    }

    @Override
    protected boolean reverseUVsVertically(BlockState state, Direction face) {
        if (!large && state.getValue(AbstractLazyShaftBlock.AXIS) == Direction.Axis.X && face.getAxis() == Direction.Axis.Z)
            return face != Direction.SOUTH;
        return super.reverseUVsVertically(state, face);
    }

    @Override
    protected boolean reverseUVsHorizontally(BlockState state, Direction face) {
        if (large)
            return super.reverseUVsHorizontally(state, face);

        if (state.getValue(AbstractLazyShaftBlock.AXIS)
                .isVertical()
                && face.getAxis()
                .isHorizontal())
            return true;

        if (state.getValue(AbstractLazyShaftBlock.AXIS) == Direction.Axis.Z && face == Direction.DOWN)
            return true;

        return super.reverseUVsHorizontally(state, face);
    }

    @Override
    public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
        Direction.Axis axis = state.getValue(AbstractLazyShaftBlock.AXIS);
        if (large || axis == direction.getAxis()) {
            if (axis == direction.getAxis() && !state
                    .getValue(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? AbstractLazyShaftBlock.POSITIVE
                            : AbstractLazyShaftBlock.NEGATIVE))
                return null;
            return super.getShift(state, direction, sprite);
        }
        return sideShifts.get(axis == Direction.Axis.X || axis == Direction.Axis.Z && direction.getAxis() == Direction.Axis.X);
    }

}
