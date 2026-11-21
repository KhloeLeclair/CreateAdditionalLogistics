package dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible;

import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.schematic.nbt.PartialSafeNBT;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotationIndicatorParticleData;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLowEntityKineticBlockEntity;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class FlexibleShaftBlockEntity extends AbstractLowEntityKineticBlockEntity implements PartialSafeNBT, TransformableBlockEntity {

    protected final byte[] sideActive;
    protected boolean validateSides;

    @Nullable
    protected Direction particleSide;
    protected int particleCountdown;


    public FlexibleShaftBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        sideActive = new byte[Iterate.directions.length];
        Arrays.fill(sideActive, (byte)0);
    }

    @Override
    protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(compound, registries, clientPacket);

        Arrays.fill(sideActive, (byte)0);
        if (compound.contains("Sides", CompoundTag.TAG_BYTE_ARRAY)) {
            var array = compound.getByteArray("Sides");
            System.arraycopy(array, 0, sideActive, 0, Math.min(array.length, sideActive.length));
        }
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        tag.putByteArray("Sides", Arrays.copyOf(sideActive, sideActive.length));
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putByteArray("Sides", Arrays.copyOf(sideActive, sideActive.length));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level.isClientSide) {
            if (validateSides) {
                validateSides = false;
                checkSides();
            }

            if (particleCountdown > 0) {
                particleCountdown--;
                if (particleCountdown == 0)
                    spawnSideParticles();
            }
        }
    }

    protected void queueSideParticles(Direction side) {
        particleSide = side;
        particleCountdown = 2;
    }

    protected void spawnSideParticles() {
        var side = particleSide;
        if (side == null)
            return;

        particleSide = null;

        if (!(level instanceof ServerLevel sl))
            return;

        float speed = getSpeed() * getRotationSpeedModifier(side);
        if (speed == 0f)
            return;

        float step = side.getAxisDirection().getStep() * 0.5f;
        var axis = side.getAxis();
        Vec3 position = worldPosition.getCenter().add(
            axis == Direction.Axis.X ? step : 0,
            axis == Direction.Axis.Y ? step : 0,
            axis == Direction.Axis.Z ? step : 0
        );

        var speedLevel = IRotate.SpeedLevel.of(speed);

        int color = speedLevel.getColor();
        int particleSpeed = speedLevel.getParticleSpeed();
        particleSpeed *= Math.signum(speed);

        var particleData = new RotationIndicatorParticleData(color, particleSpeed, .75f, .65f, 5, side.getAxis());

        sl.sendParticles(particleData, position.x, position.y, position.z, 20, 0, 0, 0, 1);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    @Override
    public boolean isValidBlockState(BlockState state) {
        validateSides = true;
        return super.isValidBlockState(state);
    }

    public boolean shouldBeActive() {
        for(byte side : sideActive) {
            if (side != 0)
                return true;
        }
        return false;
    }

    protected void checkSides() {
        boolean changed = false;
        boolean detached = false;
        var state = getBlockState();
        for(Direction side : Iterate.directions) {
            int index = side.ordinal();
            if (state.getValue(AbstractFlexibleShaftBlock.SIDES[index]) && sideActive[index] != 0) {
                if (hasConnection(side) && !detached) {
                    detached = true;
                    detachKinetics();
                }

                sideActive[index] = 0;
                changed = true;
            }
        }

        if (!shouldBeActive())
            deactivateSelf();
        else if (changed) {
            notifyUpdate();
            updateSpeed = true;
        }
    }

    public void deactivateSelf() {
        notifyConnectedToValidate();
        level.setBlockAndUpdate(worldPosition, getBlockState().setValue(AbstractFlexibleShaftBlock.ACTIVE, false));
        AbstractLowEntityKineticBlockEntity.markDirty(level, worldPosition);
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

    public void setSideUnsafe(Direction side, byte value) {
        sideActive[side.ordinal()] = value;
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
        queueSideParticles(side);

        var state = getBlockState();
        if (state.getBlock() instanceof EncasedBlock) {
            final boolean side_state = value == 0;

            if (state.getValue(AbstractFlexibleShaftBlock.SIDES[index]) != side_state) {
                level.setBlockAndUpdate(worldPosition, state.setValue(AbstractFlexibleShaftBlock.SIDES[index], side_state));
            }
        }

        updateSpeed = true;
        notifyUpdate();
        return true;
    }

    public byte getSide(Direction side) {
        return sideActive[side.ordinal()];
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        return 1f * sideActive[face.ordinal()];
    }

    @Override
    public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
        BlockPos relative = other.getBlockPos().subtract(worldPosition);
        final var dir = Direction.fromDelta(relative.getX(), relative.getY(), relative.getZ());
        if (dir != null && sideActive[dir.ordinal()] != 0 && otherState.getBlock() instanceof IRotate rot && rot.hasShaftTowards(level, other.getBlockPos(), otherState, dir.getOpposite())) {
            return false;
        }

        return super.isCustomConnection(other, state, otherState);
    }


    @Override
    public void destroy() {
        super.destroy();
        notifyConnectedToValidate();
    }

    @Override
    public void transform(BlockEntity blockEntity, StructureTransform transform) {

        byte[] sides = sideActive;

        // A side's mode is the sign of its rotation relative to the positive end of that face's
        // axis, so it has to be adjusted whenever a transform moves that positive end to the
        // negative one. Blueprints only ever use the Y axis; anything else keeps the previous
        // behaviour so that moving contraptions is unaffected.
        final boolean yAxis = transform.rotationAxis == null || transform.rotationAxis == Direction.Axis.Y;

        if (transform.mirror != null && transform.mirror != Mirror.NONE) {
            byte[] newSides = new byte[sides.length];

            for(Direction dir : Iterate.directions) {
                Direction newDir = transform.mirror.mirror(dir);
                newSides[newDir.ordinal()] = (byte) (sides[dir.ordinal()] * sideModeSign(dir, newDir, transform.mirror, null, yAxis));
            }

            sides = newSides;
        }

        if (transform.rotation != null && transform.rotation != Rotation.NONE) {
            byte[] newSides = new byte[sides.length];

            for(Direction dir : Iterate.directions) {
                Direction newDir = transform.rotation.rotate(dir);
                newSides[newDir.ordinal()] = (byte) (sides[dir.ordinal()] * sideModeSign(dir, newDir, null, transform.rotation, yAxis));
            }

            sides = newSides;
        }

        System.arraycopy(sides, 0, sideActive, 0, sides.length);

    }

    /// Determine the sign factor applied to a side's mode when a structure transform remaps it.
    ///
    /// A mirror flips handedness and therefore negates. Regardless, a mode's sign is relative to the
    /// positive end of its face's axis, so it must also be negated whenever the transform carries that
    /// positive end onto the negative one.
    private static int sideModeSign(Direction dir, Direction newDir, @Nullable Mirror mirror, @Nullable Rotation rotation, boolean yAxis) {
        if (!yAxis)
            return 1;

        Direction mapped = Direction.fromAxisAndDirection(dir.getAxis(), Direction.AxisDirection.POSITIVE);
        if (mirror != null)
            mapped = mirror.mirror(mapped);
        if (rotation != null)
            mapped = rotation.rotate(mapped);

        Direction anchor = Direction.fromAxisAndDirection(newDir.getAxis(), Direction.AxisDirection.POSITIVE);
        final int handedness = mirror == null ? 1 : -1;

        return handedness * (mapped == anchor ? 1 : -1);
    }
}
