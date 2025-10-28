package dev.khloeleclair.create.additionallogistics.common.datagen;

import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.common.registries.CALBlocks;
import dev.khloeleclair.create.additionallogistics.common.registries.CALTags;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public final class CALWashingRecipeGen extends WashingRecipeGen {

    public CALWashingRecipeGen(PackOutput output) {
        super(output, CreateAdditionalLogistics.MODID);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {

    }

    GeneratedRecipe FLEXIBLE_SHAFT = create("flexible_shaft", b ->
            b.require(CALTags.CALItemTags.FLEXIBLE_SHAFTS.tag).output(CALBlocks.FLEXIBLE_SHAFT));

}
