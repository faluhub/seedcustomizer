package me.falu.seedcustomizer.core.owner;

import net.minecraft.world.biome.Biome;

public interface OverridableBiomeArrayOwner {
    void seedcustomizer$addOverride(int x, int z, Biome biome);
    Biome seedcustomizer$getOverride(int x, int z);
}
