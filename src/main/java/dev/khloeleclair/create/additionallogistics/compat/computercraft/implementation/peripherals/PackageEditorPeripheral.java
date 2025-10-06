package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation.peripherals;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dev.khloeleclair.create.additionallogistics.common.PatternReplacement;
import dev.khloeleclair.create.additionallogistics.common.blockentities.PackageEditorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class PackageEditorPeripheral extends SyncedPeripheral<PackageEditorBlockEntity> {


    public PackageEditorPeripheral(PackageEditorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public void attach(IComputerAccess computer) {
        super.attach(computer);
        blockEntity.setComputerReplacements(null);
    }

    @Override
    public void detach(IComputerAccess computer) {
        super.detach(computer);
        blockEntity.setComputerReplacements(null);
    }

    @Nullable
    public Map<String, Object> getRule(int idx) {
        var replacements = blockEntity.getReplacements();
        if (idx < 0 || idx >= replacements.size())
            return null;

        var replacement = replacements.get(idx);

        Map<String, Object> result = new HashMap<>();

        result.put("pattern", replacement.rawPattern());
        result.put("replacement", replacement.replacement());
        result.put("ignore_case", replacement.isCaseInsensitive());
        result.put("is_stop", replacement.stop());

        return result;
    }

    @LuaFunction(mainThread = true)
    public final Map<Integer, ?> listRules() {
        var replacements = blockEntity.getReplacements();
        Map<Integer, Map<String, Object>> result = new HashMap<>();

        for(int i = 0; i < replacements.size(); i++) {
            result.put(i + 1, getRule(i));
        }

        return result;
    }

    @Nullable
    private String getString(@Nullable Object obj, String field, boolean allowNull) throws LuaException {
        if (obj == null && allowNull)
            return null;
        else if (obj instanceof String str)
            return str;
        throw new LuaException(field + " must be a string" + (allowNull ? " or nil" : ""));
    }

    private boolean getBoolean(@Nullable Object obj, String field) throws LuaException {
        if (obj instanceof Boolean bool)
            return bool;
        else if (obj == null)
            return false;
        throw new LuaException(field + " must be a boolean");
    }

    private PatternReplacement readReplacement(@Nullable Object input) throws LuaException {
        if (!(input instanceof Map<?, ?> ruleTable))
            throw new LuaException("Rule must be a table");

        @Nullable String pattern = null;
        @Nullable String replacement = null;
        boolean ignore_case = false;
        boolean is_stop = false;

        for(var entry : ruleTable.entrySet()) {
            var key = entry.getKey();
            if (key.equals("pattern"))
                pattern = getString(entry.getValue(), "pattern", false);
            else if (key.equals("replacement"))
                replacement = getString(entry.getValue(), "replacement", true);
            else if (key.equals("ignore_case"))
                ignore_case = getBoolean(entry.getValue(), "ignore_case");
            else if (key.equals("is_stop"))
                is_stop = getBoolean(entry.getValue(), "is_stop");
            else
                throw new LuaException("Invalid key in rule '" + key + "'");
        }

        if (pattern == null)
            throw new LuaException("Rule must have a pattern");

        PatternReplacement rule;
        try {
            rule = PatternReplacement.of(pattern, replacement, is_stop, ignore_case);
        } catch(PatternSyntaxException ex) {
            throw new LuaException("Invalid rule: " + ex.getMessage());
        }

        return rule;
    }

    @LuaFunction(mainThread = true)
    public final void resetRules() {
        blockEntity.setComputerReplacements(null);
    }

    @LuaFunction(mainThread = true)
    public final void clearRules() {
        blockEntity.setComputerReplacements(List.of());
    }

    @LuaFunction(mainThread = true)
    public final void setRules(IArguments arguments) throws LuaException {
        List<PatternReplacement> replacements = new ArrayList<>();

        for(int i = 0; i < arguments.count(); i++) {
            replacements.add(readReplacement(arguments.get(i)));
        }

        blockEntity.setComputerReplacements(replacements);
    }

    @LuaFunction(mainThread = true)
    public final void addRule(Object input) throws LuaException {
        var rule = readReplacement(input);
        var replacements = new ArrayList<>(blockEntity.getReplacements());
        replacements.add(rule);
        blockEntity.setComputerReplacements(replacements);
    }

    @LuaFunction(mainThread = true)
    public final void setRule(int index, Object input) throws LuaException {
        var rule = readReplacement(input);
        var replacements = new ArrayList<>(blockEntity.getReplacements());
        var idx = index - 1;
        if (idx < 0 || idx > replacements.size())
            throw new LuaException("Index " + index + " out of range, expected between 1 and " + replacements.size());
        if (idx == replacements.size())
            replacements.add(rule);
        else
            replacements.set(idx, rule);
        blockEntity.setComputerReplacements(replacements);
    }

    @LuaFunction(mainThread = true)
    public final String applyRules(String input) throws LuaException {
        return blockEntity.applyRules(input);
    }

    @Override
    public String getType() {
        return "CreateAdditionalLogistics_PackageEditor";
    }
}
