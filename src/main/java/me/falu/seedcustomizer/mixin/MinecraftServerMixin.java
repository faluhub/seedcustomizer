package me.falu.seedcustomizer.mixin;

import me.falu.seedcustomizer.core.owner.MinecraftServerDataOwner;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.RegistryTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerDataOwner {
    @Shadow @Final protected RegistryTracker.Modifiable dimensionTracker;
    @Shadow private ServerResourceManager serverResourceManager;

    @Override
    public RegistryTracker seedcustomizer$getRegistryTracker() {
        return this.dimensionTracker;
    }

    @Override
    public ResourceManager seedcustomizer$getResourceManager() {
        return this.serverResourceManager.getResourceManager();
    }
}
