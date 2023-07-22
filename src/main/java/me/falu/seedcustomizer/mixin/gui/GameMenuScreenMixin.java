package me.falu.seedcustomizer.mixin.gui;

import me.falu.seedcustomizer.core.gui.StructureConfigScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.world.gen.chunk.StructuresConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void addCustomButton(CallbackInfo ci) {
        if (this.client == null) { return; }
        this.addButton(new ButtonWidget(this.width / 2 - 100 / 2, 20, 100, 20, new LiteralText("Configs"), b -> this.client.openScreen(new StructureConfigScreen(StructuresConfig.DEFAULT_STRUCTURES))));
    }
}
