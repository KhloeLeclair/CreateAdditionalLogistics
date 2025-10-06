package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation;

import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.khloeleclair.create.additionallogistics.common.blockentities.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blockentities.PackageEditorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.data.CustomComponents;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaSalesHistoryObject;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.CashRegisterPeripheral;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.PackageEditorPeripheral;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Supplier;

public class CALComputerBehavior extends AbstractComputerBehaviour {

    IPeripheral peripheral;
    Supplier<IPeripheral> peripheralSupplier;

    public CALComputerBehavior(SmartBlockEntity sbe) {
        super(sbe);
        peripheralSupplier = getPeripheralFor(sbe);
    }

    public static Supplier<IPeripheral> getPeripheralFor(SmartBlockEntity sbe) {
        if (sbe instanceof CashRegisterBlockEntity cr)
            return () -> new CashRegisterPeripheral(cr);
        else if (sbe instanceof PackageEditorBlockEntity pe)
            return () -> new PackageEditorPeripheral(pe);

        throw new IllegalArgumentException("No peripheral available for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(sbe.getType()));
    }

    public static void registerItemDetailProviders() {
        VanillaDetailRegistries.ITEM_STACK.addProvider((out, stack) -> {
            if (stack.is(CALItems.SALES_LEDGER)) {
                var obj = new LuaSalesHistoryObject(stack.get(CustomComponents.SALES_HISTORY));
                out.put("sales", obj);
            }
        });
    }

    public IPeripheral getPeripheralCapability() {
        if (peripheral == null)
            peripheral = peripheralSupplier.get();
        return peripheral;
    }

    public void removePeripheral() {
        if (peripheral != null)
            getWorld().invalidateCapabilities(blockEntity.getBlockPos());
    }

}
