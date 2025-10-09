package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blocks.FlexibleShaftBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

public class FlexibleShaftBlockEntity extends AbstractLowEntityKineticBlockEntity {

    protected final byte[] sideActive;
    protected boolean validateSides;

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
        tag.putByteArray("Sides", sideActive);
    }

    @Override
    protected void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(compound, registries, clientPacket);
        compound.putByteArray("Sides", sideActive);
    }

    @Override
    public void tick() {
        super.tick();

        if (validateSides && !level.isClientSide) {
            validateSides = false;
            checkSides();
        }
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
            if (state.getValue(FlexibleShaftBlock.SIDES[index]) && sideActive[index] != 0) {
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

}
