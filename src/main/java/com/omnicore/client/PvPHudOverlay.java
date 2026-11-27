package com.omnicore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.omnicore.config.OmniClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * MMORPG-Style PvP HUD Badge Overlay
 * แสดงสัญลักษณ์วงกลมดาบและโล่ (Ref: Lineage II / MMORPG Alt+C)
 * โหมดสงบ (🛡️ Safe / pvp_off.png) / โหมด PvP ลุกเป็นไฟ (⚔️ PvP / pvp_on.png) / Free PK (🗡️ PK / pvp_pk.png)
 */
public class PvPHudOverlay {
    public static final ResourceLocation TEXTURE_OFF = new ResourceLocation("omnicore", "textures/gui/pvp_off.png");
    public static final ResourceLocation TEXTURE_ON = new ResourceLocation("omnicore", "textures/gui/pvp_on.png");
    public static final ResourceLocation TEXTURE_PK = new ResourceLocation("omnicore", "textures/gui/pvp_pk.png");

    public static final IGuiOverlay PVP_HUD = (gui, gfx, partialTick, screenW, screenH) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (OmniClientConfig.isHudHidden) return;
        // หากผู้เล่นเปิดหน้าต่าง PvPCursorScreen อยู่ จะไม่วาดซ้ำเพื่อไม่ให้ภาพทับซ้อน
        if (mc.screen instanceof PvPCursorScreen) return;

        int x = getBadgeX(screenW);
        int y = getBadgeY(screenH);
        renderPvPBadge(gfx, x, y, PvPCursorScreen.BTN_W, PvPCursorScreen.BTN_H, false, false);
    };

    public static int getBadgeX(int screenW) {
        int offsetX = 10;
        try {
            if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudOffsetX != null) {
                offsetX = OmniClientConfig.INSTANCE.hudOffsetX.get();
            }
        } catch (Exception ignored) {}

        switch (OmniClientConfig.currentCorner) {
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return screenW - PvPCursorScreen.BTN_W - offsetX;
            case TOP_LEFT:
            case BOTTOM_LEFT:
            default:
                return offsetX;
        }
    }

    public static int getBadgeY(int screenH) {
        int offsetY = 10;
        try {
            if (OmniClientConfig.INSTANCE != null && OmniClientConfig.INSTANCE.hudOffsetY != null) {
                offsetY = OmniClientConfig.INSTANCE.hudOffsetY.get();
            }
        } catch (Exception ignored) {}

        switch (OmniClientConfig.currentCorner) {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return screenH - PvPCursorScreen.BTN_H - offsetY;
            case TOP_LEFT:
            case TOP_RIGHT:
            default:
                return offsetY;
        }
    }

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
        ResourceLocation emblemTexture;

        if (isPk) {
            // Free PK State: สัญลักษณ์สีแดงเข้มกะพริบ พร้อมเลขนับถอยหลัง
            borderCol = hovered ? 0xFFFF3333 : 0xFFDC2626;
            bgCol = 0xE6260808;
            title = "🗡 PK [" + secLeft + "s]";
            titleCol = (System.currentTimeMillis() % 600 < 300) ? 0xFFFF4444 : 0xFFFCD34D;
            emblemTexture = TEXTURE_PK;
        } else if (isPvp) {
            // PvP Active: สัญลักษณ์ดาบและโล่ลุกเป็นไฟโชติช่วง (Fiery Flame Emblem)
            borderCol = hovered ? 0xFFFF6666 : 0xFFB91C1C;
            bgCol = 0xE61F0A0A;
            title = "⚔ PVP [ON]";
            titleCol = hovered ? 0xFFFFFFFF : 0xFFF87171;
            emblemTexture = TEXTURE_ON;
        } else {
            // Peaceful / Safe Mode: สัญลักษณ์ดาบและโล่แบบมืด (Dark Shield Emblem)
            borderCol = hovered ? 0xFF34D399 : 0xFF059669;
            bgCol = 0xE6051E14;
            title = "🛡 SAFE [PVE]";
            titleCol = hovered ? 0xFFFFFFFF : 0xFF6EE7B7;
            emblemTexture = TEXTURE_OFF;
        }

        RenderSystem.enableBlend();

        // 1. Drop Shadow Outer Frame
        gfx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0x99000000);
        // 2. Themed Frame (โกลว์กรอบทองเมื่อโฮเวอร์)
        if (hovered) {
            gfx.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xAAFFD700);
        }
        gfx.fill(x, y, x + w, y + h, borderCol);
        // 3. Dark Obsidian Inner Background
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, bgCol);

        // 4. Render Authentic Emblem (24x24 px) from ref images
        gfx.blit(emblemTexture, x + 2, y + 2, 24, 24, 0f, 0f, 64, 64, 64, 64);

        // 5. Header Title
        gfx.drawString(font, title, x + 29, y + 4, titleCol, true);

        // 6. Subtitle / Control Shortcut Hint
        String hint = cursorActive ? "§e[L:Toggle §7| §6R:Corner]" : "§7[Ctrl] UI [Alt+C]";
        gfx.drawString(font, hint, x + 29, y + 15, 0xFFAAAAAA, false);

        RenderSystem.disableBlend();
    }
}
