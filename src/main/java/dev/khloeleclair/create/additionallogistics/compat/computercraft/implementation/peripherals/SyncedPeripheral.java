package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.createmod.catnip.platform.CatnipServices;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.Map;

public abstract class SyncedPeripheral<T extends SmartBlockEntity> implements IPeripheral {

    protected final T blockEntity;
    private final Map<Integer, WeakReference<IComputerAccess>> computers = new Int2ObjectOpenHashMap<>();

    public SyncedPeripheral(T blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public void attach(IComputerAccess computer) {
        synchronized (computers) {
            computers.put(computer.getID(), new WeakReference<>(computer));
        }
        updateBlockEntity();
    }

    @Override
    public void detach(IComputerAccess computer) {
        synchronized (computers) {
            computers.remove(computer.getID());
        }
        updateBlockEntity();
    }

    public void queuePositionedEvent(String event, Object... arguments) {
        pruneDeadComputers();
        for(var ref : computers.values()) {
            var computer = ref.get();
            if (computer != null) {
                Object[] args = new Object[arguments.length + 1];
                System.arraycopy(arguments, 0, args, 1, arguments.length);
                args[0] = computer.getAttachmentName();

                computer.queueEvent(event, args);
            }
        }
    }

    public void queueEvent(String event, Object... arguments) {
        pruneDeadComputers();
        for(var ref : computers.values()) {
            var computer = ref.get();
            if (computer != null)
                computer.queueEvent(event, arguments);
        }
    }

    private void pruneDeadComputers() {
        synchronized (computers) {
            computers.values().removeIf(ref -> ref.get() == null);
        }
    }

    private void updateBlockEntity() {
        pruneDeadComputers();
        boolean hasAttachedComputer = ! computers.isEmpty();

        blockEntity.getBehaviour(ComputerBehaviour.TYPE).setHasAttachedComputer(hasAttachedComputer);
        CatnipServices.NETWORK.sendToAllClients(new AttachedComputerPacket(blockEntity.getBlockPos(), hasAttachedComputer));
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }

}