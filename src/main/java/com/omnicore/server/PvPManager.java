package com.omnicore.server;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MMORPG-Style PvP & Free PK Engine (Forge 1.20.1)
 * คุณสมบัติ:
 * 1. Keybind Alt + C หรือคำสั่ง /pvp เพื่อเปิด/ปิดโหมด PvP
 * 2. สถานะ PvP (PvP ON): สัญลักษณ์สีแดงรวมกับชื่อ (§c⚔ [PvP] PlayerName)
 * 3. สถานะ Free PK (🗡️): เมื่อเริ่มโจมตีผู้เล่น จะติดสัญลักษณ์ดาบ 45 วินาที กลายเป็น Free PK (ทุกคนสามารถรุมตีได้)
 * 4. เมื่อครบ 45 วินาที: หลุดจากสถานะ PK สัญลักษณ์ดาบหายไป กลับมาเป็นสีแดงหน้าชื่อตามปกติ
 * 5. ป้องกัน Friendly Fire สำหรับสมาชิกปาร์ตี้ (FTB Teams & Vanilla Teams) ตีกันไม่ได้เด็ดขาด
 */
public class PvPManager {
    public static final PvPManager INSTANCE = new PvPManager();

    public static class PlayerPvPData {
        public boolean pvpEnabled = false;       // State 1: PvP ON via Alt+C or /pvp
        public long pkExpireAtMillis = 0L;       // State 2: Free PK sword until this timestamp

        public boolean isPkFlagged() {
            return System.currentTimeMillis() < pkExpireAtMillis;
        }
    }

    private final Map<UUID, PlayerPvPData> pvpData = new ConcurrentHashMap<>();
    private long tickCounter = 0L;

    private PvPManager() {}

    public PlayerPvPData getOrCreate(UUID uuid) {
        return pvpData.computeIfAbsent(uuid, k -> new PlayerPvPData());
    }

    public boolean isPvPEnabled(UUID uuid) {
        PlayerPvPData data = pvpData.get(uuid);
        return data != null && data.pvpEnabled;
    }

    public boolean isPkFlagged(UUID uuid) {
        PlayerPvPData data = pvpData.get(uuid);
        return data != null && data.isPkFlagged();
    }

    public void togglePvP(ServerPlayer player) {
        if (player == null) return;
        PlayerPvPData data = getOrCreate(player.getUUID());

        // ห้ามปิด PvP ขณะติดสถานะ Free PK (ต่อสู้อยู่)
        if (data.isPkFlagged()) {
            int secLeft = Math.max(1, (int) ((data.pkExpireAtMillis - System.currentTimeMillis()) / 1000L));
            player.displayClientMessage(Component.literal("§c❌ ไม่สามารถปิด PvP ได้ในขณะติดสถานะ Free PK / กำลังต่อสู้! (รออีก " + secLeft + " วินาที)"), true);
            return;
        }

        data.pvpEnabled = !data.pvpEnabled;
        syncPlayerName(player);

        if (data.pvpEnabled) {
            player.displayClientMessage(Component.literal("§c⚔️ [PvP ON] เปิดโหมด PvP แล้ว! (ชื่อเปลี่ยนเป็นสีแดง มีสัญลักษณ์ [PvP])"), true);
            player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.8f, 1.2f);
        } else {
            player.displayClientMessage(Component.literal("§7🛡️ [PvP OFF] ปิดโหมด PvP แล้ว (สถานะ: สงบ / Peaceful)"), true);
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    public void setPvPState(ServerPlayer player, boolean enabled) {
        if (player == null) return;
        PlayerPvPData data = getOrCreate(player.getUUID());
        if (data.isPkFlagged() && !enabled) {
            int secLeft = Math.max(1, (int) ((data.pkExpireAtMillis - System.currentTimeMillis()) / 1000L));
            player.displayClientMessage(Component.literal("§c❌ ไม่สามารถปิด PvP ได้ในขณะติดสถานะ Free PK / กำลังต่อสู้! (รออีก " + secLeft + " วินาที)"), true);
            return;
        }
        data.pvpEnabled = enabled;
        syncPlayerName(player);
    }

    public Component getFormattedDisplayName(Player player) {
        if (player == null) return null;
        PlayerPvPData data = pvpData.get(player.getUUID());
        String rawName = player.getGameProfile().getName();

        if (data != null && data.isPkFlagged()) {
            // State 2: Free PK (สัญลักษณ์ดาบ 🗡️ + สีแดงเข้ม)
            return Component.literal("§4🗡️ [PK] §c" + rawName);
        } else if (data != null && data.pvpEnabled) {
            // State 1: PvP Mode ON (สัญลักษณ์สีแดงรวมกับชื่อ §c⚔ [PvP])
            return Component.literal("§c⚔ [PvP] §f" + rawName);
        }
        // State 0: Peaceful
        return Component.literal(rawName);
    }

    public void syncPlayerName(ServerPlayer player) {
        if (player == null) return;
        player.refreshDisplayName();
        player.refreshTabListName();
        MinecraftServer server = player.getServer();
        if (server != null) {
            server.getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                            player
                    )
            );
        }
    }

    /**
     * ตรวจสอบว่าผู้เล่น 2 คนอยู่ในปาร์ตี้เดียวกันหรือไม่ (FTB Teams + Vanilla Scoreboard)
     */
    public static boolean areInSameParty(Player playerA, Player playerB) {
        if (playerA == null || playerB == null) return false;
        if (playerA.getUUID().equals(playerB.getUUID())) return true;

        // 1. ตรวจสอบ FTB Teams API ผ่าน Reflection
        try {
            Class<?> apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
            Object apiInstance = apiClass.getMethod("api").invoke(null);
            if (apiInstance != null) {
                Object manager = apiInstance.getClass().getMethod("getManager").invoke(apiInstance);
                if (manager != null) {
                    Boolean inSame = (Boolean) manager.getClass()
                            .getMethod("arePlayersInSameTeam", UUID.class, UUID.class)
                            .invoke(manager, playerA.getUUID(), playerB.getUUID());
                    if (inSame != null && inSame) return true;
                }
            }
        } catch (Throwable ignored) {}

        // 2. ตรวจสอบ Vanilla Scoreboard Team
        try {
            if (playerA.getTeam() != null && playerB.getTeam() != null) {
                return playerA.getTeam().isAlliedTo(playerB.getTeam());
            }
        } catch (Throwable ignored) {}

        return false;
    }

    /* ------------------------------------------------------------------ */
    /* ⚔️ Event Handlers                                                    */
    /* ------------------------------------------------------------------ */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        // ค้นหาผู้เล่นที่เป็นผู้โจมตี (ทั้งโจมตีตรง และโจมตีผ่านกระสุน/ธนู)
        ServerPlayer attacker = null;
        if (event.getSource().getEntity() instanceof ServerPlayer sp) {
            attacker = sp;
        }

        if (attacker == null || attacker.getUUID().equals(victim.getUUID())) return;

        // 1. สมาชิกปาร์ตี้เดียวกัน -> ตีกันไม่ได้เด็ดขาด (Cancel Damage 100%)
        if (areInSameParty(attacker, victim)) {
            event.setCanceled(true);
            return;
        }

        PlayerPvPData attackerData = getOrCreate(attacker.getUUID());
        PlayerPvPData victimData = getOrCreate(victim.getUUID());

        // 2. กรณีเป้าหมายติดสถานะ Free PK (สัญลักษณ์ดาบ 🗡️):
        // ทุกคนในเซิร์ฟเวอร์สามารถรุมโจมตีเป้าหมายนี้ได้ฟรีโดยไม่ผิดกฎ!
        if (victimData.isPkFlagged()) {
            if (attackerData.pvpEnabled) {
                attackerData.pkExpireAtMillis = System.currentTimeMillis() + 45_000L;
                syncPlayerName(attacker);
            }
            return; // อนุญาตให้ตีได้
        }

        // 3. กรณีผู้โจมตีไม่ได้เปิดโหมด PvP (PvP OFF):
        // ห้ามโจมตีผู้เล่นอื่น (ยกเว้นเป้าหมาย Free PK ด้านบน)
        if (!attackerData.pvpEnabled) {
            event.setCanceled(true);
            attacker.displayClientMessage(Component.literal("§c❌ คุณปิดโหมด PvP อยู่! (กด Alt + C หรือพิมพ์ /pvp เพื่อเปิดโหมดต่อสู้)"), true);
            return;
        }

        // 4. ผู้โจมตีเปิดโหมด PvP (PvP ON):
        // เมื่อเริ่มโจมตีผู้เล่นอื่น -> ผู้โจมตีจะได้รับสัญลักษณ์ดาบ 🗡️ กลายเป็น Free PK ทันที 45 วินาที!
        attackerData.pkExpireAtMillis = System.currentTimeMillis() + 45_000L;
        syncPlayerName(attacker);
        attacker.displayClientMessage(Component.literal("§4🗡️ [FREE PK] คุณเปิดฉากโจมตีผู้เล่น! ติดสถานะดาบ 45 วินาที (ทุกคนสามารถโจมตีคุณได้)"), true);
        victim.displayClientMessage(Component.literal("§c⚔️ คุณกำลังถูกโจมตีโดย " + attacker.getName().getString() + "!"), true);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPvPData data = pvpData.get(player.getUUID());
            if (data != null && data.isPkFlagged()) {
                data.pkExpireAtMillis = 0L; // รีเซ็ตสถานะดาบเมื่อตาย
                syncPlayerName(player);
            }
        }
    }

    @SubscribeEvent
    public void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Component formatted = getFormattedDisplayName(player);
            if (formatted != null) {
                event.setDisplayname(formatted);
            }
        }
    }

    @SubscribeEvent
    public void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Component formatted = getFormattedDisplayName(player);
            if (formatted != null) {
                event.setDisplayName(formatted);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        if (tickCounter % 20 != 0) return; // ทำงานทุกๆ 1 วินาที (20 ticks)

        MinecraftServer server = event.getServer();
        if (server == null) return;
        long now = System.currentTimeMillis();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerPvPData data = pvpData.get(player.getUUID());
            if (data == null) continue;

            if (data.pkExpireAtMillis > 0) {
                if (now >= data.pkExpireAtMillis) {
                    // หลุดจากสถานะ Free PK ครบ 45 วินาที -> กลับมาเป็นสีแดงหน้าชื่อ
                    data.pkExpireAtMillis = 0L;
                    syncPlayerName(player);
                    player.displayClientMessage(Component.literal("§a✨ หลุดจากสถานะ Free PK แล้ว! สัญลักษณ์ดาบหายไป กลับสู่สถานะสีแดง [PvP]"), false);
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
                } else {
                    // กำลังติดสถานะ Free PK -> แสดงเวลานับถอยหลังที่ Action Bar
                    int secLeft = Math.max(1, (int) ((data.pkExpireAtMillis - now) / 1000L));
                    player.displayClientMessage(Component.literal("§4🗡️ [FREE PK] เหลือเวลาติดสถานะดาบ: §e" + secLeft + " วินาที"), true);
                }
            }
        }
    }
}
