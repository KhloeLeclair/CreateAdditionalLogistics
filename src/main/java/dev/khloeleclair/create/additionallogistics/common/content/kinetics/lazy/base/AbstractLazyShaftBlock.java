package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base;

import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractLazyShaftBlock<T extends AbstractLowEntityKineticBlockEntity> extends AbstractLowEntityKineticBlock<T> implements EncasableBlock, ProperWaterloggedBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");


    public AbstractLazyShaftBlock(Properties properties) {
        super(properties);

        var state = defaultBlockState();
        if (state.hasProperty(WATERLOGGED))
            state = state.setValue(WATERLOGGED, false);

        registerDefaultState(state
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(NEGATIVE, false)
                .setValue(POSITIVE, false));
    }

    protected BlockState copyValuesForCasing(BlockState from, BlockState to) {
        if (from.hasProperty(WATERLOGGED) && to.hasProperty(WATERLOGGED))
            to = to.setValue(WATERLOGGED, from.getValue(WATERLOGGED));
        if (from.hasProperty(AXIS) && to.hasProperty(AXIS))
            to = to.setValue(AXIS, from.getValue(AXIS));
        if (from.hasProperty(POSITIVE) && to.hasProperty(POSITIVE))
            to = to.setValue(POSITIVE, from.getValue(POSITIVE));
        if (from.hasProperty(NEGATIVE) && to.hasProperty(NEGATIVE))
            to = to.setValue(NEGATIVE, from.getValue(NEGATIVE));
        return to;
    }

    @Override
    public FluidState fluidState(BlockState state) {
        if (!state.hasProperty(WATERLOGGED))
            return Fluids.EMPTY.defaultFluidState();

        return ProperWaterloggedBlock.super.fluidState(state);
    }

    @Override
    public void updateWater(LevelAccessor level, BlockState state, BlockPos pos) {
        if (state.hasProperty(WATERLOGGED))
            ProperWaterloggedBlock.super.updateWater(level, state, pos);
    }

    @Override
    public BlockState withWater(BlockState placementState, BlockPlaceContext ctx) {
        if (placementState.hasProperty(WATERLOGGED))
            return ProperWaterloggedBlock.super.withWater(placementState, ctx);
        return placementState;
    }

    @Override
    protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
        if (!(oldState.getBlock() instanceof AbstractLazyShaftBlock<?>) || !(newState.getBlock() instanceof AbstractLazyShaftBlock<?>))
            return false;

        if (!oldState.hasProperty(AXIS) || !newState.hasProperty(AXIS))
            return false;

        return oldState.getValue(AXIS) == newState.getValue(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        byte key = 0;
        final var axis = state.getValue(AXIS);
        final boolean positive = state.getValue(POSITIVE);
        final boolean negative = state.getValue(NEGATIVE);

        if (positive)
            key |= (1 << Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE).ordinal());
        if (negative)
            key |= (1 << Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE).ordinal());

        return getShapeWithSides(key);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        switch(rotation) {
            case COUNTERCLOCKWISE_90:
            case CLOCKWISE_90:
                switch(state.getValue(AXIS)) {
                    case X:
                        return state.setValue(AXIS, Direction.Axis.Z);
                    case Z:
                        return state.setValue(AXIS, Direction.Axis.X);
                    default:
                        return state;
                }
            default:
                return state;
        }
    }

    protected BlockState withSides(LevelAccessor level, BlockPos pos, BlockState state) {
        var axis = state.getValue(AXIS);

        var positive = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        var negative = positive.getOpposite();

        state = state.setValue(POSITIVE, connectsTo(level, pos, state, positive));
        state = state.setValue(NEGATIVE, connectsTo(level, pos, state, negative));

        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS, POSITIVE, NEGATIVE);
        if (!(this instanceof EncasedBlock))
            builder.add(WATERLOGGED);
    }

    @Nullable
    public static Direction.Axis getPreferredAxis(BlockPlaceContext context) {
        Direction.Axis preferredAxis = null;
        final var level = context.getLevel();
        final var pos = context.getClickedPos();

        for(Direction side : Iterate.directions) {
            var axis = side.getAxis();
            var newPos = pos.relative(side);
            var newState = level.getBlockState(newPos);
            var newBlock = newState.getBlock();
            if (newBlock instanceof FlexibleShaftBlock) {
                if (context.getClickedFace() == side.getOpposite())
                    return axis;
                if (preferredAxis != null && preferredAxis != axis) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = axis;
            }
            if (newBlock instanceof IRotate rot && rot.hasShaftTowards(level, newPos, newState, side.getOpposite())) {
                if (context.getClickedFace() == side.getOpposite())
                    return axis;

                if (preferredAxis != null && preferredAxis != axis) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = axis;
            }
        }
        return preferredAxis;
    }

    @Nullable
    protected Direction.Axis getAxisForPlacement(BlockPlaceContext context) {
        return getPreferredAxis(context);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        updateWater(level, state, pos);

        BlockState newState = withSides(level, pos, state);
        if (newState != state && level instanceof ServerLevel sl)
                AbstractLowEntityKineticBlockEntity.markDirty(sl, pos);

        return newState;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state;
        Direction.Axis preferred = getAxisForPlacement(context);
        if (preferred != null && (context.getPlayer() != null || !context.getPlayer().isShiftKeyDown()))
            state = defaultBlockState().setValue(AXIS, preferred);
        else
            state = defaultBlockState().setValue(AXIS, preferred != null && context.getPlayer().isShiftKeyDown()
                    ? context.getClickedFace().getAxis() : context.getNearestLookingDirection().getAxis());

        return withWater(withSides(context.getLevel(), context.getClickedPos(), state), context);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (face.getAxis() != state.getValue(AXIS))
            return false;

        var dir = face.getAxisDirection();
        if (dir == Direction.AxisDirection.POSITIVE)
            return !state.getValue(POSITIVE);

        return !state.getValue(NEGATIVE);
    }

    @Override
    protected boolean shouldConnectImpl(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, BlockPos otherPos, BlockState otherState) {
        if (direction.getAxis() != state.getValue(AXIS))
            return false;

        return super.shouldConnectImpl(level, pos, state, direction, otherPos, otherState);
    }

    @Override
    public boolean isActive(BlockState state) {
        return !state.getValue(NEGATIVE) || !state.getValue(POSITIVE);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

}
