import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;
import me.zziger.obsoverlay.port.OverlayRenderer;

/** Rendering may borrow Minecraft textures, but cannot replace its reload-owned resources. */
public final class TextureOwnershipTest {
    static List<String> violations(byte[] bytes) {
        var result=new ArrayList<String>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode,String owner,String method,String descriptor,boolean itf) {
                        if((owner.equals("net/minecraft/client/gui/render/GuiRenderer") && method.equals("registerPanoramaTextures")) ||
                           (owner.equals("net/minecraft/client/renderer/CubeMap") && method.equals("registerTextures")) ||
                           (owner.equals("net/minecraft/client/renderer/texture/TextureManager") &&
                               Set.of("register","registerAndLoad","registerForNextReload","release","close").contains(method)))
                            result.add(name+" -> "+owner+"."+method);
                    }
                };
            }
        },ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);
        return result;
    }
    public static void main(String[] args) throws Exception {
        // Ensure the guard detects the exact resource replacement path that caused alpha.1's crash.
        var fixture=new ClassWriter(0);
        fixture.visit(Opcodes.V25,Opcodes.ACC_PUBLIC,"BadRegistration",null,"java/lang/Object",null);
        var method=fixture.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"replace","()V",null,null);
        method.visitCode(); method.visitInsn(Opcodes.ACONST_NULL); method.visitInsn(Opcodes.ACONST_NULL);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"net/minecraft/client/gui/render/GuiRenderer","registerPanoramaTextures","(Lnet/minecraft/client/renderer/texture/TextureManager;)V",false);
        method.visitInsn(Opcodes.RETURN); method.visitMaxs(2,0); method.visitEnd(); fixture.visitEnd();
        if(violations(fixture.toByteArray()).size()!=1) throw new AssertionError("Regression guard did not detect texture replacement");
        Path root=Path.of(OverlayRenderer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try(var files=Files.walk(root.resolve("me/zziger/obsoverlay/port"))) {
            for(Path file:files.filter(p->p.toString().endsWith(".class")).toList()) {
                var found=violations(Files.readAllBytes(file));
                if(!found.isEmpty()) throw new AssertionError(file+": mutates Minecraft-owned textures: "+found);
            }
        }
        System.out.println("TEXTURE_OWNERSHIP_PASS: local presentation cannot register, replace or release Minecraft-owned textures; alpha.1 failure path detected");
    }
}
