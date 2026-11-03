package me.zziger.obsoverlay.port;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import java.util.Map;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private boolean vanilla;
    public SettingsScreen(Screen parent) { super(Component.translatable("obs_overlay.settings")); this.parent = parent; }
    @Override protected void init() {
        int left = width / 2 - 155;
        addRenderableWidget(Button.builder(Component.translatable("obs_overlay.mods"), b -> { vanilla = false; rebuildWidgets(); }).bounds(left, 32, 153, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("obs_overlay.vanilla"), b -> { vanilla = true; rebuildWidgets(); }).bounds(left + 157, 32, 153, 20).build());
        if (!vanilla) {
            int row = 0;
            for (ModTarget target : ModTarget.values()) {
                int y = 64 + row++ * 26;
                toggle(target.label + " HUD", OBSOverlay.config.hud, target.id, left, y);
                toggle(target.label + " GUI", OBSOverlay.config.gui, target.id, left + 157, y);
            }
        } else {
            String[] keys = {"debug", "chat", "player_list", "subtitles", "scoreboard", "actionbar", "title", "effects", "main_hud"};
            for (int i = 0; i < keys.length; i++) toggle(Component.translatable("obs_overlay.vanilla." + keys[i]).getString(), OBSOverlay.config.vanilla, keys[i], left + (i % 2) * 157, 60 + (i / 2) * 24);
        }
        addRenderableWidget(Button.builder(label(Component.translatable("obs_overlay.all_screens").getString(), OBSOverlay.config.hideAllScreens), b -> {
            OBSOverlay.config.hideAllScreens = !OBSOverlay.config.hideAllScreens; OBSOverlay.config.save(); b.setMessage(label(Component.translatable("obs_overlay.all_screens").getString(), OBSOverlay.config.hideAllScreens));
        }).bounds(left, height - 56, 153, 20).build());
        addRenderableWidget(Button.builder(label(Component.translatable("obs_overlay.test").getString(), OBSOverlay.config.showTestIcon), b -> {
            OBSOverlay.config.showTestIcon = !OBSOverlay.config.showTestIcon; OBSOverlay.config.save(); b.setMessage(label(Component.translatable("obs_overlay.test").getString(), OBSOverlay.config.showTestIcon));
        }).bounds(left + 157, height - 56, 153, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 28, 200, 20).build());
    }
    private static Component label(String name, boolean value) { return Component.literal(name + ": ").append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF); }
    private void toggle(String name, Map<String, Boolean> values, String key, int x, int y) {
        addRenderableWidget(Button.builder(label(name, Boolean.TRUE.equals(values.get(key))), b -> {
            values.put(key, !Boolean.TRUE.equals(values.get(key))); OBSOverlay.config.save(); b.setMessage(label(name, values.get(key)));
        }).bounds(x, y, 153, 20).tooltip(Tooltip.create(Component.translatable("obs_overlay.toggle.tooltip"))).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        if(OBSOverlay.renderer!=null) context.centeredText(font,OBSOverlay.renderer.presentationDescription(),width/2,height-86,0xFFB0C8D8);
        context.centeredText(font, Component.translatable(OBSOverlay.renderer != null && OBSOverlay.renderer.ready() ? "obs_overlay.ready" : "obs_overlay.unavailable"), width / 2, height - 72, OBSOverlay.renderer != null && OBSOverlay.renderer.ready() ? 0xFF80FF80 : 0xFFFF8080);
    }
    @Override public void onClose() { OBSOverlay.config.save(); minecraft.setScreenAndShow(parent); }
}
