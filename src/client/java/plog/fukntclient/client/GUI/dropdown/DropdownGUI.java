package plog.fukntclient.client.GUI.dropdown;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import plog.fukntclient.client.Modules.Category;
import plog.fukntclient.client.Util.RenderUtil;

import java.util.ArrayList;
import java.util.List;

public class DropdownGUI extends Screen {
    private final List<CategoryFrame> frames = new ArrayList<>();

    private static final int FRAME_WIDTH    = 94;
    private static final int HEADER_HEIGHT  = 12;
    private static final int MODULE_HEIGHT  = 10;
    private static final int SETTING_HEIGHT = 9;

    public DropdownGUI() {
        super(Component.literal("Fuknt ClickGUI"));
    }

    @Override
    protected void init() {
        if (frames.isEmpty()) {
            int offset = 4;
            for (Category cat : Category.values()) {
                CategoryFrame cf = new CategoryFrame(cat, offset, 4, FRAME_WIDTH, HEADER_HEIGHT);
                for (ModuleFrame mf : cf.modules) {
                    mf.height = MODULE_HEIGHT;
                    for (SettingFrame sf : mf.settings) {
                        sf.height = SETTING_HEIGHT;
                    }
                }
                frames.add(cf);
                offset += FRAME_WIDTH + 4;
            }

            // only register once, right after building
            for (CategoryFrame cf : frames) {
                this.addRenderableWidget(cf);
                for (ModuleFrame mf : cf.modules) {
                    for (SettingFrame sf : mf.settings) {
                        this.addRenderableWidget(sf);
                    }
                }
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, RenderUtil.pack(10, 10, 14, 90));
        for (CategoryFrame cf : frames) {
            cf.render(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (CategoryFrame cf : frames) {
            cf.onScroll(mouseX, mouseY, scrollY);
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}