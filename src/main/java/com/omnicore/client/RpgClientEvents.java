package com.omnicore.client;

import com.omnicore.network.DoubleJumpPacket;
import com.omnicore.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "omnicore", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RpgClientEvents {

    private static boolean canDoubleJumpClient = true;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Player player = mc.player;

        // Reset double jump flag when on ground
        if (player.onGround()) {
            canDoubleJumpClient = true;
            return;
        }

        // Zephyr Feather: Detect double jump input in the air
        if (canDoubleJumpClient && mc.options.keyJump.consumeClick()) {
            if (player.hasEffect(MobEffects.SLOW_FALLING) || player.hasEffect(MobEffects.JUMP)) {
                canDoubleJumpClient = false;

                // Client-side instant impulse for responsive feel
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(new Vec3(look.x * 0.4, 0.55, look.z * 0.4));
                player.fallDistance = 0;

                // Send sync packet to server
                ModNetwork.CHANNEL.sendToServer(new DoubleJumpPacket());
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.isUsingItem()) {
            if (player.getUseItem().getItem() instanceof net.minecraft.world.item.BowItem
                    || player.getUseItem().getItem() instanceof net.minecraft.world.item.CrossbowItem) {
                // Marksman Focus: Smooth sniper zoom
                event.setNewFovModifier(event.getFovModifier() * 0.85f);
            }
        }
    }
}
