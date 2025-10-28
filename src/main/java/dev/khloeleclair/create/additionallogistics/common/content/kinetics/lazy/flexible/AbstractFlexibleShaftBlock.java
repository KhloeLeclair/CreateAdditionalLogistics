package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible;

import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPackets;
import dev.khloeleclair.create.additionallogistics.mixin.RotationPropagatorInvoker;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFlexibleShaftBlock extends AbstractLowEntityKineticBlock<FlexibleShaftBlockEntity> {

    public static final BooleanProperty[] SIDES = {
            BooleanProperty.create("down"),
            BooleanProperty.create("up"),
            BlockStateProperties.NORTH,
            BlockStateProperties.SOUTH,
            BlockStateProperties.WEST,
            BlockStateProperties.EAST
    };

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    //public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    @Nullable
    protected final DyeColor color;

    protected AbstractFlexibleShaftBlock(Properties properties, @Nullable DyeColor color) {
        super(properties);
        this.color = color;

        var state = defaultBlockState()
                .setValue(ACTIVE, false);

        if (state.hasProperty(ProperWaterloggedBlock.WATERLOGGED))
            state = state.setValue(ProperWaterloggedBlock.WATERLOGGED, false);

        for(var side : SIDES)
            state = state.setValue(side, false);

        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SIDES);
        builder.add(ACTIVE);
        if (this instanceof ProperWaterloggedBlock)
            builder.add(ProperWaterloggedBlock.WATERLOGGED);
    }

    @Override
    public boolean isActive(BlockState state) {
        return state.getValue(ACTIVE);
    }

    @Override
    public @Nullable DyeColor getColor(BlockState state) {
        return color;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        var newState = state;

        for(Direction dir : Iterate.directions) {
            newState = newState.setValue(SIDES[mirror.mirror(dir).ordinal()], state.getValue(SIDES[dir.ordinal()]));
        }

        return newState;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        var newState = state;

        for(Direction dir : Iterate.directions) {
            newState = newState.setValue(SIDES[rotation.rotate(dir).ordinal()], state.getValue(SIDES[dir.ordinal()]));
        }

        return newState;
    }

    protected BlockState getConnectedState(Level level, BlockPos pos, BlockState initialState) {
        var state = initialState;

        for(Direction dir : Iterate.directions) {
            int index = dir.ordinal();

            BlockPos dpos = pos.relative(dir);
            BlockState dstate = level.getBlockState(dpos);

            boolean connects = connectsTo(level, pos, state, dir, dpos, dstate);
            state = state.setValue(SIDES[index], connects);
        }

        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        final var level = context.getLevel();
        final var pos = context.getClickedPos();

        return getConnectedState(level, pos, super.getStateForPlacement(context));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        byte key = 0;
        for(Direction dir : Iterate.directions) {
            int index = dir.ordinal();
            if (state.getValue(SIDES[index]))
                key |= (1 << index);
        }

        return getShapeWithSides(key);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        var prop = SIDES[direction.ordinal()];

        // Do we connect to whatever the new neighbor state is?
        boolean connects = connectsTo(level, pos, state, direction, neighborPos, level.getBlockState(neighborPos));

        // And is that different from our previous state?
        if (state.getValue(prop) != connects && level instanceof ServerLevel sl) {
            state = state.setValue(prop, connects);
            AbstractLowEntityKineticBlockEntity.markDirty(sl, pos);
        }

        return state;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        if (!isActive(state) || !(world.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb))
            return false;

        return fsb.getSide(face) != 0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty())
            return InteractionResult.FAIL;

        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            CALPackets.OpenFlexibleShaftScreen.of(pos).send(sp);
        }

        return InteractionResult.SUCCESS;
    }

    public void setSide(Level level, BlockPos pos, Direction side, byte value) {
        if (level.isClientSide || !level.isLoaded(pos))
            return;

        var state = level.getBlockState(pos);
        if (!state.getValue(ACTIVE)) {
            if (value == 0)
                return;

            level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
            AbstractLowEntityKineticBlockEntity.markDirty(level, pos);
        }

        if (!(level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb))
            return;

        fsb.setSide(side, value);

        // If we don't need a tile entity, remove it.
        if (! fsb.shouldBeActive())
            fsb.deactivateSelf();
    }


    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        final var level = context.getLevel();
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        final var side = context.getClickedFace();
        if (state.getValue(SIDES[side.ordinal()])) {
            if (!(this instanceof EncasedBlock))
                return InteractionResult.SUCCESS;
        }

        final BlockPos pos = context.getClickedPos();

        // Ensure we have a tile entity if we didn't already.
        if (!state.getValue(ACTIVE)) {
            level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
            AbstractLowEntityKineticBlockEntity.markDirty(level, pos);
        }

        // If the block entity doesn't exist yet for some reason, return.
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof FlexibleShaftBlockEntity fsb))
            return InteractionResult.SUCCESS;

        byte mode = fsb.getSide(side);
        if (mode == 0) {
            // Check if we're connecting to a block that already has rotation.
            var offsetPos = pos.relative(side);
            var offsetState = level.getBlockState(offsetPos);
            if (offsetState.getBlock() instanceof IRotate rot && rot.hasShaftTowards(level, offsetPos, offsetState, side.getOpposite()) && level.getBlockEntity(offsetPos) instanceof KineticBlockEntity kbe) {
                fsb.setSideUnsafe(side, (byte)1);

                float neighborSpeed = kbe.getTheoreticalSpeed();
                float newSpeed = RotationPropagatorInvoker.callGetConveyedSpeed(fsb, kbe);
                float oppositeSpeed = RotationPropagatorInvoker.callGetConveyedSpeed(fsb, kbe);

                if (newSpeed != 0 && oppositeSpeed != 0) {
                    boolean incompatible = Math.signum(newSpeed) != Math.signum(neighborSpeed) && (newSpeed != 0 && neighborSpeed != 0);
                    if (incompatible) {
                        fsb.setSide(side, (byte)-1);
                        return InteractionResult.SUCCESS;
                    }
                }

                // This didn't work out, restore the previous side value before toggling normally.
                fsb.setSideUnsafe(side, mode);
            }
        }

        fsb.toggleSide(side);

        // If we don't need a tile entity, remove it.
        if (! fsb.shouldBeActive()) {
            fsb.deactivateSelf();
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<FlexibleShaftBlockEntity> getBlockEntityClass() {
        return FlexibleShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FlexibleShaftBlockEntity> getBlockEntityType() {
        return CALBlockEntityTypes.FLEXIBLE_SHAFT.get();
    }

}
