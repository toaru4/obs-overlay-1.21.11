package meteordevelopment.meteorclient.smoketest;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
public final class Producer {
    public static void submit(GuiRenderState state,GuiElementRenderState value) { state.addGuiElement(value); }
}
