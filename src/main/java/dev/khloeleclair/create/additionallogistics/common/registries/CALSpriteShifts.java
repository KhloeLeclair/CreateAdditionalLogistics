package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;

public class CALSpriteShifts {

    public static final CTSpriteShiftEntry
            COPPER_ENCASED_COGWHEEL_SIDE = vertical("copper_encased_cogwheel_side"),
            COPPER_ENCASED_COGWHEEL_OTHERSIDE = horizontal("copper_encased_cogwheel_side"),

            INDUSTRIAL_IRON_ENCASED_COGWHEEL_SIDE = vertical("industrial_iron_encased_cogwheel_side"),
            INDUSTRIAL_IRON_ENCASED_COGWHEEL_OTHERSIDE = horizontal("industrial_iron_encased_cogwheel_side");


    private static CTSpriteShiftEntry horizontal(String name) {
        return getCT(AllCTTypes.HORIZONTAL, name);
    }

    private static CTSpriteShiftEntry vertical(String name) {
        return getCT(AllCTTypes.VERTICAL, name);
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, CreateAdditionalLogistics.asResource("block/" + blockTextureName),
                CreateAdditionalLogistics.asResource("block/" + connectedTextureName + "_connected"));
    }

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return getCT(type, blockTextureName, blockTextureName);
    }

}
