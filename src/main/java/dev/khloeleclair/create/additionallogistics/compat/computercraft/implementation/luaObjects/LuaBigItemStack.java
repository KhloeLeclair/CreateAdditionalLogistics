package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.luaObjects;

import com.simibubi.create.content.logistics.BigItemStack;
import dan200.computercraft.api.detail.VanillaDetailRegistries;

import java.util.Map;

public class LuaBigItemStack implements LuaComparable {
    private final BigItemStack stack;

    public LuaBigItemStack(BigItemStack stack) {
        this.stack = stack;
    }

    public Map<String, ?> getSimple() {
        Map<String, Object> simple = VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack.stack);
        simple.put("count", stack.count);
        return simple;
    }

    public Map<String, ?> getDetailed() {
        Map<String, Object> details = VanillaDetailRegistries.ITEM_STACK.getDetails(stack.stack);
        details.put("count", stack.count);
        return details;
    }

    @Override
    public Map<?, ?> getTableRepresentation() {
        return getDetailed();
    }
}