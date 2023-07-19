package me.falu.seedcustomizer.mixin.access;

import net.minecraft.world.SaveProperties;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelProperties.class)
public interface LevelPropertiesAccessor extends SaveProperties {
    @Accessor("field_25425") @Mutable void setGeneratorOptions(GeneratorOptions options);
}
