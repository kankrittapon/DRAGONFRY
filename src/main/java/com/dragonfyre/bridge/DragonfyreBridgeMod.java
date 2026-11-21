package com.dragonfyre.bridge;

import com.dragonfyre.bridge.network.ModNetwork;
import com.dragonfyre.bridge.server.AnnouncementTriggerHandler;
import com.dragonfyre.bridge.server.GasHotReloadManager;
import com.dragonfyre.bridge.server.UniversalBossManager;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("dragonfyre_bridge")
public class DragonfyreBridgeMod {
    public static final String MOD_ID = "dragonfyre_bridge";

    public DragonfyreBridgeMod() {
        ModNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(UniversalBossManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(AnnouncementTriggerHandler.INSTANCE);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        UniversalBossManager.INSTANCE.initDefaults();
        GasHotReloadManager.INSTANCE.start(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        GasHotReloadManager.INSTANCE.stop();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("dfboss")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(UniversalBossManager.INSTANCE.formatRegistrySummary()), false);
                    return 1;
                }))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("entityId", StringArgumentType.string())
                        .executes(ctx -> {
                            String eid = StringArgumentType.getString(ctx, "entityId");
                            boolean ok = UniversalBossManager.INSTANCE.spawnWorldBossNow(ctx.getSource().getServer(), eid);
                            ctx.getSource().sendSuccess(() -> Component.literal(ok ? "§aเสก World Boss สำเร็จ: " + eid : "§cไม่พบ Entity ID ใน Modpack: " + eid), true);
                            return ok ? 1 : 0;
                        })))
                .then(Commands.literal("promote")
                    .then(Commands.argument("entityId", StringArgumentType.string())
                        .then(Commands.argument("hpMult", FloatArgumentType.floatArg(1.0f, 50.0f))
                            .then(Commands.argument("atkMult", FloatArgumentType.floatArg(1.0f, 20.0f))
                                .then(Commands.argument("chancePercent", IntegerArgumentType.integer(1, 100))
                                    .executes(ctx -> {
                                        String eid = StringArgumentType.getString(ctx, "entityId");
                                        float hp = FloatArgumentType.getFloat(ctx, "hpMult");
                                        float atk = FloatArgumentType.getFloat(ctx, "atkMult");
                                        int chance = IntegerArgumentType.getInteger(ctx, "chancePercent");
                                        UniversalBossManager.INSTANCE.registerDynamicBoss(eid, hp, atk, chance);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§6อัปเกรดม็อบเป็น Custom Boss แล้ว: " + eid + " (HP x" + hp + ", โอกาส " + chance + "%)"), true);
                                        return 1;
                                    }))))))
                .then(Commands.literal("demote")
                    .then(Commands.argument("entityId", StringArgumentType.string())
                        .executes(ctx -> {
                            String eid = StringArgumentType.getString(ctx, "entityId");
                            boolean removed = UniversalBossManager.INSTANCE.removeBossRule(eid);
                            ctx.getSource().sendSuccess(() -> Component.literal(removed ? "§eถอดม็อบออกจากระบบบอสแล้ว: " + eid : "§cไม่พบในรายชื่อบอส"), true);
                            return removed ? 1 : 0;
                        })))
                .then(Commands.literal("announce")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String msg = StringArgumentType.getString(ctx, "message");
                            AnnouncementTriggerHandler.broadcastCustom("📢 " + msg, "ประกาศจากผู้ดูแลเซิร์ฟเวอร์", 0xF59E0B, "RUNE_GLOW", "minecraft:block.bell.use", 120);
                            return 1;
                        })))
                .then(Commands.literal("sync").executes(ctx -> {
                    GasHotReloadManager.INSTANCE.triggerImmediatePoll(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("§aสั่งซิงก์ค่าตั้งค่าจาก Google Apps Script ทันทีแล้ว!"), false);
                    return 1;
                }))
        );
    }
}
