package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.AttachedComputerPacket;
import com.simibubi.create.compat.computercraft.implementation.ComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.NotAttachedException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.createmod.catnip.platform.CatnipServices;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class SyncedPeripheral<T extends SmartBlockEntity> implements IPeripheral {

    protected final T blockEntity;
    private final Map<Integer, IComputerAccess> computers = new Int2ObjectOpenHashMap<>();

    public SyncedPeripheral(T blockEntity) {
        this.blockEntity = blockEntity;
    }

    @Override
    public void attach(IComputerAccess computer) {
        synchronized (computers) {
            computers.put(computer.getID(), computer);
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
        if (computers.isEmpty())
            return;

        synchronized (computers) {
            var it = computers.values().iterator();
            while(it.hasNext()) {
                var computer = it.next();
                try {
                    String name = computer.getAttachmentName();
                    Object[] args = new Object[arguments.length + 1];
                    System.arraycopy(arguments, 0, args, 1, arguments.length);
                    args[0] = name;

                    computer.queueEvent(event, args);
                } catch(NotAttachedException ex) {
                    it.remove();
                }
            }
        }
    }

    public void queueEvent(String event, Object... arguments) {
        synchronized (computers) {
            var it = computers.values().iterator();
            while(it.hasNext()) {
                var computer = it.next();
                try {
                    computer.queueEvent(event, arguments);
                } catch(NotAttachedException ex) {
                    it.remove();
                }
            }
        }
    }

    private void updateBlockEntity() {
        boolean hasAttachedComputer = ! computers.isEmpty();

        blockEntity.getBehaviour(ComputerBehaviour.TYPE).setHasAttachedComputer(hasAttachedComputer);
        CatnipServices.NETWORK.sendToAllClients(new AttachedComputerPacket(blockEntity.getBlockPos(), hasAttachedComputer));
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }

}