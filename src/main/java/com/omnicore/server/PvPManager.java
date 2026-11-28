package com.omnicore.server;

import com.omnicore.network.ModNetwork;
import com.omnicore.network.SyncPvPStatePacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Method;
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
 * 5. ป้องกัน Friendly Fire สำหรับสมาชิกปาร์ตี้ (FTB Teams & Vanilla Teams) ตีกันไม่ได้เด็ดขาด 100%
 * 6. รองรับการป้องกันดาเมจทุกรูปแบบ: Melee, ลูกธนู, เวทมนตร์/กระสุนเวท (Iron's Spells),
 *    สกิลพื้นที่ (AOE / Lingering Clouds), ซัมมอน/มินเนียน (IMagicSummon, Vex, Zombie),
 *    สัตว์เลี้ยง (Tamable/Ownable), สายฟ้า, และสกิลพลังจาก Origins / Origins++
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

    public boolean canAttack(Player attacker, Player victim) {
        if (attacker == null || victim == null) return false;
        if (attacker.getUUID().equals(victim.getUUID())) return false;
        if (areInSameParty(attacker, victim)) return false;

        PlayerPvPData attackerData = getOrCreate(attacker.getUUID());
        PlayerPvPData victimData = getOrCreate(victim.getUUID());

        if (victimData.isPkFlagged()) return true;
        if (!victimData.pvpEnabled) return false;
        return attackerData.pvpEnabled;
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
        syncPvPStateToClient(player);
    }

    public void syncPvPStateToClient(ServerPlayer player) {
        if (player == null) return;
        PlayerPvPData data = getOrCreate(player.getUUID());
        int secLeft = 0;
        if (data.isPkFlagged()) {
            secLeft = Math.max(1, (int) ((data.pkExpireAtMillis - System.currentTimeMillis()) / 1000L));
        }
        ModNetwork.sendToPlayer(player, new SyncPvPStatePacket(data.pvpEnabled, secLeft));
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
    /* 🔍 Attacker & Owner Resolution Engine                              */
    /* ------------------------------------------------------------------ */

    /**
     * ค้นหาผู้เล่นที่เป็นต้นตอของความเสียหาย (Attacker Root Player)
     * รองรับความเสียหายทุกรูปแบบ ทั้งการโจมตีปกติและเวทมนตร์/สกิลจาก Mod:
     * 1. การโจมตีประชิด / ยิงธนู / ปาตรีศูล
     * 2. เวทมนตร์ลูกศร/กระสุนเวท (Iron's Spells: Fireball, Magic Missile, Icicle, Ray)
     * 3. เวทมนตร์พื้นที่ / หมอกพิษ / พลังงานตกค้าง (Iron's Spells AOE / Cloud / Lingering)
     * 4. สิ่งมีชีวิตที่ถูกอัญเชิญ (Iron's Spells Summons: Vex, Zombie, Skeleton, Polar Bear, Swords)
     * 5. สัตว์เลี้ยงและสิ่งอัญเชิญทั่วไป (Tamable / Ownable Entities: Wolves, Horses, Minions)
     * 6. สกิลและพลังจาก Origins / Origins++ / Medieval Origins (Origins powers, passive damage, special traits)
     * 7. สายฟ้าที่เกิดจากสกิล (Lightning Bolt created by spells)
     * 8. การสะท้อน / แฉลบ / ลูกโซ่ (Apotheosis affixes, chain damage)
     */
    public static ServerPlayer resolveAttackingPlayer(DamageSource source) {
        if (source == null) return null;

        // 1. ตรวจสอบ source.getEntity() (ต้นตอความเสียหายที่แท้จริงตามที่ระบบระบุ)
        ServerPlayer player = resolvePlayerFromEntity(source.getEntity());
        if (player != null) return player;

        // 2. ตรวจสอบ source.getDirectEntity() (ตัวส่งความเสียหาย เช่น กระสุน, หมอก, คาถา)
        player = resolvePlayerFromEntity(source.getDirectEntity());
        if (player != null) return player;

        return null;
    }

    /**
     * แกะรอย Entity ใดๆ ย้อนกลับหา ServerPlayer สูงสุด 5 ชั้น (Recursive Owner Resolution)
     */
    public static ServerPlayer resolvePlayerFromEntity(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof ServerPlayer sp) return sp;

        Entity current = entity;
        for (int depth = 0; depth < 5; depth++) {
            if (current instanceof ServerPlayer sp) {
                return sp;
            }

            Entity owner = null;

            // 1. Projectile (Vanilla Arrows, Tridents, Iron's Spells AbstractMagicProjectile, AoeEntity)
            if (current instanceof Projectile projectile) {
                owner = projectile.getOwner();
            }
            // 2. TraceableEntity (Vanilla & Modded traceable objects)
            else if (current instanceof TraceableEntity traceable) {
                owner = traceable.getOwner();
            }
            // 3. OwnableEntity (Vanilla tamable pets, mounts, modded summons)
            else if (current instanceof OwnableEntity ownable) {
                owner = ownable.getOwner();
            }
            // 4. AreaEffectCloud (หมอกเวทมนตร์ / ยาพิษตกค้าง)
            else if (current instanceof AreaEffectCloud aec) {
                owner = aec.getOwner();
            }
            // 5. LightningBolt (สายฟ้าที่ถูกเรียกโดยสกิลหรือคาถา)
            else if (current instanceof LightningBolt lightning) {
                owner = lightning.getCause();
            }

            // 6. ตรวจสอบ Modded APIs & Reflection (Iron's Spells IMagicSummon, Custom Spells, Origins)
            if (owner == null) {
                owner = probeModdedOwner(current);
            }

            if (owner == null || owner == current) {
                break;
            }
            current = owner;
        }

        if (current instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    /**
     * ค้นหาเจ้าของหรือผู้ร่าย (Caster / Summoner / Owner) ผ่าน Modded API และ Reflection
     * รองรับ:
     * - Iron's Spells 'n Spellbooks: IMagicSummon (getSummoner()), AbstractSpellEntity, AoeEntity
     * - Origins / Origins++: Custom projectile / power sources
     * - Apotheosis & Custom Mod Summons / Pets
     */
    private static Entity probeModdedOwner(Entity entity) {
        if (entity == null) return null;
        Class<?> clazz = entity.getClass();

        // 1. ลองเรียก getSummoner() (เช่น io.redspace.ironsspellbooks.entity.mobs.IMagicSummon)
        try {
            Method m = clazz.getMethod("getSummoner");
            Object res = m.invoke(entity);
            if (res instanceof Entity e) return e;
        } catch (Throwable ignored) {}

        // 2. ลองเรียก getCaster() (พบบ่อยใน Modded Spell Entities)
        try {
            Method m = clazz.getMethod("getCaster");
            Object res = m.invoke(entity);
            if (res instanceof Entity e) return e;
        } catch (Throwable ignored) {}

        // 3. ลองเรียก getOwner() หากไม่ได้ implement Traceable/Ownable แต่มี method นี้
        try {
            Method m = clazz.getMethod("getOwner");
            Object res = m.invoke(entity);
            if (res instanceof Entity e) return e;
        } catch (Throwable ignored) {}

        // 4. ลองเรียก getTrueOwner()
        try {
            Method m = clazz.getMethod("getTrueOwner");
            Object res = m.invoke(entity);
            if (res instanceof Entity e) return e;
        } catch (Throwable ignored) {}

        // 5. ตรวจสอบ UUID เจ้าของ (getOwnerUUID หรือ getSummonerUUID) แล้ว lookup กับ Server
        try {
            UUID ownerUuid = null;
            try {
                Method m = clazz.getMethod("getOwnerUUID");
                Object res = m.invoke(entity);
                if (res instanceof UUID u) ownerUuid = u;
            } catch (Throwable ignored) {}

            if (ownerUuid == null) {
                try {
                    Method m = clazz.getMethod("getSummonerUUID");
                    Object res = m.invoke(entity);
                    if (res instanceof UUID u) ownerUuid = u;
                } catch (Throwable ignored) {}
            }

            if (ownerUuid != null && entity.getServer() != null) {
                return entity.getServer().getPlayerList().getPlayer(ownerUuid);
            }
        } catch (Throwable ignored) {}

        return null;
    }

    /**
     * สั่งให้ Mob / Summon / Pet ยกเลิกความก้าวร้าวต่อเป้าหมาย เมื่อการโจมตีถูกปฏิเสธ
     */
    private static void dropMobAggro(DamageSource source, ServerPlayer victim) {
        if (source == null || victim == null) return;
        if (source.getDirectEntity() instanceof Mob mob) {
            if (mob.getTarget() == victim) {
                mob.setTarget(null);
            }
            mob.setLastHurtByMob(null);
        }
        if (source.getEntity() instanceof Mob mob) {
            if (mob.getTarget() == victim) {
                mob.setTarget(null);
            }
            mob.setLastHurtByMob(null);
        }
    }

    /* ------------------------------------------------------------------ */
    /* ⚔️ Event Handlers                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * ดักจับการคลิกโจมตี / เหวี่ยงอาวุธใส่ผู้เล่นอื่น
     * ป้องกันการผลัก (Knockback), แอนิเมชันดาบ, เสียงโดนฟัน หากไม่ได้เปิดโหมด PvP ทั้งคู่
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player rawAttacker = event.getEntity();
        if (!(rawAttacker instanceof ServerPlayer attacker)) return;

        Entity target = event.getTarget();

        // Target อาจเป็น Player หรือเป็น Pet/Summon ของ Player
        ServerPlayer victim = null;
        if (target instanceof ServerPlayer sp) {
            victim = sp;
        } else {
            victim = resolvePlayerFromEntity(target);
        }

        if (victim == null || attacker.getUUID().equals(victim.getUUID())) return;

        // 1. สมาชิกปาร์ตี้เดียวกัน -> ตีกันไม่ได้เด็ดขาด 100% (รวมถึงสัตว์เลี้ยง/ซัมมอนของเพื่อนในปาร์ตี้)
        if (areInSameParty(attacker, victim)) {
            event.setCanceled(true);
            return;
        }

        PlayerPvPData attackerData = getOrCreate(attacker.getUUID());
        PlayerPvPData victimData = getOrCreate(victim.getUUID());

        // 2. หากเป้าหมายเป็น Free PK (🗡️) -> อนุญาตให้ทุกคนรุมตีได้
        if (victimData.isPkFlagged()) {
            return;
        }

        // 3. หากผู้ถูกโจมตีอยู่ในโหมดสงบ (ไม่ได้เปิด Alt + C) -> ห้ามใครตีเด็ดขาด (อมตะ PVE)
        if (!victimData.pvpEnabled) {
            event.setCanceled(true);
            attacker.displayClientMessage(Component.literal("§7🛡️ ผู้เล่นคนนี้อยู่ในโหมดสงบ (ไม่ได้เปิด PvP) ไม่สามารถโจมตีได้!"), true);
            return;
        }

        // 4. หากผู้โจมตีไม่ได้เปิด Alt + C -> ห้ามโจมตีคนอื่น
        if (!attackerData.pvpEnabled) {
            event.setCanceled(true);
            attacker.displayClientMessage(Component.literal("§c❌ คุณปิดโหมด PvP อยู่! (กด Alt + C หรือพิมพ์ /pvp เพื่อเปิดโหมดต่อสู้)"), true);
        }
    }

    /**
     * ดักจับความเสียหายเบื้องต้น (รวมการยิงธนู / กระสุนเวทมนตร์ / ทักษะ / ซัมมอน / สกิล Origin)
     * การ Canceled ในระดับ LivingAttackEvent จะหยุดยั้งความเสียหาย, เอฟเฟกต์กระเด็น, และการติดสถานะพิษ/ไฟ
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // ค้นหาเป้าหมาย (อาจเป็น ServerPlayer หรือสัตว์เลี้ยง/ซัมมอนของผู้เล่น)
        ServerPlayer victim = null;
        if (event.getEntity() instanceof ServerPlayer sp) {
            victim = sp;
        } else {
            victim = resolvePlayerFromEntity(event.getEntity());
        }
        if (victim == null) return;

        // ค้นหาผู้โจมตี (ต้นตอ ไม่ว่าจะเป็นคนยิง, คนร่ายเวท, สกิล Origin, หรือซัมมอน)
        ServerPlayer attacker = resolveAttackingPlayer(event.getSource());
        if (attacker == null || attacker.getUUID().equals(victim.getUUID())) return;

        // 1. สมาชิกปาร์ตี้เดียวกัน (FTB Teams & Vanilla) -> ยกเลิก 100%
        if (areInSameParty(attacker, victim)) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
            return;
        }

        PlayerPvPData attackerData = getOrCreate(attacker.getUUID());
        PlayerPvPData victimData = getOrCreate(victim.getUUID());

        // 2. Free PK (🗡️) อนุญาตให้โจมตีได้
        if (victimData.isPkFlagged()) return;

        // 3. ฝ่ายถูกโจมตีอยู่ในโหมดสงบ (PvP OFF) -> ป้องกัน 100%
        if (!victimData.pvpEnabled) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
            return;
        }

        // 4. ฝ่ายโจมตีปิดโหมด PvP -> ห้ามทำร้ายใคร
        if (!attackerData.pvpEnabled) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
        }
    }

    /**
     * ดักจับความเสียหายเมื่อผ่านเกราะ/การคำนวณขั้นสุดท้าย (Safety Net & Free PK Trigger)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // ค้นหาเป้าหมาย
        ServerPlayer victim = null;
        if (event.getEntity() instanceof ServerPlayer sp) {
            victim = sp;
        } else {
            victim = resolvePlayerFromEntity(event.getEntity());
        }
        if (victim == null) return;

        // ค้นหาผู้โจมตี
        ServerPlayer attacker = resolveAttackingPlayer(event.getSource());
        if (attacker == null || attacker.getUUID().equals(victim.getUUID())) return;

        // 1. สมาชิกปาร์ตี้เดียวกัน -> ตีกันไม่ได้เด็ดขาด (Cancel Damage 100%)
        if (areInSameParty(attacker, victim)) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
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

        // 3. หากผู้ถูกโจมตีอยู่ในโหมดสงบ (ไม่ได้เปิด PvP) -> ป้องกัน 100%
        if (!victimData.pvpEnabled) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
            attacker.displayClientMessage(Component.literal("§7🛡️ ผู้เล่นคนนี้อยู่ในโหมดสงบ (ไม่ได้เปิด PvP) ไม่สามารถโจมตีได้!"), true);
            return;
        }

        // 4. กรณีผู้โจมตีไม่ได้เปิดโหมด PvP (PvP OFF):
        if (!attackerData.pvpEnabled) {
            event.setCanceled(true);
            dropMobAggro(event.getSource(), victim);
            attacker.displayClientMessage(Component.literal("§c❌ คุณปิดโหมด PvP อยู่! (กด Alt + C หรือพิมพ์ /pvp เพื่อเปิดโหมดต่อสู้)"), true);
            return;
        }

        // 5. ผู้เล่นเปิดโหมด PvP ทั้งคู่ -> อนุญาตให้ต่อสู้กันได้ และผู้เปิดฉากโจมตีติดสถานะ Free PK 45 วินาที
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
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerName(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncPlayerName(player);
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
                    syncPvPStateToClient(player);
                }
            }
        }
    }
}
