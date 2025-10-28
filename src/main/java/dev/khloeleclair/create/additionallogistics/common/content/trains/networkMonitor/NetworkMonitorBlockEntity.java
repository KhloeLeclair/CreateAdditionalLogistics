package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.AbstractEventfulComputerBehavior;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class NetworkMonitorBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<NetworkMonitor> edgePoint;
    public AbstractEventfulComputerBehavior computerBehavior;

    public NetworkMonitorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(edgePoint = new TrackTargetingBehaviour<>(this, NetworkMonitor.NETWORK_MONITOR));
        behaviours.add(computerBehavior = CALComputerCraftProxy.behavior(this));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        computerBehavior.removePeripheral();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (computerBehavior.isPeripheralCap(cap))
            return computerBehavior.getPeripheralCapability();

        return super.getCapability(cap, side);
    }

    public void onTrainArrival(UUID train, UUID station) {
        computerBehavior.queuePositionedEvent("train_arrival", train.toString(), station.toString());
    }

    public void onTrainDeparture(UUID train) {
        computerBehavior.queuePositionedEvent("train_departure", train.toString());
    }

}
