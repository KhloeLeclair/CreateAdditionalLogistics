package dev.khloeleclair.create.additionallogistics.compat.computercraft.implementation;

import com.simibubi.create.content.logistics.box.PackageItem;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.createmod.catnip.data.Glob;

import javax.annotation.Nullable;
import java.util.regex.PatternSyntaxException;

public class PackageApi implements ILuaAPI {

    @Override
    public String[] getNames() {
        return new String[0];
    }

    //@Override
    public @Nullable String getModuleName() {
        return CreateAdditionalLogistics.MODID + ".package_addresses";
    }

    @LuaFunction
    public String toRegexPattern(String input) throws LuaException {
        try {
            return Glob.toRegexPattern(input);
        } catch(PatternSyntaxException ex) {
            throw new LuaException(ex.getMessage());
        }
    }

    @LuaFunction
    public boolean matches(String box, String address) {
        return PackageItem.matchAddress(box, address);
    }

}
