package com.omnicore.client;

import com.omnicore.network.ModNetwork;
import com.omnicore.network.TogglePvPPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

/**
 * หน้าจอ Transparent Screen ปลดล็อก Cursor เมาส์เมื่อกดปุ่ม Ctrl
 * ให้ผู้เล่นสามารถใช้เมาส์คลิกปุ่มสลับโหมด PvP บน HUD ได้อย่างอิสระสไตล์เกม MMORPG
 */
public class PvPCursorScreen extends Screen {
    public static final int BTN_X = 10;
    public static final int BTN_Y = 10;
    public static final int BTN_W = 110;
    public static final int BTN_H = 26;

    public PvPCursorScreen() {
        super(Component.literal("OmniCore PvP Control"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // ไม่หยุดเกม รักษาการทำงานของโลกแบบ Real-time
    }

    @Override
    public void renderBackground(GuiGraphics gfx) {
        // ไม่วาดเงาดำบังจอ คงความโปร่งใส 100% ให้มองเห็นโลกในเกมได้ชัดเจน
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isMouseOverButton(mouseX, mouseY);
        PvPHudOverlay.renderPvPBadge(gfx, BTN_X, BTN_Y, BTN_W, BTN_H, hovered, true);

        if (hovered) {
            Component tooltip = ClientPvPState.isPkFlagged()
                    ? Component.literal("§c🗡️ ติดสถานะ Free PK! (ห้ามปิดจนกว่าเวลาจะหมด)")
                    : (ClientPvPState.pvpEnabled
                    ? Component.literal("§e⚔️ คลิกเพื่อปิดโหมด PvP (§aเข้าสู่โหมดสงบ PVE§e)")
                    : Component.literal("§c⚔️ คลิกเพื่อเปิดโหมด PvP (§cเปิดการต่อสู้ PVP§e)"));
            gfx.renderTooltip(this.font, tooltip, mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private boolean isMouseOverButton(double mouseX, double mouseY) {
        return mouseX >= BTN_X && mouseX <= BTN_X + BTN_W && mouseY >= BTN_Y && mouseY <= BTN_Y + BTN_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverButton(mouseX, mouseY)) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (PvPKeyHandler.TOGGLE_PVP_KEY.matches(keyCode, scanCode)) {
            ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // เมื่อปล่อยปุ่ม Ctrl หรือ Cursor Key จะปิดหน้าจอทันทีเพื่อกลับสู่การควบคุมมุมกล้องตามปกติ
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
                || PvPKeyHandler.CURSOR_KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
