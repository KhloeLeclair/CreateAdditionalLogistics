package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.placement.PoleHelper;
import dev.khloeleclair.create.additionallogistics.common.blockentities.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blockentities.LazyShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class LazyShaftBlock extends AbstractLowEntityKineticBlock<LazyShaftBlockEntity> implements EncasableBlock, ProperWaterloggedBlock {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final BooleanProperty NEGATIVE = BooleanProperty.create("negative");
    public static final BooleanProperty POSITIVE = BooleanProperty.create("positive");

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public static final VoxelShape[] CONNECTOR_SHAPES = {
            Block.box(0, 3, 3, 16, 13, 13), // X
            Block.box(3, 0, 3, 13, 16, 13), // Y
            Block.box(3, 3, 0, 13, 13, 16), // Z
    };

    public LazyShaftBlock(Properties properties) {
        super(properties);

        registerDefaultState(defaultBlockState()
                .setValue(WATERLOGGED, false)
                .setValue(AXIS, Direction.Axis.Y)
                .setValue(NEGATIVE, false)
                .setValue(POSITIVE, false));
    }

    protected BlockState copyValuesForCasing(BlockState from, BlockState to) {
        if (from.hasProperty(AXIS) && to.hasProperty(AXIS))
            to = to.setValue(AXIS, from.getValue(AXIS));
        if (from.hasProperty(POSITIVE) && to.hasProperty(POSITIVE))
            to = to.setValue(POSITIVE, from.getValue(POSITIVE));
        if (from.hasProperty(NEGATIVE) && to.hasProperty(NEGATIVE))
            to = to.setValue(NEGATIVE, from.getValue(NEGATIVE));
        return to;
    }


    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        byte key = 0;
        final var axis = state.getValue(AXIS);
        final boolean positive = state.getValue(POSITIVE);
        final boolean negative = state.getValue(NEGATIVE);

        if (positive && negative) {
            if (axis == Direction.Axis.X)
                return CONNECTOR_SHAPES[0];
            else if (axis == Direction.Axis.Y)
                return CONNECTOR_SHAPES[1];
            else
                return CONNECTOR_SHAPES[2];
        }

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
        builder.add(AXIS, POSITIVE, NEGATIVE, WATERLOGGED);
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

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {

        updateWater(level, state, pos);

        BlockState newState = withSides(level, pos, state);
        if (newState != state && level instanceof ServerLevel sl)
            AbstractLowEntityKineticBlockEntity.markDirty(sl, pos);

        return newState;
        //return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return fluidState(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state;
        Direction.Axis preferred = getPreferredAxis(context);
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
    @Nullable DyeColor getColor(BlockState state) {
        return null;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public Class<LazyShaftBlockEntity> getBlockEntityClass() {
        return LazyShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LazyShaftBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.LAZY_SHAFT.get();
    }

    @Override
    public @Nullable IPlacementHelper getPlacementHelper() {
        return PlacementHelpers.get(placementHelperId);
    }

    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof LazyShaftBlock, state -> state.getValue(AXIS), AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof LazyShaftBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> {
                if (s.getBlock() instanceof LazyShaftBlock)
                    return true;
                return s.getBlock() instanceof FlexibleShaftBlock;
            };
        }

        public PlacementOffset getOffsetFlexible(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
            List<Direction> directions = IPlacementHelper.orderedByDistance(pos, ray.getLocation());

            final Direction side = directions.getFirst();
            BlockPos newPos = pos.relative(side);
            BlockState newState = world.getBlockState(newPos);
            if (newState.canBeReplaced())
                return PlacementOffset.success(newPos, b -> b.setValue(AXIS, side.getAxis()));

            return PlacementOffset.fail();
        }

        @Override
        public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
            PlacementOffset offset = state.getBlock() instanceof FlexibleShaftBlock
                    ? getOffsetFlexible(player, world, state, pos, ray)
                    : super.getOffset(player, world, state, pos, ray);

            if (offset.isSuccessful())
                offset.withTransform(offset.getTransform()
                        .andThen(s -> world.isClientSide() ? s
                                : ((LazyShaftBlock) s.getBlock()).withSides(world, offset.getBlockPos(), s)));
            return offset;
        }
    }

}
