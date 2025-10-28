package dev.khloeleclair.create.additionallogistics.common.content.kinetics.verticalBelt;

import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class EncasedVerticalBeltBlock extends AbstractVerticalBeltBlock implements EncasedBlock {

    private final Supplier<Block> casing;

    public EncasedVerticalBeltBlock(Properties properties, Supplier<Block> casing) {
        super(properties);
        this.casing = casing;
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

}
