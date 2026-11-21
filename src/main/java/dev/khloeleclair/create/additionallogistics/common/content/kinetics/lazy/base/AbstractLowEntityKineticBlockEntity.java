package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALPackets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractLowEntityKineticBlockEntity extends SplitShaftBlockEntity {

    @Nullable
    protected List<BlockPos> connections;

    @Nullable
    private List<BlockPos> pendingConnections;

    protected boolean checkInvalid;
    protected boolean lazyDirty;

    public AbstractLowEntityKineticBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);
        connections = null;

        // A blueprint or a contraption hands us offsets that describe where the blocks used to be.
        // They are not rotated by transform() and become meaningless once the structure moves, so
        // rebuild the cluster from the world instead of trusting the saved list.
        if (wasMoved) {
            lazyDirty = true;
        } else if (compound.contains("Connected", CompoundTag.TAG_LONG_ARRAY)) {
            var cache = compound.getLongArray("Connected");
            connections = new ArrayList<>(cache.length);
            for(long pos : cache)
                connections.add(BlockPos.of(pos));
        } else if (compound.contains("Connections", CompoundTag.TAG_LONG_ARRAY)) {
            lazyDirty = true;
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
            compound.putLongArray("Connected", cache);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level.isClientSide || isVirtual()) {
            if (lazyDirty || connections == null) {
                lazyDirty = false;
                markDirty(level, worldPosition);
            }
            if (checkInvalid) {
                checkInvalid = false;
                removeInvalidConnections();
            }
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
            connections.forEach(p -> {
                // Because the native propagation code adds immediately adjacent positions, we need to check
                // that any neighbor we add isn't already in the list. Which is annoying, since it's a linked
                // list, but it should only have at most 6 entries before it gets to us so just... deal with it.

                // If not for this, we'd potentially be adding duplicate entries to the list and causing
                // unnecessary work up to a potential infinite loop.
                var pos = worldPosition.offset(p);
                if (!neighbours.contains(pos))
                    neighbours.add(pos);
            });

        if (!ICogWheel.isLargeCog(state))
            return super.addPropagationLocations(block, state, neighbours);

        BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1))
                .forEach(offset -> {
                    if (offset.distSqr(BlockPos.ZERO) == 2)
                        neighbours.add(worldPosition.offset(offset));
                });
        return neighbours;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (target instanceof AbstractLowEntityKineticBlockEntity && connections != null && connections.contains(target.getBlockPos().subtract(worldPosition)))
            return 1;

        return 0;
    }

    @Override
    public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        // Unlike in addPropagationLocations, we don't have any special logic for adjacent blocks here.
        // It isn't required.
        BlockPos relative = other.getBlockPos().subtract(worldPosition);
        return connections != null && connections.contains(relative);
    }

    protected boolean setConnections(List<BlockPos> newConnections) {
        // The walk reports every entity of the cluster, but the cluster also contains blocks that
        // have no block entity at all: a flexible shaft with no configured port is deactivated and
        // drops its block entity, and Create cannot see such a block as a kinetic node. Two real
        // entities therefore often sit on either side of one or more of those relay blocks, and the
        // offset recorded here has to reach across them, because propagation can only travel
        // between block entities.
        //
        // Sorting the cluster by position and taking the entries before and after us only works
        // when the chain happens to run in coordinate order. It breaks as soon as the chain turns,
        // which is why an L shaped cluster could carry rotation in one orientation and not in the
        // other. Walk the connectors instead.
        final Set<BlockPos> cluster = new HashSet<>(newConnections);
        if (!cluster.contains(worldPosition))
            return false;

        final List<BlockPos> found = new ArrayList<>(4);
        final Set<BlockPos> visited = new HashSet<>();
        final Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(worldPosition);
        queue.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            BlockState currentState = level.getBlockState(current);
            if (!(currentState.getBlock() instanceof AbstractLowEntityKineticBlock<?> currentBlock))
                continue;

            for(Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (visited.contains(next))
                    continue;

                BlockState nextState = level.getBlockState(next);
                if (!(nextState.getBlock() instanceof AbstractLowEntityKineticBlock<?>))
                    continue;

                // Only travel along links that are actually connected; a dyed or misaligned
                // neighbour must not become part of this chain.
                if (!currentBlock.connectsTo(level, current, currentState, dir, next, nextState))
                    continue;

                visited.add(next);

                // Relay blocks have no block entity and only exist to pass the chain along.
                if (cluster.contains(next))
                    found.add(next.subtract(worldPosition));
                else
                    queue.add(next);
            }
        }

        // If our connections aren't changing, we don't need to do anything and we can stop now.
        if (connections != null && connections.equals(found))
            return false;

        //CreateAdditionalLogistics.LOGGER.debug("Changed connections for {} -- {}", worldPosition, found);

        // Something did change, so we need to detach from the kinetic network.
        detachKinetics();

        // Make a new list of our new connections, and save it as pending for now. We won't swap in the new
        // list until after all our entities go through this step.
        pendingConnections = found;

        return true;
    }

    /// Finalize the connection update process started with a call to setConnections.
    protected void finalizeConnections() {
        if (pendingConnections == null)
            return;

        connections = pendingConnections;
        pendingConnections = null;

        // Notify things that our state changed so that changes are saved / synced as appropriate.
        notifyUpdate();
        // And set a flag so we reconnect to kinetics next tick.
        updateSpeed = true;
    }


    // Discovery / Walking

    public record WalkResult(Set<BlockPos> visited, TreeMap<BlockPos, AbstractLowEntityKineticBlockEntity> entities) {

        public static WalkResult EMPTY = new WalkResult(Collections.emptySet(), new TreeMap<>());

    }

    public static WalkResult walkBlocks(Level level, BlockPos pos) {
        return walkBlocks(level, pos, Integer.MAX_VALUE);
    }

    /// Discover all connected flexible shaft blocks and entities.
    public static WalkResult walkBlocks(Level level, BlockPos pos, int limit) {
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AbstractLowEntityKineticBlock<?> block))
            return WalkResult.EMPTY;

        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        TreeMap<BlockPos, AbstractLowEntityKineticBlockEntity> entities = new TreeMap<>(Vec3i::compareTo);
        List<BlockPos> queue = new LinkedList<>();

        visited.add(pos);
        if (level.getBlockEntity(pos) instanceof AbstractLowEntityKineticBlockEntity lek) {
            entities.put(pos, lek);
            if (entities.size() >= limit)
                return new WalkResult(visited, entities);
        }

        addToDirtyList(level, pos, state, block, visited, queue);

        while (!queue.isEmpty()) {
            BlockPos qpos = queue.removeFirst();
            if (level.isLoaded(qpos)) {
                var qstate = level.getBlockState(qpos);
                if (qstate.getBlock() instanceof AbstractLowEntityKineticBlock<?> qblock) {
                    if (level.getBlockEntity(qpos) instanceof AbstractLowEntityKineticBlockEntity qlek) {
                        entities.put(qpos, qlek);
                        if (entities.size() >= limit)
                            return new WalkResult(visited, entities);
                    }

                    addToDirtyList(level, qpos, qstate, qblock, visited, queue);
                }
            }
        }

        return new WalkResult(visited, entities);
    }

    private static void addToDirtyList(Level world, BlockPos pos, BlockState state, AbstractLowEntityKineticBlock<?> block, Set<BlockPos> visited, List<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos p = pos.relative(direction);
            if (!visited.contains(p) && world.isLoaded(p) && block.connectsTo(world, pos, state, direction, p, world.getBlockState(p))) {
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
        if (level.isClientSide) {
            AbstractLowEntityKineticBlock.clearInformationWalkCache();
            return;
        }

        dirtyPositions.add(new LevelBlockPos(level, pos.immutable()));
    }

    public static void updateDirty() {
        LinkedList<LevelBlockPos> dirty = new LinkedList<>(dirtyPositions);
        dirtyPositions.clear();

        List<AbstractLowEntityKineticBlockEntity> toFinalize = new ArrayList<>();
        Set<ServerLevel> levels = new ObjectOpenHashSet<>();

        while(!dirty.isEmpty()) {
            LevelBlockPos pos = dirty.removeFirst();
            var result = walkBlocks(pos.level, pos.pos);
            if (pos.level instanceof ServerLevel sl)
                levels.add(sl);

            //CreateAdditionalLogistics.LOGGER.debug("Updating dirty lazy network from {}, found {} connected lazy blocks with {} entities.", pos.pos, result.visited.size(), result.entities.size());

            if (!result.entities.isEmpty()) {
                // Make a list.
                List<BlockPos> entityPositions = result.entities.navigableKeySet().stream().toList();

                // Update every entity. This calls detachKinetics on relevant entities, and returns
                // true so we can collect them to call finalize on after we're done.
                for (var entity : result.entities.values()) {
                    if (entity.setConnections(entityPositions))
                        toFinalize.add(entity);
                }
            }

            // Ensure we don't walk any of these positions twice.
            if (!result.visited.isEmpty())
                dirty.removeIf(x -> result.visited.contains(x.pos));
        }

        // Finish changing connections on any updated entities.
        for(var entity : toFinalize)
            entity.finalizeConnections();

        for(var level : levels)
            CALPackets.ServerToClientEvent.CLEAR_INFORMATION.send(level);
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (!dirtyPositions.isEmpty())
            updateDirty();
    }

}
