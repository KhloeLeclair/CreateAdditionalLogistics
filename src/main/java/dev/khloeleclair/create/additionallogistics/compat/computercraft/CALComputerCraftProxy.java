package dev.khloeleclair.create.additionallogistics.compat.computercraft;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.FallbackComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.CALComputerBehavior;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class CALComputerCraftProxy {

    private static final Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> fallbackFactory = FallbackComputerBehaviour::new;
    @Nullable
    private static Function<SmartBlockEntity, ? extends AbstractComputerBehaviour> computerFactory;

    public static void register() {
        Mods.COMPUTERCRAFT.executeIfInstalled(() -> CALComputerCraftProxy::registerWithDependency);
    }

    private static void registerWithDependency() {
        computerFactory = CALComputerBehavior::new;
        CALComputerBehavior.registerItemDetailProviders();
    }

    public static AbstractComputerBehaviour behavior(SmartBlockEntity sbe) {
        if (computerFactory == null)
            return fallbackFactory.apply(sbe);
        return computerFactory.apply(sbe);
    }

}
