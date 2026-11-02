package smoke;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import me.zziger.obsoverlay.port.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Full client test with a temporary integrated world, never connects to external servers. */
public final class GameLaunchSmoke {
    static int stage,frames; static long start=System.nanoTime();
    static AtomicInteger completed=new AtomicInteger();
    static Path directory=Path.of("smoke-images");
    public static void frame() {
        try {
            var mc=Minecraft.getInstance();
            if(System.nanoTime()-start>150_000_000_000L) throw new AssertionError("Full-game rendering test timed out, stage="+stage);
            if(!mc.isGameLoadFinished() || mc.gui.overlay()!=null) return;
            if(stage==0) {
                OBSOverlay.config.hud.put("meteor",true); OBSOverlay.config.gui.put("meteor",true); OBSOverlay.config.showTestIcon=true;
                mc.execute(()-> { mc.options.renderDistance().set(2); mc.setScreenAndShow(new TitleScreen()); }); stage=1; frames=0; return;
            }
            if(stage==1 && ++frames>60) {
                capture("menu"); stage=2; return;
            }
            if(stage==2 && completed.get()==2) {
                OBSOverlay.config.showTestIcon=false;
                mc.execute(()-> {
                    var settings=new LevelSettings("Overlay rendering regression",GameType.CREATIVE,
                        new LevelSettings.DifficultySettings(net.minecraft.world.Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("overlay-regression-"+System.currentTimeMillis(),settings,new WorldOptions(12345L,false,false),WorldPresets::createNormalWorldDimensions,null);
                }); stage=3; frames=0; return;
            }
            if(stage==3 && mc.level!=null && mc.player!=null && frames==20) {
                var origin=mc.player.blockPosition(); var server=mc.getSingleplayerServer();
                server.execute(()-> {
                    server.overworld().setBlockAndUpdate(origin.offset(2,0,4),net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                    server.overworld().setBlockAndUpdate(origin.offset(-2,0,4),net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
                });
            }
            if(stage==3 && mc.level!=null && mc.player!=null && ++frames>160) {
                capture("world"); stage=4; return;
            }
            if(stage==4 && completed.get()==4) {
                System.out.println("GAME_RENDER_PASS: full client menu and integrated world rendered with independent local/capture screenshots; no live OBS acceptance");
                stage=5; mc.execute(mc::stop);
            }
        } catch(Throwable e) { e.printStackTrace(); System.exit(1); }
    }
    static void capture(String name) throws Exception {
        Files.createDirectories(directory);
        var mc=Minecraft.getInstance(); var scene=OBSOverlay.renderer.localScene();
        if(scene==null || !OBSOverlay.renderer.ready()) throw new AssertionError("No local scene: "+name);
        Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),img-> {
            try(img) { img.writeToFile(directory.resolve(name+"-captured.png")); System.out.println("GAME_CAPTURE "+name+" captured="+nonblack(img)); completed.incrementAndGet(); }
            catch(Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        Screenshot.takeScreenshot(scene,img-> {
            try(img) {
                img.writeToFile(directory.resolve(name+"-local.png")); int visible=nonblack(img);
                System.out.println("GAME_CAPTURE "+name+" local="+visible);
                if(visible<50) throw new AssertionError("Local scene is black: "+name);
                completed.incrementAndGet();
            } catch(Throwable e) { e.printStackTrace(); System.exit(1); }
        });
    }
    static int nonblack(com.mojang.blaze3d.platform.NativeImage img) {
        int count=0;
        for(int y=0;y<img.getHeight();y+=8) for(int x=0;x<img.getWidth();x+=8) if((img.getPixel(x,y)&0x00FFFFFF)>0x00040404) count++;
        return count;
    }
}
