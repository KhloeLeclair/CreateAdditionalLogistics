package dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlockEntityTypes;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.CALComputerCraftProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NetworkMonitorBlockEntity extends SmartBlockEntity {

    public TrackTargetingBehaviour<NetworkMonitor> edgePoint;
    public AbstractComputerBehaviour computerBehavior;

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

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (Mods.COMPUTERCRAFT.isLoaded()) {
            event.registerBlockEntity(
                    PeripheralCapability.get(),
                    CALBlockEntityTypes.NETWORK_MONITOR.get(),
                    (be, context) -> be.computerBehavior.getPeripheralCapability()
            );
        }
    }


    @Nullable
    public NetworkMonitor getMonitor() {
        return edgePoint.getEdgePoint();
    }

}
