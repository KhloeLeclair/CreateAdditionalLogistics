package dev.khloeleclair.create.additionallogistics.common.registries;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesHistoryData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CALDataComponents {

    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CreateAdditionalLogistics.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SalesHistoryData>> SALES_HISTORY = REGISTRAR.registerComponentType(
            "sales_history",
            builder -> builder.persistent(SalesHistoryData.CODEC)
    );

    public static void register(IEventBus modBus) {
        REGISTRAR.register(modBus);
    }

}
