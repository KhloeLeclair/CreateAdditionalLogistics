package dev.khloeleclair.create.additionallogistics.common;

import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageEditor.PackageEditorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class Events {

    @EventBusSubscriber
    public static class ModBusEvents {

        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            CashRegisterBlockEntity.registerCapabilities(event);
            //PackageAcceleratorBlockEntity.registerCapabilities(event);
            PackageEditorBlockEntity.registerCapabilities(event);
            NetworkMonitorBlockEntity.registerCapabilities(event);
        }

    }

}
