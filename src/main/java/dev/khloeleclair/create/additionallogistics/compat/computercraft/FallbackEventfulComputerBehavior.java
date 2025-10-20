package dev.khloeleclair.create.additionallogistics.compat.computercraft;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;

public class FallbackEventfulComputerBehavior extends AbstractEventfulComputerBehavior {

    public FallbackEventfulComputerBehavior(SmartBlockEntity te) {
        super(te);
    }

    @Override
    public boolean hasAttachedComputer() {
        return false;
    }

}
