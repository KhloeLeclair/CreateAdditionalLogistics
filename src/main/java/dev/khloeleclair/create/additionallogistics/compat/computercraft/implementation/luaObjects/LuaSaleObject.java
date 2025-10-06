package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.logistics.BigItemStack;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.data.SalesHistoryData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class LuaSaleObject implements LuaComparable {

    private final SalesHistoryData Data;
    private final SalesHistoryData.Sale Sale;

    public LuaSaleObject(SalesHistoryData parent, SalesHistoryData.Sale sale) {
        Data = parent;
        Sale = sale;
    }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, ?> getPlayer() {
        var player = Data.getPlayers().get(Sale.player());
        final var server = CreateAdditionalLogistics.getServer();
        if (server == null || server.getProfileCache() == null || player == null)
            return null;

        var cached = server.getProfileCache().get(player);
        if (cached.isEmpty())
            return null;

        Map<String, String> result = new HashMap<>();

        result.put("id", cached.get().getId().toString());
        result.put("name", cached.get().getName());

        return result;
    }

    private Map<Integer, ?> hydrateItems(Collection<Map.Entry<String, Integer>> entries) {
        Map<Integer, Object> result = new HashMap<>();
        int i = 0;
        for(var entry : entries) {
            var item = Data.getItem(entry.getKey());
            @Nullable LuaBigItemStack stack;
            if (item == null)
                stack = null;
            else
                stack = new LuaBigItemStack(new BigItemStack(item.getDefaultInstance(), entry.getValue()));

            i++;
            result.put(i, stack == null ? null : stack.getSimple());
        }

        return result;
    }

    @LuaFunction(mainThread = true)
    public final long getTimestamp() {
        return Sale.timestamp();
    }

    @LuaFunction(mainThread = true)
    public final int paymentCount() {
        final var payment = Sale.payment();
        return payment == null ? 0 : payment.size();
    }

    @LuaFunction(mainThread = true)
    public final int purchasedCount() {
        final var purchased = Sale.purchase();
        return purchased == null ? 0 : purchased.size();
    }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<Integer, ?> payment() {
        final var source = Sale.payment();
        if (source == null || source.isEmpty())
            return null;

        return hydrateItems(source.entrySet());
    }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, ?> getPaymentDetail(int slot) throws LuaException {
        final var source = Sale.payment();
        if (source == null)
            return null;

        int idx = slot - 1;
        if (idx < 0 || idx >= source.size())
            throw new LuaException("Slot " + slot + " out of range, available slots between 1 and " + (source.size() + 1));

        int i = 0;
        for(var entry : source.keySet()) {
            if (i == idx) {
                var item = Data.getItem(entry);
                if (item == null)
                    return null;
                return new LuaBigItemStack(new BigItemStack(item.getDefaultInstance(), source.get(entry))).getDetailed();
            }
            i++;
        }

        throw new LuaException("Runtime error");
    }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<Integer, ?> purchased() {
        final var purchased = Sale.purchase();
        if (purchased == null || purchased.isEmpty())
            return null;

        return hydrateItems(purchased.entrySet());
    }

    @LuaFunction(mainThread = true)
    @Nullable
    public final Map<String, ?> getPurchasedDetail(int slot) throws LuaException {
        final var source = Sale.purchase();
        if (source == null)
            return null;

        int idx = slot - 1;
        if (idx < 0 || idx >= source.size())
            throw new LuaException("Slot " + slot + " out of range, available slots between 1 and " + (source.size() + 1));

        int i = 0;
        for(var entry : source.keySet()) {
            if (i == idx) {
                var item = Data.getItem(entry);
                if (item == null)
                    return null;
                return new LuaBigItemStack(new BigItemStack(item.getDefaultInstance(), source.get(entry))).getDetailed();
            }
            i++;
        }

        throw new LuaException("Runtime error");
    }

    @Override
    public Map<?, ?> getTableRepresentation() {
        Map<String, Object> map = new HashMap<>();
        map.put("player", getPlayer());
        map.put("timestamp", getTimestamp());
        map.put("payment", payment());
        map.put("purchasedItems", purchased());
        return map;
    }
}
