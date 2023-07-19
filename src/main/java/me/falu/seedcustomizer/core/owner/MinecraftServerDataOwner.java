package me.falu.seedcustomizer.core.owner;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.registry.RegistryTracker;

public interface MinecraftServerDataOwner {
    RegistryTracker seedcustomizer$getRegistryTracker();
    ResourceManager seedcustomizer$getResourceManager();
}
