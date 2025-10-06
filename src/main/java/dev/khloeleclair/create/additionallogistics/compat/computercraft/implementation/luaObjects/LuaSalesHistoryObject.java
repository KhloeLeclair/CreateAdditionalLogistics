package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.common.data.SalesHistoryData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class LuaSalesHistoryObject implements LuaComparable {

    private final SalesHistoryData Data;

    public LuaSalesHistoryObject(@Nullable SalesHistoryData data) {
        this.Data = data == null ? SalesHistoryData.EMPTY : data;
    }

    @LuaFunction(mainThread = true)
    public int size() {
        return Data.saleCount();
    }

    @LuaFunction(mainThread = true)
    public final long getStartTime() {
        final var first = Data.firstSale();
        if (first == null)
            return -1;

        return first.timestamp();
    }

    @LuaFunction(mainThread = true)
    public final long getEndTime() {
        final var last = Data.lastSale();
        if (last == null)
            return -1;

        return last.timestamp();
    }

    @LuaFunction(mainThread = true)
    public final LuaSaleObject getSale(int slot) throws LuaException {
        var list = Data.getSales();
        int idx = slot - 1;
        if (idx < 0 || idx >+ list.size())
            throw new LuaException("Sale '" + slot + "' does not exist.");

        return new LuaSaleObject(Data, list.get(idx));
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, LuaSaleObject> list() {
        Map<Integer, LuaSaleObject> result = new HashMap<>();
        var list = Data.getSales();
        final var count = list.size();
        for(int i = 0; i < count; i++) {
            result.put(i + 1, new LuaSaleObject(Data, list.get(i)));
        }
        return result;
    }

    @Override
    public Map<?, ?> getTableRepresentation() {
        Map<String, Object> map = new HashMap<>();
        map.put("size", size());
        map.put("sales", list());
        map.put("start", getStartTime());
        map.put("end", getEndTime());
        return map;
    }
}
