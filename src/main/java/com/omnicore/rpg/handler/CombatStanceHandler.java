package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;

/**
 * ⚔️ Combat Stances:
 * 2. Berserk Bloodlust (Strength หรือ HP < 35% ➔ Attack speed, Lifesteal, Knockback immune; -30% armor, -50% heal)
 * 3. Titan Bastion (Resistance + ยกโล่/ยืนนิ่ง ➔ -60% dmg, 25% reflect; Slowness II, jump disabled, hunger drain)
 * 8. Toxic Coating (ชโลมพิษ Decay กัดเกราะ, ตัดฮีล 50%; Max HP ลดลง 2 หัวใจ)
 */
public class CombatStanceHandler {

    public static void onPlayerAttack(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        LivingEntity target = event.getEntity();

        // 2. Berserk Bloodlust (Lifesteal)
        if (isBerserk(player)) {
            ItemStack weapon = player.getMainHandItem();
            int pactLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BLOODLETTER_PACT.get(), weapon);

            float lifestealRatio = 0.15f + (pactLvl * 0.03f);
            float healAmount = event.getAmount() * lifestealRatio;
            player.heal(healAmount);

            player.displayClientMessage(Component.literal(String.format("§4§l🩸 BERSERK! §a+%.1f HP §7(Lifesteal)", healAmount)), true);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART,
                        player.getX(), player.getY() + 1.2, player.getZ(),
                        3, 0.2, 0.2, 0.2, 0.05);
            }
        }

        // 8. Toxic Coating
        if (player.hasEffect(MobEffects.POISON) || player.getPersistentData().getBoolean("omnicore_toxic_coated")) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
            target.getPersistentData().putLong("omnicore_heal_cut_until", System.currentTimeMillis() + 5000L);

            player.displayClientMessage(Component.literal("§2§l☠️ TOXIC WITHER! §7(Decay & Heal Cut 50%)"), true);

            if (target.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.05);
            }
        }
    }

    public static void onPlayerHurt(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // 2. Berserk Drawback: รับดาเมจแรงขึ้น 30% (เสมือนเกราะบางลง)
        if (isBerserk(player)) {
            event.setAmount(event.getAmount() * 1.30f);
        }

        // 3. Titan Bastion: ลดดาเมจ 60% + สะท้อนดาเมจ 25%
        if (isTitanBastion(player)) {
            event.setAmount(event.getAmount() * 0.40f); // ลด 60%

            // คำนวณสะท้อนดาเมจ
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                int spikesLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BASTION_SPIKES.get(), chest);
                float reflectRatio = 0.25f + (spikesLvl * 0.10f);
                float reflectDmg = event.getAmount() * reflectRatio;

                attacker.hurt(player.damageSources().thorns(player), reflectDmg);

                player.displayClientMessage(Component.literal(String.format("§9§l🛡️ BASTION REFLECT! §b%.1f DMG", reflectDmg)), true);

                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT,
                            attacker.getX(), attacker.getY() + 1.0, attacker.getZ(),
                            8, 0.2, 0.2, 0.2, 0.1);
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.8f);
                }
            }

            // Drawback: สูญเสียค่าความหิวเร็วขึ้น
            ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            int spikesLvl = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BASTION_SPIKES.get(), chest);
            float exhaustion = Math.max(0.05f, 0.2f - (spikesLvl * 0.04f));
            player.causeFoodExhaustion(exhaustion);
        }
    }

    public static void onLivingHeal(LivingHealEvent event) {
        // Berserk Drawback: การฮีลจากอาหาร/ยาฟื้นฟูลดลง 50%
        if (event.getEntity() instanceof Player player && isBerserk(player)) {
            event.setAmount(event.getAmount() * 0.50f);
        }

        // Toxic Coating Target Drawback: เป้าหมายที่โดนพิษตัดฮีล 50%
        if (event.getEntity().getPersistentData().getLong("omnicore_heal_cut_until") > System.currentTimeMillis()) {
            event.setAmount(event.getAmount() * 0.50f);
        }
    }

    public static boolean isBerserk(Player player) {
        return player.hasEffect(MobEffects.DAMAGE_BOOST) || (player.getHealth() / player.getMaxHealth() <= 0.35f);
    }

    public static boolean isTitanBastion(Player player) {
        if (!player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) return false;
        return player.isBlocking() || player.getDeltaMovement().horizontalDistanceSqr() < 0.005;
    }
}
