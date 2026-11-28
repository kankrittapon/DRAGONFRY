package com.omnicore.rpg.handler;

import com.omnicore.enchantment.ModEnchantments;
import com.omnicore.server.PvPManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ⚔️ Multislash (มัลติสแลช) Handler
 * ปลดปล่อยคลื่นคมดาบรูปพัด (Fan-Spread Slash Waves) เมื่อฟันโจมตี
 * รองรับอาวุธทุกประเภท รวมถึงอาวุธตีไกลจาก Simply Swords
 */
public class MultislashHandler {

    public static void onPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        ItemStack weapon = player.getMainHandItem();
        int level = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.MULTISLASH.get(), weapon);
        if (level <= 0) return;

        executeMultislash(player, weapon, level, event.getTarget());
    }

    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        // Triggered on client when swinging air, packet can be sent or handled in swing
    }

    public static void executeMultislash(Player player, ItemStack weapon, int level, net.minecraft.world.entity.Entity primaryTarget) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // Base attack damage from weapon
        double baseDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double damageRatio = switch (level) {
            case 1 -> 0.40;
            case 2 -> 0.60;
            default -> 0.80;
        };
        double waveDamage = Math.max(2.0, baseDmg * damageRatio);

        // Fan angles based on enchantment level
        double[] angles = switch (level) {
            case 1 -> new double[]{-20.0, 20.0};
            case 2 -> new double[]{-30.0, 0.0, 30.0};
            default -> new double[]{-40.0, -20.0, 0.0, 20.0, 40.0};
        };

        double reach = 4.5 + (level * 0.8);
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();

        Set<Integer> hitEntityIds = new HashSet<>();
        if (primaryTarget != null) {
            hitEntityIds.add(primaryTarget.getId());
        }

        // Play authentic sweeping wind sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f + (level * 0.15f));

        for (double angle : angles) {
            Vec3 spreadDir = rotateAroundY(lookVec, angle);

            // Spawn slash wave particles along the trajectory
            for (double d = 1.2; d <= reach; d += 0.8) {
                Vec3 particlePos = eyePos.add(spreadDir.scale(d));
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        particlePos.x, particlePos.y - 0.2, particlePos.z,
                        1, 0.1, 0.1, 0.1, 0.0);

                if (level >= 3) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            particlePos.x, particlePos.y, particlePos.z,
                            2, 0.15, 0.15, 0.15, 0.05);
                }
            }

            // Detect and damage entities along the slash line
            Vec3 endPos = eyePos.add(spreadDir.scale(reach));
            AABB slashBox = new AABB(eyePos, endPos).inflate(1.2, 0.8, 1.2);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, slashBox, e ->
                    e != player && !(e instanceof ArmorStand) && e.isAlive() && !hitEntityIds.contains(e.getId())
            );

            for (LivingEntity target : targets) {
                if (target instanceof Player targetPlayer) {
                    if (!PvPManager.INSTANCE.canAttack(player, targetPlayer)) continue;
                }

                // Check dot product to ensure target is within the fan sector
                Vec3 toTarget = target.position().add(0, target.getEyeHeight() * 0.5, 0).subtract(eyePos).normalize();
                if (spreadDir.dot(toTarget) > 0.65) {
                    hitEntityIds.add(target.getId());

                    // Deal slashing wave damage
                    target.hurt(player.damageSources().playerAttack(player), (float) waveDamage);

                    // Slight knockback in wave direction
                    target.knockback(0.35 + (level * 0.1), -spreadDir.x, -spreadDir.z);

                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            target.getX(), target.getY() + target.getEyeHeight() * 0.5, target.getZ(),
                            6, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }

        if (!hitEntityIds.isEmpty()) {
            player.displayClientMessage(Component.literal(String.format("§b§l⚔️ MULTISLASH! §f(%d Hits)", hitEntityIds.size())), true);
        }

        // Weapon durability penalty (1 durability used)
        if (player instanceof ServerPlayer serverPlayer && !player.isCreative()) {
            weapon.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
    }

    private static Vec3 rotateAroundY(Vec3 vec, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(vec.x * cos + vec.z * sin, vec.y, -vec.x * sin + vec.z * cos);
    }
}
