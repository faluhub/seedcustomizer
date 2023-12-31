package me.falu.seedcustomizer.mixin.access;

import com.google.common.collect.ImmutableMap;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructuresConfig.class)
public interface StructuresConfigAccessor {
    @Accessor("DEFAULT_STRUCTURES")
    @Mutable
    static void setDefaultStructures(ImmutableMap<StructureFeature<?>, StructureConfig> value) {}
}
