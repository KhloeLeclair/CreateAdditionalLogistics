package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.createmod.catnip.platform.CatnipServices;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SyncedPeripheral<T extends SmartBlockEntity> implements IPeripheral {

    protected final T blockEntity;
    private final AtomicInteger computers = new AtomicInteger();

    public SyncedPeripheral(T blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.incrementAndGet();
        updateBlockEntity();
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.decrementAndGet();
        updateBlockEntity();
    }

    private void updateBlockEntity() {
        boolean hasAttachedComputer = computers.get() > 0;

        blockEntity.getBehaviour(ComputerBehaviour.TYPE).setHasAttachedComputer(hasAttachedComputer);
        CatnipServices.NETWORK.sendToAllClients(new AttachedComputerPacket(blockEntity.getBlockPos(), hasAttachedComputer));
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }

}