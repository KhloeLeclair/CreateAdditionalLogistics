package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft;

import com.simibubi.create.foundation.placement.PoleHelper;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.AbstractFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazySimpleKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLazyShaftBlock extends AbstractLazySimpleKineticBlock<LazyShaftBlockEntity> {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public static final VoxelShape[] CONNECTOR_SHAPES = {
            Block.box(0, 3, 3, 16, 13, 13), // X
            Block.box(3, 0, 3, 13, 16, 13), // Y
            Block.box(3, 3, 0, 13, 13, 16), // Z
    };

    public AbstractLazyShaftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
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

        return super.getShape(state, level, pos, context);
    }

    @Override
    public @Nullable DyeColor getColor(BlockState state) {
        return null;
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
            super(state -> state.getBlock() instanceof AbstractLazyShaftBlock, state -> state.getValue(AXIS), AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof AbstractLazyShaftBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return s -> {
                if (s.getBlock() instanceof AbstractLazyShaftBlock)
                    return true;
                return s.getBlock() instanceof AbstractFlexibleShaftBlock;
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
            PlacementOffset offset = state.getBlock() instanceof AbstractFlexibleShaftBlock
                    ? getOffsetFlexible(player, world, state, pos, ray)
                    : super.getOffset(player, world, state, pos, ray);

            if (offset.isSuccessful())
                offset.withTransform(offset.getTransform()
                        .andThen(s -> world.isClientSide() ? s
                                : ((AbstractLazyShaftBlock) s.getBlock()).withSides(world, offset.getBlockPos(), s)));
            return offset;
        }
    }

}
