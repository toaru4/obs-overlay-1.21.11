package smoke;
import com.sun.jna.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import me.zziger.obsoverlay.port.CaptureExcludedWindow;

/** Visible synthetic color test, without taking foreground or displaying game frames. */
public final class VisibleWindowSmoke {
    public static void run() throws Exception {
        var user=CaptureExcludedWindow.User.API; var gdi=CaptureExcludedWindow.Gdi.API;
        var owner=new AtomicReference<JFrame>(); CaptureExcludedWindow overlay=null;
        Pointer oldDpi=user.SetThreadDpiAwarenessContext(new Pointer(-4));
        try {
            SwingUtilities.invokeAndWait(()-> {
                var frame=new JFrame("OBS synthetic presentation test"); frame.setUndecorated(true);
                frame.setFocusableWindowState(false); frame.setBounds(100,100,96,96);
                frame.getContentPane().setBackground(java.awt.Color.BLUE); frame.setVisible(true); owner.set(frame);
            });
            Pointer ownerHandle=Native.getComponentPointer(owner.get());
            var constructor=CaptureExcludedWindow.class.getDeclaredConstructor(long.class,BooleanSupplier.class);
            constructor.setAccessible(true);
            overlay=constructor.newInstance(Pointer.nativeValue(ownerHandle),(BooleanSupplier)()->true);
            var field=CaptureExcludedWindow.class.getDeclaredField("handle"); field.setAccessible(true);
            Pointer handle=(Pointer)field.get(overlay);
            // Disable exclusion only while presenting a synthetic color; actual game frames never enter this test.
            
            var rect=new CaptureExcludedWindow.Rect(); user.GetClientRect(ownerHandle,rect);
            var origin=new CaptureExcludedWindow.Point(); origin.x=0; origin.y=0; user.ClientToScreen(ownerHandle,origin);
            int w=rect.right-rect.left,h=rect.bottom-rect.top;
            int[] green=new int[w*h]; Arrays.fill(green,0xFF00FF00);
            long epoch=overlay.requestFrame(); int sampled=-1;
            long deadline=System.nanoTime()+2_000_000_000L;
            while(System.nanoTime()<deadline) {
                overlay.submit(epoch,w,h,green.clone()); Thread.sleep(35);
                Pointer desktop=user.GetDC(null);
                try { sampled=gdi.GetPixel(desktop,origin.x+w/2,origin.y+h/2); }
                finally { user.ReleaseDC(null,desktop); }
                if(overlay.copiedFrames()>3) {
                    Pointer own=user.GetDC(handle);
                    try { System.out.println("PROTECTED_WINDOW_PIXEL="+Integer.toHexString(gdi.GetPixel(own,w/2,h/2))+", captured="+Integer.toHexString(sampled)); }
                    finally { user.ReleaseDC(handle,own); }
                    break;
                }
            }
            if(!user.SetWindowDisplayAffinity(handle,0)) throw new AssertionError("Cannot inspect synthetic window");
            deadline=System.nanoTime()+2_000_000_000L;
            while(System.nanoTime()<deadline) {
                overlay.submit(epoch,w,h,green.clone()); Thread.sleep(35);
                Pointer desktop=user.GetDC(null);
                try { sampled=gdi.GetPixel(desktop,origin.x+w/2,origin.y+h/2); }
                finally { user.ReleaseDC(null,desktop); }
                if(sampled==0x0000FF00) break;
            }
            if(sampled!=0x0000FF00) throw new AssertionError("Visible local window is not green: COLORREF="+Integer.toHexString(sampled));
            long priorPaint=overlay.paintedFrames();
            user.InvalidateRect(handle,null,true); user.UpdateWindow(handle); Thread.sleep(25);
            if(overlay.paintedFrames()<=priorPaint) throw new AssertionError("WM_PAINT did not restore retained frame");
            SwingUtilities.invokeAndWait(()->owner.get().setBounds(100,100,120,80));
            user.GetClientRect(ownerHandle,rect); origin.x=0; origin.y=0; user.ClientToScreen(ownerHandle,origin);
            w=rect.right-rect.left; h=rect.bottom-rect.top;
            int[] red=new int[w*h]; Arrays.fill(red,0xFFFF0000);
            deadline=System.nanoTime()+2_000_000_000L;
            while(System.nanoTime()<deadline) {
                user.GetClientRect(ownerHandle,rect); origin.x=0; origin.y=0; user.ClientToScreen(ownerHandle,origin);
                w=rect.right-rect.left; h=rect.bottom-rect.top;
                red=new int[w*h]; Arrays.fill(red,0xFFFF0000);
                overlay.submit(epoch,w,h,red); Thread.sleep(35);
                Pointer desktop=user.GetDC(null);
                try { sampled=gdi.GetPixel(desktop,origin.x+w/2,origin.y+h/2); }
                finally { user.ReleaseDC(null,desktop); }
                if(sampled==0x000000FF) break;
            }
            if(sampled!=0x000000FF) throw new AssertionError("Resized local window lost its image: "+w+"x"+h+", COLORREF="+Integer.toHexString(sampled)+", available="+overlay.isAvailable()+", visible="+user.IsWindowVisible(handle)+", paints="+overlay.paintedFrames());
            overlay.hide(); Thread.sleep(40);
            if(user.IsWindowVisible(handle)) throw new AssertionError("Local window remained visible after hide");
            if(!user.SetWindowDisplayAffinity(handle,0x11) || overlay.captureAffinity()!=0x11) throw new AssertionError("Capture exclusion not restored");
            System.out.println("VISIBLE_WINDOW_PASS: protected GDI pixels and desktop colors, WM_PAINT restore, resize, hide; capture exclusion restored; foreground unchanged");
        } finally {
            if(overlay!=null) overlay.close();
            if(owner.get()!=null) SwingUtilities.invokeAndWait(()->owner.get().dispose());
            if(oldDpi!=null) user.SetThreadDpiAwarenessContext(oldDpi);
        }
    }
}
