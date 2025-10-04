package dev.khloeleclair.create.additionallogistics.mixin;

import dev.khloeleclair.create.additionallogistics.common.Config;
import dev.khloeleclair.create.additionallogistics.common.SafeRegex;
import net.createmod.catnip.data.Glob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Glob.class)
public class MixinGlob {

    @Inject(
            method = "toRegexPattern",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void CPE$toRegexPattern(String input, CallbackInfoReturnable<String> ci) {
        if (Config.Common.globAllowRegex.get() && input.regionMatches(true, 0, "regex:", 0, 6)) {
            String result = input.substring(6);

            // We need to sanity check this for parity with the original function.
            // If the pattern is invalid, this will throw a PatternSyntaxException.
            // We also do some safety checking to ensure there's no backtracking issues.
            SafeRegex.assertSafe(result, Config.Common.maxStarHeight.get(), Config.Common.maxRepetitions.get(), Config.Common.allowBackrefs.get());

            ci.setReturnValue(result);
        }
    }

}
