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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🌊🔥❄️ Elemental Stances:
 * 5. Phoenix Soul (Fire Resistance + ไฟ/ลาวา ➔ ฮีลเลือด Regen II, ฟันระเบิดไฟ; แพ้น้ำโดน Steam burn)
 * 9. Abyssal Dweller (Water Breathing ➔ ดำน้ำไม่จำกัด, ว่ายเร็ว Dolphin, ตีใต้น้ำแรง +35%; บนบกช้า แพ้ไฟ)
 * 10. Frostbite Aura (ออร่าไอเย็น 4 บล็อก ➔ ม็อบติด Freeze, Slow II, ตีช้าลง; วิ่งนานติด Slow ตัวเอง)
 */
public class ElementalHandler {

    private static final Map<UUID, Integer> SPRINT_TICKS = new ConcurrentHashMap<>();

    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) player.level();

        // 5. Phoenix Soul
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            if (player.isInLava() || player.isOnFire()) {
                // ฮีลเลือดทุก 1 วินาที (20 ticks)
                if (player.tickCount % 20 == 0) {
                    player.heal(1.5f);
                    sl.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 0.8, player.getZ(),
                            5, 0.2, 0.2, 0.2, 0.02);
                }
            } else if (player.isInWaterOrRain()) {
                // Drawback: แพ้น้ำ เกิดไอน้ำช็อก
                if (player.tickCount % 20 == 0) {
                    ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                    int pyreticLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.PYRETIC_CORE.get(), chest);
                    float dmg = Math.max(0.5f, 2.0f - (pyreticLvl * 0.6f));

                    player.hurt(player.damageSources().generic(), dmg);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));

                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                            player.getX(), player.getY() + 1.0, player.getZ(),
                            4, 0.2, 0.3, 0.2, 0.05);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
            }
        }

        // 9. Abyssal Dweller
        if (player.hasEffect(MobEffects.WATER_BREATHING)) {
            if (player.isInWater()) {
                player.setAirSupply(player.getMaxAirSupply());
                player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, false));
            } else {
                // Drawback: บนบกเดินช้าลงเล็กน้อย (ตัวแห้ง)
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
            }
        }

        // 10. Frostbite Aura (ทำงานทุก 10 ticks)
        if (player.getPersistentData().getBoolean("omnicore_frost_aura") || player.getItemBySlot(EquipmentSlot.LEGS).isEnchanted()) {
            if (player.tickCount % 10 == 0) {
                AABB auraBox = player.getBoundingBox().inflate(4.0);
                List<Mob> nearbyMobs = sl.getEntitiesOfClass(Mob.class, auraBox, m -> m.isAlive() && !m.isAlliedTo(player));

                for (Mob mob : nearbyMobs) {
                    mob.setTicksFrozen(Math.min(mob.getTicksRequiredToFreeze() + 60, mob.getTicksFrozen() + 30));
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));

                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                            mob.getX(), mob.getY() + 0.8, mob.getZ(),
                            2, 0.2, 0.2, 0.2, 0.02);
                }
            }

            // Drawback: วิ่งสปรินต์นานเกิน 4 วินาที (80 ticks) ร่างกายจะชา
            if (player.isSprinting()) {
                int ticks = SPRINT_TICKS.getOrDefault(player.getUUID(), 0) + 1;
                SPRINT_TICKS.put(player.getUUID(), ticks);
                if (ticks > 80) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    sl.sendParticles(ParticleTypes.ITEM_SNOWBALL, player.getX(), player.getY() + 0.5, player.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                }
            } else {
                SPRINT_TICKS.remove(player.getUUID());
            }
        }
    }

    public static void onPlayerAttack(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();

        // 5. Phoenix Soul: โจมตีระเบิดไฟ 3 บล็อก
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE) && (player.isOnFire() || player.isInLava())) {
            target.setSecondsOnFire(5);
            if (player.level() instanceof ServerLevel sl) {
                AABB blastBox = target.getBoundingBox().inflate(3.0);
                List<Mob> extraTargets = sl.getEntitiesOfClass(Mob.class, blastBox, m -> m != target && m.isAlive());
                for (Mob extra : extraTargets) {
                    extra.setSecondsOnFire(3);
                    extra.hurt(player.damageSources().inFire(), 3.0f);
                }
                sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.8, target.getZ(), 1, 0, 0, 0, 0);
            }
        }

        // 9. Abyssal Dweller: ตีใต้น้ำแรงขึ้น +35% และสร้างน้ำวน
        if (player.hasEffect(MobEffects.WATER_BREATHING) && player.isInWater()) {
            event.setAmount(event.getAmount() * 1.35f);
            if (player.level() instanceof ServerLevel sl) {
                AABB vortexBox = target.getBoundingBox().inflate(3.5);
                List<Mob> vortexTargets = sl.getEntitiesOfClass(Mob.class, vortexBox, m -> m != target && m.isAlive());
                for (Mob m : vortexTargets) {
                    Vec3 pull = target.position().subtract(m.position()).normalize().scale(0.35);
                    m.setDeltaMovement(m.getDeltaMovement().add(pull));
                }
            }
        }
    }

    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 9. Abyssal Dweller: แพ้ไฟบนบก รับดาเมจไฟแรงขึ้น +50%
        if (player.hasEffect(MobEffects.WATER_BREATHING) && !player.isInWater()) {
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                event.setAmount(event.getAmount() * 1.50f);
            }
        }
    }
}
