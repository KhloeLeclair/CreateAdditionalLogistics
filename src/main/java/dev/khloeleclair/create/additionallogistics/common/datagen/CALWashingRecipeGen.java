package dev.khloeleclair.create.additionallogistics.common.datagen;

import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import dev.khloeleclair.create.additionallogistics.common.registries.CALTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import java.util.concurrent.CompletableFuture;

public final class CALWashingRecipeGen extends WashingRecipeGen {

    public CALWashingRecipeGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateAdditionalLogistics.MODID);
    }

    GeneratedRecipe FLEXIBLE_SHAFT = create("flexible_shaft", b ->
            b.require(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag).output(CALBlocks.FLEXIBLE_SHAFT));

}
