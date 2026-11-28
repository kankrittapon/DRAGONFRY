package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ⚡🪶 Mobility Stances:
 * 4. Overcharge Rush (Speed II+ ➔ Step Assist 1.0, Electric Dash ชนสตัน; เมื่อบัฟหมดติด Exhaustion)
 * 6. Zephyr Feather (Slow Falling / Jump ➔ Double Jump, ไม่โดน Fall Damage; ปลิวกระเด็นแรงขึ้น x2 กลางอากาศ)
 */
public class MobilityHandler {

    private static final Map<UUID, Boolean> HAD_SPEED_LAST_TICK = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> DOUBLE_JUMPS_REMAINING = new ConcurrentHashMap<>();

    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 4. Overcharge Rush (Speed II+)
        MobEffectInstance speedEffect = player.getEffect(MobEffects.MOVEMENT_SPEED);
        boolean hasOvercharge = speedEffect != null && speedEffect.getAmplifier() >= 1;

        if (hasOvercharge) {
            HAD_SPEED_LAST_TICK.put(player.getUUID(), true);

            // Step Assist: ก้าวข้าม 1.0 บล็อกอัตโนมัติ
            if (player.maxUpStep() < 1.0f) {
                player.setMaxUpStep(1.0f);
            }

            // Electric Dash: วิ่งชนม็อบทำดาเมจไฟฟ้าและ Stun 0.5s
            if (player.isSprinting() && !player.level().isClientSide() && player.tickCount % 6 == 0) {
                AABB dashBox = player.getBoundingBox().inflate(0.8, 0.2, 0.8);
                List<Mob> collidedMobs = player.level().getEntitiesOfClass(Mob.class, dashBox, m -> m.isAlive() && !m.isAlliedTo(player));

                for (Mob mob : collidedMobs) {
                    mob.hurt(player.damageSources().lightningBolt(), 3.5f);
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 4)); // Stun 0.75s

                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                mob.getX(), mob.getY() + 0.8, mob.getZ(),
                                6, 0.2, 0.2, 0.2, 0.1);
                        sl.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7f, 1.8f);
                    }
                }
            }
        } else {
            // คืนค่าความสูงการก้าวตามปกติ
            if (player.maxUpStep() > 0.6f) {
                player.setMaxUpStep(0.6f);
            }

            // Drawback: เมื่อบัฟ Speed II หมด จะติดอาการ Exhaustion
            Boolean hadSpeed = HAD_SPEED_LAST_TICK.remove(player.getUUID());
            if (hadSpeed != null && hadSpeed && !player.level().isClientSide()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0));
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }

        // 6. Zephyr Feather: รีเซ็ต Double Jump เมื่อเท้าแตะพื้น
        if (player.onGround()) {
            DOUBLE_JUMPS_REMAINING.put(player.getUUID(), 1);
        }
    }

    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player) {
            // ยกเลิก Fall Damage ทั้งหมดหากมีบัฟ Slow Falling หรือ Jump Boost
            if (player.hasEffect(MobEffects.SLOW_FALLING) || player.hasEffect(MobEffects.JUMP)) {
                event.setCanceled(true);
            }
        }
    }

    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 6. Zephyr Feather Drawback: ลอยตัวกลางอากาศแล้วโดนตี จะกระเด็นไกลขึ้น x2 (ลดทอนด้วย Aerial Grace)
        if (!player.onGround() && (player.hasEffect(MobEffects.SLOW_FALLING) || player.hasEffect(MobEffects.JUMP))) {
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            int graceLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.AERIAL_GRACE.get(), boots);

            float extraKnockback = Math.max(1.0f, 2.0f - (graceLvl * 0.5f));
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(delta.x * extraKnockback, delta.y, delta.z * extraKnockback);
        }
    }

    /**
     * ดำเนินการ Double Jump (เรียกจาก Client Key หรือ Packet)
     */
    public static boolean tryDoubleJump(Player player) {
        if (player.onGround()) return false;
        if (!player.hasEffect(MobEffects.SLOW_FALLING) && !player.hasEffect(MobEffects.JUMP)) return false;

        Integer jumps = DOUBLE_JUMPS_REMAINING.getOrDefault(player.getUUID(), 0);
        if (jumps > 0) {
            DOUBLE_JUMPS_REMAINING.put(player.getUUID(), jumps - 1);

            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            int graceLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.AERIAL_GRACE.get(), boots);
            double boost = 0.55 + (graceLvl * 0.15);

            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(new Vec3(look.x * 0.4, boost, look.z * 0.4));
            player.fallDistance = 0;

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 8, 0.2, 0.1, 0.2, 0.05);
                sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
            }
            return true;
        }
        return false;
    }
}
