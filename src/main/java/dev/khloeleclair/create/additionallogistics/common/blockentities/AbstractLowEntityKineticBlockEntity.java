package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.blocks.AbstractLowEntityKineticBlock;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
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
        if (compound.contains("Connected", CompoundTag.TAG_LONG_ARRAY)) {
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

        if (!level.isClientSide) {
            if (lazyDirty) {
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
            connections.forEach(p -> neighbours.add(worldPosition.offset(p)));

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
        BlockPos relative = other.getBlockPos().subtract(worldPosition);
        return connections != null && connections.contains(relative);
    }

    protected boolean setConnections(List<BlockPos> newConnections) {
        int index = 0;
        for (; index < newConnections.size(); index++) {
            if (worldPosition.equals(newConnections.get(index)))
                break;
        }

        if (index >= newConnections.size())
            return false;

        int previous = index - 1;
        int next = index + 1;

        if (connections == null)
            connections = new ArrayList<>();

        @Nullable BlockPos prevPos = previous == -1 ? null : newConnections.get(previous).subtract(worldPosition);
        @Nullable BlockPos nextPos = next >= newConnections.size() ? null : newConnections.get(next).subtract(worldPosition);

        int count = (prevPos == null ? 0 : 1) + (nextPos == null ? 0 : 1);

        boolean contains_previous = prevPos == null || connections.contains(prevPos);
        boolean contains_next = nextPos == null || connections.contains(nextPos);

        // Nothing changed.
        if (contains_previous && contains_next && connections.size() == count)
            return false;

        // Something changed. Did the fire nation attack?
        detachKinetics();

        pendingConnections = new ArrayList<>();
        if (prevPos != null)
            pendingConnections.add(prevPos);
        if (nextPos != null)
            pendingConnections.add(nextPos);

        return true;
    }

    protected void finalizeConnections() {
        if (pendingConnections == null)
            return;

        connections = pendingConnections;
        pendingConnections = null;

        notifyUpdate();
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
        if (level.isClientSide)
            return;

        dirtyPositions.add(new LevelBlockPos(level, pos));
    }

    public static void updateDirty() {
        LinkedList<LevelBlockPos> dirty = new LinkedList<>(dirtyPositions);
        dirtyPositions.clear();

        List<AbstractLowEntityKineticBlockEntity> toFinalize = new ArrayList<>();

        while(!dirty.isEmpty()) {
            LevelBlockPos pos = dirty.removeFirst();
            var result = walkBlocks(pos.level, pos.pos);

            CreateAdditionalLogistics.LOGGER.debug("Updating dirty lazy network from {}, found {} connected lazy blocks with {} entities.", pos.pos, result.visited.size(), result.entities.size());

            if (!result.entities.isEmpty()) {
                // Make a list.
                List<BlockPos> entityPositions = result.entities.navigableKeySet().stream().toList();

                // Update every entity.
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
    }

    public static void onTick(ServerTickEvent.Post event) {
        if (!dirtyPositions.isEmpty())
            updateDirty();
    }

}
