package dev.khloeleclair.create.additionallogistics.common.blocks;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntityTicker;
import dev.khloeleclair.create.additionallogistics.common.blockentities.FlexibleShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FlexibleShaftBlock extends KineticBlock implements IBE<FlexibleShaftBlockEntity> {

    public static final BooleanProperty[] SIDES = {
            BooleanProperty.create("down"),
            BooleanProperty.create("up"),
            BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH,
            BlockStateProperties.WEST,
            BlockStateProperties.EAST
    };

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    @Nullable
    protected final DyeColor color;

    public FlexibleShaftBlock(Properties properties) {
        this(properties, null);
    }

    public FlexibleShaftBlock(Properties properties, @Nullable DyeColor color) {
        super(properties);
        this.color = color;

        BlockState state = getStateDefinition().any()
                .setValue(ACTIVE, false);

        for(var side : SIDES)
            state = state.setValue(side, false);

        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIDES);
        builder.add(ACTIVE);
    }

    public boolean connectsTo(BlockState state) {
        if (!(state.getBlock() instanceof FlexibleShaftBlock fsb))
            return false;

        return color == null || fsb.color == null || color == fsb.color;
    }

    public boolean isValidNeighbor(LevelReader world, BlockPos pos, BlockState state, Direction relative) {
        if (state.getBlock() instanceof FlexibleShaftBlock)
            return false;
        if (state.getBlock() instanceof IRotate ir)
            return ir.hasShaftTowards(world, pos, state, relative.getOpposite());
        return false;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        final var level = context.getLevel();
        final var pos = context.getClickedPos();

        BlockState state = super.getStateForPlacement(context);
        BlockState newState = getState(level, pos, state);

        return newState == null ? state : newState;
    }

    @Nullable
    public BlockState getState(Level level, BlockPos pos, BlockState state) {
        boolean changed = false;

        for(Direction dir : Iterate.directions) {
            int index = dir.ordinal();

            BlockPos dpos = pos.relative(dir);
            BlockState dstate = level.getBlockState(dpos);

            boolean connects = connectsTo(dstate);

            if (connects != state.getValue(SIDES[index])) {
                state = state.setValue(SIDES[index], connects);
                changed = true;
            }
        }

        return changed ? state : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.isClientSide)
            return;

        int x = pos.getX() - neighborPos.getX();
        int y = pos.getY() - neighborPos.getY();
        int z = pos.getZ() - neighborPos.getZ();

        var side = Direction.fromDelta(x, y, z);
        if (side == null)
            return;

        var prop = SIDES[side.getOpposite().ordinal()];

        // Do we connect to whatever the new neighbor state is?
        boolean connects = connectsTo(level.getBlockState(neighborPos));

        // And is that different from our previous state?
        if (state.getValue(prop) != connects) {
            level.setBlockAndUpdate(pos, state.setValue(prop, connects));
            FlexibleShaftBlockEntity.markDirty(level, pos);
        }
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        //return super.hasShaftTowards(world, pos, state, face);
        if (!(world.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb))
            return false;

        return fsb.getSide(face) != 0;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        final var level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        final var side = context.getClickedFace();
        if (state.getValue(SIDES[side.ordinal()]))
            return InteractionResult.SUCCESS;

        // Ensure we have a tile entity if we didn't already.
        if (!state.getValue(ACTIVE)) {
            BlockPos pos = context.getClickedPos();
            level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
            FlexibleShaftBlockEntity.markDirty(level, pos);
        }

        // If the block entity doesn't exist yet for some reason, return.
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof FlexibleShaftBlockEntity fsb))
            return InteractionResult.SUCCESS;

        fsb.toggleSide(context.getClickedFace());

        // If we don't need a tile entity, remove it.
        if (! fsb.shouldBeActive()) {
            fsb.deactivateSelf();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (state.getValue(ACTIVE)) {
            return new SmartBlockEntityTicker<>();
        }
        return null;
    }

    @Override
    public Class<FlexibleShaftBlockEntity> getBlockEntityClass() {
        return FlexibleShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FlexibleShaftBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.FLEXIBLE_SHAFT.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        if (blockState.getValue(ACTIVE)) {
            return new FlexibleShaftBlockEntity(getBlockEntityType(), blockPos, blockState);
        }
        return null;
    }
}
