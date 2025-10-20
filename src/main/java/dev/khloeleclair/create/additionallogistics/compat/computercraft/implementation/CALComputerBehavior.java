package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesHistoryData;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageEditor.PackageEditorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataComponents;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.AbstractEventfulComputerBehavior;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaSaleObject;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaSalesHistoryObject;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.CashRegisterPeripheral;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.NetworkMonitorPeripheral;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.PackageEditorPeripheral;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals.SyncedPeripheral;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CALComputerBehavior extends AbstractEventfulComputerBehavior {

    @Nullable
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
        else if (sbe instanceof NetworkMonitorBlockEntity nm)
            return () -> new NetworkMonitorPeripheral(nm);

        throw new IllegalArgumentException("No peripheral available for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(sbe.getType()));
    }

    public static void registerItemDetailProviders() {
        VanillaDetailRegistries.ITEM_STACK.addProvider((out, stack) -> {
            if (stack.is(CALItems.SALES_LEDGER)) {
                var obj = new LuaSalesHistoryObject(stack.get(CALDataComponents.SALES_HISTORY));
                out.put("sales", obj);
            }
        });
    }

    public void updateArguments(Object... arguments) {
        for(int i = 0; i < arguments.length; i++) {
            var arg = arguments[i];
            if (arg instanceof ItemStack is)
                arguments[i] = VanillaDetailRegistries.ITEM_STACK.getDetails(is);
            else if (arg instanceof SalesHistoryData history)
                arguments[i] = new LuaSalesHistoryObject(history);
            else if (arg instanceof Pair<?,?> tuple) {
                var first = tuple.getFirst();
                var second = tuple.getSecond();

                if (first instanceof SalesHistoryData history && second instanceof SalesHistoryData.Sale sale)
                    arguments[i] = new LuaSaleObject(history, sale);
            }
        }
    }

    @Override
    public void queueEvent(String event, Object... arguments) {
        updateArguments(arguments);
        if (peripheral instanceof SyncedPeripheral<?> sp)
            sp.queueEvent(event, arguments);
    }

    public void queuePositionedEvent(String event, Object... arguments) {
        updateArguments(arguments);
        if (peripheral instanceof SyncedPeripheral<?> sp)
            sp.queuePositionedEvent(event, arguments);
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
