package dev.khloeleclair.create.additionallogistics.common.blockentities;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.ISetLazyTickCounter;
import dev.khloeleclair.create.additionallogistics.common.blocks.PackageAcceleratorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PackageAcceleratorBlockEntity extends KineticBlockEntity {

    public PackageAcceleratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    @Override
    public float calculateStressApplied() {
        float impact = (float) Config.Common.acceleratorStressImpact.getAsDouble();
        lastStressApplied = impact;
        return impact;
    }

    @Override
    public void tick() {
        super.tick();

        var facing = getBlockState().getOptionalValue(PackageAcceleratorBlock.FACING);
        if (facing.isEmpty() || level == null)
            return;

        float speed = Math.abs(getSpeed());
        int ticks = 1;
        if (speed < IRotate.SpeedLevel.MEDIUM.getSpeedValue())
            return;

        if (speed >= 256)
            ticks = 8;
        else if (speed >= 128)
            ticks = 4;
        else if (speed >= 64)
            ticks = 2;

        if (level.getBlockEntity(worldPosition.relative(facing.get())) instanceof PackagerBlockEntity packager) {
            var tc = (ISetLazyTickCounter) (Object) packager;
            tc.setLazyTickCounter(tc.getLazyTickCounter() - ticks);

            if (packager.animationTicks > 1)
                packager.animationTicks = Math.max(1, packager.animationTicks - ticks);

            if (packager.buttonCooldown > 0)
                packager.buttonCooldown = Math.max(0, packager.buttonCooldown - (2 * ticks));
        }
    }
}
