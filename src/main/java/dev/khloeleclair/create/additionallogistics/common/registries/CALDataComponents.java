package dev.khloeleclair.create.additionallogistics.common.registries;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesHistoryData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public class CALDataComponents {

    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateAdditionalLogistics.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SalesHistoryData>> SALES_HISTORY = REGISTRAR.registerComponentType(
            "sales_history",
            builder -> builder.persistent(SalesHistoryData.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> CASH_REGISTER_POS = REGISTRAR.registerComponentType(
            "cash_register_pos",
            builder -> builder.persistent(BlockPos.CODEC)
    );

    public static void register(IEventBus modBus) {
        REGISTRAR.register(modBus);
    }

}
