package me.falu.seedcustomizer.mixin;

import me.falu.seedcustomizer.core.ChunkRegenerator;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "processF3", at = @At("TAIL"), cancellable = true)
    private void customShortcut(int key, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && key == GLFW.GLFW_KEY_V) {
            if (this.client.player != null && this.client.getServer() != null) {
                ServerWorld world = this.client.getServer().getWorld(this.client.player.world.getRegistryKey());
                if (world != null) {
                    ServerPlayerEntity player = this.client.getServer().getPlayerManager().getPlayer(this.client.player.getUuid());
                    if (player != null) {
                        world.getChunkManager().mainThreadExecutor.execute(new ChunkRegenerator(world, player));
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
