package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import com.simibubi.create.content.logistics.packager.PackagerVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.renderers.CashRegisterBlockEntityRenderer;
import dev.khloeleclair.create.additionallogistics.client.renderers.PackageAcceleratorRenderer;
import dev.khloeleclair.create.additionallogistics.client.visuals.PackageAcceleratorVisual;
import dev.khloeleclair.create.additionallogistics.common.blockentities.CashRegisterBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blockentities.PackageAcceleratorBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.blockentities.PackageEditorBlockEntity;

public class CALBlockEntityTypes {

    private static final CreateRegistrate REGISTRATE = CreateAdditionalLogistics.REGISTRATE.get();

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
