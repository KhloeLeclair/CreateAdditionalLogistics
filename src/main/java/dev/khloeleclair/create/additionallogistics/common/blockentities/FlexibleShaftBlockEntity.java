package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blocks.FlexibleShaftBlock;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Iterate;
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

public class FlexibleShaftBlockEntity extends SplitShaftBlockEntity {

    @Nullable
    private List<BlockPos> connections;

    private final byte[] sideActive;
    protected boolean checkInvalid;

    public FlexibleShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        sideActive = new byte[Iterate.directions.length];
        Arrays.fill(sideActive, (byte)0);
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

        Arrays.fill(sideActive, (byte)0);
        if (compound.contains("Sides", CompoundTag.TAG_BYTE_ARRAY)) {
            var array = compound.getByteArray("Sides");
            System.arraycopy(array, 0, sideActive, 0, Math.min(array.length, sideActive.length));
        }
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putByteArray("Sides", sideActive);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putByteArray("Sides", sideActive);

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
    protected boolean isNoisy() {
        return false;
    }

    public void removeInvalidConnections() {
        boolean changed = false;
        if (connections != null) {
            var iterator = connections.iterator();
            while(iterator.hasNext()) {
                var pos = iterator.next();
                var relative = worldPosition.offset(pos);

                if (!level.isLoaded(relative) || level.getBlockEntity(relative) instanceof FlexibleShaftBlockEntity fsb)
                    continue;

                iterator.remove();
                changed = true;
            }
        }
        if (changed)
            notifyUpdate();
    }


    public boolean shouldBeActive() {
        for(byte side : sideActive) {
            if (side != 0)
                return true;
        }
        return false;
    }

    public void notifyConnectedToValidate() {
        if (connections != null && level != null)
            for(BlockPos pos : connections) {
                BlockPos relative = worldPosition.offset(pos);
                if (level.isLoaded(relative) && level.getBlockEntity(relative) instanceof FlexibleShaftBlockEntity fsb)
                    fsb.checkInvalid = true;
            }
    }

    public void deactivateSelf() {
        notifyConnectedToValidate();
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(FlexibleShaftBlock.ACTIVE, false));
    }

    public boolean hasConnection(Direction side) {
        if (level == null || sideActive[side.ordinal()] == 0 || !hasNetwork())
            return false;

        var pos = worldPosition.relative(side);
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IRotate rot))
            return false;

        return rot.hasShaftTowards(level, pos, state, side.getOpposite());
    }

    public void toggleSide(Direction side) {
        byte value = sideActive[side.ordinal()];
        if (value == 0)
            value = 1;
        else if (value == 1)
            value = -1;
        else
            value = 0;

        setSide(side, value);
    }

    public boolean setSide(Direction side, byte value) {
        int index = side.ordinal();
        byte old_value = sideActive[index];
        if (old_value == value)
            return false;

        if (old_value != 0 && hasConnection(side)) {
            if (hasNetwork())
                getOrCreateNetwork().remove(this);
            detachKinetics();
            removeSource();
        }

        sideActive[index] = value;

        updateSpeed = true;
        notifyUpdate();
        return true;
    }

    public byte getSide(Direction side) {
        return sideActive[side.ordinal()];
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        return state.getValue(FlexibleShaftBlock.ACTIVE);
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        if (connections != null)
            connections.forEach(p -> neighbours.add(worldPosition.offset(p)));
        return super.addPropagationLocations(block, state, neighbours);
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (target instanceof FlexibleShaftBlockEntity && connections != null && connections.contains(target.getBlockPos().subtract(worldPosition)))
            return 1;

        return 0;
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        return 1f * sideActive[face.ordinal()];
    }

    protected void setConnections(List<BlockPos> newConnections) {
        int index = -1;
        while(index++ < newConnections.size()) {
            if (worldPosition.equals(newConnections.get(index)))
                break;
        }

        if (index == -1)
            return;

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

    @Override
    public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        BlockPos relative = other.getBlockPos().subtract(worldPosition);
        final var dir = Direction.fromDelta(relative.getX(), relative.getY(), relative.getZ());
        if (dir != null && sideActive[dir.ordinal()] != 0 && otherState.getBlock() instanceof IRotate rot && rot.hasShaftTowards(level, other.getBlockPos(), otherState, dir.getOpposite())) {
            return false;
        }
        return connections != null && connections.contains(relative);
    }

    @Override
    public void destroy() {
        super.destroy();
        notifyConnectedToValidate();
    }

    public record WalkResult(Set<BlockPos> visited, List<BlockPos> entityPositions, Set<FlexibleShaftBlockEntity> entities) {

        public static WalkResult EMPTY = new WalkResult(Collections.emptySet(), List.of(), Collections.emptySet());

    }

    /// Discover all connected flexible shaft blocks and entities.
    public static WalkResult walkShafts(Level level, BlockPos pos) {

        if(!(level.getBlockState(pos).getBlock() instanceof FlexibleShaftBlock block))
            return WalkResult.EMPTY;

        Set<BlockPos> visited = new ObjectOpenHashSet<>();
        List<BlockPos> entityPositions = new ArrayList<>();
        Set<FlexibleShaftBlockEntity> entities = new ObjectOpenHashSet<>();
        List<BlockPos> queue = new LinkedList<>();

        visited.add(pos);
        if (level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb) {
            entityPositions.add(pos);
            entities.add(fsb);
        }

        addToDirtyList(level, pos, block, visited, queue);

        while (!queue.isEmpty()) {
            BlockPos qpos = queue.removeFirst();
            if (level.isLoaded(qpos) && level.getBlockState(qpos).getBlock() instanceof FlexibleShaftBlock qblock) {
                if (level.getBlockEntity(qpos) instanceof FlexibleShaftBlockEntity qfsb) {
                    entityPositions.add(qpos);
                    entities.add(qfsb);
                }

                addToDirtyList(level, qpos, qblock, visited, queue);
            }
        }

        return new WalkResult(visited, entityPositions, entities);
    }

    private static void addToDirtyList(Level world, BlockPos pos, FlexibleShaftBlock block, Set<BlockPos> visited, List<BlockPos> queue) {
        for (Direction direction : Direction.values()) {
            BlockPos p = pos.relative(direction);
            if (!visited.contains(p) && block.connectsTo(world.getBlockState(p)) && ! queue.contains(p)) {
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
            var result = walkShafts(pos.level, pos.pos);

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
