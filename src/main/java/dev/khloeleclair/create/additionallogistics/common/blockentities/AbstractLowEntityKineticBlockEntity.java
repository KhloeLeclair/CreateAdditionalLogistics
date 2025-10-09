package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blocks.AbstractLowEntityKineticBlock;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractLowEntityKineticBlockEntity extends SplitShaftBlockEntity {

    @Nullable
    protected List<BlockPos> connections;
    protected boolean checkInvalid;

    public AbstractLowEntityKineticBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        connections = null;
        if (compound.contains("Connections", CompoundTag.TAG_LONG_ARRAY)) {
            var cache = compound.getLongArray("Connections");
            connections = new ArrayList<>(cache.length);
            for(long pos : cache)
                connections.add(BlockPos.of(pos));
        }
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        if (connections != null) {
            var cache = new long[connections.size()];
            int i = 0;
            for(var entry : connections) {
                cache[i] = entry.asLong();
                i++;
            }
            compound.putLongArray("Connections", cache);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (checkInvalid && !level.isClientSide) {
            checkInvalid = false;
            removeInvalidConnections();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        notifyConnectedToValidate();
    }

    public void removeInvalidConnections() {
        boolean changed = false;
        if (connections != null) {
            var iterator = connections.iterator();
            while(iterator.hasNext()) {
                var pos = iterator.next();
                var relative = worldPosition.offset(pos);

                if (!level.isLoaded(relative) || level.getBlockEntity(relative) instanceof AbstractLowEntityKineticBlockEntity)
                    continue;

                iterator.remove();
                changed = true;
            }
        }
        if (changed)
            notifyUpdate();
    }

    public void notifyConnectedToValidate() {
        if (connections != null && level != null)
            for(BlockPos pos : connections) {
                BlockPos relative = worldPosition.offset(pos);
                if (level.isLoaded(relative) && level.getBlockEntity(relative) instanceof AbstractLowEntityKineticBlockEntity lek)
                    lek.checkInvalid = true;
            }
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.getBlock() instanceof AbstractLowEntityKineticBlock<?> lek && lek.isActive(state);
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        if (connections != null)
            connections.forEach(p -> neighbours.add(worldPosition.offset(p)));
        return super.addPropagationLocations(block, state, neighbours);
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (target instanceof AbstractLowEntityKineticBlockEntity && connections != null && connections.contains(target.getBlockPos().subtract(worldPosition)))
            return 1;

        return 0;
    }

    @Override
    public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        BlockPos relative = other.getBlockPos().subtract(worldPosition);
        return connections != null && connections.contains(relative);
    }

    protected void setConnections(List<BlockPos> newConnections) {
        int index = -1;
        while(index++ < newConnections.size()) {
            if (worldPosition.equals(newConnections.get(index)))
                break;
        }

        if (index >= newConnections.size())
            return;

        int previous = index - 1;
        int next = index + 1;

        // For the entities at the ends of the list, loop around.
        if (newConnections.size() > 2) {
            if (previous == -1)
                previous = newConnections.size() - 1;
            if (next >= newConnections.size())
                next = 0;
        }

        if (connections == null)
            connections = new ArrayList<>();

        @Nullable BlockPos prevPos = previous == -1 ? null : newConnections.get(previous).subtract(worldPosition);
        @Nullable BlockPos nextPos = next >= newConnections.size() ? null : newConnections.get(next).subtract(worldPosition);

        int count = (prevPos == null ? 0 : 1) + (nextPos == null ? 0 : 1);

        boolean contains_previous = prevPos == null || connections.contains(prevPos);
        boolean contains_next = nextPos == null || connections.contains(nextPos);

        // Nothing changed.
        if (contains_previous && contains_next && connections.size() == count)
            return;

        detachKinetics();

        connections.clear();
        if (prevPos != null)
            connections.add(prevPos);
        if (nextPos != null)
            connections.add(nextPos);

        notifyUpdate();
        updateSpeed = true;
    }



    // Discovery / Walking

    public record WalkResult(Set<BlockPos> visited, List<BlockPos> entityPositions, Set<AbstractLowEntityKineticBlockEntity> entities) {

        public static WalkResult EMPTY = new WalkResult(Collections.emptySet(), List.of(), Collections.emptySet());

    }

    /// Discover all connected flexible shaft blocks and entities.
    public static WalkResult walkBlocks(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractLowEntityKineticBlock<?> block))
            return WalkResult.EMPTY;

        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        List<BlockPos> entityPositions = new ArrayList<>();
        Set<AbstractLowEntityKineticBlockEntity> entities = new ObjectOpenHashSet<>();
        List<BlockPos> queue = new LinkedList<>();

        visited.add(pos);
        if (level.getBlockEntity(pos) instanceof AbstractLowEntityKineticBlockEntity lek) {
            entityPositions.add(pos);
            entities.add(lek);
        }

        addToDirtyList(level, pos, state, block, visited, queue);

        while (!queue.isEmpty()) {
            BlockPos qpos = queue.removeFirst();
            if (level.isLoaded(qpos)) {
                var qstate = level.getBlockState(qpos);
                if (qstate.getBlock() instanceof AbstractLowEntityKineticBlock<?> qblock) {
                    if (level.getBlockEntity(qpos) instanceof AbstractLowEntityKineticBlockEntity qlek) {
                        entityPositions.add(qpos);
                        entities.add(qlek);
                    }

                    addToDirtyList(level, qpos, qstate, qblock, visited, queue);
                }
            }
        }

        return new WalkResult(visited, entityPositions, entities);
    }

    private static void addToDirtyList(Level world, BlockPos pos, BlockState state, AbstractLowEntityKineticBlock<?> block, Set<BlockPos> visited, List<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos p = pos.relative(direction);
            if (!visited.contains(p) && world.isLoaded(p) && block.connectsTo(world, pos, state, direction, p, world.getBlockState(p)) && !queue.contains(p)) {
                visited.add(p);
                queue.add(p);
            }
        }
    }

    private record LevelBlockPos(Level level, BlockPos pos) {};

    private static final Set<LevelBlockPos> dirtyPositions = new ObjectOpenHashSet<>();

    /// Mark a position dirty. Dirty positions are saved in a list and processed at the
    /// end of each tick, to ensure checks aren't performed more than once.
    public static void markDirty(Level level, BlockPos pos) {
        if (level.isClientSide)
            return;

        dirtyPositions.add(new LevelBlockPos(level, pos));
    }

    public static void updateDirty() {
        LinkedList<LevelBlockPos> dirty = new LinkedList<>(dirtyPositions);
        dirtyPositions.clear();

        while(!dirty.isEmpty()) {
            LevelBlockPos pos = dirty.removeFirst();
            var result = walkBlocks(pos.level, pos.pos);

            // Update every entity.
            for(var entity: result.entities)
                entity.setConnections(result.entityPositions);

            // Ensure we don't walk any of these positions twice.
            dirty.removeIf(x -> result.visited.contains(x.pos));
        }
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (!dirtyPositions.isEmpty())
            updateDirty();
    }

}
