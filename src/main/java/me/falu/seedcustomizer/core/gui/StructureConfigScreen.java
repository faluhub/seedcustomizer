package me.falu.seedcustomizer.core.gui;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.systems.RenderSystem;
import me.falu.seedcustomizer.core.gui.widget.FreeSizeButtonWidget;
import me.falu.seedcustomizer.mixin.access.StructuresConfigAccessor;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class StructureConfigScreen extends Screen {
    private final Map<StructureFeature<?>, StructureConfig> configMap;
    private final List<StructureConfigWidget> widgets = new ArrayList<>();
    private StructureConfigWidget activeWidget;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;

    public StructureConfigScreen(Map<StructureFeature<?>, StructureConfig> configMap) {
        super(new LiteralText("Structures"));
        this.configMap = new HashMap<>(configMap);
    }

    @Override
    protected void init() {
        this.widgets.clear();
        this.configMap.forEach((k, v) -> {
            StructureConfigWidget widget = new StructureConfigWidget(k, v);
            this.widgets.add(widget);
        });
        this.activeWidget = this.widgets.get(0);
        this.activeWidget.createElements();
        this.prevButton = this.addButton(new FreeSizeButtonWidget(
                0,
                this.height / 4,
                40,
                this.height / 2,
                new LiteralText("<"),
                b -> {
                    if (this.activeWidget != null) {
                        int currentIndex = this.widgets.indexOf(this.activeWidget);
                        if (currentIndex - 1 >= 0 && currentIndex - 1 < this.widgets.size() - 1) {
                            this.activeWidget = this.widgets.get(currentIndex - 1);
                            this.nextButton.active = true;
                            if (currentIndex - 1 == 0) {
                                b.active = false;
                            }
                        }
                    }
                }
        ));
        this.prevButton.active = false;
        this.nextButton = this.addButton(new FreeSizeButtonWidget(
                this.width - 20 * 2,
                this.height / 4,
                40,
                this.height / 2,
                new LiteralText(">"),
                b -> {
                    if (this.activeWidget != null) {
                        int currentIndex = this.widgets.indexOf(this.activeWidget);
                        if (currentIndex + 1 < this.widgets.size()) {
                            this.activeWidget = this.widgets.get(currentIndex + 1);
                            this.prevButton.active = true;
                            if (currentIndex + 1 == this.widgets.size() - 1) {
                                b.active = false;
                            }
                        }
                    }
                }
        ));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.activeWidget.render(matrices, mouseX, mouseY, delta);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        StructuresConfigAccessor.setDefaultStructures(ImmutableMap.copyOf(this.configMap));
        super.onClose();
    }

    private class StructureConfigWidget {
        private final StructureFeature<?> key;
        private StructureConfig value;
        private final List<TextFieldWidget> fields;

        public StructureConfigWidget(StructureFeature<?> key, StructureConfig value) {
            this.key = key;
            this.value = value;
            this.fields = new ArrayList<>();
        }

        private Consumer<String> createListener(TextFieldWidget widget) {
            return s -> {
                try {
                    int ignored = Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                    widget.eraseCharacters(widget.getCursor() - 1);
                }
            };
        }

        private void createElements() {
            this.fields.clear();
            int fieldWidth = 100;

            TextFieldWidget spacingField = new TextFieldWidget(
                    StructureConfigScreen.this.textRenderer,
                    StructureConfigScreen.this.width / 3 - fieldWidth / 2,
                    StructureConfigScreen.this.height - 60,
                    fieldWidth,
                    40,
                    new LiteralText("Spacing")
            );
            spacingField.setText(String.valueOf(this.value.getSpacing()));
//            spacingField.setChangedListener(this.createListener(spacingField));
//            spacingField.setSelected(true);
            this.fields.add(StructureConfigScreen.this.addChild(spacingField));

            TextFieldWidget separationField = new TextFieldWidget(
                    StructureConfigScreen.this.textRenderer,
                    StructureConfigScreen.this.width / 2 - fieldWidth / 2,
                    StructureConfigScreen.this.height - 60,
                    fieldWidth,
                    40,
                    new LiteralText("Separation")
            );
            separationField.setText(String.valueOf(this.value.getSeparation()));
//            separationField.setChangedListener(this.createListener(separationField));
//            separationField.setEditable(true);
            this.fields.add(StructureConfigScreen.this.addChild(separationField));

            TextFieldWidget saltField = new TextFieldWidget(
                    StructureConfigScreen.this.textRenderer,
                    StructureConfigScreen.this.width - StructureConfigScreen.this.width / 3 - fieldWidth / 2,
                    StructureConfigScreen.this.height - 60,
                    fieldWidth,
                    40,
                    new LiteralText("Salt")
            );
            saltField.setText(String.valueOf(this.value.getSalt()));
//            saltField.setChangedListener(this.createListener(saltField));
//            saltField.setEditable(true);
            this.fields.add(StructureConfigScreen.this.addChild(saltField));

            StructureConfigScreen.this.addButton(new ButtonWidget(
                    StructureConfigScreen.this.width / 2 - 100 / 2,
                    StructureConfigScreen.this.height / 2,
                    100,
                    20,
                    new LiteralText("Save"),
                    b -> {
                        try {
                            this.value = new StructureConfig(
                                    Integer.parseInt(spacingField.getText()),
                                    Integer.parseInt(separationField.getText()),
                                    Integer.parseInt(saltField.getText())
                            );
                            StructureConfigScreen.this.configMap.put(this.key, this.value);
                        } catch (NumberFormatException ignored) {}
                    }
            ));
        }

        @SuppressWarnings("deprecation")
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            for (TextFieldWidget field : this.fields) {
                field.render(matrices, mouseX, mouseY, delta);
                StructureConfigScreen.this.drawTextWithShadow(
                        matrices,
                        StructureConfigScreen.this.textRenderer,
                        field.getMessage(),
                        field.x,
                        field.y - StructureConfigScreen.this.textRenderer.fontHeight,
                        0xFFFFFF
                );
            }
            float scale = 2.5F;
            RenderSystem.pushMatrix();
            RenderSystem.scalef(scale, scale, 1.0F);
            StructureConfigScreen.this.drawCenteredString(
                    matrices,
                    StructureConfigScreen.this.textRenderer,
                    this.key.getName(),
                    (int) (StructureConfigScreen.this.width / 2 / scale),
                    (int) (StructureConfigScreen.this.height / 4 / scale),
                    0xFFFFFF
            );
            RenderSystem.popMatrix();
        }
    }
}
