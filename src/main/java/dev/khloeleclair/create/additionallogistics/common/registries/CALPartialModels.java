package dev.khloeleclair.create.additionallogistics.common.registries;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;

public class CALPartialModels {

    private static PartialModel block(String path) { return PartialModel.of(CreateAdditionalLogistics.asResource("block/" + path)); }

    public static final PartialModel CASH_REGISTER_GLASS = block("cash_register/glass");
    public static final PartialModel CASH_REGISTER_SALE_NOTICE = block("cash_register/sale_notice");

    public static void register() { }

}
