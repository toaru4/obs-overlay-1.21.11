package me.zziger.obsoverlay.port;

import com.sun.jna.*;
import com.sun.jna.ptr.*;
import com.sun.jna.win32.StdCallLibrary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Software-only Win32 layered window: no additional graphics swapchain, input or activation. */
public final class CaptureExcludedWindow implements AutoCloseable {
    public interface User extends StdCallLibrary {
        User API=Native.load("user32",User.class);
        short RegisterClassExW(WindowClass type);
        boolean UnregisterClassW(WString name,Pointer instance);
        long DefWindowProcW(Pointer window,int message,long wParam,long lParam);
        Pointer CreateWindowExW(int ex,WString type,WString title,int style,int x,int y,int width,int height,Pointer owner,Pointer menu,Pointer instance,Pointer parameter);
        boolean DestroyWindow(Pointer window);
        boolean SetWindowDisplayAffinity(Pointer window,int affinity);
        boolean GetWindowDisplayAffinity(Pointer window,IntByReference affinity);
        long GetWindowLongPtrW(Pointer window,int index);
        boolean GetClientRect(Pointer window,Rect rect);
        boolean ClientToScreen(Pointer window,Point point);
        Pointer GetForegroundWindow();
        int GetWindowThreadProcessId(Pointer window,IntByReference process);
        boolean IsIconic(Pointer window);
        boolean IsWindowVisible(Pointer window);
        boolean SetWindowPos(Pointer window,Pointer after,int x,int y,int width,int height,int flags);
        boolean ShowWindow(Pointer window,int command);
        boolean SetLayeredWindowAttributes(Pointer window,int key,byte alpha,int flags);
        Pointer GetDC(Pointer window);
        int ReleaseDC(Pointer window,Pointer dc);
        boolean PeekMessageW(Message message,Pointer window,int min,int max,int flags);
        boolean TranslateMessage(Message message);
        long DispatchMessageW(Message message);
        Pointer SetThreadDpiAwarenessContext(Pointer context);
    }
    public interface WindowProcedure extends StdCallLibrary.StdCallCallback { long invoke(Pointer window,int message,long wParam,long lParam); }
    @Structure.FieldOrder({"size","style","procedure","classExtra","windowExtra","instance","icon","cursor","background","menu","name","smallIcon"})
    public static class WindowClass extends Structure {
        public int size,style; public WindowProcedure procedure; public int classExtra,windowExtra;
        public Pointer instance,icon,cursor,background; public WString menu,name; public Pointer smallIcon;
    }
    public interface Gdi extends StdCallLibrary {
        Gdi API=Native.load("gdi32",Gdi.class);
        Pointer CreateCompatibleDC(Pointer dc);
        boolean DeleteDC(Pointer dc);
        Pointer CreateDIBSection(Pointer dc,BitmapHeader info,int usage,PointerByReference pixels,Pointer section,int offset);
        Pointer SelectObject(Pointer dc,Pointer object);
        boolean DeleteObject(Pointer object);
        boolean BitBlt(Pointer target,int x,int y,int width,int height,Pointer source,int sourceX,int sourceY,int operation);
    }
    @Structure.FieldOrder({"left","top","right","bottom"})
    public static class Rect extends Structure { public int left,top,right,bottom; }
    @Structure.FieldOrder({"x","y"})
    public static class Point extends Structure { public int x,y; }
    @Structure.FieldOrder({"size","width","height","planes","bits","compression","imageSize","xDensity","yDensity","used","important"})
    public static class BitmapHeader extends Structure {
        public int size=40,width,height;
        public short planes=1,bits=32;
        public int compression,imageSize,xDensity,yDensity,used,important;
    }
    @Structure.FieldOrder({"window","message","wParam","lParam","time","point","reserved"})
    public static class Message extends Structure {
        public Pointer window; public int message; public long wParam,lParam; public int time; public Point point=new Point(); public int reserved;
    }
    private record Frame(int width,int height,int[] pixels,long time,long generation) {}
    private static final int EXCLUDE=0x11;
    private static final int STYLES=0x00080000|0x20|0x08000000|0x80;
    private final Pointer owner;
    private final WindowProcedure procedure=(h,m,w,l)->User.API.DefWindowProcW(h,m,w,l);
    private final AtomicReference<Frame> pending=new AtomicReference<>();
    private final CountDownLatch initialized=new CountDownLatch(1);
    private final Thread worker;
    private volatile Pointer handle;
    private volatile boolean closed,requested;
    private volatile long generation;
    private volatile Throwable failure;
    private volatile long copiedFrames;
    // The following resources belong exclusively to the window thread.
    private Pointer dc,bitmap,previousBitmap,pixels;
    private int bitmapWidth,bitmapHeight;
    private Frame displayed;
    private boolean visible;

    public CaptureExcludedWindow(long ownerHandle) {
        if(!System.getProperty("os.name").startsWith("Windows") || Native.POINTER_SIZE!=8) throw new IllegalStateException("Vulkan local window requires Windows x64");
        owner=new Pointer(ownerHandle);
        var process=new IntByReference();
        if(ownerHandle==0 || User.API.GetWindowThreadProcessId(owner,process)==0 || process.getValue()!=ProcessHandle.current().pid()) throw new IllegalStateException("Invalid game window owner");
        worker=Thread.ofPlatform().daemon(true).name("OBS local-only Win32 presentation").unstarted(this::run);
        worker.start();
        try {
            if(!initialized.await(5,TimeUnit.SECONDS)) { close(); throw new IllegalStateException("Local window initialization timed out"); }
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); close(); throw new IllegalStateException("Local window interrupted",e); }
        if(failure!=null) { close(); throw new IllegalStateException("Local window initialization failed",failure); }
    }
    private void run() {
        Pointer oldDpi=null;
        WString className=new WString("ObsOverlayLocal"+Long.toHexString(System.nanoTime()));
        boolean registered=false;
        try {
            oldDpi=User.API.SetThreadDpiAwarenessContext(new Pointer(-4));
            var type=new WindowClass(); type.size=type.size(); type.procedure=procedure; type.name=className;
            if(User.API.RegisterClassExW(type)==0) throw new IllegalStateException("Cannot register local window class: "+Native.getLastError());
            registered=true;
            handle=User.API.CreateWindowExW(STYLES,className,new WString("OBS local-only Vulkan presentation"),0x80000000,-20000,-20000,1,1,owner,null,null,null);
            if(handle==null) throw new IllegalStateException("Cannot create local window: "+Native.getLastError());
            if((extendedStyle()&0x08000020L)!=0x08000020L) throw new IllegalStateException("Click-through/no-activate flags unavailable");
            dc=Gdi.API.CreateCompatibleDC(null);
            if(dc==null) throw new IllegalStateException("Cannot create software presentation DC");
            allocate(1,1); pixels.setInt(0,0xFF000000);
            // Constant-alpha layered windows retain a DWM redirection surface, unlike
            // UpdateLayeredWindow's per-pixel mode, which can reject capture affinity.
            if(!User.API.SetLayeredWindowAttributes(handle,0,(byte)255,2)) throw new IllegalStateException("Cannot initialize local surface: "+Native.getLastError());
            boolean protectedWindow=User.API.SetWindowDisplayAffinity(handle,EXCLUDE);
            int setError=Native.getLastError(), affinity=captureAffinity();
            if(!protectedWindow || affinity!=EXCLUDE) throw new IllegalStateException("Windows capture exclusion unavailable: set="+protectedWindow+", error="+setError+", affinity="+affinity);
            initialized.countDown();
            var message=new Message();
            while(!closed) {
                while(User.API.PeekMessageW(message,null,0,0,1)) {
                    if(message.message==0x12) { closed=true; break; }
                    User.API.TranslateMessage(message); User.API.DispatchMessageW(message);
                }
                refresh();
                try { Thread.sleep(16); } catch(InterruptedException ignored) { if(closed) break; }
            }
        } catch(Throwable e) {
            failure=e;
            if(initialized.getCount()==0) OBSOverlay.LOGGER.error("Vulkan local window stopped",e);
        } finally {
            closed=true; initialized.countDown();
            releaseBitmap();
            if(dc!=null) Gdi.API.DeleteDC(dc);
            if(handle!=null) { User.API.ShowWindow(handle,0); User.API.DestroyWindow(handle); }
            if(registered) User.API.UnregisterClassW(className,null);
            if(oldDpi!=null) User.API.SetThreadDpiAwarenessContext(oldDpi);
        }
    }
    public boolean isAvailable() { return !closed && failure==null && handle!=null; }
    public long copiedFrames() { return copiedFrames; }
    public int captureAffinity() {
        var value=new IntByReference();
        return handle!=null && User.API.GetWindowDisplayAffinity(handle,value)?value.getValue():0;
    }
    public long extendedStyle() { return handle==null?0:User.API.GetWindowLongPtrW(handle,-20); }
    public synchronized long requestFrame() {
        if(!requested) { generation++; requested=true; }
        return generation;
    }
    public synchronized void submit(long epoch,int width,int height,int[] data) {
        if(closed || !requested || epoch!=generation) return;
        if(width<=0 || height<=0 || (long)width*height!=data.length) throw new IllegalArgumentException("Invalid local frame dimensions");
        pending.set(new Frame(width,height,data,System.nanoTime(),epoch));
    }
    public synchronized void hide() {
        if(requested) { requested=false; generation++; }
        pending.set(null);
    }
    private void refresh() {
        if(closed) return;
        Frame next=pending.getAndSet(null);
        if(next!=null && requested && next.generation==generation) {
            allocate(next.width,next.height);
            // ARGB ints on little-endian Windows are the BGRA layout required by the DIB.
            pixels.write(0,next.pixels,0,next.pixels.length);
            copiedFrames++;
            displayed=next;
        }
        if(!requested || displayed==null || displayed.generation!=generation || System.nanoTime()-displayed.time>250_000_000L
                || User.API.IsIconic(owner) || !User.API.IsWindowVisible(owner) || !owner.equals(User.API.GetForegroundWindow())) { conceal(); return; }
        var rect=new Rect(); var origin=new Point();
        if(!User.API.GetClientRect(owner,rect) || !User.API.ClientToScreen(owner,origin)) { conceal(); return; }
        // Do not present an old-sized frame during a resize or DPI transition.
        if(rect.right-rect.left!=bitmapWidth || rect.bottom-rect.top!=bitmapHeight) { conceal(); return; }
        User.API.SetWindowPos(handle,new Pointer(-1),origin.x,origin.y,bitmapWidth,bitmapHeight,0x0010|0x0040);
        Pointer destination=User.API.GetDC(handle);
        if(destination==null) throw new IllegalStateException("Local image DC unavailable");
        try {
            if(!Gdi.API.BitBlt(destination,0,0,bitmapWidth,bitmapHeight,dc,0,0,0x00CC0020)) throw new IllegalStateException("Local image upload failed: "+Native.getLastError());
        } finally { User.API.ReleaseDC(handle,destination); }
        visible=true;
    }
    private void conceal() { if(visible && handle!=null) { User.API.ShowWindow(handle,0); visible=false; } }
    private void allocate(int width,int height) {
        if(bitmap!=null && width==bitmapWidth && height==bitmapHeight) return;
        releaseBitmap();
        var info=new BitmapHeader(); info.width=width; info.height=-height;
        var data=new PointerByReference();
        bitmap=Gdi.API.CreateDIBSection(dc,info,0,data,null,0);
        pixels=data.getValue();
        if(bitmap==null || pixels==null) throw new IllegalStateException("Cannot allocate local image bitmap");
        previousBitmap=Gdi.API.SelectObject(dc,bitmap); bitmapWidth=width; bitmapHeight=height;
    }
    private void releaseBitmap() {
        if(bitmap!=null) {
            if(dc!=null && previousBitmap!=null) Gdi.API.SelectObject(dc,previousBitmap);
            Gdi.API.DeleteObject(bitmap); bitmap=null; pixels=null; previousBitmap=null;
        }
    }
    @Override public void close() {
        closed=true; hide(); worker.interrupt();
        if(Thread.currentThread()!=worker) try { worker.join(2000); } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
