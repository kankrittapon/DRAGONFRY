package com.omnicore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * MMORPG-Style PvP HUD Badge Overlay
 * แสดงสถานะ โหมดสงบ (🛡️ Safe) / โหมด PvP (⚔️ PvP) / Free PK (🗡️ PK Countdown)
 */
public class PvPHudOverlay {
    public static final IGuiOverlay PVP_HUD = (gui, gfx, partialTick, screenW, screenH) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        // หากผู้เล่นเปิดหน้าต่าง PvPCursorScreen อยู่ จะไม่วาดซ้ำเพื่อไม่ให้ภาพทับซ้อน
        if (mc.screen instanceof PvPCursorScreen) return;

        renderPvPBadge(gfx, PvPCursorScreen.BTN_X, PvPCursorScreen.BTN_Y, PvPCursorScreen.BTN_W, PvPCursorScreen.BTN_H, false, false);
    };

    public static void renderPvPBadge(GuiGraphics gfx, int x, int y, int w, int h, boolean hovered, boolean cursorActive) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        boolean isPk = ClientPvPState.isPkFlagged();
        boolean isPvp = ClientPvPState.pvpEnabled;
        int secLeft = ClientPvPState.getPkRemainingSeconds();

        int borderCol;
        int bgCol;
        String title;
        int titleCol;

        if (isPk) {
            // Free PK State: สีแดงเข้มกะพริบ พร้อมเลขนับถอยหลัง
            borderCol = hovered ? 0xFFFF3333 : 0xFFDC2626;
            bgCol = 0xE6260808;
            title = "🗡 PK [" + secLeft + "s]";
            titleCol = (System.currentTimeMillis() % 600 < 300) ? 0xFFFF4444 : 0xFFFCD34D;
        } else if (isPvp) {
            // PvP Active: สีแดงเพลิง
            borderCol = hovered ? 0xFFFF6666 : 0xFFB91C1C;
            bgCol = 0xE61F0A0A;
            title = "⚔ PVP [ON]";
            titleCol = hovered ? 0xFFFFFFFF : 0xFFF87171;
        } else {
            // Peaceful / Safe Mode: สีเขียวมรกต
            borderCol = hovered ? 0xFF34D399 : 0xFF059669;
            bgCol = 0xE6051E14;
            title = "🛡 SAFE [PVE]";
            titleCol = hovered ? 0xFFFFFFFF : 0xFF6EE7B7;
        }

        RenderSystem.enableBlend();

        // 1. Drop Shadow Outer Frame
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x99000000);
        // 2. Themed Frame
        gfx.fill(x, y, x + w, y + h, borderCol);
        // 3. Dark Obsidian Inner
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgCol);

        // 4. Accent Left Indicator Bar
        int accentBarCol = isPk ? 0xFFEF4444 : (isPvp ? 0xFFDC2626 : 0xFF10B981);
        gfx.fill(x + 1, y + 1, x + 4, y + h - 1, accentBarCol);

        // 5. Header Title
        gfx.drawString(font, title, x + 8, y + 4, titleCol, true);

        // 6. Subtitle / Control Shortcut Hint
        String hint = cursorActive ? "§e[CLICK TO TOGGLE]" : "§7[Ctrl] Cursor [Alt+C]";
        gfx.drawString(font, hint, x + 8, y + 15, 0xFFAAAAAA, false);

        RenderSystem.disableBlend();
    }
}
