package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class CALComputerUtil {

    public static Map<String, Object> getPosition(BlockPos pos, ResourceKey<Level> dimension) {
        var result = getPosition(pos);
        result.put("dimension", dimension.location().toString());
        return result;
    }

    public static Map<String, Object> getPosition(BlockPos pos) {
        Map<String, Object> result = new HashMap<>();
        result.put("x", pos.getX());
        result.put("y", pos.getY());
        result.put("z", pos.getZ());
        return result;
    }

}