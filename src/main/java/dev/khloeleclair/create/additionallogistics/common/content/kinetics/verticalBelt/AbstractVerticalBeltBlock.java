package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.utilities.FlexiblePoleHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Predicate;

public abstract class AbstractVerticalBeltBlock extends HorizontalKineticBlock {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

    public AbstractVerticalBeltBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    public static void initBelt(Level world, BlockPos pos) {
        if (world.isClientSide || (world instanceof ServerLevel sl && sl.getChunkSource().getGenerator() instanceof DebugLevelSource))
            return;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractVerticalBeltBlock))
            return;

        // Find controller
        int limit = 1000;

        BlockPos currentPos = pos;
        while(limit-- > 0) {
            BlockPos nextPos = currentPos.below();
            var nextState = world.getBlockState(nextPos);
            if (!(nextState.getBlock() instanceof AbstractVerticalBeltBlock))
                break;

            currentPos = nextPos;
        }

        // Init belts
        int index = 0;
        var positions = new VerticalBeltHelper.Positions(world, currentPos);

        for(BlockPos beltPos : positions) {
            if (world.getBlockEntity(beltPos) instanceof VerticalBeltBlockEntity vbbe) {
                vbbe.setController(currentPos);
                vbbe.beltHeight = positions.size();
                vbbe.index = index;
                vbbe.attachKinetics();
                vbbe.setChanged();
                vbbe.sendData();

                // Potentially eject inventory?
            } else {
                world.destroyBlock(currentPos, true);
                return;
            }

            index++;
        }
    }


    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.mayBuild())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (this instanceof EncasableBlock encasable) {
            ItemInteractionResult result = encasable.tryEncase(state, level, pos, stack, player, hand, hitResult);
            if (result.consumesAction())
                return result;
        }

        var helper = PlacementHelpers.get(placementHelperId);
        if (helper != null && helper.matchesItem(stack))
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    public static class PlacementHelper extends FlexiblePoleHelper<Direction> {

        public PlacementHelper() {
            super(
                    state -> state.getBlock() instanceof AbstractVerticalBeltBlock,
                    state -> state.getValue(HORIZONTAL_FACING),
                    HORIZONTAL_FACING
            );
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof AbstractVerticalBeltBlock;
        }

    }

}
