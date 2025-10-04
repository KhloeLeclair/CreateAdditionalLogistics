package dev.khloeleclair.create.additionallogistics.common;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class CALLang extends Lang {

    public static String key(String path) {
        return CreateAdditionalLogistics.MODID + "." + path;
    }

    public static LangBuilder builder() {
        return new LangBuilder(CreateAdditionalLogistics.MODID);
    }

    public static LangBuilder blockName(BlockState state) {
        return builder().add(state.getBlock().getName());
    }

    public static LangBuilder itemName(ItemStack stack) {
        return builder().add(stack.getHoverName().copy());
    }

    public static LangBuilder number(double d) {
        return builder().text(LangNumberFormat.format(d));
    }

    public static LangBuilder translate(String key, Object... args) {
        return builder().translate(key, args);
    }

    public static LangBuilder text(String text) {
        return builder().text(text);
    }

}
