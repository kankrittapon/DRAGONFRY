package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 👁️🏹⚡🖤 Special Combat Stances:
 * 7. Clairvoyance (Night Vision ➔ Glowing ม็อบใน 24 บล็อก; แดดจ้าติด Flashblind)
 * 11. Marksman Focus (ง้างธนู ➔ ลูกธนูพุ่งตรงเจาะเกราะ; ตรึงอยู่กับที่)
 * 12. Static Conduit (สะสมประจุ ➔ ปล่อย Chain Lightning; ดึงดูดฟ้าผ่า + แพ้แรงระเบิด)
 * 13. Soul Siphon (Wither ➔ ดูดเลือดม็อบรอบตัว 5 บล็อก; ปฏิเสธ Totem of Undying)
 */
public class SpecialCombatHandler {

    public static final Map<UUID, Integer> STATIC_CHARGES = new ConcurrentHashMap<>();

    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        ServerLevel sl = (ServerLevel) player.level();

        // 7. Clairvoyance (Night Vision)
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            // สแกนม็อบรอบตัว 24 บล็อก ให้ติด Glowing
            if (player.tickCount % 20 == 0) {
                AABB scanBox = player.getBoundingBox().inflate(24.0);
                List<Mob> nearby = sl.getEntitiesOfClass(Mob.class, scanBox, m -> m.isAlive() && !m.isAlliedTo(player));
                for (Mob m : nearby) {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
                }
            }

            // Drawback: แดดจ้าตอนกลางวันติด Flashblind 1s
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                if (player.tickCount % 100 == 0) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 0, false, false));
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7f, 1.8f);
                }
            }
        }

        // 11. Marksman Focus: ขณะง้างธนู ห้ามเดิน (Rooted)
        if (player.isUsingItem() && (player.getUseItem().getItem() instanceof net.minecraft.world.item.BowItem
                || player.getUseItem().getItem() instanceof net.minecraft.world.item.CrossbowItem)) {
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(0, delta.y, 0); // ตรึงขาให้อยู่กับที่
        }

        // 13. Soul Siphon: เมื่อติด Wither แปลงเป็นออร่าดูดเลือดม็อบ 5 บล็อก
        if (player.hasEffect(MobEffects.WITHER)) {
            if (player.tickCount % 20 == 0) {
                AABB drainBox = player.getBoundingBox().inflate(5.0);
                List<Mob> targets = sl.getEntitiesOfClass(Mob.class, drainBox, m -> m.isAlive() && !m.isAlliedTo(player));
                float totalDrained = 0;
                for (Mob m : targets) {
                    m.hurt(player.damageSources().wither(), 2.0f);
                    totalDrained += 1.0f;
                    sl.sendParticles(ParticleTypes.SCULK_SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 2, 0.1, 0.1, 0.1, 0.02);
                }
                if (totalDrained > 0) {
                    player.heal(Math.min(6.0f, totalDrained));
                    sl.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
                    player.displayClientMessage(Component.literal(String.format("§5§l🔮 SOUL DRAIN! §a+%.1f HP §7(Drained %d)", Math.min(6.0f, totalDrained), targets.size())), true);
                }
            }
        }
    }

    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        // 11. Marksman Focus: ลูกธนูพุ่งตรงเจาะเกราะ
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof Player player) {
            arrow.setNoGravity(true); // พุ่งตรง
            arrow.setPierceLevel((byte) 5); // เจาะเกราะทะลุตัว
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1.35)); // เพิ่มความเร็ว 35%
        }
    }

    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 12. Static Conduit: เมื่อโดนดาเมจ สะสมประจุไฟฟ้า (สูงสุด 5 สแตก)
        ItemStack weapon = player.getMainHandItem();
        int teslaLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.TESLA_COIL.get(), weapon);
        int chargeGain = (teslaLvl > 0) ? 2 : 1;

        int current = STATIC_CHARGES.getOrDefault(player.getUUID(), 0);
        int newCharge = Math.min(5, current + chargeGain);
        STATIC_CHARGES.put(player.getUUID(), newCharge);

        if (player.level() instanceof ServerLevel sl && newCharge >= 5) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
        }

        // Drawback: แพ้แรงระเบิด Creeper / TNT แรงขึ้น +50%
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) {
            event.setAmount(event.getAmount() * 1.50f);
        }
    }

    public static void onPlayerAttack(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();

        // 12. Static Conduit: ปลดปล่อย Chain Lightning เมื่อประจุเต็ม 5
        Integer charges = STATIC_CHARGES.get(player.getUUID());
        if (charges != null && charges >= 5) {
            STATIC_CHARGES.remove(player.getUUID());

            ItemStack weapon = player.getMainHandItem();
            int teslaLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.TESLA_COIL.get(), weapon);
            int maxChains = 3 + teslaLvl;

            if (player.level() instanceof ServerLevel sl) {
                // สายฟ้าผ่าลงเป้าหมายหลัก
                target.hurt(player.damageSources().lightningBolt(), 6.0f);
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.4, 0.4, 0.4, 0.1);
                sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);

                // ชิ่งไปยังเป้าหมายข้างเคียง
                AABB chainBox = target.getBoundingBox().inflate(6.0);
                List<Mob> nearby = sl.getEntitiesOfClass(Mob.class, chainBox, m -> m != target && m.isAlive() && !m.isAlliedTo(player));

                int chained = 0;
                for (Mob m : nearby) {
                    if (chained >= maxChains) break;
                    m.hurt(player.damageSources().lightningBolt(), 4.0f);
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, m.getX(), m.getY() + 0.8, m.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                    chained++;
                }

                player.displayClientMessage(Component.literal(String.format("§e§l⚡ CHAIN LIGHTNING! §f(%d Targets)", chained + 1)), true);
            }
        }
    }
}
