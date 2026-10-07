package com.flintfix.client.mixin;

import com.flintfix.client.FlintFixClient;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No Weather: the client world reports clear skies, which removes rain and
 * snow, their sounds and splashes, and the darker rainy sky. Only the client's
 * own world is affected; the server and other players see the real weather.
 */
@Mixin(Level.class)
public abstract class WorldWeatherMixin {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void flintfix$hideRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (flintfix$weatherHidden()) cir.setReturnValue(0.0f);
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void flintfix$hideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (flintfix$weatherHidden()) cir.setReturnValue(0.0f);
    }

    @Unique
    private boolean flintfix$weatherHidden() {
        return ((Level) (Object) this).isClientSide()
            && FlintFixClient.CONFIG != null && FlintFixClient.CONFIG.hideWeatherEnabled;
    }
}
