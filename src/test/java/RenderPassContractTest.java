import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import com.mojang.renderpearl.api.commands.RenderPass;
import me.zziger.obsoverlay.port.MirroredRenderPass;

public class RenderPassContractTest {
    static RenderPass fake(String name,List<String> events) {
        return (RenderPass) Proxy.newProxyInstance(RenderPass.class.getClassLoader(),new Class<?>[]{RenderPass.class},(proxy,method,args)-> {
            String detail=method.getName().equals("pushConstants") ? ":"+((ByteBuffer)args[0]).get(0) : "";
            events.add(name+":"+method.getName()+detail);
            return null;
        });
    }
    public static void main(String[] args) {
        var events=new ArrayList<String>();
        var buffer=ByteBuffer.allocateDirect(4); buffer.put(0,(byte)7);
        var pass=new MirroredRenderPass(fake("capture",events),()-> {
            if (!events.getLast().equals("capture:close")) throw new AssertionError("Nested render passes");
            return fake("local",events);
        });
        pass.pushConstants(buffer);
        pass.drawIndexed(0,0,6,1,0);
        buffer.put(0,(byte)99);
        if(events.size()!=2) throw new AssertionError("Replay was not deferred");
        pass.close(); pass.close();
        var expected=List.of("capture:pushConstants:7","capture:drawIndexed","capture:close","local:pushConstants:7","local:drawIndexed","local:close");
        if (!events.equals(expected)) throw new AssertionError(events.toString());
        try { pass.draw(0,3,1,0); throw new AssertionError("Closed pass accepted drawing"); }
        catch (IllegalStateException expectedFailure) {}
        System.out.println("RENDERPEARL_CONTRACT_PASS: real 26.3 RenderPass interface, deferred replay, mutable-memory snapshots, idempotent close and use-after-close checks");
    }
}
