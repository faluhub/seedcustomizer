package me.falu.seedcustomizer.mixin;

import me.falu.seedcustomizer.core.owner.OverridableBiomeArrayOwner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(BiomeArray.class)
public class BiomeArrayMixin implements OverridableBiomeArrayOwner {
    @Unique private final Map<BlockPos, Biome> overrides = new HashMap<>();

    @Override
    public void seedcustomizer$addOverride(int x, int z, Biome biome) {
        this.overrides.put(new BlockPos(x, 0, z), biome);
    }

    @Override
    public Biome seedcustomizer$getOverride(int x, int z) {
        return this.overrides.get(new BlockPos(x, 0, z));
    }

    @Inject(method = "getBiomeForNoiseGen", at = @At("HEAD"), cancellable = true)
    private void applyOverrides(int biomeX, int biomeY, int biomeZ, CallbackInfoReturnable<Biome> cir) {
        Biome override = this.seedcustomizer$getOverride(biomeX, biomeZ);
        if (override != null) {
            cir.setReturnValue(override);
            cir.cancel();
        }
    }
}
