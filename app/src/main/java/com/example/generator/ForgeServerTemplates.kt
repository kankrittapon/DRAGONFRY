package com.example.generator

import com.example.data.local.AnnouncementRuleEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity

object ForgeServerTemplates {

    fun generateMainMod(config: StudioConfigEntity): GeneratedFile {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")

        val content = """
package $pkg;

import $pkg.network.ModNetwork;
import $pkg.server.AnnouncementTriggerHandler;
import $pkg.server.GasHotReloadManager;
import $pkg.server.UniversalBossManager;
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

@Mod("$modId")
public class DragonfyreBridgeMod {
    public static final String MOD_ID = "$modId";

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
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/java/$pkgPath/DragonfyreBridgeMod.java",
            displayName = "DragonfyreBridgeMod.java",
            category = GeneratedFileCategory.FORGE_SERVER,
            language = "java",
            descriptionTh = "คลาสหลักของ Forge Mod 1.20.1 พร้อมคำสั่ง /dfboss (spawn, promote, demote, announce, sync)",
            content = content
        )
    }

    fun generateUniversalBossManager(
        config: StudioConfigEntity,
        bosses: List<WorldBossProfileEntity>
    ): GeneratedFile {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')

        val seedEntries = bosses.joinToString("\n        ") { b ->
            val safeName = b.displayName.replace("\"", "\\\"")
            val safeMod = b.modPackTag.replace("\"", "\\\"")
            val safeEntity = b.entityId.replace("\"", "\\\"")
            val safeDim = b.dimensionId.replace("\"", "\\\"")
            val safeAffix = b.affixTraits.replace("\"", "\\\"")
            val safeReward = b.rewardCommand.replace("\"", "\\\"")
            """register(new BossProfile("$safeName", "$safeEntity", "$safeMod", "${b.bossMode}", ${b.promotionChancePercent}, ${b.hpMultiplier}f, ${b.attackMultiplier}f, ${b.armorBonus}f, ${b.speedMultiplier}f, ${b.spawnIntervalMinutes}, ${b.fightDurationMinutes}, ${b.minSpawnRadius}, ${b.maxSpawnRadius}, "$safeDim", ${b.glowingAura}, ${b.xaeroRedMarker}, "$safeAffix", "$safeReward", ${b.enabled}));"""
        }

        val content = """
package $pkg.server;

import $pkg.network.ModNetwork;
import $pkg.network.WorldBossSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Universal Multi-Mod World Boss & Mob-to-Boss Promotion Engine (Forge 1.20.1)
 * - ใช้ได้กับม็อบจาก Mod ใดก็ได้ (Cisco Dragonfyre, Ultimate, Cataclysm, Ice & Fire, Twilight Forest, Mowzie's, Vanilla ฯลฯ)
 * - รองรับ 2 โหมด: (1) สุ่มเกิดเป็น World Boss ตามเวลาบนแผนที่ (2) อัปเกรดม็อบที่เกิดตามธรรมชาติเป็น Custom Boss อัตโนมัติ
 */
public class UniversalBossManager {
    public static final UniversalBossManager INSTANCE = new UniversalBossManager();
    public static final String NBT_BOSS_TAG = "df_universal_boss";
    public static final String NBT_PROFILE_ID = "df_boss_entity_id";

    public static class BossProfile {
        public String displayName, entityId, modPackTag, bossMode, dimensionId, affixTraits, rewardCommand;
        public int promotionChancePercent, spawnIntervalMinutes, fightDurationMinutes, minRadius, maxRadius;
        public float hpMult, atkMult, armorBonus, speedMult;
        public boolean glowingAura, xaeroRedMarker, enabled;

        public BossProfile(String displayName, String entityId, String modPackTag, String bossMode,
                           int promotionChancePercent, float hpMult, float atkMult, float armorBonus, float speedMult,
                           int spawnIntervalMinutes, int fightDurationMinutes, int minRadius, int maxRadius,
                           String dimensionId, boolean glowingAura, boolean xaeroRedMarker,
                           String affixTraits, String rewardCommand, boolean enabled) {
            this.displayName = displayName;
            this.entityId = entityId;
            this.modPackTag = modPackTag;
            this.bossMode = bossMode;
            this.promotionChancePercent = promotionChancePercent;
            this.hpMult = hpMult;
            this.atkMult = atkMult;
            this.armorBonus = armorBonus;
            this.speedMult = speedMult;
            this.spawnIntervalMinutes = spawnIntervalMinutes;
            this.fightDurationMinutes = fightDurationMinutes;
            this.minRadius = minRadius;
            this.maxRadius = maxRadius;
            this.dimensionId = dimensionId;
            this.glowingAura = glowingAura;
            this.xaeroRedMarker = xaeroRedMarker;
            this.affixTraits = affixTraits;
            this.rewardCommand = rewardCommand;
            this.enabled = enabled;
        }
    }

    private static class ActiveBoss {
        final UUID uuid;
        final BossProfile profile;
        final ResourceKey<Level> dimension;
        long expireAtMillis;

        ActiveBoss(UUID uuid, BossProfile profile, ResourceKey<Level> dimension, long expireAtMillis) {
            this.uuid = uuid;
            this.profile = profile;
            this.dimension = dimension;
            this.expireAtMillis = expireAtMillis;
        }
    }

    private final Map<String, BossProfile> registry = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Hot-Reloadable Live Settings
    public volatile boolean worldBossSystemEnabled = ${config.worldBossSystemEnabled};
    public volatile boolean autoPromoteSystemEnabled = ${config.autoPromoteMobSystemEnabled};
    public volatile float globalHpScale = ${config.globalBossHpScale}f;
    public volatile float globalAtkScale = ${config.globalBossAtkScale}f;
    public volatile String activeBarTheme = "${config.bossBarTheme}";

    private long tickCounter = 0L;
    private long nextScheduledSpawnMs = System.currentTimeMillis() + 60_000L * 15L;

    public void initDefaults() {
        registry.clear();
        $seedEntries
    }

    public void register(BossProfile profile) {
        if (profile != null && profile.entityId != null && !profile.entityId.isBlank()) {
            registry.put(profile.entityId.toLowerCase(Locale.ROOT), profile);
        }
    }

    public void replaceRegistryFromGas(List<BossProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;
        registry.clear();
        for (BossProfile p : profiles) register(p);
    }

    public void registerDynamicBoss(String entityId, float hpMult, float atkMult, int chancePercent) {
        String cleanId = entityId.toLowerCase(Locale.ROOT);
        String niceName = cleanId.contains(":") ? cleanId.split(":")[1].replace('_', ' ') : cleanId;
        BossProfile p = new BossProfile(
                "Apex " + niceName, cleanId, "Custom Mod", "BOTH",
                chancePercent, hpMult, atkMult, 10f, 1.15f,
                45, 20, 200, 1200, "minecraft:overworld",
                true, true, "ENRAGED,IRON_SKIN", "give @p minecraft:diamond 5", true
        );
        register(p);
    }

    public boolean removeBossRule(String entityId) {
        return registry.remove(entityId.toLowerCase(Locale.ROOT)) != null;
    }

    public String formatRegistrySummary() {
        if (registry.isEmpty()) return "§eยังไม่มีบอสในคลัง";
        return "§6[Universal Boss Registry (" + registry.size() + ")]§r\n" +
                registry.values().stream()
                        .map(p -> String.format(" - %s (§b%s§r) [%s | HPx%.1f | %d%%]", p.displayName, p.entityId, p.bossMode, p.hpMult, p.promotionChancePercent))
                        .collect(Collectors.joining("\n"));
    }

    /**
     * โหมด 2: อัปเกรดม็อบจาก Mod ใดก็ได้ที่เกิดตามธรรมชาติ ให้กลายเป็น Custom Boss อัตโนมัติ
     */
    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !autoPromoteSystemEnabled) return;
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.getPersistentData().getBoolean(NBT_BOSS_TAG)) return;

        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (key == null) return;
        BossProfile profile = registry.get(key.toString().toLowerCase(Locale.ROOT));
        if (profile == null || !profile.enabled) return;
        if (!"AUTO_PROMOTE_NATURAL".equals(profile.bossMode) && !"BOTH".equals(profile.bossMode)) return;

        String dimId = event.getLevel().dimension().location().toString();
        if (!profile.dimensionId.equals("*") && !profile.dimensionId.equalsIgnoreCase(dimId)) return;

        int roll = random.nextInt(100) + 1;
        if (roll <= profile.promotionChancePercent) {
            applyBossEnhancements(living, profile);
            long expire = System.currentTimeMillis() + profile.fightDurationMinutes * 60_000L;
            activeBosses.put(living.getUUID(), new ActiveBoss(living.getUUID(), profile, event.getLevel().dimension(), expire));
            AnnouncementTriggerHandler.broadcastBossSpawn(profile.displayName, living.blockPosition(), profile.fightDurationMinutes);
        }
    }

    /**
     * โหมด 1: สุ่มเสก World Boss บนแผนที่พร้อมค้นหาพื้นปลอดภัย
     */
    public boolean spawnWorldBossNow(MinecraftServer server, String requestedEntityId) {
        List<BossProfile> candidates = registry.values().stream()
                .filter(p -> p.enabled && ("SCHEDULED_WORLD_BOSS".equals(p.bossMode) || "BOTH".equals(p.bossMode)))
                .toList();

        BossProfile chosen = null;
        if (requestedEntityId != null && !requestedEntityId.isBlank()) {
            chosen = registry.get(requestedEntityId.toLowerCase(Locale.ROOT));
            if (chosen == null) {
                registerDynamicBoss(requestedEntityId, 3.0f, 1.8f, 100);
                chosen = registry.get(requestedEntityId.toLowerCase(Locale.ROOT));
            }
        } else if (!candidates.isEmpty()) {
            chosen = candidates.get(random.nextInt(candidates.size()));
        }
        if (chosen == null) return false;

        ResourceLocation entityRl = ResourceLocation.tryParse(chosen.entityId);
        if (entityRl == null) return false;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityRl);
        if (type == null) return false;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.tryParse(chosen.dimensionId.equals("*") ? "minecraft:overworld" : chosen.dimensionId));
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) level = server.overworld();

        BlockPos spawnCenter = level.getSharedSpawnPos();
        double angle = random.nextDouble() * Math.PI * 2.0;
        int dist = chosen.minRadius + random.nextInt(Math.max(50, chosen.maxRadius - chosen.minRadius));
        int x = spawnCenter.getX() + (int) (Math.cos(angle) * dist);
        int z = spawnCenter.getZ() + (int) (Math.sin(angle) * dist);
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) return false;

        living.moveTo(x + 0.5, y + 1.0, z + 0.5, random.nextFloat() * 360f, 0f);
        applyBossEnhancements(living, chosen);
        level.addFreshEntity(living);

        long expire = System.currentTimeMillis() + chosen.fightDurationMinutes * 60_000L;
        activeBosses.put(living.getUUID(), new ActiveBoss(living.getUUID(), chosen, level.dimension(), expire));
        AnnouncementTriggerHandler.broadcastBossSpawn(chosen.displayName, new BlockPos(x, y, z), chosen.fightDurationMinutes);
        GasHotReloadManager.INSTANCE.logEventAsync("BOSS_SPAWN", "SERVER", chosen.displayName + " (" + chosen.entityId + ") at " + x + "," + y + "," + z);
        return true;
    }

    public void despawnAllActiveBosses(MinecraftServer server) {
        for (ActiveBoss ab : new ArrayList<>(activeBosses.values())) {
            ServerLevel level = server.getLevel(ab.dimension);
            if (level != null) {
                Entity e = level.getEntity(ab.uuid);
                if (e != null) e.discard();
            }
            sendRemovePacket(ab);
            activeBosses.remove(ab.uuid);
        }
    }

    private void applyBossEnhancements(LivingEntity boss, BossProfile profile) {
        boss.getPersistentData().putBoolean(NBT_BOSS_TAG, true);
        boss.getPersistentData().putString(NBT_PROFILE_ID, profile.entityId);
        boss.setCustomName(Component.literal("§c§l☠ " + profile.displayName));
        boss.setCustomNameVisible(true);

        AttributeInstance maxHp = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double scaled = maxHp.getBaseValue() * profile.hpMult * globalHpScale;
            maxHp.setBaseValue(Math.min(100_000.0, Math.max(40.0, scaled)));
            boss.setHealth(boss.getMaxHealth());
        }
        AttributeInstance atk = boss.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(atk.getBaseValue() * profile.atkMult * globalAtkScale);
        }
        AttributeInstance armor = boss.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            armor.setBaseValue(armor.getBaseValue() + profile.armorBonus);
        }
        AttributeInstance spd = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.setBaseValue(spd.getBaseValue() * profile.speedMult);
        }

        if (profile.glowingAura) {
            boss.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 60 * profile.fightDurationMinutes, 0, false, false));
        }
        if (profile.affixTraits.contains("IRON_SKIN")) {
            boss.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * profile.fightDurationMinutes, 0, false, false));
        }
        if (profile.affixTraits.contains("SPEED_SURGE")) {
            boss.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60 * profile.fightDurationMinutes, 0, false, false));
        }
    }

    @SubscribeEvent
    public void onBossHurtCombatAffix(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        // Life Steal เมื่อบอสโจมตีผู้เล่น
        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker.getPersistentData().getBoolean(NBT_BOSS_TAG)) {
            ActiveBoss ab = activeBosses.get(attacker.getUUID());
            if (ab != null && ab.profile.affixTraits.contains("LIFE_STEAL")) {
                attacker.heal(event.getAmount() * 0.25f);
            }
            if (ab != null && ab.profile.affixTraits.contains("LIGHTNING_CALLER") && random.nextFloat() < 0.20f) {
                if (attacker.level() instanceof ServerLevel sl) {
                    LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(event.getEntity().position());
                        sl.addFreshEntity(bolt);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onBossDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        UUID id = event.getEntity().getUUID();
        ActiveBoss ab = activeBosses.remove(id);
        if (ab == null) return;

        sendRemovePacket(ab);
        String killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp.getName().getString() : "นักผจญภัย";
        AnnouncementTriggerHandler.broadcastBossDefeated(killer, ab.profile.displayName);
        GasHotReloadManager.INSTANCE.logEventAsync("BOSS_DEFEATED", killer, "Defeated " + ab.profile.displayName);

        if (event.getEntity().getServer() != null && !ab.profile.rewardCommand.isBlank()) {
            String cmd = ab.profile.rewardCommand.replace("@p", killer);
            event.getEntity().getServer().getCommands().performPrefixedCommand(
                    event.getEntity().getServer().createCommandSourceStack().withSuppressedOutput(), cmd
            );
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        MinecraftServer server = event.getServer();

        // สุ่มเกิด World Boss ตามตารางเวลา
        if (worldBossSystemEnabled && tickCounter % 200 == 0 && System.currentTimeMillis() >= nextScheduledSpawnMs && activeBosses.isEmpty()) {
            spawnWorldBossNow(server, null);
            int intervalMin = registry.values().stream().filter(p -> p.enabled).mapToInt(p -> p.spawnIntervalMinutes).min().orElse(45);
            nextScheduledSpawnMs = System.currentTimeMillis() + intervalMin * 60_000L;
        }

        // ซิงก์ Custom Boss Bar + Xaero Red Pin ทุกๆ 10 ticks (0.5 วินาที)
        if (tickCounter % 10 != 0 || activeBosses.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (ActiveBoss ab : new ArrayList<>(activeBosses.values())) {
            ServerLevel level = server.getLevel(ab.dimension);
            Entity entity = level != null ? level.getEntity(ab.uuid) : null;
            int remainingSec = (int) ((ab.expireAtMillis - now) / 1000L);

            if (!(entity instanceof LivingEntity living) || !living.isAlive() || remainingSec <= 0) {
                if (living != null && living.isAlive() && remainingSec <= 0) {
                    living.discard();
                    AnnouncementTriggerHandler.broadcastCustom("⌛ " + ab.profile.displayName + " หายตัวไปเนื่องจากหมดเวลา!", "เตรียมพร้อมสำหรับ World Boss รอบถัดไป", 0x94A3B8, "SLIDE_DOWN", "minecraft:entity.enderman.teleport", 100);
                }
                sendRemovePacket(ab);
                activeBosses.remove(ab.uuid);
                continue;
            }

            BlockPos pos = living.blockPosition();
            ModNetwork.sendToAll(new WorldBossSyncPacket(
                    ab.uuid, ab.profile.displayName, ab.profile.modPackTag,
                    living.getHealth(), living.getMaxHealth(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    ab.dimension.location().toString(), remainingSec,
                    ab.profile.affixTraits, activeBarTheme,
                    ab.profile.xaeroRedMarker, true
            ));
        }
    }

    private void sendRemovePacket(ActiveBoss ab) {
        ModNetwork.sendToAll(new WorldBossSyncPacket(
                ab.uuid, ab.profile.displayName, ab.profile.modPackTag,
                0f, 1f, 0, 0, 0, ab.dimension.location().toString(),
                0, "", activeBarTheme, ab.profile.xaeroRedMarker, false
        ));
    }
}
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/java/$pkgPath/server/UniversalBossManager.java",
            displayName = "UniversalBossManager.java",
            category = GeneratedFileCategory.FORGE_SERVER,
            language = "java",
            descriptionTh = "ระบบ Universal World Boss & Mob-to-Boss Promoter ใช้ได้กับม็อบจากทุก Mod พร้อม Affix, สุ่มเกิด, ตั้งเวลา และรางวัล",
            content = content
        )
    }

    fun generateAnnouncementHandler(
        config: StudioConfigEntity,
        rules: List<AnnouncementRuleEntity>
    ): GeneratedFile {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')

        val lootRule = rules.firstOrNull { it.triggerType == "RARITY_LOOT" && it.enabled }
        val killRule = rules.firstOrNull { it.triggerType == "MOB_KILL" && it.enabled }
        val lootFilter = (lootRule?.matchFilter ?: "apotheosis:epic,apotheosis:mythic,apotheosis:ancient,EPIC").replace("\"", "\\\"")
        val killFilter = (killRule?.matchFilter ?: "cataclysm:*,iceandfire:*_dragon,twilightforest:*,mowziesmobs:*").replace("\"", "\\\"")

        val content = """
package $pkg.server;

import $pkg.network.AnnouncementPacket;
import $pkg.network.ModNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ระบบตรวจจับเหตุการณ์สำหรับป้ายประกาศกลางจอ:
 * - ตรวจจับไอเทมระดับ Epic/Mythic/Ancient จาก Apotheosis (Cisco Dragonfyre/Ultimate) และ Vanilla Rarity
 * - ตรวจจับการสังหารบอส/ม็อบพิเศษข้าม Mod ด้วย wildcard filter
 * - มีระบบ Cooldown & Dedup ป้องกันสแปมประกาศซ้ำตอนโยนของเก็บใหม่
 */
public class AnnouncementTriggerHandler {
    public static final AnnouncementTriggerHandler INSTANCE = new AnnouncementTriggerHandler();
    private static final String NBT_ANNOUNCED = "df_loot_announced";

    public volatile boolean announcementsEnabled = ${config.announcementsEnabled};
    public volatile String minLootRarity = "${config.minLootRarity}";
    public volatile String lootFilterCsv = "$lootFilter";
    public volatile String mobKillFilterCsv = "$killFilter";

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public static void broadcastCustom(String headline, String subtitle, int colorRgb, String anim, String soundId, int durationTicks) {
        if (!INSTANCE.announcementsEnabled) return;
        ModNetwork.sendToAll(new AnnouncementPacket(headline, subtitle, colorRgb, anim, soundId, durationTicks));
    }

    public static void broadcastBossSpawn(String bossName, BlockPos pos, int minutes) {
        broadcastCustom(
                "🔥 World Boss " + bossName + " ปรากฏตัวแล้ว!",
                String.format("พิกัด (%d, %d, %d) • เปิด Xaero's World Map ดูจุดแดง • เหลือเวลา %d นาที", pos.getX(), pos.getY(), pos.getZ(), minutes),
                0xEF4444, "SLIDE_DOWN", "minecraft:entity.ender_dragon.growl", 140
        );
    }

    public static void broadcastBossDefeated(String playerName, String bossName) {
        broadcastCustom(
                "👑 " + playerName + " ปราบ World Boss " + bossName + " สำเร็จ!",
                "จุดแดงบนแผนที่ถูกนำออกแล้ว • บันทึกสถิติขึ้น Google Sheets",
                0xF59E0B, "SCALE_POP", "minecraft:ui.toast.challenge_complete", 120
        );
    }

    @SubscribeEvent
    public void onItemPickup(EntityItemPickupEvent event) {
        if (!announcementsEnabled || event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem().getItem();
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.getBoolean(NBT_ANNOUNCED)) return;

        String detectedRarity = detectApotheosisOrVanillaRarity(stack);
        if (detectedRarity == null) return;

        String cdKey = player.getUUID() + ":loot";
        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(cdKey, 0L) < 8000L) return;
        cooldowns.put(cdKey, now);

        tag.putBoolean(NBT_ANNOUNCED, true);
        String itemName = stack.getHoverName().getString();
        int color = detectedRarity.contains("MYTHIC") || detectedRarity.contains("ANCIENT") ? 0xF59E0B : 0xA855F7;

        broadcastCustom(
                "⚔ " + player.getName().getString() + " ได้รับไอเทมระดับ " + detectedRarity + ": " + itemName,
                "ตรวจพบไอเทมหายากในระบบเซิร์ฟเวอร์",
                color, "RUNE_GLOW", "minecraft:ui.toast.challenge_complete", 120
        );
        GasHotReloadManager.INSTANCE.logEventAsync("EPIC_LOOT", player.getName().getString(), detectedRarity + " - " + itemName);
    }

    private String detectApotheosisOrVanillaRarity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("affix_data")) {
            String apoRarity = tag.getCompound("affix_data").getString("rarity").toLowerCase(Locale.ROOT);
            for (String token : lootFilterCsv.split(",")) {
                String clean = token.trim().toLowerCase(Locale.ROOT);
                if (!clean.isEmpty() && apoRarity.contains(clean)) {
                    return apoRarity.replace("apotheosis:", "").toUpperCase(Locale.ROOT);
                }
            }
        }
        if (stack.getRarity() == Rarity.EPIC) {
            return "EPIC";
        }
        return null;
    }

    @SubscribeEvent
    public void onMobKilled(LivingDeathEvent event) {
        if (!announcementsEnabled || event.getEntity().level().isClientSide()) return;
        if (event.getEntity().getPersistentData().getBoolean(UniversalBossManager.NBT_BOSS_TAG)) return; // จัดการใน BossManager แล้ว
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ResourceLocation entityRl = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (entityRl == null) return;
        String idStr = entityRl.toString().toLowerCase(Locale.ROOT);

        if (matchesWildcardList(idStr, mobKillFilterCsv)) {
            String mobName = event.getEntity().getDisplayName().getString();
            broadcastCustom(
                    "☠ " + player.getName().getString() + " สังหารม็อบพิเศษ " + mobName + " สำเร็จ!",
                    "Entity: " + idStr,
                    0x38BDF8, "SLIDE_DOWN", "minecraft:entity.wither.death", 100
            );
            GasHotReloadManager.INSTANCE.logEventAsync("MOB_KILL", player.getName().getString(), mobName + " (" + idStr + ")");
        }
    }

    private boolean matchesWildcardList(String entityId, String csv) {
        for (String raw : csv.split(",")) {
            String p = raw.trim().toLowerCase(Locale.ROOT);
            if (p.isEmpty()) continue;
            if (p.equals("*") || p.equals(entityId)) return true;
            if (p.endsWith("*") && entityId.startsWith(p.substring(0, p.length() - 1))) return true;
            if (p.startsWith("*") && entityId.endsWith(p.substring(1))) return true;
        }
        return false;
    }
}
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/java/$pkgPath/server/AnnouncementTriggerHandler.java",
            displayName = "AnnouncementTriggerHandler.java",
            category = GeneratedFileCategory.FORGE_SERVER,
            language = "java",
            descriptionTh = "ตรวจจับการได้อาวุธระดับ Epic/Mythic (Apotheosis) และการสังหารม็อบพิเศษข้าม Mod พร้อมกันสแปม",
            content = content
        )
    }

    fun generateGasHotReloadManager(config: StudioConfigEntity): GeneratedFile {
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val pkgPath = pkg.replace('.', '/')
        val gasUrl = config.gasWebAppUrl.ifBlank { "https://script.google.com/macros/s/REPLACE_WITH_YOUR_DEPLOY_ID/exec" }.replace("\"", "\\\"")
        val secret = config.gasSecretToken.replace("\"", "\\\"")

        val content = """
package $pkg.server;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * ระบบ Google Apps Script Auto-Polling & Hot-Reload Server Settings (ไม่ต้อง Restart Server!)
 * - ทำงานบน Background Thread ไม่ดึง TPS เซิร์ฟเวอร์
 * - นำค่าที่เปลี่ยนไปอัปเดต GameRules, Difficulty, Boss Scaling, Universal Boss Registry และคิวคำสั่งบน Main Thread อย่างปลอดภัย
 */
public class GasHotReloadManager {
    public static final GasHotReloadManager INSTANCE = new GasHotReloadManager();

    private String gasWebAppUrl = "$gasUrl";
    private String secretToken = "$secret";
    private int pollIntervalSeconds = ${config.pollIntervalSeconds.coerceIn(10, 300)};

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private ScheduledExecutorService scheduler;
    private String lastConfigVersion = "";

    public void start(MinecraftServer server) {
        stop();
        if (gasWebAppUrl.contains("REPLACE_WITH_YOUR_DEPLOY_ID") || gasWebAppUrl.isBlank()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Dragonfyre-GAS-Poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> pollNow(server), 5, pollIntervalSeconds, TimeUnit.SECONDS);
    }

    public void triggerImmediatePoll(MinecraftServer server) {
        CompletableFuture.runAsync(() -> pollNow(server));
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void pollNow(MinecraftServer server) {
        try {
            String sep = gasWebAppUrl.contains("?") ? "&" : "?";
            String url = gasWebAppUrl + sep + "action=poll&secret=" + URLEncoder.encode(secretToken, StandardCharsets.UTF_8)
                    + "&lastVersion=" + URLEncoder.encode(lastConfigVersion, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return;

            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            if (!json.has("ok") || !json.get("ok").getAsBoolean()) return;

            boolean changed = json.has("configChanged") && json.get("configChanged").getAsBoolean();
            String newVersion = json.has("configVersion") ? json.get("configVersion").getAsString() : lastConfigVersion;
            JsonObject settings = changed && json.has("settings") && json.get("settings").isJsonObject()
                    ? json.getAsJsonObject("settings") : null;
            JsonArray bossArr = changed && json.has("bossRegistry") && json.get("bossRegistry").isJsonArray()
                    ? json.getAsJsonArray("bossRegistry") : null;
            JsonArray commands = json.has("pendingCommands") && json.get("pendingCommands").isJsonArray()
                    ? json.getAsJsonArray("pendingCommands") : null;

            server.execute(() -> {
                if (settings != null) {
                    applyHotReloadSettings(server, settings);
                    lastConfigVersion = newVersion;
                }
                if (bossArr != null) {
                    applyBossRegistryHotReload(bossArr);
                }
                if (commands != null && !commands.isEmpty()) {
                    List<String> executedIds = executeCommandQueue(server, commands);
                    if (!executedIds.isEmpty()) {
                        ackCommandsAsync(executedIds);
                    }
                }
            });
        } catch (Exception ignored) {
            // Silent retry on next polling cycle
        }
    }

    private void applyHotReloadSettings(MinecraftServer server, JsonObject s) {
        GameRules rules = server.getGameRules();
        if (s.has("keepInventory")) rules.getRule(GameRules.RULE_KEEPINVENTORY).set(s.get("keepInventory").getAsBoolean(), server);
        if (s.has("mobGriefing")) rules.getRule(GameRules.RULE_MOBGRIEFING).set(s.get("mobGriefing").getAsBoolean(), server);
        if (s.has("doDaylightCycle")) rules.getRule(GameRules.RULE_DAYLIGHT).set(s.get("doDaylightCycle").getAsBoolean(), server);
        if (s.has("pvpEnabled")) server.setPvpAllowed(s.get("pvpEnabled").getAsBoolean());
        if (s.has("serverDifficulty")) {
            Difficulty diff = Difficulty.byName(s.get("serverDifficulty").getAsString().toLowerCase(Locale.ROOT));
            if (diff != null) server.setDifficulty(diff, true);
        }
        if (s.has("worldBossSystemEnabled")) UniversalBossManager.INSTANCE.worldBossSystemEnabled = s.get("worldBossSystemEnabled").getAsBoolean();
        if (s.has("autoPromoteMobSystemEnabled")) UniversalBossManager.INSTANCE.autoPromoteSystemEnabled = s.get("autoPromoteMobSystemEnabled").getAsBoolean();
        if (s.has("globalBossHpScale")) UniversalBossManager.INSTANCE.globalHpScale = s.get("globalBossHpScale").getAsFloat();
        if (s.has("globalBossAtkScale")) UniversalBossManager.INSTANCE.globalAtkScale = s.get("globalBossAtkScale").getAsFloat();
        if (s.has("bossBarTheme")) UniversalBossManager.INSTANCE.activeBarTheme = s.get("bossBarTheme").getAsString();
        if (s.has("announcementsEnabled")) AnnouncementTriggerHandler.INSTANCE.announcementsEnabled = s.get("announcementsEnabled").getAsBoolean();
        if (s.has("minLootRarity")) AnnouncementTriggerHandler.INSTANCE.minLootRarity = s.get("minLootRarity").getAsString();
    }

    private void applyBossRegistryHotReload(JsonArray arr) {
        List<UniversalBossManager.BossProfile> list = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            list.add(new UniversalBossManager.BossProfile(
                    str(o, "displayName", "World Boss"),
                    str(o, "entityId", "minecraft:warden"),
                    str(o, "modPackTag", "Universal"),
                    str(o, "bossMode", "BOTH"),
                    numInt(o, "promotionChancePercent", 100),
                    numFloat(o, "hpMultiplier", 3.0f),
                    numFloat(o, "attackMultiplier", 1.8f),
                    numFloat(o, "armorBonus", 10f),
                    numFloat(o, "speedMultiplier", 1.1f),
                    numInt(o, "spawnIntervalMinutes", 45),
                    numInt(o, "fightDurationMinutes", 20),
                    numInt(o, "minSpawnRadius", 200),
                    numInt(o, "maxSpawnRadius", 1200),
                    str(o, "dimensionId", "minecraft:overworld"),
                    bool(o, "glowingAura", true),
                    bool(o, "xaeroRedMarker", true),
                    str(o, "affixTraits", "ENRAGED"),
                    str(o, "rewardCommand", ""),
                    bool(o, "enabled", true)
            ));
        }
        UniversalBossManager.INSTANCE.replaceRegistryFromGas(list);
    }

    private List<String> executeCommandQueue(MinecraftServer server, JsonArray commands) {
        List<String> done = new ArrayList<>();
        for (JsonElement el : commands) {
            if (!el.isJsonObject()) continue;
            JsonObject cmd = el.getAsJsonObject();
            String id = str(cmd, "commandId", "");
            String type = str(cmd, "commandType", "");
            JsonObject payload = cmd.has("payload") && cmd.get("payload").isJsonObject()
                    ? cmd.getAsJsonObject("payload") : new JsonObject();

            switch (type) {
                case "BROADCAST_ANNOUNCEMENT" -> AnnouncementTriggerHandler.broadcastCustom(
                        str(payload, "headline", "📢 ประกาศจากเซิร์ฟเวอร์"),
                        str(payload, "subtitle", ""),
                        0xF59E0B, "RUNE_GLOW", "minecraft:block.bell.use", 120
                );
                case "SPAWN_BOSS_NOW" -> UniversalBossManager.INSTANCE.spawnWorldBossNow(server, str(payload, "entityId", ""));
                case "DESPAWN_BOSS" -> UniversalBossManager.INSTANCE.despawnAllActiveBosses(server);
                case "RUN_SERVER_COMMAND" -> {
                    String raw = str(payload, "command", "");
                    if (!raw.isBlank()) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), raw);
                    }
                }
            }
            if (!id.isBlank()) done.add(id);
        }
        return done;
    }

    private void ackCommandsAsync(List<String> ids) {
        JsonObject body = new JsonObject();
        body.addProperty("action", "ack_commands");
        body.addProperty("secret", secretToken);
        JsonArray arr = new JsonArray();
        ids.forEach(arr::add);
        body.add("commandIds", arr);
        postJsonAsync(body);
    }

    public void logEventAsync(String eventType, String player, String detail) {
        if (${!config.enableEventLogging}) return;
        JsonObject body = new JsonObject();
        body.addProperty("action", "log_event");
        body.addProperty("secret", secretToken);
        body.addProperty("eventType", eventType);
        body.addProperty("player", player);
        body.addProperty("detail", detail);
        postJsonAsync(body);
    }

    private void postJsonAsync(JsonObject body) {
        if (gasWebAppUrl.contains("REPLACE_WITH_YOUR_DEPLOY_ID") || gasWebAppUrl.isBlank()) return;
        HttpRequest req = HttpRequest.newBuilder(URI.create(gasWebAppUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
    }

    private static String str(JsonObject o, String k, String def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def; }
    private static int numInt(JsonObject o, String k, int def) { try { return o.has(k) ? o.get(k).getAsInt() : def; } catch (Exception e) { return def; } }
    private static float numFloat(JsonObject o, String k, float def) { try { return o.has(k) ? o.get(k).getAsFloat() : def; } catch (Exception e) { return def; } }
    private static boolean bool(JsonObject o, String k, boolean def) { try { return o.has(k) ? o.get(k).getAsBoolean() : def; } catch (Exception e) { return def; } }
}
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/java/$pkgPath/server/GasHotReloadManager.java",
            displayName = "GasHotReloadManager.java",
            category = GeneratedFileCategory.FORGE_SERVER,
            language = "java",
            descriptionTh = "ระบบ Auto-Polling ดึงค่าตั้งค่า Hot-Reload และคิวคำสั่งจาก Google Apps Script โดยไม่ต้องเปิดพอร์ตหรือ Restart Server",
            content = content
        )
    }
}
