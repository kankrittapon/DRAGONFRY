package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🥷 1. Shadow Veil (เงามรณะพรางตา)
 * Invisibility + Shift ➔ Mob untargetable 100% (Detection = 0)
 * Backstab x2.5 (เพิ่มขึ้นตาม Shadow Edge Enchantment)
 * Drawback: เดินช้าลง 20%, โดนตี/โจมตีหลุดสถานะ + ติด Exposed (โดนดาเมจแรงขึ้น 15%)
 */
public class StealthHandler {

    // เก็บเวลาหมดอายุของดีบัฟ Exposed (โดนดาเมจแรงขึ้น 15%)
    public static final Map<UUID, Long> EXPOSED_UNTIL = new ConcurrentHashMap<>();

    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            // หากผู้เล่นมีบัฟ Invisibility และกำลังย่อง (Shift) ม็อบจะหลุดการล็อกเป้า 100%
            if (isStealthed(player)) {
                event.setCanceled(true);
            }
        }
    }

    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && mob.getTarget() instanceof Player player) {
            if (isStealthed(player)) {
                mob.setTarget(null);
            }
        }
    }

    public static void onPlayerAttack(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();

        if (isStealthed(player)) {
            // ตรวจสอบมุม Backstab (ทิศทางหันหน้าของผู้เล่นกับม็อบใกล้เคียงกัน < 60 องศา)
            Vec3 playerLook = player.getLookAngle().normalize();
            Vec3 targetLook = target.getLookAngle().normalize();
            double dot = playerLook.dot(targetLook);

            if (dot > 0.5) { // มุมแทงข้างหลัง
                ItemStack weapon = player.getMainHandItem();
                int shadowEdgeLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHADOW_EDGE.get(), weapon);

                float multiplier = 2.5f + (shadowEdgeLvl * 0.25f);
                event.setAmount(event.getAmount() * multiplier);

                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                            target.getX(), target.getY() + target.getEyeHeight(), target.getZ(),
                            10, 0.3, 0.3, 0.3, 0.1);
                    sl.sendParticles(ParticleTypes.SMOKE,
                            target.getX(), target.getY() + target.getEyeHeight(), target.getZ(),
                            15, 0.4, 0.4, 0.4, 0.05);
                    sl.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.7f);
                }
            }

            // หลุดสถานะล่องหนเมื่อโจมตี
            player.removeEffect(MobEffects.INVISIBILITY);

            // ติด Exposed ชั่วคราว (ลดเวลาลงตาม Shadow Edge)
            ItemStack weapon = player.getMainHandItem();
            int shadowEdgeLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.SHADOW_EDGE.get(), weapon);
            int durationSec = Math.max(2, 5 - shadowEdgeLvl);
            EXPOSED_UNTIL.put(player.getUUID(), System.currentTimeMillis() + (durationSec * 1000L));
        }
    }

    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // หากติดสถานะ Exposed โดนดาเมจแรงขึ้น 15%
        Long exposedUntil = EXPOSED_UNTIL.get(player.getUUID());
        if (exposedUntil != null) {
            if (System.currentTimeMillis() < exposedUntil) {
                event.setAmount(event.getAmount() * 1.15f);
            } else {
                EXPOSED_UNTIL.remove(player.getUUID());
            }
        }

        // หากโดนดาเมจขณะล่องหน สถานะจะหลุดทันที
        if (isStealthed(player)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            EXPOSED_UNTIL.put(player.getUUID(), System.currentTimeMillis() + 5000L);
        }
    }

    public static boolean isStealthed(Player player) {
        return player.hasEffect(MobEffects.INVISIBILITY) && player.isCrouching();
    }
}
