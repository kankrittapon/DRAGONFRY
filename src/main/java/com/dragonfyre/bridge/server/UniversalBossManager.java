package com.dragonfyre.bridge.server;

import com.dragonfyre.bridge.network.ModNetwork;
import com.dragonfyre.bridge.network.WorldBossSyncPacket;
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
    public volatile boolean worldBossSystemEnabled = true;
    public volatile boolean autoPromoteSystemEnabled = true;
    public volatile float globalHpScale = 1.0f;
    public volatile float globalAtkScale = 1.0f;
    public volatile String activeBarTheme = "DRAGON_GOLD";

    private long tickCounter = 0L;
    private long nextScheduledSpawnMs = System.currentTimeMillis() + 60_000L * 15L;

    public void initDefaults() {
        registry.clear();
        register(new BossProfile("Ignis, Lord of Dragonfyre", "cataclysm:ignis", "Cisco Dragonfyre", "BOTH", 100, 3.5f, 2.0f, 14.0f, 1.15f, 60, 20, 300, 1500, "minecraft:overworld", true, true, "FIRE_NOVA,ENRAGED", "give @p minecraft:netherite_ingot 5", true));
        register(new BossProfile("Netherite Monstrosity", "cataclysm:netherite_monstrosity", "Cisco Ultimate", "SCHEDULED_WORLD_BOSS", 100, 4.0f, 1.9f, 18.0f, 1.1f, 75, 25, 350, 1600, "minecraft:overworld", true, true, "IRON_SKIN,FIRE_NOVA", "give @p minecraft:diamond_block 3", true));
        register(new BossProfile("Crimson Elder Fire Dragon", "iceandfire:fire_dragon", "Ice & Fire", "SCHEDULED_WORLD_BOSS", 100, 2.8f, 1.8f, 12.0f, 1.1f, 90, 25, 400, 1800, "minecraft:overworld", true, true, "FIRE_NOVA,LIGHTNING_CALLER", "give @p minecraft:enchanted_golden_apple 2", true));
        register(new BossProfile("Ferrous Wroughtnaut Apex", "mowziesmobs:ferrous_wroughtnaut", "Mowzie's Mobs", "AUTO_PROMOTE_NATURAL", 100, 2.5f, 1.6f, 16.0f, 1.2f, 45, 15, 200, 1000, "minecraft:overworld", true, true, "IRON_SKIN,ENRAGED", "give @p minecraft:netherite_scrap 4", true));
        register(new BossProfile("Cursed Zombie Warlord (Promoted)", "minecraft:zombie", "Universal Promoter", "AUTO_PROMOTE_NATURAL", 3, 15.0f, 3.2f, 12.0f, 1.3f, 30, 12, 150, 900, "minecraft:overworld", true, true, "LIFE_STEAL,SPEED_SURGE,IRON_SKIN", "xp add @p 500 points", true));
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
