package me.zziger.obsoverlay.port;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OverlayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("obs_overlay-1.21.11.json");
    public Map<String, Boolean> hud = new LinkedHashMap<>();
    public Map<String, Boolean> gui = new LinkedHashMap<>();
    public Map<String, Boolean> vanilla = new LinkedHashMap<>();
    public boolean hideAllScreens;
    public boolean showTestIcon;
    public OverlayConfig() {
        for (ModTarget target : ModTarget.values()) { hud.put(target.id, false); gui.put(target.id, false); }
        for (String key : new String[]{"debug", "chat", "player_list", "subtitles", "scoreboard", "actionbar", "title", "effects", "main_hud"}) vanilla.put(key, key.equals("debug"));
    }
    public boolean enabled(ModTarget target, boolean screen) { return Boolean.TRUE.equals((screen ? gui : hud).get(target.id)); }
    public boolean anyHud() { return hud.containsValue(true); }
    public static OverlayConfig load() {
        if (Files.isRegularFile(PATH)) {
            try (var reader = Files.newBufferedReader(PATH)) {
                OverlayConfig value = GSON.fromJson(reader, OverlayConfig.class);
                if (value != null && value.hud != null && value.gui != null && value.vanilla != null) return value;
            } catch (IOException | RuntimeException e) { OBSOverlay.LOGGER.error("Cannot read {}", PATH, e); }
        }
        return new OverlayConfig();
    }
    public void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Path temp = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(this));
            Files.move(temp, PATH, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) { OBSOverlay.LOGGER.error("Cannot save {}", PATH, e); }
    }
}
