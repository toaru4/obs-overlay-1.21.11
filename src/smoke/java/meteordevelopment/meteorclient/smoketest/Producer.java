package meteordevelopment.meteorclient.smoketest;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
/** Synthetic producer for routing tests; never included in the shipped mod. */
public class Producer {
    public static void submit(GuiRenderState state, SimpleGuiElementRenderState quad) { state.addSimpleElement(quad); }
}
