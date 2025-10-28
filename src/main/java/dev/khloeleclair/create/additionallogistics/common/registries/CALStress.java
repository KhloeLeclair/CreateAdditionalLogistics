package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.khloeleclair.create.additionallogistics.common.Config;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

public class CALStress {

    static {
        BlockStressValues.IMPACTS.registerProvider(CALStress::getImpact);
    }

    @Nullable
    public static DoubleSupplier getImpact(Block block) {

        if (CALBlocks.PACKAGE_ACCELERATOR.is(block))
            return Config.Common.acceleratorStressImpact::get;

        return null;

    }

    public static void register() { }

}
