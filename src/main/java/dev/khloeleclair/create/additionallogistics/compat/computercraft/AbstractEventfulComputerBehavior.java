package dev.khloeleclair.create.additionallogistics.compat.computercraft;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

import java.util.function.Supplier;

public class AbstractEventfulComputerBehavior extends AbstractComputerBehaviour {

    public AbstractEventfulComputerBehavior(SmartBlockEntity te) {
        super(te);
    }

    public void runIfInstalled(Supplier<Runnable> runnable) {
        Mods.COMPUTERCRAFT.executeIfInstalled(runnable);
    }

    public void queueEvent(String event, Object... arguments) {
        // Do nothing~
    }

    public void queuePositionedEvent(String event, Object... arguments) {
        // Do nothing~
    }

}
