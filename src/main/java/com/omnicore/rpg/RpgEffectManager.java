package com.omnicore.rpg;

import com.omnicore.rpg.handler.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ศูนย์รวมการจัดการ Forge Events ของระบบ RPG 13 Status Effects & Multislash
 */
public class RpgEffectManager {
    public static final RpgEffectManager INSTANCE = new RpgEffectManager();

    private boolean isEnabled() {
        try {
            if (com.omnicore.config.OmniConfig.INSTANCE != null && com.omnicore.config.OmniConfig.INSTANCE.rpgEffectsEnabled != null) {
                return com.omnicore.config.OmniConfig.INSTANCE.rpgEffectsEnabled.get();
            }
        } catch (Throwable ignored) {}
        return true;
    }

    @SubscribeEvent
    public void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onLivingVisibility(event);
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onLivingChangeTarget(event);
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onLivingTick(event);
        ElementalHandler.onPlayerTick(event);
        MobilityHandler.onPlayerTick(event);
        SpecialCombatHandler.onPlayerTick(event);
    }

    @SubscribeEvent
    public void onCriticalHit(CriticalHitEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onCriticalHit(event);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onPlayerPreAttack(event);
        MultislashHandler.onPlayerAttack(event);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!isEnabled()) return;
        StealthHandler.onPlayerAttack(event);
        StealthHandler.onPlayerHurt(event);

        CombatStanceHandler.onPlayerAttack(event);
        CombatStanceHandler.onPlayerHurt(event);

        ElementalHandler.onPlayerAttack(event);
        ElementalHandler.onPlayerHurt(event);

        MobilityHandler.onPlayerHurt(event);

        SpecialCombatHandler.onPlayerAttack(event);
        SpecialCombatHandler.onPlayerHurt(event);
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (!isEnabled()) return;
        CombatStanceHandler.onLivingHeal(event);
    }

    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (!isEnabled()) return;
        MobilityHandler.onLivingFall(event);
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!isEnabled()) return;
        SpecialCombatHandler.onArrowSpawn(event);
    }
}
