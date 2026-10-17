package me.zziger.obsoverlay.port;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Retain the callback strongly for the complete native hook lifetime. */
public final class NativeSwapHook {
    interface Kernel extends StdCallLibrary {
        Kernel INSTANCE = Native.load("kernel32", Kernel.class);
        Pointer GetModuleHandleA(String name);
        Pointer GetProcAddress(Pointer module, String name);
    }
    interface Swap extends StdCallLibrary.StdCallCallback { boolean invoke(Pointer dc); }
    interface MinHook extends StdCallLibrary {
        int MH_Initialize();
        int MH_CreateHook(Pointer target, Swap callback, PointerByReference original);
        int MH_EnableHook(Pointer target);
        int MH_DisableHook(Pointer target);
        int MH_RemoveHook(Pointer target);
    }
    private static MinHook library;
    private static Swap callback;
    private static Pointer target;
    private static boolean installed;
    public static void install(Runnable draw) {
        if (installed) return;
        if (!System.getProperty("os.name").startsWith("Windows")) throw new IllegalStateException("OBS Overlay requires Windows and OpenGL");
        if (!System.getProperty("os.arch").equals("amd64") && !System.getProperty("os.arch").equals("x86_64")) throw new IllegalStateException("This build requires Windows x64");
        try {
            Path directory = Files.createTempDirectory("obs-overlay-native-");
            Path dll = directory.resolve("MinHook.dll");
            try (var stream = NativeSwapHook.class.getResourceAsStream("/lib/MinHook.x64.dll")) {
                if (stream == null) throw new IllegalStateException("Missing MinHook.x64.dll");
                Files.copy(stream, dll, StandardCopyOption.REPLACE_EXISTING);
            }
            dll.toFile().deleteOnExit(); directory.toFile().deleteOnExit();
            library = Native.load(dll.toString(), MinHook.class);
            int init = library.MH_Initialize();
            if (init != 0 && init != 1) check(init, "initialize");
            target = Kernel.INSTANCE.GetProcAddress(Kernel.INSTANCE.GetModuleHandleA("opengl32.dll"), "wglSwapBuffers");
            if (target == null) throw new IllegalStateException("wglSwapBuffers unavailable");
            PointerByReference original = new PointerByReference();
            callback = dc -> {
                try { draw.run(); }
                catch (Throwable e) { OBSOverlay.LOGGER.error("Local overlay composition failed", e); }
                return (Boolean) Function.getFunction(original.getValue(), Function.ALT_CONVENTION).invoke(Boolean.class, new Object[]{dc});
            };
            check(library.MH_CreateHook(target, callback, original), "create");
            check(library.MH_EnableHook(target), "enable");
            installed = true;
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot extract MinHook", e); }
    }
    private static void check(int result, String operation) { if (result != 0) throw new IllegalStateException("MinHook " + operation + " failed: " + result); }
    public static void uninstall() {
        if (installed) { library.MH_DisableHook(target); library.MH_RemoveHook(target); installed = false; callback = null; }
    }
}
