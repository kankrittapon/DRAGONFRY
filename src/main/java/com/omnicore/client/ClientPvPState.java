package com.omnicore.client;

/**
 * แคชสถานะ PvP ฝั่ง Client สำหรับแสดงผล HUD และ UI Interaction
 */
public class ClientPvPState {
    public static volatile boolean pvpEnabled = false;
    public static volatile int pkRemainingSeconds = 0;
    public static volatile long pkEndTime = 0;

    public static void update(boolean pvp, int pkSec) {
        pvpEnabled = pvp;
        pkRemainingSeconds = pkSec;
        if (pkSec > 0) {
            pkEndTime = System.currentTimeMillis() + (pkSec * 1000L);
        } else {
            pkEndTime = 0;
        }
    }

    public static boolean isPkFlagged() {
        return System.currentTimeMillis() < pkEndTime;
    }

    public static int getPkRemainingSeconds() {
        if (!isPkFlagged()) return 0;
        return (int) Math.max(1, (pkEndTime - System.currentTimeMillis()) / 1000L);
    }
}
