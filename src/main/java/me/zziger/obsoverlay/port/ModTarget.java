package me.zziger.obsoverlay.port;

public enum ModTarget {
    METEOR("meteor", "Meteor Client", "meteordevelopment.meteorclient."),
    RUSHERHACK("rusherhack", "RusherHack", "org.rusherhack.", "rusherhack."),
    XAERO("xaero", "Xaero's Maps", "xaero."),
    MALILIB("malilib", "MaLiLib", "fi.dy.masa.malilib.");
    public final String id;
    public final String label;
    private final String[] packages;
    private static final ClassValue<java.util.Optional<ModTarget>> CLASS_OWNERS = new ClassValue<>() {
        @Override protected java.util.Optional<ModTarget> computeValue(Class<?> type) {
            ModTarget target = ofClassName(type.getName());
            if (target == null) {
                try {
                    var source = type.getProtectionDomain().getCodeSource();
                    if (source != null) target = ofSource(source.getLocation().toString());
                } catch (SecurityException ignored) { }
            }
            return java.util.Optional.ofNullable(target);
        }
    };
    ModTarget(String id, String label, String... packages) { this.id = id; this.label = label; this.packages = packages; }
    public static ModTarget ofClassName(String name) {
        for (ModTarget target : values()) for (String prefix : target.packages) if (name.startsWith(prefix)) return target;
        return null;
    }
    public static ModTarget ofScreen(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            ModTarget target = ofClass(c);
            if (target != null) return target;
        }
        return null;
    }
    public static ModTarget ofClass(Class<?> type) { return CLASS_OWNERS.get(type).orElse(null); }
    static ModTarget ofSource(String source) {
        String name = source.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.contains("rusherhack") && name.endsWith(".jar")) return RUSHERHACK;
        return null;
    }
}
