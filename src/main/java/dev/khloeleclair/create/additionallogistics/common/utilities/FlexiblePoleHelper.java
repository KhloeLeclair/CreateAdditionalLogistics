package dev.khloeleclair.create.additionallogistics.common.utilities;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class FlexiblePoleHelper<T extends Comparable<T>> implements IPlacementHelper {

    protected final Predicate<BlockState> statePredicate;
    protected final Function<BlockState, T> comparableSupplier;
    @Nullable
    protected final Property<T> property;

    public FlexiblePoleHelper(Predicate<BlockState> statePredicate, Function<BlockState, T> comparableSupplier, @Nullable Property<T> property) {
        this.statePredicate = statePredicate;
        this.comparableSupplier = comparableSupplier;
        this.property = property;
    }

    public boolean matches(BlockState state, T value) {
        if (!statePredicate.test(state))
            return false;

        return comparableSupplier.apply(state) == value;
    }

    public int attachedPoles(Level world, BlockPos pos, Direction direction, T value) {
        BlockPos checkPos = pos.relative(direction);
        BlockState state = world.getBlockState(checkPos);
        int count = 0;
        while (matches(state, value)) {
            count++;
            checkPos = checkPos.relative(direction);
            state = world.getBlockState(checkPos);
        }
        return count;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return statePredicate;
    }

    public BlockState getStateForPlacement(Level world, BlockPos pos, BlockState state, BlockPos sourcePos, BlockState sourceState) {
        if (property != null && state.hasProperty(property))
            return state.setValue(property, sourceState.getValue(property));

        return state;
    }

    @Override
    public PlacementOffset getOffset(@Nullable Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
        List<Direction> directions = IPlacementHelper.orderedByDistanceOnlyAxis(pos, ray.getLocation(), Direction.Axis.Y);
        int range = AllConfigs.server().equipment.placementAssistRange.get();
        if (player != null) {
            AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id()))
                range += 4;
        }

        T sourceProperty = comparableSupplier.apply(state);

        for(Direction dir : directions) {
            int poles = attachedPoles(world, pos, dir, sourceProperty);
            if (poles >= range)
                continue;

            BlockPos newPos = pos.relative(dir, poles + 1);
            BlockState newState = world.getBlockState(newPos);

            if (newState.canBeReplaced())
                return PlacementOffset.success(newPos, s -> getStateForPlacement(world, newPos, s, pos, state));
        }

        return PlacementOffset.fail();
    }
}
