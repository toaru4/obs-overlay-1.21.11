package smoke;
import com.sun.jna.Native;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import me.zziger.obsoverlay.port.CaptureExcludedWindow;

/** Creates only hidden native windows: tests exclusion and input styles without changing foreground apps. */
public final class WindowPresentationSmoke {
    public static void run() throws Exception {
        var owner=new AtomicReference<JFrame>();
        SwingUtilities.invokeAndWait(()-> {
            var frame=new JFrame("Hidden overlay regression owner");
            frame.setBounds(-20000,-20000,64,64); frame.addNotify(); owner.set(frame);
        });
        CaptureExcludedWindow overlay=null;
        try {
            overlay=new CaptureExcludedWindow(com.sun.jna.Pointer.nativeValue(Native.getComponentPointer(owner.get())));
            if(overlay.captureAffinity()!=0x11) throw new AssertionError("Windows capture exclusion unavailable");
            if((overlay.extendedStyle()&0x08000020L)!=0x08000020L) throw new AssertionError("Overlay intercepts input or focus");
            long stale=overlay.requestFrame(); overlay.hide();
            overlay.submit(stale,1,1,new int[]{0xFFFFFFFF});
            long epoch=overlay.requestFrame();
            overlay.submit(epoch,1,1,new int[]{0xFF00FF00});
            long deadline=System.nanoTime()+1_000_000_000L;
            while(overlay.copiedFrames()==0 && System.nanoTime()<deadline) Thread.sleep(2);
            if(overlay.copiedFrames()==0) throw new AssertionError("Native software bitmap was not uploaded");
            var bits=CaptureExcludedWindow.class.getDeclaredField("pixels"); bits.setAccessible(true);
            if(((com.sun.jna.Pointer)bits.get(overlay)).getInt(0)!=0xFF00FF00) throw new AssertionError("DIB BGRA pixel mismatch");
            overlay.hide();
            System.out.println("WINDOW_PRESENTATION_PASS: hidden Win32 HWND, capture exclusion, click-through/no-activate flags, software DIB pixels, lifecycle; no live OBS acceptance tested");
        } finally {
            if(overlay!=null) overlay.close();
            SwingUtilities.invokeAndWait(()->owner.get().dispose());
        }
    }
}
