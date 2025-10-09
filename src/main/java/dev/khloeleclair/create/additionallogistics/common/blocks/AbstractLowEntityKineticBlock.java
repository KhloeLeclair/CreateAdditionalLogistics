package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.khloeleclair.create.additionallogistics.common.blockentities.AbstractLowEntityKineticBlockEntity;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class AbstractLowEntityKineticBlock<T extends AbstractLowEntityKineticBlockEntity> extends KineticBlock implements IBE<T> {

    public static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    public static final VoxelShape[] SIDE_SHAPES = {
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(3, 14, 3, 13, 16, 13),
            Block.box(3, 3, 0, 13, 13, 2),
            Block.box(3, 3, 14, 13, 13, 16),
            Block.box(0, 3, 3, 2, 13, 13),
            Block.box(14, 3, 3, 16, 13, 13)
    };

    protected static final Map<Byte, VoxelShape> SHAPE_CACHE = new Byte2ObjectOpenHashMap<>();

    protected VoxelShape getShapeWithSides(Direction... sides) {
        byte key = 0;
        for (var side : sides)
            key |= (1 << side.ordinal());
        return getShapeWithSides(key);
    }

    protected VoxelShape getShapeWithSides(byte key) {
        VoxelShape cached;

        synchronized (SHAPE_CACHE) {
            cached = SHAPE_CACHE.get(key);
        }

        if (cached == null) {
            cached = SHAPE;
            for(Direction dir : Iterate.directions) {
                int index = dir.ordinal();
                if ((key & (1 << index)) != 0)
                    cached = Shapes.joinUnoptimized(cached, SIDE_SHAPES[index], BooleanOp.OR);
            }

            cached = cached.optimize();
            synchronized (SHAPE_CACHE) {
                SHAPE_CACHE.put(key, cached);
            }
        }

        return cached;
    }


    public AbstractLowEntityKineticBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Nullable
    public IPlacementHelper getPlacementHelper() {
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.mayBuild())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        var helper = getPlacementHelper();
        if (helper != null && helper.matchesItem(stack))
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /// Determine if this low entity kinetic block can connect to another one.
    protected boolean shouldConnectImpl(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, BlockPos otherPos, BlockState otherState) {
        var otherBlock = otherState.getBlock();
        if (otherBlock instanceof AbstractLowEntityKineticBlock<?> lek) {
            var color = getColor(state);
            var otherColor = lek.getColor(otherState);
            return color == null || otherColor == null || color == otherColor;
        }

        return false;
    }

    public final boolean connectsTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction) {
        final var otherPos = pos.relative(direction);
        final var otherState = level.getBlockState(otherPos);
        return connectsTo(level, pos, state, direction, otherPos, otherState);
    }

    public final boolean connectsTo(LevelAccessor level, BlockPos pos, BlockState state, Direction direction, BlockPos otherPos, BlockState otherState) {
        if (!(otherState.getBlock() instanceof AbstractLowEntityKineticBlock<?> lek))
            return false;

        return shouldConnectImpl(level, pos, state, direction, otherPos, otherState) && lek.shouldConnectImpl(level, otherPos, otherState, direction.getOpposite(), pos, state);
    }

    public abstract boolean isActive(BlockState state);

    @Nullable
    abstract DyeColor getColor(BlockState state);

    @Override
    public @Nullable <S extends BlockEntity> BlockEntityTicker<S> getTicker(Level level, BlockState state, BlockEntityType<S> type) {
        if (isActive(state))
            return new SmartBlockEntityTicker<>();
        return null;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (isActive(state))
            return getBlockEntityType().create(pos, state);
        return null;
    }

}
