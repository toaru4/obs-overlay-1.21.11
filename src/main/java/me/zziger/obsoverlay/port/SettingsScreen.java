package me.zziger.obsoverlay.port;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import java.util.Map;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private boolean vanilla;
    public SettingsScreen(Screen parent) { super(Text.translatable("obs_overlay.settings")); this.parent = parent; }
    @Override protected void init() {
        int left = width / 2 - 155;
        addDrawableChild(ButtonWidget.builder(Text.translatable("obs_overlay.mods"), b -> { vanilla = false; clearAndInit(); }).dimensions(left, 32, 153, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("obs_overlay.vanilla"), b -> { vanilla = true; clearAndInit(); }).dimensions(left + 157, 32, 153, 20).build());
        if (!vanilla) {
            int row = 0;
            for (ModTarget target : ModTarget.values()) {
                int y = 64 + row++ * 26;
                toggle(target.label + " HUD", OBSOverlay.config.hud, target.id, left, y);
                toggle(target.label + " GUI", OBSOverlay.config.gui, target.id, left + 157, y);
            }
        } else {
            String[] keys = {"debug", "chat", "player_list", "subtitles", "scoreboard", "actionbar", "title", "effects", "main_hud"};
            for (int i = 0; i < keys.length; i++) toggle(Text.translatable("obs_overlay.vanilla." + keys[i]).getString(), OBSOverlay.config.vanilla, keys[i], left + (i % 2) * 157, 60 + (i / 2) * 24);
        }
        addDrawableChild(ButtonWidget.builder(label(Text.translatable("obs_overlay.all_screens").getString(), OBSOverlay.config.hideAllScreens), b -> {
            OBSOverlay.config.hideAllScreens = !OBSOverlay.config.hideAllScreens; OBSOverlay.config.save(); b.setMessage(label(Text.translatable("obs_overlay.all_screens").getString(), OBSOverlay.config.hideAllScreens));
        }).dimensions(left, height - 56, 153, 20).build());
        addDrawableChild(ButtonWidget.builder(label(Text.translatable("obs_overlay.test").getString(), OBSOverlay.config.showTestIcon), b -> {
            OBSOverlay.config.showTestIcon = !OBSOverlay.config.showTestIcon; OBSOverlay.config.save(); b.setMessage(label(Text.translatable("obs_overlay.test").getString(), OBSOverlay.config.showTestIcon));
        }).dimensions(left + 157, height - 56, 153, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close()).dimensions(width / 2 - 100, height - 28, 200, 20).build());
    }
    private static Text label(String name, boolean value) { return Text.literal(name + ": ").append(value ? ScreenTexts.ON : ScreenTexts.OFF); }
    private void toggle(String name, Map<String, Boolean> values, String key, int x, int y) {
        addDrawableChild(ButtonWidget.builder(label(name, Boolean.TRUE.equals(values.get(key))), b -> {
            values.put(key, !Boolean.TRUE.equals(values.get(key))); OBSOverlay.config.save(); b.setMessage(label(name, values.get(key)));
        }).dimensions(x, y, 153, 20).tooltip(Tooltip.of(Text.translatable("obs_overlay.toggle.tooltip"))).build());
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable(OBSOverlay.renderer != null && OBSOverlay.renderer.ready() ? "obs_overlay.ready" : "obs_overlay.unavailable"), width / 2, height - 72, OBSOverlay.renderer != null && OBSOverlay.renderer.ready() ? 0xFF80FF80 : 0xFFFF8080);
    }
    @Override public void close() { OBSOverlay.config.save(); client.setScreen(parent); }
}
