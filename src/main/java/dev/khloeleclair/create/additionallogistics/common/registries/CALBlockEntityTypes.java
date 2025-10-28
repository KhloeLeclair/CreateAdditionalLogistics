package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.content.kinetics.base.ShaftVisual;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.content.logistics.packager.PackagerVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.FlexibleShaftVisual;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.LazyCogVisual;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.LowEntityKineticBlockEntityRenderer;
import dev.khloeleclair.create.additionallogistics.client.content.logistics.cashRegister.CashRegisterBlockEntityRenderer;
import dev.khloeleclair.create.additionallogistics.client.content.logistics.packageAccelerator.PackageAcceleratorRenderer;
import dev.khloeleclair.create.additionallogistics.client.content.logistics.packageAccelerator.PackageAcceleratorVisual;
import dev.khloeleclair.create.additionallogistics.client.content.trains.networkMonitor.NetworkMonitorRenderer;
import dev.khloeleclair.create.additionallogistics.client.content.trains.networkMonitor.NetworkMonitorVisual;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.cog.LazyCogWheelBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.shaft.LazyShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageAccelerator.PackageAcceleratorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.packageEditor.PackageEditorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.trains.networkMonitor.NetworkMonitorBlockEntity;

public class CALBlockEntityTypes {

    private static final CreateRegistrate REGISTRATE = CreateAdditionalLogistics.REGISTRATE.get();

    // Vertical Belt
    /*public static final BlockEntityEntry<VerticalBeltBlockEntity> VERTICAL_BELT =
            REGISTRATE.blockEntity("vertical_belt", VerticalBeltBlockEntity::new)
                    .validBlocks(
                            CALBlocks.VERTICAL_BELT
                    )
                    .register();*/

    // Flexible Shaft
    public static final BlockEntityEntry<FlexibleShaftBlockEntity> FLEXIBLE_SHAFT =
            REGISTRATE.blockEntity("flexible_shaft", FlexibleShaftBlockEntity::new)
                    .visual(() -> FlexibleShaftVisual::new)
                    .validBlocks(
                            CALBlocks.FLEXIBLE_SHAFT,
                            CALBlocks.ANDESITE_ENCASED_FLEXIBLE_SHAFT, CALBlocks.BRASS_ENCASED_FLEXIBLE_SHAFT,
                            CALBlocks.COPPER_ENCASED_FLEXIBLE_SHAFT,
                            CALBlocks.INDUSTRIAL_IRON_ENCASED_FLEXIBLE_SHAFT, CALBlocks.WEATHERED_IRON_ENCASED_FLEXIBLE_SHAFT
                    )
                    .validBlocks(CALBlocks.DYED_FLEXIBLE_SHAFTS.toArray())
                    .validBlocks(CALBlocks.DYED_ANDESITE_ENCASED_FLEXIBLE_SHAFTS.toArray())
                    .validBlocks(CALBlocks.DYED_BRASS_ENCASED_FLEXIBLE_SHAFTS.toArray())
                    .validBlocks(CALBlocks.DYED_COPPER_ENCASED_FLEXIBLE_SHAFTS.toArray())
                    .validBlocks(CALBlocks.DYED_INDUSTRIAL_IRON_ENCASED_FLEXIBLE_SHAFTS.toArray())
                    .validBlocks(CALBlocks.DYED_WEATHERED_IRON_ENCASED_FLEXIBLE_SHAFTS.toArray())
                    .renderer(() -> LowEntityKineticBlockEntityRenderer::new)
                    .register();

    // Lazy Shaft
    public static final BlockEntityEntry<LazyShaftBlockEntity> LAZY_SHAFT =
            REGISTRATE.blockEntity("lazy_shaft", LazyShaftBlockEntity::new)
                    .visual(() -> ShaftVisual::new)
                    .validBlocks(
                            CALBlocks.LAZY_SHAFT,
                            CALBlocks.ANDESITE_ENCASED_LAZY_SHAFT, CALBlocks.BRASS_ENCASED_LAZY_SHAFT,
                            CALBlocks.COPPER_ENCASED_LAZY_SHAFT, //CALBlocks.RAILWAY_ENCASED_LAZY_SHAFT,
                            CALBlocks.INDUSTRIAL_IRON_ENCASED_LAZY_SHAFT, CALBlocks.WEATHERED_IRON_ENCASED_LAZY_SHAFT
                    )
                    .renderer(() -> LowEntityKineticBlockEntityRenderer::new)
                    .register();

    // Lazy CogWheel
    public static final BlockEntityEntry<LazyCogWheelBlockEntity> LAZY_COGWHEEL =
            REGISTRATE.blockEntity("lazy_cogwheel", LazyCogWheelBlockEntity::new)
                    .visual(() -> LazyCogVisual::small)
                    .validBlocks(
                            CALBlocks.LAZY_COGWHEEL,
                            CALBlocks.ANDESITE_ENCASED_LAZY_COGWHEEL, CALBlocks.BRASS_ENCASED_LAZY_COGWHEEL,
                            CALBlocks.COPPER_ENCASED_LAZY_COGWHEEL,
                            CALBlocks.INDUSTRIAL_IRON_ENCASED_LAZY_COGWHEEL, CALBlocks.WEATHERED_IRON_ENCASED_LAZY_COGWHEEL
                    )
                    .renderer(() -> LowEntityKineticBlockEntityRenderer::new)
                    .register();

    public static final BlockEntityEntry<LazyCogWheelBlockEntity> LAZY_LARGE_COGWHEEL =
            REGISTRATE.blockEntity("lazy_large_cogwheel", LazyCogWheelBlockEntity::new)
                    .visual(() -> LazyCogVisual::large)
                    .validBlocks(
                            CALBlocks.LAZY_LARGE_COGWHEEL,
                            CALBlocks.ANDESITE_ENCASED_LAZY_LARGE_COGWHEEL, CALBlocks.BRASS_ENCASED_LAZY_LARGE_COGWHEEL,
                            CALBlocks.COPPER_ENCASED_LAZY_LARGE_COGWHEEL,
                            CALBlocks.INDUSTRIAL_IRON_ENCASED_LAZY_LARGE_COGWHEEL, CALBlocks.WEATHERED_IRON_ENCASED_LAZY_LARGE_COGWHEEL
                    )
                    .renderer(() -> LowEntityKineticBlockEntityRenderer::new)
                    .register();

    // Network Monitor
    public static final BlockEntityEntry<NetworkMonitorBlockEntity> NETWORK_MONITOR =
            REGISTRATE.blockEntity("network_monitor", NetworkMonitorBlockEntity::new)
                    .visual(() -> NetworkMonitorVisual::new)
                    .validBlocks(CALBlocks.NETWORK_MONITOR)
                    .renderer(() -> NetworkMonitorRenderer::new)
                    .register();

    // Cash Register
    public static final BlockEntityEntry<CashRegisterBlockEntity> CASH_REGISTER =
            REGISTRATE.blockEntity("cash_register", CashRegisterBlockEntity::new)
                    .validBlocks(CALBlocks.CASH_REGISTER)
                    .renderer(() -> CashRegisterBlockEntityRenderer::new)
                    .register();

    // Package Accelerator
    public static final BlockEntityEntry<PackageAcceleratorBlockEntity> PACKAGE_ACCELERATOR =
            REGISTRATE.blockEntity("package_accelerator", PackageAcceleratorBlockEntity::new)
                    .visual(() -> PackageAcceleratorVisual::new, false)
                    .validBlocks(CALBlocks.PACKAGE_ACCELERATOR)
                    .renderer(() -> PackageAcceleratorRenderer::new)
                    .register();

    // Package Editor
    public static final BlockEntityEntry<PackageEditorBlockEntity> PACKAGE_EDITOR =
            REGISTRATE.blockEntity("package_editor", PackageEditorBlockEntity::new)
                    .visual(() -> PackagerVisual::new, true)
                    .validBlocks(CALBlocks.PACKAGE_EDITOR)
                    .renderer(() -> PackagerRenderer::new)
                    .register();


    public static void register() { }

}
