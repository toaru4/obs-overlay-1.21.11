package smoke;
import com.mojang.blaze3d.platform.NativeLibrariesBootstrap;
import me.zziger.obsoverlay.port.*;
import org.lwjgl.opengl.GL;
import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLVideo.*;

/** Real SDL3/OpenGL hidden window; no Minecraft account, assets or world. */
public final class GlPresentationSmoke {
    public static void run() throws Exception {
        NativeLibrariesBootstrap.loadLibraries();
        if(!SDL_Init(SDL_INIT_VIDEO)) throw new AssertionError("SDL video initialization failed");
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION,3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION,3);
        SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK,SDL_GL_CONTEXT_PROFILE_CORE);
        long window=SDL_CreateWindow("Hidden SDL overlay check",64,64,SDL_WINDOW_OPENGL|SDL_WINDOW_HIDDEN);
        if(window==0) throw new AssertionError("SDL OpenGL window unavailable");
        long context=SDL_GL_CreateContext(window);
        if(context==0 || !SDL_GL_MakeCurrent(window,context)) throw new AssertionError("SDL OpenGL context unavailable");
        GL.createCapabilities();
        int local=texture(0,255,0),captured=texture(255,0,0),fbo=glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER,fbo); glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,captured,0);
        glBindFramebuffer(GL_FRAMEBUFFER,0); glClearColor(0,0,1,1); glClear(GL_COLOR_BUFFER_BIT);
        glBindFramebuffer(GL_FRAMEBUFFER,fbo); glViewport(3,4,19,23); glEnable(GL_SCISSOR_TEST); glScissor(1,2,5,6); glEnable(GL_DEPTH_TEST); glDisable(GL_BLEND);
        int[] calls={0};
        try(var compositor=new LocalCompositor()) {
            NativeSwapHook.install(()-> {
                compositor.draw(local,64,64);
                if(glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)!=fbo || !glIsEnabled(GL_SCISSOR_TEST) || !glIsEnabled(GL_DEPTH_TEST) || glIsEnabled(GL_BLEND)) throw new AssertionError("GL state not restored");
                pixel(255,0,0);
                glBindFramebuffer(GL_READ_FRAMEBUFFER,0); glReadBuffer(GL_BACK); pixel(0,255,0);
                glBindFramebuffer(GL_READ_FRAMEBUFFER,fbo); calls[0]++;
            });
            SDL_GL_SwapWindow(window);
            if(calls[0]!=1) throw new AssertionError("Native hook did not run through SDL3 swap: "+calls[0]);
            if(glGetError()!=GL_NO_ERROR) throw new AssertionError("OpenGL error");
            System.out.println("SDL_OPENGL_PRESENTATION_PASS: real SDL3 swap, native hook, local green pixels, captured red texture preserved, graphics state restoration");
        } finally {
            NativeSwapHook.uninstall(); glBindFramebuffer(GL_FRAMEBUFFER,0);
            glDeleteFramebuffers(fbo); glDeleteTextures(local); glDeleteTextures(captured);
            SDL_GL_DestroyContext(context); SDL_DestroyWindow(window); SDL_Quit();
        }
    }
    static int texture(int r,int g,int b) {
        int id=glGenTextures(); glBindTexture(GL_TEXTURE_2D,id);
        var data=org.lwjgl.BufferUtils.createByteBuffer(4).put((byte)r).put((byte)g).put((byte)b).put((byte)255).flip();
        glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,1,1,0,GL_RGBA,GL_UNSIGNED_BYTE,data);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST); glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        return id;
    }
    static void pixel(int r,int g,int b) {
        ByteBuffer data=org.lwjgl.BufferUtils.createByteBuffer(4); glReadPixels(0,0,1,1,GL_RGBA,GL_UNSIGNED_BYTE,data);
        if(Byte.toUnsignedInt(data.get(0))!=r || Byte.toUnsignedInt(data.get(1))!=g || Byte.toUnsignedInt(data.get(2))!=b) throw new AssertionError("Unexpected GL pixel");
    }
}
