package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import com.simibubi.create.compat.computercraft.implementation.peripherals.StockTickerPeripheral;
import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.registries.CALDataComponents;
import dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects.LuaSalesHistoryObject;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class CashRegisterPeripheral extends StockTickerPeripheral { //} SyncedPeripheral<CashRegisterBlockEntity> {

    public CashRegisterPeripheral(CashRegisterBlockEntity blockEntity) {
        super(blockEntity);
    }

    @LuaFunction(mainThread = true)
    public final LuaSalesHistoryObject getSales() {
        var ledger = blockEntity instanceof CashRegisterBlockEntity be ? be.getLedger() : ItemStack.EMPTY;
        return new LuaSalesHistoryObject(ledger.get(CALDataComponents.SALES_HISTORY));
    }

    @LuaFunction(mainThread = true)
    public final Map<String, ?> getLedger() throws LuaException {
        var ledger = blockEntity instanceof CashRegisterBlockEntity be ? be.getLedger() : ItemStack.EMPTY;
        return VanillaDetailRegistries.ITEM_STACK.getDetails(ledger);
    }

    @Override
    public String getType() {
        return "CreateAdditionalLogistics_CashRegister";
    }

    @Override
    public @Nullable Object getTarget() {
        return blockEntity;
    }

}
