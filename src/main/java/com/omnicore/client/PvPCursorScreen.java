package com.omnicore.client;

import com.omnicore.config.OmniClientConfig;
import com.omnicore.network.ModNetwork;
import com.omnicore.network.TogglePvPPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * หน้าจอ Transparent Screen ปลดล็อก Cursor เมาส์เมื่อกดปุ่ม Ctrl
 * ให้ผู้เล่นสามารถใช้เมาส์คลิกปุ่มสลับโหมด PvP บน HUD ได้อย่างอิสระสไตล์เกม MMORPG
 * รองรับการคลิกขวาเพื่อย้ายมุมแสดงผล HUD (Top-Left / Top-Right / Bottom-Left / Bottom-Right)
 */
public class PvPCursorScreen extends Screen {
    public static final int BTN_W = 124;
    public static final int BTN_H = 28;

    public PvPCursorScreen() {
        super(Component.literal("OmniCore PvP Control"));
    }

    public int getX() {
        return PvPHudOverlay.getBadgeX(this.width);
    }

    public int getY() {
        return PvPHudOverlay.getBadgeY(this.height);
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
        int x = getX();
        int y = getY();
        boolean hovered = isMouseOverButton(mouseX, mouseY);
        PvPHudOverlay.renderPvPBadge(gfx, x, y, BTN_W, BTN_H, hovered, true);

        if (hovered) {
            List<Component> tooltipLines = new ArrayList<>();
            if (ClientPvPState.isPkFlagged()) {
                tooltipLines.add(Component.literal("§c🗡️ ติดสถานะ Free PK! (ห้ามปิดจนกว่าเวลาจะหมด)"));
            } else if (ClientPvPState.pvpEnabled) {
                tooltipLines.add(Component.literal("§e⚔️ สถานะ: §cเปิดโหมด PVP"));
            } else {
                tooltipLines.add(Component.literal("§e🛡️ สถานะ: §aโหมดสงบ PVE"));
            }
            tooltipLines.add(Component.literal("§8-----------------------------"));
            tooltipLines.add(Component.literal("§f• §aคลิกซ้าย: §7สลับโหมด PvP / PvE"));
            tooltipLines.add(Component.literal("§f• §eคลิกขวา: §7ย้ายมุม HUD (§6" + formatCornerName(OmniClientConfig.currentCorner) + "§7)"));
            tooltipLines.add(Component.literal("§f• §bAlt+H: §7ซ่อน/แสดง HUD (§b" + (OmniClientConfig.isHudHidden ? "ซ่อนอยู่" : "แสดงอยู่") + "§7)"));
            gfx.renderComponentTooltip(this.font, tooltipLines, mouseX, mouseY);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private boolean isMouseOverButton(double mouseX, double mouseY) {
        int x = getX();
        int y = getY();
        return mouseX >= x && mouseX <= x + BTN_W && mouseY >= y && mouseY <= y + BTN_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverButton(mouseX, mouseY)) {
            Minecraft mc = Minecraft.getInstance();
            if (button == 0) { // Left-Click: Toggle PvP
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                ModNetwork.CHANNEL.sendToServer(new TogglePvPPacket());
                return true;
            } else if (button == 1) { // Right-Click: Cycle Corner Position
                cycleCorner(mc);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static void cycleCorner(Minecraft mc) {
        OmniClientConfig.HudCorner current = OmniClientConfig.currentCorner;
        OmniClientConfig.HudCorner next;
        switch (current) {
            case TOP_LEFT:
                next = OmniClientConfig.HudCorner.TOP_RIGHT;
                break;
            case TOP_RIGHT:
                next = OmniClientConfig.HudCorner.BOTTOM_RIGHT;
                break;
            case BOTTOM_RIGHT:
                next = OmniClientConfig.HudCorner.BOTTOM_LEFT;
                break;
            case BOTTOM_LEFT:
            default:
                next = OmniClientConfig.HudCorner.TOP_LEFT;
                break;
        }

        OmniClientConfig.currentCorner = next;

        try {
            if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudCorner != null) {
                OmniClientConfig.INSTANCE.hudCorner.set(next);
            }
        } catch (Throwable ignored) {}

        if (mc != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.25f));
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("§6[OmniCore] §eย้ายตำแหน่ง PvP HUD ไปที่: §a" + formatCornerName(next)),
                    true // action bar
                );
            }
        }
    }

    public static String formatCornerName(OmniClientConfig.HudCorner corner) {
        switch (corner) {
            case TOP_LEFT: return "บนซ้าย (Top-Left)";
            case TOP_RIGHT: return "บนขวา (Top-Right)";
            case BOTTOM_RIGHT: return "ล่างขวา (Bottom-Right)";
            case BOTTOM_LEFT: return "ล่างซ้าย (Bottom-Left)";
            default: return corner.name();
        }
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
