package me.falu.seedcustomizer.mixin;

import me.falu.seedcustomizer.core.IntegratedServerWatchdog;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.logging.UncaughtExceptionHandler;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Inject(method = "setupServer", at = @At("TAIL"))
    private void addWatchdog(CallbackInfoReturnable<Boolean> cir) {
        Thread thread2 = new Thread(new IntegratedServerWatchdog((IntegratedServer) (Object) this));
        thread2.setUncaughtExceptionHandler(new UncaughtExceptionHandler(LOGGER));
        thread2.setName("Server Watchdog");
        thread2.setDaemon(true);
        thread2.start();
    }
}
