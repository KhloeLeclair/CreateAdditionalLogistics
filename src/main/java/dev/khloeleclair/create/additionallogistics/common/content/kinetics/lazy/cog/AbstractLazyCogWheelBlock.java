package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.cog;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazySimpleKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractLazyCogWheelBlock extends AbstractLazySimpleKineticBlock<LazyCogWheelBlockEntity> implements ICogWheel {

    boolean isLarge;

    public AbstractLazyCogWheelBlock(boolean large, Properties properties) {
        super(properties);
        this.isLarge = large;
    }

    @Override
    public boolean isLargeCog() {
        return isLarge;
    }

    @Override
    public boolean isSmallCog() {
        return !isLarge;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(state, level, pos, context);

        // TODO: Large Cog Shape + Caching

        //return Shapes.or(
        //        super.getShape(state, level, pos, context),
        //        AllShapes.LARGE_GEAR.get(state.getValue(AXIS))
        //);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return CogWheelBlock.isValidCogwheelPosition(ICogWheel.isLargeCog(state), level, pos, state.getValue(AXIS));
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(worldIn, pos, state, placer, stack);
        if (placer instanceof Player player)
            triggerShiftingGearsAdvancement(worldIn, pos, state, player);
    }

    protected void triggerShiftingGearsAdvancement(Level world, BlockPos pos, BlockState state, Player player) {
        if (world.isClientSide || player == null)
            return;

        Direction.Axis axis = state.getValue(CogWheelBlock.AXIS);
        for (Direction.Axis perpendicular1 : Iterate.axes) {
            if (perpendicular1 == axis)
                continue;

            Direction d1 = Direction.get(Direction.AxisDirection.POSITIVE, perpendicular1);
            for (Direction.Axis perpendicular2 : Iterate.axes) {
                if (perpendicular1 == perpendicular2)
                    continue;
                if (axis == perpendicular2)
                    continue;

                Direction d2 = Direction.get(Direction.AxisDirection.POSITIVE, perpendicular2);
                for (int offset1 : Iterate.positiveAndNegative) {
                    for (int offset2 : Iterate.positiveAndNegative) {
                        BlockPos connectedPos = pos.relative(d1, offset1)
                                .relative(d2, offset2);
                        BlockState blockState = world.getBlockState(connectedPos);
                        if (!(blockState.getBlock() instanceof CogWheelBlock))
                            continue;
                        if (blockState.getValue(CogWheelBlock.AXIS) != axis)
                            continue;
                        if (ICogWheel.isLargeCog(blockState) == isLarge)
                            continue;

                        AllAdvancements.COGS.awardTo(player);
                    }
                }
            }
        }
    }

    @Override
    public @Nullable IPlacementHelper getPlacementHelper() {
        return null;
    }

    @Override
    protected @Nullable Direction.Axis getAxisForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && context.getPlayer()
                .isShiftKeyDown())
            return context.getClickedFace()
                    .getAxis();

        Level world = context.getLevel();

        BlockPos placedOnPos = context.getClickedPos()
                .relative(context.getClickedFace()
                        .getOpposite());
        BlockState placedAgainst = world.getBlockState(placedOnPos);

        Block block = placedAgainst.getBlock();
        if (ICogWheel.isSmallCog(placedAgainst))
            return ((IRotate) block).getRotationAxis(placedAgainst);

        Direction.Axis preferredAxis = getPreferredAxis(context);
        return preferredAxis != null ? preferredAxis
                : context.getClickedFace()
                .getAxis();
    }

    @Override
    public float getParticleTargetRadius() {
        return isLarge ? 1.125f : .65f;
    }

    @Override
    public float getParticleInitialRadius() {
        return isLarge ? 1f : .75f;
    }

    @Override
    public boolean isDedicatedCogWheel() {
        return true;
    }

    @Override
    public @Nullable DyeColor getColor(BlockState state) {
        return null;
    }

    @Override
    public boolean isActive(BlockState state) {
        return true;
    }

    @Override
    public Class<LazyCogWheelBlockEntity> getBlockEntityClass() {
        return LazyCogWheelBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LazyCogWheelBlockEntity> getBlockEntityType() {
        return isLarge
                ? CALBlockEntityTypes.LAZY_LARGE_COGWHEEL.get()
                : CALBlockEntityTypes.LAZY_COGWHEEL.get();
    }
}
