package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🥷 1. Shadow Veil (เงามรณะพรางตา)
 * - Invisibility + Shift (ย่อง) ➔ Mob มองไม่เห็น 100% (Visibility = 0, หัวม็อบไม่หันมาจ้อง)
 * - Back Attack (Backstab) x2.5 ถึง x3.25 (ตามระดับ Shadow Edge)
 * - การแจ้งเตือนสถานะการโจมตี (Critical, Back Attack, Ambush, Exposed) บน Action Bar
 * - Balance & AI Alert: เมื่อโจมตี สถานะพรางตัวจะหลุดทันที ติดสถานะ In-Combat 4 วินาที,
 *   ม็อบเป้าหมายจะหันมาโจมตีสวนทันที และเรียกม็อบรอบตัว 12 บล็อกให้ช่วยรุม
 * - Drawback: เดินช้าลงขณะย่อง, หลังโจมตีติด Exposed โดนดาเมจแรงขึ้น 15% (3-5 วินาที)
 */
public class StealthHandler {

    // เวลาหมดอายุของดีบัฟ Exposed (โดนดาเมจแรงขึ้น 15%)
    public static final Map<UUID, Long> EXPOSED_UNTIL = new ConcurrentHashMap<>();

    // เวลาหมดอายุของสถานะ In-Combat (ไม่สามารถเข้าสู่สถานะ Stealth ได้ขณะต่อสู้)
    public static final Map<UUID, Long> COMBAT_UNTIL = new ConcurrentHashMap<>();

    // บันทึกว่าผู้เล่นโจมตีออกมาจาก Stealth หรือไม่ (ไว้คำนวณใน LivingDamageEvent)
    public static final Map<UUID, Long> WAS_STEALTHED = new ConcurrentHashMap<>();

    // บันทึกว่าการโจมตีนี้เป็น Critical Hit หรือไม่
    public static final Map<UUID, Long> CRITICAL_ATTACK = new ConcurrentHashMap<>();

    // ป้องกันการแจ้งเตือน Stealth บน Action Bar ถี่เกินไป
    public static final Map<UUID, Long> LAST_STEALTH_NOTIFY = new ConcurrentHashMap<>();

    /**
     * ตรวจสอบว่าผู้เล่นกำลังอยู่ในสถานะ In-Combat หรือไม่
     */
    public static boolean isInCombat(Player player) {
        Long until = COMBAT_UNTIL.get(player.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    /**
     * ตั้งค่าสถานะ In-Combat
     */
    public static void setInCombat(Player player, long durationMs) {
        COMBAT_UNTIL.put(player.getUUID(), System.currentTimeMillis() + durationMs);
    }

    /**
     * ตรวจสอบว่าผู้เล่นอยู่ในสถานะ Stealth สมบูรณ์หรือไม่ (ต้องไม่ติด In-Combat)
     */
    public static boolean isStealthed(Player player) {
        return player.hasEffect(MobEffects.INVISIBILITY) && player.isCrouching() && !isInCombat(player);
    }

    /**
     * ตัดการมองเห็นของ Mob ลงเหลือ 0% ขณะที่ผู้เล่นอยู่ใน Stealth
     * ป้องกันไม่ให้ AI (รวมถึง LookAtPlayerGoal) หันหน้ามาจ้องผู้เล่น
     */
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isStealthed(player)) {
                event.modifyVisibility(0.0);
            }
        }
    }

    /**
     * ดักจับการล็อกเป้าของม็อบ: ม็อบจะไม่สามารถเปลี่ยนเป้ามาหาผู้เล่นที่อยู่ใน Stealth ได้
     */
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            if (isStealthed(player)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * เคลียร์เป้าหมายและการเดินของม็อบ หากผู้เล่นเข้าสู่สถานะ Stealth
     */
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            if (mob.getTarget() instanceof Player player) {
                if (isStealthed(player)) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
            }
        } else if (event.getEntity() instanceof Player player && !player.level().isClientSide()) {
            // แจ้งเตือนข้อความสถานะ Stealth ให้ผู้เล่นทราบอย่างนุ่มนวล
            if (isStealthed(player)) {
                long now = System.currentTimeMillis();
                Long lastNotify = LAST_STEALTH_NOTIFY.get(player.getUUID());
                if (lastNotify == null || (now - lastNotify) > 10000L) {
                    LAST_STEALTH_NOTIFY.put(player.getUUID(), now);
                    player.displayClientMessage(Component.literal("§8§l🥷 SHADOW VEIL §7(Hidden & Undetectable)"), true);
                }
            }
        }
    }

    /**
     * ดักจับ Critical Hit
     */
    public static void onCriticalHit(CriticalHitEvent event) {
        if (event.isVanillaCritical() || event.getResult() == net.minecraftforge.eventbus.api.Event.Result.ALLOW) {
            CRITICAL_ATTACK.put(event.getEntity().getUUID(), System.currentTimeMillis() + 600L);
        }
    }

    /**
     * ดักจับก่อนที่การโจมตีจะสร้างดาเมจ เพื่อปลดสถานะ Stealth และเปิด In-Combat ก่อนที่ Mob จะโดนตี
     */
    public static void onPlayerPreAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (isStealthed(player)) {
            WAS_STEALTHED.put(player.getUUID(), System.currentTimeMillis() + 800L);
            player.removeEffect(MobEffects.INVISIBILITY);
        }

        // เข้าสู่ In-Combat ทันที เพื่อให้การตอบโต้ของ Mob ทำงานได้ทันที
        setInCombat(player, 4000L);
    }

    /**
     * คำนวณความเสียหายและการแจ้งเตือนเมื่อโจมตีโดนเป้าหมาย
     */
    public static void onPlayerAttack(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();

        long now = System.currentTimeMillis();
        Long stealthTime = WAS_STEALTHED.remove(player.getUUID());
        boolean wasStealthed = (stealthTime != null && (now - stealthTime) < 800L) || isStealthed(player);

        Long critTime = CRITICAL_ATTACK.remove(player.getUUID());
        boolean isCritical = critTime != null && (now - critTime) < 600L;
        if (!isCritical) {
            // Fallback ตรวจสอบเงื่อนไข Critical เบื้องต้น
            isCritical = player.fallDistance > 0.0F && !player.onGround() && !player.isInWater() && !player.isPassenger();
        }

        // ตรวจสอบมุม Back Attack (ยืนอยู่ด้านหลัง และหันหน้าไปทิศเดียวกัน)
        boolean isBackAttack = checkBackAttack(player, target);

        ItemStack weapon = player.getMainHandItem();
        int shadowEdgeLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHADOW_EDGE.get(), weapon);

        if (wasStealthed) {
            // ปลด Invisibility ทันที (เผื่อหลงเหลือ)
            player.removeEffect(MobEffects.INVISIBILITY);
            setInCombat(player, 4000L);

            // ติดดีบัฟ Exposed (ลดเวลาลงตามระดับ Shadow Edge)
            int durationSec = Math.max(2, 5 - shadowEdgeLvl);
            EXPOSED_UNTIL.put(player.getUUID(), now + (durationSec * 1000L));

            if (isBackAttack) {
                // 🗡️ SHADOW BACKSTAB
                float backstabMult = 2.5f + (shadowEdgeLvl * 0.25f);
                event.setAmount(event.getAmount() * backstabMult);

                float displayMult = isCritical ? (backstabMult * 1.5f) : backstabMult;
                String title = isCritical ? "§c§l💥 CRITICAL BACK ATTACK!" : "§c§l🗡️ SHADOW BACKSTAB!";
                player.displayClientMessage(Component.literal(String.format("%s §e(×%.2f DMG) §4[EXPOSED]", title, displayMult)), true);

                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                            12, 0.3, 0.3, 0.3, 0.1);
                    sl.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                            10, 0.3, 0.3, 0.3, 0.2);
                    sl.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                            12, 0.3, 0.3, 0.3, 0.05);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.3f, 0.7f);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0f, 1.2f);
                }
            } else {
                // ⚔️ AMBUSH HIT (โจมตีจาก Stealth แต่ไม่ใช่ข้างหลัง)
                float ambushMult = 1.25f;
                event.setAmount(event.getAmount() * ambushMult);

                player.displayClientMessage(Component.literal("§e§l⚔️ AMBUSH HIT! §7(Stealth Broken) §4[EXPOSED]"), true);

                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                            8, 0.3, 0.3, 0.3, 0.05);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0f, 0.9f);
                }
            }

            // AI Aggro & Pack Alert: ให้ม็อบเป้าหมายและพวกพ้องรอบตัว 12 บล็อกตื่นตัวและรุมโจมตีผู้เล่น
            alertMobs(player, target);

        } else if (isBackAttack) {
            // 🗡️ COMBAT BACK ATTACK (โจมตีตลบหลังระหว่างการต่อสู้ทั่วไป)
            float flankMult = 1.25f + (shadowEdgeLvl * 0.15f);
            event.setAmount(event.getAmount() * flankMult);

            float displayMult = isCritical ? (flankMult * 1.5f) : flankMult;
            String title = isCritical ? "§6§l💥 CRITICAL BACK ATTACK!" : "§6§l🗡️ BACK ATTACK!";
            player.displayClientMessage(Component.literal(String.format("%s §e(×%.2f DMG)", title, displayMult)), true);

            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                        target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                        8, 0.25, 0.25, 0.25, 0.1);
                sl.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + target.getEyeHeight() * 0.7, target.getZ(),
                        6, 0.2, 0.2, 0.2, 0.1);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1f, 1.0f);
            }
        } else if (isCritical) {
            // ⚡ REGULAR CRITICAL HIT
            player.displayClientMessage(Component.literal("§6§l⚡ CRITICAL HIT! §e(×1.50 DMG)"), true);
            if (target.level() instanceof ServerLevel sl) {
                sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.1f, 1.2f);
            }
        }
    }

    /**
     * ดักจับดาเมจที่ผู้เล่นได้รับ (เมื่อติด Exposed จะโดนแรงขึ้น 15% และแจ้งเตือน)
     */
    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // โดนโจมตี ➔ เข้าสู่สถานะ In-Combat
        setInCombat(player, 4000L);

        // หากติดสถานะ Exposed โดนดาเมจแรงขึ้น 15% พร้อมเตือนบน Action Bar
        Long exposedUntil = EXPOSED_UNTIL.get(player.getUUID());
        if (exposedUntil != null) {
            if (System.currentTimeMillis() < exposedUntil) {
                event.setAmount(event.getAmount() * 1.15f);
                player.displayClientMessage(Component.literal("§c§l⚠️ EXPOSED! §7(+15% Damage Taken)"), true);
                if (player.level() instanceof ServerLevel sl) {
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 1.5f);
                }
            } else {
                EXPOSED_UNTIL.remove(player.getUUID());
            }
        }

        // หากโดนดาเมจขณะล่องหน สถานะจะหลุดทันที และติด Exposed
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            EXPOSED_UNTIL.put(player.getUUID(), System.currentTimeMillis() + 4000L);
            player.displayClientMessage(Component.literal("§c§l⚠️ STEALTH BROKEN! §4[EXPOSED]"), true);
        }
    }

    /**
     * คำนวณว่าผู้เล่นอยู่ด้านหลังของเป้าหมายหรือไม่
     * 1) ตำแหน่งผู้เล่นต้องอยู่ด้านหลังตัวเป้าหมาย (Relative Position Dot < 0)
     * 2) ทิศทางหันหน้าของผู้เล่นต้องหันไปทางเดียวกับเป้าหมาย (Look Angle Dot > 0.3)
     */
    private static boolean checkBackAttack(Player player, LivingEntity target) {
        Vec3 playerLook = player.getViewVector(1.0F);
        Vec3 targetLook = target.getViewVector(1.0F);

        // Normalize บนระนาบ 2D (XZ)
        double pX = playerLook.x;
        double pZ = playerLook.z;
        double pLen = Math.sqrt(pX * pX + pZ * pZ);
        if (pLen > 0.001) {
            pX /= pLen;
            pZ /= pLen;
        }

        double tX = targetLook.x;
        double tZ = targetLook.z;
        double tLen = Math.sqrt(tX * tX + tZ * tZ);
        if (tLen > 0.001) {
            tX /= tLen;
            tZ /= tLen;
        }

        // เวกเตอร์จากเป้าหมายไปยังผู้เล่น
        double toPlayerX = player.getX() - target.getX();
        double toPlayerZ = player.getZ() - target.getZ();
        double toLen = Math.sqrt(toPlayerX * toPlayerX + toPlayerZ * toPlayerZ);
        if (toLen > 0.001) {
            toPlayerX /= toLen;
            toPlayerZ /= toLen;
        }

        // ตำแหน่ง: ผู้เล่นต้องอยู่ด้านหลังเป้าหมาย (dot product กับทิศหันหน้าของเป้าหมาย < 0.1)
        double posDot = (toPlayerX * tX) + (toPlayerZ * tZ);

        // มุมมอง: ผู้เล่นต้องมองไปทางเดียวกับที่เป้าหมายมอง (dot product > 0.35)
        double lookDot = (pX * tX) + (pZ * tZ);

        return posDot < 0.15 && lookDot > 0.35;
    }

    /**
     * ปลุกม็อบเป้าหมายและฝูงม็อบรอบตัว 12 บล็อก ให้หันมารุมผู้เล่นทันทีที่การซุ่มโจมตีเกิดขึ้น
     */
    private static void alertMobs(Player player, LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setTarget(player);
            mob.getNavigation().moveTo(player, 1.25);
        }

        if (target.level() instanceof ServerLevel sl) {
            List<Mob> nearby = sl.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(12.0));
            for (Mob ally : nearby) {
                if (ally != target && ally.isAlive() && ally.getTarget() == null) {
                    ally.setTarget(player);
                    ally.getNavigation().moveTo(player, 1.15);
                }
            }
        }
    }
}
