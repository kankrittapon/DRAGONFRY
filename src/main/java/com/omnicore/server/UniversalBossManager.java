package com.omnicore.server;

import com.omnicore.config.OmniConfig;
import com.omnicore.network.ModNetwork;
import com.omnicore.network.WorldBossSyncPacket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Universal Multi-Mod World Boss & Mob-to-Boss Engine (Forge 1.20.1)
 * คุณสมบัติ:
 * 1. ระบบล็อกจุดแลนด์มาร์ก (Anchor Landmark) — เลือกสถานที่แห่งเดียวในแมพเป็น World Boss
 * 2. ระบบสุ่มเกิดห่างจากผู้เล่น 500 บล็อก (Player-Relative Safe Surface)
 * 3. บาเรียสนามประลองห้ามขุดทำลาย รัศมี 500 บล็อก (Anti-Grief 500-Block Zone)
 * 4. ระบบหลอดเลือดอัจฉริยะ (Native Boss Bar vs Custom HUD Bar)
 * 5. ระบบ Raid Dynamic Scaling (+15% HP ต่อผู้เล่นที่รุมตีใน 40m)
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

        // ฟีเจอร์ขั้นสูง (ตำแหน่งเฉพาะจุด, ตารางเวลา, ป้องกันขุด, หลอดเลือด)
        public String spawnStrategy; // "ANCHOR_LANDMARK", "PLAYER_RELATIVE_500M", "DYNAMIC_BIOME"
        public int anchorX, anchorY, anchorZ, anchorRadius;
        public boolean antiGriefEnabled;
        public int protectRadius; // รัศมีป้องกันขุด (ค่าเริ่มต้น 500 บล็อก)
        public String scheduleHours; // เช่น "18:00,21:00"
        public boolean nativeBossBar; // true = ใช้หลอดเลือดเดิมของบอส, false = ใช้หลอด Custom
        public boolean raidScaling; // true = เลือดปรับเพิ่มตามคนรุมตี

        public BossProfile(String displayName, String entityId, String modPackTag, String bossMode,
                           int promotionChancePercent, float hpMult, float atkMult, float armorBonus, float speedMult,
                           int spawnIntervalMinutes, int fightDurationMinutes, int minRadius, int maxRadius,
                           String dimensionId, boolean glowingAura, boolean xaeroRedMarker,
                           String affixTraits, String rewardCommand, boolean enabled) {
            this(displayName, entityId, modPackTag, bossMode, promotionChancePercent, hpMult, atkMult, armorBonus, speedMult,
                    spawnIntervalMinutes, fightDurationMinutes, minRadius, maxRadius, dimensionId, glowingAura, xaeroRedMarker,
                    affixTraits, rewardCommand, enabled, "DYNAMIC_BIOME", 0, 0, 0, 60, true, 500, "", true, true);
        }

        public BossProfile(String displayName, String entityId, String modPackTag, String bossMode,
                           int promotionChancePercent, float hpMult, float atkMult, float armorBonus, float speedMult,
                           int spawnIntervalMinutes, int fightDurationMinutes, int minRadius, int maxRadius,
                           String dimensionId, boolean glowingAura, boolean xaeroRedMarker,
                           String affixTraits, String rewardCommand, boolean enabled,
                           String spawnStrategy, int anchorX, int anchorY, int anchorZ, int anchorRadius,
                           boolean antiGriefEnabled, int protectRadius, String scheduleHours,
                           boolean nativeBossBar, boolean raidScaling) {
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
            this.spawnStrategy = spawnStrategy != null ? spawnStrategy : "DYNAMIC_BIOME";
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.anchorZ = anchorZ;
            this.anchorRadius = anchorRadius > 0 ? anchorRadius : 60;
            this.antiGriefEnabled = antiGriefEnabled;
            this.protectRadius = protectRadius > 0 ? protectRadius : 500;
            this.scheduleHours = scheduleHours != null ? scheduleHours : "";
            this.nativeBossBar = nativeBossBar;
            this.raidScaling = raidScaling;
        }
    }

    public static class ActiveBoss {
        public final UUID uuid;
        public final BossProfile profile;
        public final ResourceKey<Level> dimension;
        public long expireAtMillis;
        public BlockPos spawnOrigin;
        public ServerBossEvent vanillaBossBar;

        public final Map<UUID, Float> damageContribution = new ConcurrentHashMap<>();
        public final Set<UUID> activeMinionUuids = Collections.newSetFromMap(new ConcurrentHashMap<>());
        public long lastMinionSummonMs = 0;
        public long lastUltimateCastMs = 0;
        public long lastTeleportMs = 0;
        public boolean ultimateCharging = false;
        public long ultimateDetonateAtMs = 0;
        public final Set<Integer> triggeredPhases = Collections.newSetFromMap(new ConcurrentHashMap<>());

        ActiveBoss(UUID uuid, BossProfile profile, ResourceKey<Level> dimension, long expireAtMillis, BlockPos spawnOrigin) {
            this.uuid = uuid;
            this.profile = profile;
            this.dimension = dimension;
            this.expireAtMillis = expireAtMillis;
            this.spawnOrigin = spawnOrigin;
            // 100% Server-side Boss Bar: สร้าง Vanilla ServerBossEvent หากบอสไม่มี Native Bar ติดมา
            if (!profile.nativeBossBar) {
                this.vanillaBossBar = new ServerBossEvent(
                        Component.literal("§6⚔ " + profile.displayName),
                        BossEvent.BossBarColor.RED,
                        BossEvent.BossBarOverlay.PROGRESS
                );
                this.vanillaBossBar.setVisible(true);
            }
        }
    }

    public static class ArenaZone {
        public final UUID bossUuid;
        public final ResourceKey<Level> dimension;
        public final BlockPos center;
        public final int radius;
        public final long expireAt;

        ArenaZone(UUID bossUuid, ResourceKey<Level> dimension, BlockPos center, int radius, long expireAt) {
            this.bossUuid = bossUuid;
            this.dimension = dimension;
            this.center = center;
            this.radius = radius;
            this.expireAt = expireAt;
        }

        public boolean isInside(ResourceKey<Level> dim, BlockPos pos) {
            if (!this.dimension.equals(dim)) return false;
            double dx = pos.getX() - center.getX();
            double dz = pos.getZ() - center.getZ();
            return (dx * dx + dz * dz) <= (radius * (double) radius);
        }
    }

    public static class PromoteResult {
        public final boolean success;
        public final String message;

        public PromoteResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private final Map<String, BossProfile> registry = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveBoss> activeBosses = new ConcurrentHashMap<>();
    private final Map<UUID, ArenaZone> activeArenas = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPromoteTimeByProfile = new ConcurrentHashMap<>();
    private final Map<String, Long> nextRespawnTimeByProfile = new ConcurrentHashMap<>();

    // Keep Inventory & Arena Respawn Cache
    private final Map<UUID, BlockPos> arenaDeathRespawns = new ConcurrentHashMap<>();
    private final Map<UUID, List<ItemStack>> savedInventories = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> savedXp = new ConcurrentHashMap<>();
    private final Set<UUID> playersInArena = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Random random = new Random();
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("OmniCoreBosses");

    // Hot-Reloadable Live Settings
    public volatile boolean worldBossSystemEnabled = true;
    public volatile boolean autoPromoteSystemEnabled = true;
    public volatile float globalHpScale = 1.0f;
    public volatile float globalAtkScale = 1.0f;
    public volatile String activeBarTheme = "DRAGON_GOLD";

    private long tickCounter = 0L;
    private long nextScheduledSpawnMs = System.currentTimeMillis() + 60_000L * 15L;
    private String lastExecutedClockMinute = "";

    public int getActiveBossCount() {
        return activeBosses.size();
    }

    public int getRegisteredBossCount() {
        return registry.size();
    }

    public void initDefaults() {
        try {
            worldBossSystemEnabled = OmniConfig.INSTANCE.worldBossEnabled.get();
            autoPromoteSystemEnabled = OmniConfig.INSTANCE.autoPromoteEnabled.get();
            globalHpScale = OmniConfig.INSTANCE.globalBossHpScale.get().floatValue();
            globalAtkScale = OmniConfig.INSTANCE.globalBossAtkScale.get().floatValue();
            activeBarTheme = OmniConfig.INSTANCE.bossBarTheme.get();
        } catch (Exception ignored) {}

        File configFile = new File("config/omnicore_bosses.json");
        if (!configFile.exists() && new File("config/dragonfyre_bosses.json").exists()) configFile = new File("config/dragonfyre_bosses.json");
        if (configFile.exists()) {
            loadRegistryFromFile();
            if (!registry.isEmpty()) return;
        }

        registry.clear();
        // 1. Ignis (Cataclysm) — ใช้หลอดเลือดเดิมของบอส (nativeBossBar = true), แนะนำล็อก Anchor ที่ Burning Arena หรือเกิด 500m
        register(new BossProfile("Ignis, Lord of Dragonfyre", "cataclysm:ignis", "Cisco Dragonfyre", "BOTH",
                100, 3.5f, 2.0f, 14.0f, 1.15f, 60, 20, 300, 1500, "minecraft:overworld",
                true, true, "FIRE_NOVA,ENRAGED", "give @p minecraft:netherite_ingot 5", true,
                "ANCHOR_LANDMARK", 0, 0, 0, 80, true, 500, "18:00,21:00", true, true));

        // 2. Netherite Monstrosity (Cataclysm) — ใช้หลอดเลือดเดิม (nativeBossBar = true), เกิดห่างผู้เล่น 500m ปลอดภัย
        register(new BossProfile("Netherite Monstrosity", "cataclysm:netherite_monstrosity", "Cisco Ultimate", "SCHEDULED_WORLD_BOSS",
                100, 4.0f, 1.9f, 18.0f, 1.1f, 75, 25, 350, 1600, "minecraft:overworld",
                true, true, "IRON_SKIN,FIRE_NOVA", "give @p minecraft:diamond_block 3", true,
                "PLAYER_RELATIVE_500M", 0, 0, 0, 60, true, 500, "20:00", true, true));

        // 3. Crimson Elder Fire Dragon (Ice & Fire) — ใช้หลอดเลือดเดิม
        register(new BossProfile("Crimson Elder Fire Dragon", "iceandfire:fire_dragon", "Ice & Fire", "SCHEDULED_WORLD_BOSS",
                100, 2.8f, 1.8f, 12.0f, 1.1f, 90, 25, 400, 1800, "minecraft:overworld",
                true, true, "FIRE_NOVA,LIGHTNING_CALLER", "give @p minecraft:enchanted_golden_apple 2", true,
                "PLAYER_RELATIVE_500M", 0, 0, 0, 60, true, 500, "", true, true));

        // 4. Ferrous Wroughtnaut Apex (Mowzie's) — ใช้หลอดเลือดเดิม
        register(new BossProfile("Ferrous Wroughtnaut Apex", "mowziesmobs:ferrous_wroughtnaut", "Mowzie's Mobs", "AUTO_PROMOTE_NATURAL",
                100, 2.5f, 1.6f, 16.0f, 1.2f, 45, 15, 200, 1000, "minecraft:overworld",
                true, true, "IRON_SKIN,ENRAGED", "give @p minecraft:netherite_scrap 4", true,
                "DYNAMIC_BIOME", 0, 0, 0, 60, true, 500, "", true, true));

        saveRegistryToFile();
    }

    public void loadRegistryFromFile() {
        File configFile = new File("config/omnicore_bosses.json");
        if (!configFile.exists() && new File("config/dragonfyre_bosses.json").exists()) configFile = new File("config/dragonfyre_bosses.json");
        if (!configFile.exists()) return;
        try {
            String text = Files.readString(configFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(text);
            if (root.isJsonArray()) {
                registry.clear();
                for (JsonElement el : root.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    BossProfile bp = new BossProfile(
                            o.has("displayName") ? o.get("displayName").getAsString() : "Custom Boss",
                            o.has("entityId") ? o.get("entityId").getAsString() : "minecraft:zombie",
                            o.has("modPackTag") ? o.get("modPackTag").getAsString() : "Custom",
                            o.has("bossMode") ? o.get("bossMode").getAsString() : "BOTH",
                            o.has("promotionChancePercent") ? o.get("promotionChancePercent").getAsInt() : 100,
                            o.has("hpMultiplier") ? o.get("hpMultiplier").getAsFloat() : 3.0f,
                            o.has("attackMultiplier") ? o.get("attackMultiplier").getAsFloat() : 1.8f,
                            o.has("armorBonus") ? o.get("armorBonus").getAsFloat() : 10.0f,
                            o.has("speedMultiplier") ? o.get("speedMultiplier").getAsFloat() : 1.15f,
                            o.has("spawnIntervalMinutes") ? o.get("spawnIntervalMinutes").getAsInt() : 45,
                            o.has("fightDurationMinutes") ? o.get("fightDurationMinutes").getAsInt() : 20,
                            o.has("minSpawnRadius") ? o.get("minSpawnRadius").getAsInt() : 250,
                            o.has("maxSpawnRadius") ? o.get("maxSpawnRadius").getAsInt() : 1400,
                            o.has("dimensionId") ? o.get("dimensionId").getAsString() : "minecraft:overworld",
                            o.has("glowingAura") ? o.get("glowingAura").getAsBoolean() : true,
                            o.has("xaeroRedMarker") ? o.get("xaeroRedMarker").getAsBoolean() : true,
                            o.has("affixTraits") ? o.get("affixTraits").getAsString() : "FIRE_NOVA",
                            o.has("rewardCommand") ? o.get("rewardCommand").getAsString() : "",
                            o.has("enabled") ? o.get("enabled").getAsBoolean() : true,
                            o.has("spawnStrategy") ? o.get("spawnStrategy").getAsString() : "ANCHOR_LANDMARK",
                            o.has("anchorX") ? o.get("anchorX").getAsInt() : 0,
                            o.has("anchorY") ? o.get("anchorY").getAsInt() : 0,
                            o.has("anchorZ") ? o.get("anchorZ").getAsInt() : 0,
                            o.has("anchorRadius") ? o.get("anchorRadius").getAsInt() : 80,
                            o.has("antiGriefEnabled") ? o.get("antiGriefEnabled").getAsBoolean() : true,
                            o.has("protectRadius") ? o.get("protectRadius").getAsInt() : 500,
                            o.has("scheduleHours") ? o.get("scheduleHours").getAsString() : "",
                            o.has("nativeBossBar") ? o.get("nativeBossBar").getAsBoolean() : true,
                            o.has("raidScaling") ? o.get("raidScaling").getAsBoolean() : true
                    );
                    register(bp);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveRegistryToFile() {
        try {
            File configFile = new File("config/omnicore_bosses.json");
        if (!configFile.exists() && new File("config/dragonfyre_bosses.json").exists()) configFile = new File("config/dragonfyre_bosses.json");
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            JsonArray arr = new JsonArray();
            for (BossProfile p : registry.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", p.entityId.hashCode());
                o.addProperty("displayName", p.displayName);
                o.addProperty("entityId", p.entityId);
                o.addProperty("modPackTag", p.modPackTag);
                o.addProperty("bossMode", p.bossMode);
                o.addProperty("promotionChancePercent", p.promotionChancePercent);
                o.addProperty("hpMultiplier", p.hpMult);
                o.addProperty("attackMultiplier", p.atkMult);
                o.addProperty("armorBonus", p.armorBonus);
                o.addProperty("speedMultiplier", p.speedMult);
                o.addProperty("spawnIntervalMinutes", p.spawnIntervalMinutes);
                o.addProperty("fightDurationMinutes", p.fightDurationMinutes);
                o.addProperty("minSpawnRadius", p.minRadius);
                o.addProperty("maxSpawnRadius", p.maxRadius);
                o.addProperty("dimensionId", p.dimensionId);
                o.addProperty("glowingAura", p.glowingAura);
                o.addProperty("xaeroRedMarker", p.xaeroRedMarker);
                o.addProperty("affixTraits", p.affixTraits);
                o.addProperty("rewardCommand", p.rewardCommand);
                o.addProperty("enabled", p.enabled);
                o.addProperty("spawnStrategy", p.spawnStrategy);
                o.addProperty("anchorX", p.anchorX);
                o.addProperty("anchorY", p.anchorY);
                o.addProperty("anchorZ", p.anchorZ);
                o.addProperty("anchorRadius", p.anchorRadius);
                o.addProperty("antiGriefEnabled", p.antiGriefEnabled);
                o.addProperty("protectRadius", p.protectRadius);
                o.addProperty("scheduleHours", p.scheduleHours);
                o.addProperty("nativeBossBar", p.nativeBossBar);
                o.addProperty("raidScaling", p.raidScaling);
                arr.add(o);
            }
            Files.writeString(configFile.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(arr), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(BossProfile profile) {
        if (profile != null && profile.entityId != null && !profile.entityId.isBlank()) {
            registry.put(profile.entityId.toLowerCase(Locale.ROOT), profile);
        }
    }

    public BossProfile getProfile(String entityId) {
        return entityId != null ? registry.get(entityId.toLowerCase(Locale.ROOT)) : null;
    }

    public List<BossProfile> getAllProfiles() {
        return new ArrayList<>(registry.values());
    }

    public void replaceRegistryFromGas(List<BossProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;
        registry.clear();
        for (BossProfile p : profiles) register(p);
    }

    public boolean setAnchorForBoss(String entityId, BlockPos pos, int radius) {
        BossProfile p = findBossProfile(entityId);
        if (p == null) {
            ResourceLocation rl = ResourceLocation.tryParse(entityId.toLowerCase(Locale.ROOT));
            if (rl != null && ForgeRegistries.ENTITY_TYPES.containsKey(rl)) {
                EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
                String name = type != null ? type.getDescription().getString() : entityId;
                p = new BossProfile("Ancient " + name, entityId, rl.getNamespace(), "BOTH",
                        100, 3.0f, 1.8f, 14.0f, 1.1f, 60, 25, 300, 1500, "minecraft:overworld",
                        true, true, "IRON_SKIN,ENRAGED", "give @p cisco_mod:champion_coin 2", true,
                        "ANCHOR_LANDMARK", pos.getX(), pos.getY(), pos.getZ(), radius > 0 ? radius : 80,
                        true, 500, "", true, true);
                register(p);
            }
        }
        if (p != null) {
            p.spawnStrategy = "ANCHOR_LANDMARK";
            p.anchorX = pos.getX();
            p.anchorY = pos.getY();
            p.anchorZ = pos.getZ();
            p.anchorRadius = radius > 0 ? radius : 80;
            saveRegistryToFile();
            return true;
        }
        return false;
    }

    public PromoteResult promoteExistingEntity(LivingEntity living, int anchorRadius) {
        if (living == null || living.level().isClientSide()) {
            return new PromoteResult(false, "เป้าหมายไม่ถูกต้อง หรืออยู่ฝั่ง Client");
        }
        if (living.getPersistentData().getBoolean(NBT_BOSS_TAG)) {
            return new PromoteResult(false, "ม็อบตัวนี้เป็น World Boss อยู่แล้ว!");
        }

        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(living.getType());
        if (key == null) return new PromoteResult(false, "ไม่พบ ResourceLocation ของม็อบ");
        String entityId = key.toString();

        // -------------------------------------------------------------
        // 🔒 ข้อจำกัดที่ 1: ม็อบ 1 ชนิด สามารถเป็น World Boss ได้ 1 ตัวเท่านั้นพร้อมกัน
        // -------------------------------------------------------------
        for (ActiveBoss ab : activeBosses.values()) {
            if (ab.profile.entityId.equalsIgnoreCase(entityId) && !ab.uuid.equals(living.getUUID())) {
                return new PromoteResult(false, String.format(
                        "ม็อบชนิด '%s' มีตัวที่เป็น World Boss กำลังมีชีวิตอยู่ในเซิร์ฟเวอร์แล้ว (%s) ไม่สามารถโปรโมตซ้ำได้! (จำกัด 1 ม็อบ 1 ตัวเท่านั้น)",
                        entityId, ab.profile.displayName));
            }
        }

        // -------------------------------------------------------------
        // 🔒 ข้อจำกัดที่ 2: ม็อบ 1 ชนิด สามารถล็อกได้เพียง 1 Landmark Area เท่านั้น
        // -------------------------------------------------------------
        BossProfile p = findBossProfile(entityId);
        int finalRadius = anchorRadius > 0 ? anchorRadius : (p != null && p.anchorRadius > 0 ? p.anchorRadius : 80);

        if (p != null && "ANCHOR_LANDMARK".equals(p.spawnStrategy) && (p.anchorX != 0 || p.anchorZ != 0)) {
            double dx = living.getBlockX() - p.anchorX;
            double dz = living.getBlockZ() - p.anchorZ;
            double distSq = dx * dx + dz * dz;
            int existingRadius = p.anchorRadius > 0 ? p.anchorRadius : 80;

            // หากจุดที่ผู้เล่นต้องการโปรโมต อยู่นอกพื้นที่แลนด์มาร์กเดิมเกินระยะรัศมี
            if (distSq > (existingRadius * (double) existingRadius)) {
                return new PromoteResult(false, String.format(
                        "ม็อบชนิด '%s' ถูกล็อกเป็น Landmark Area ไว้แล้วที่ (%d, %d, %d) รัศมี %d บล็อก! (1 ม็อบสามารถมีได้เพียง 1 Area เท่านั้น ไม่สามารถสร้างซ้ำได้ หากต้องการย้ายจุด กรุณาใช้คำสั่ง /dfboss anchor clear %s ก่อน)",
                        entityId, p.anchorX, p.anchorY, p.anchorZ, existingRadius, entityId));
            }
        }

        if (p == null) {
            String name = living.getType().getDescription().getString();
            p = new BossProfile("Ancient " + name, entityId, key.getNamespace(), "BOTH",
                    100, 3.0f, 1.8f, 14.0f, 1.1f, 60, 25, 300, 1500, living.level().dimension().location().toString(),
                    true, true, "IRON_SKIN,ENRAGED", "give @p cisco_mod:champion_coin 2", true,
                    "ANCHOR_LANDMARK", living.getBlockX(), living.getBlockY(), living.getBlockZ(), finalRadius,
                    true, 500, "", true, true);
            register(p);
        } else {
            p.spawnStrategy = "ANCHOR_LANDMARK";
            p.anchorX = living.getBlockX();
            p.anchorY = living.getBlockY();
            p.anchorZ = living.getBlockZ();
            p.anchorRadius = finalRadius;
        }
        saveRegistryToFile();

        // ยกระดับ Entity ตัวนี้ให้เป็น World Boss ทันที
        nextRespawnTimeByProfile.remove(p.entityId.toLowerCase(Locale.ROOT));
        applyBossEnhancements(living, p, living.level());
        long expire = System.currentTimeMillis() + p.fightDurationMinutes * 60_000L;
        BlockPos origin = living.blockPosition();
        activeBosses.put(living.getUUID(), new ActiveBoss(living.getUUID(), p, living.level().dimension(), expire, origin));

        if (p.antiGriefEnabled && finalRadius > 0) {
            activeArenas.put(living.getUUID(), new ArenaZone(living.getUUID(), living.level().dimension(), origin, finalRadius, expire));
        }

        AnnouncementTriggerHandler.broadcastBossSpawn(p.displayName, origin, p.fightDurationMinutes);
        return new PromoteResult(true, String.format(
                "โปรโมต %s เป็น World Boss สำเร็จ! ล็อกจุด Landmark ประจำพื้นที่ที่ (%d, %d, %d) รัศมี %d บล็อก (มีได้ 1 ตัว และ 1 Area ถาวร)",
                p.displayName, origin.getX(), origin.getY(), origin.getZ(), finalRadius));
    }

    public void clearAnchorForBoss(String entityId) {
        BossProfile p = findBossProfile(entityId);
        if (p != null) {
            p.spawnStrategy = "DYNAMIC_BIOME";
            saveRegistryToFile();
        }
    }

    public boolean removeBossRule(String entityId) {
        return registry.remove(entityId.toLowerCase(Locale.ROOT)) != null;
    }

    public String formatRegistrySummary() {
        if (registry.isEmpty()) return "§eยังไม่มีบอสในคลัง";
        StringBuilder sb = new StringBuilder("§6=== [World Boss List ในระบบ (" + registry.size() + ")] ===§r\n");
        int idx = 1;
        for (BossProfile p : registry.values()) {
            sb.append(String.format(" §e[%d]§r %s (§b%s§r)\n   » โหมด: %s | HPx%.1f | จุดเกิด: %s\n",
                    idx, p.displayName, p.entityId, p.bossMode, p.hpMult,
                    "ANCHOR_LANDMARK".equals(p.spawnStrategy) ? String.format("Anchor(%d,%d,%d)", p.anchorX, p.anchorY, p.anchorZ) : p.spawnStrategy));
            idx++;
        }
        sb.append("§7พิมพ์ /dfboss spawnhere <1-5 หรือชื่อ> เพื่อเสกตรงหน้า หรือ /dfboss spawnnear <1-5> ห่าง 500m");
        return sb.toString();
    }

    public String formatAreaStatusSummary(MinecraftServer server) {
        StringBuilder sb = new StringBuilder("§6=== [🏰 World Boss Landmark Areas & Status] ===§r\n");
        long now = System.currentTimeMillis();

        // 1. รายชื่อบอสที่กำลังมีชีวิตอยู่ (Active Bosses)
        if (activeBosses.isEmpty()) {
            sb.append(" §7• ขณะนี้ไม่มี World Boss ในสนามรบ\n");
        } else {
            sb.append(String.format(" §e⚔️ กำลังต่อสู้อยู่ (%d ตัว):§r\n", activeBosses.size()));
            for (ActiveBoss ab : activeBosses.values()) {
                ServerLevel sl = server != null ? server.getLevel(ab.dimension) : null;
                Entity ent = sl != null ? sl.getEntity(ab.uuid) : null;
                LivingEntity living = ent instanceof LivingEntity le ? le : null;
                float hp = living != null ? living.getHealth() : 0f;
                float maxHp = living != null ? living.getMaxHealth() : 1f;
                int secLeft = Math.max(0, (int) ((ab.expireAtMillis - now) / 1000L));
                sb.append(String.format("   » §c%s§r | เลือด: §a%.0f/%.0f§r | จุดเกิด: (%d, %d, %d) | หมดเวลาใน: §e%d นาที %d วินาที§r\n",
                        ab.profile.displayName, hp, maxHp, ab.spawnOrigin.getX(), ab.spawnOrigin.getY(), ab.spawnOrigin.getZ(),
                        secLeft / 60, secLeft % 60));
            }
        }

        // 2. รายชื่อโซน Landmark ที่ล็อกพื้นที่ไว้
        sb.append(" §b🏰 โซน Landmark ประจำพื้นที่ (เกิดซ้ำอัตโนมัติ):§r\n");
        int count = 0;
        for (BossProfile bp : registry.values()) {
            if (!bp.enabled || !"ANCHOR_LANDMARK".equals(bp.spawnStrategy) || (bp.anchorX == 0 && bp.anchorZ == 0)) continue;
            count++;
            String pKey = bp.entityId.toLowerCase(Locale.ROOT);
            boolean isAlive = activeBosses.values().stream()
                    .anyMatch(active -> active.profile.entityId.equalsIgnoreCase(bp.entityId));

            if (isAlive) {
                sb.append(String.format("   » §a[มีชีวิต]§r §f%s§r @ (%d, %d, %d) รัศมี %d บล็อก\n",
                        bp.displayName, bp.anchorX, bp.anchorY, bp.anchorZ, bp.anchorRadius));
            } else {
                Long nextMs = nextRespawnTimeByProfile.get(pKey);
                if (nextMs != null && nextMs > now) {
                    int sec = (int) ((nextMs - now) / 1000L);
                    sb.append(String.format("   » §e[รอเกิดใหม่]§r §f%s§r @ (%d, %d, %d) » อีก §6%d นาที %d วินาที§r (รอบละ %d นาที)\n",
                            bp.displayName, bp.anchorX, bp.anchorY, bp.anchorZ, sec / 60, sec % 60, bp.spawnIntervalMinutes));
                } else {
                    sb.append(String.format("   » §7[พร้อมเกิด]§r §f%s§r @ (%d, %d, %d) » รอเกิดรอบถัดไป\n",
                            bp.displayName, bp.anchorX, bp.anchorY, bp.anchorZ));
                }
            }
        }
        if (count == 0) {
            sb.append("   §7(ยังไม่มีโซน Landmark ที่กำหนดพิกัด)\n");
        }
        return sb.toString();
    }

    public boolean forceRespawnLandmark(MinecraftServer server, String entityId) {
        BossProfile p = findBossProfile(entityId);
        if (p == null || !"ANCHOR_LANDMARK".equals(p.spawnStrategy)) return false;
        nextRespawnTimeByProfile.remove(p.entityId.toLowerCase(Locale.ROOT));
        for (ActiveBoss ab : new ArrayList<>(activeBosses.values())) {
            if (ab.profile.entityId.equalsIgnoreCase(p.entityId)) {
                ServerLevel sl = server != null ? server.getLevel(ab.dimension) : null;
                if (sl != null) {
                    Entity ent = sl.getEntity(ab.uuid);
                    if (ent != null) ent.discard();
                    cleanupArena(sl, ab);
                }
                sendRemovePacket(ab);
                activeBosses.remove(ab.uuid);
                activeArenas.remove(ab.uuid);
            }
        }
        return spawnWorldBossNow(server, p.entityId, null);
    }

    public BossProfile findBossProfile(String query) {
        if (query == null || query.isBlank()) return null;
        String clean = query.trim().toLowerCase(Locale.ROOT);

        // 1. Direct match with registry key (entityId)
        BossProfile bp = registry.get(clean);
        if (bp != null) return bp;

        // 2. Match by numeric ID (e.g. 1, 2, 3, 4)
        try {
            int num = Integer.parseInt(clean);
            int idx = 1;
            for (BossProfile p : registry.values()) {
                if (idx == num || (p.entityId.hashCode() == num)) return p;
                idx++;
            }
        } catch (NumberFormatException ignored) {}

        // 3. Match by partial entityId or displayName (e.g. "ignis", "dragon", "monstrosity")
        for (BossProfile p : registry.values()) {
            if (p.entityId.toLowerCase(Locale.ROOT).contains(clean) ||
                p.displayName.toLowerCase(Locale.ROOT).contains(clean)) {
                return p;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* 🛡️ ระบบบาเรียป้องกันสนามประลอง 500 บล็อก (Anti-Grief 500m Zone)      */
    /* ------------------------------------------------------------------ */
    public ArenaZone getActiveArena(ResourceKey<Level> dim, BlockPos pos) {
        if (activeArenas.isEmpty() || pos == null) return null;
        long now = System.currentTimeMillis();
        for (ArenaZone az : activeArenas.values()) {
            if (now < az.expireAt && az.isInside(dim, pos)) {
                return az;
            }
        }
        return null;
    }

    public boolean isInsideActiveArena(ResourceKey<Level> dim, BlockPos pos) {
        return getActiveArena(dim, pos) != null;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() == null || event.getPlayer().isCreative()) return;
        if (event.getLevel() instanceof Level lvl && isInsideActiveArena(lvl.dimension(), event.getPos())) {
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(
                    Component.literal("§c🛡️ เขตแดน World Boss (รัศมี 500 บล็อก) ห้ามขุดทำลายในระหว่างการต่อสู้!"), true
            );
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isCreative()) return;
        if (event.getLevel() instanceof Level lvl && isInsideActiveArena(lvl.dimension(), event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.displayClientMessage(
                        Component.literal("§c🛡️ ไม่สามารถวางบล็อกในสนามประลอง World Boss ได้!"), true
                );
            }
        }
    }

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        ResourceKey<Level> dim = event.getLevel().dimension();
        event.getAffectedBlocks().removeIf(pos -> isInsideActiveArena(dim, pos));
    }

    /* ------------------------------------------------------------------ */
    /* ⚔️ ระบบคุ้มครองผู้เล่นในสนามประลอง (Keep Inventory & In-Arena Respawn) */
    /* ------------------------------------------------------------------ */
    @SubscribeEvent
    public void onPlayerDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (isInsideActiveArena(player.serverLevel().dimension(), player.blockPosition())) {
                event.setCanceled(true); // ของไม่ตกในเขต World Boss!
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            List<ItemStack> saved = savedInventories.remove(original.getUUID());
            if (saved != null) {
                for (int i = 0; i < Math.min(saved.size(), newPlayer.getInventory().getContainerSize()); i++) {
                    newPlayer.getInventory().setItem(i, saved.get(i));
                }
            }
            Integer xp = savedXp.remove(original.getUUID());
            if (xp != null) {
                newPlayer.giveExperiencePoints(xp);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BlockPos safePos = arenaDeathRespawns.remove(player.getUUID());
            if (safePos != null) {
                ArenaZone az = getActiveArena(player.serverLevel().dimension(), safePos);
                if (az != null) {
                    ServerLevel sl = player.serverLevel();
                    int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, safePos.getX(), safePos.getZ());
                    player.teleportTo(sl, safePos.getX() + 0.5, Math.max(safePos.getY(), y) + 1.0, safePos.getZ() + 0.5, player.getYRot(), 0f);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4)); // อมตะ 5 วินาที
                    player.displayClientMessage(Component.literal("§a⚔️ ฟื้นคืนชีพที่ขอบสนามประลอง World Boss แล้ว! (ของและเลเวลอยู่ครบ เตียงเดิมไม่ถูกแทนที่)"), false);
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* 🛡️ ระบบจำกัดดาเมจ 2 ระดับ (Normal 5% Cap vs Telegraphed Ultimate)     */
    /* ------------------------------------------------------------------ */
    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 1. ผู้เล่นโจมตี World Boss -> บันทึก DPS Contribution
        if (event.getEntity().getPersistentData().getBoolean(NBT_BOSS_TAG)) {
            ActiveBoss ab = activeBosses.get(event.getEntity().getUUID());
            if (ab != null && event.getSource().getEntity() instanceof ServerPlayer player) {
                float dealt = Math.min(event.getAmount(), event.getEntity().getHealth());
                ab.damageContribution.merge(player.getUUID(), dealt, Float::sum);
            }
            return;
        }

        // 2. World Boss หรือสมุน โจมตีผู้เล่น -> ป้องกัน One-Shot
        if (event.getEntity() instanceof ServerPlayer player) {
            Entity attacker = event.getSource().getEntity();
            boolean isBossAttack = false;
            ActiveBoss sourceBoss = null;

            if (attacker != null) {
                if (attacker.getPersistentData().getBoolean(NBT_BOSS_TAG)) {
                    isBossAttack = true;
                    sourceBoss = activeBosses.get(attacker.getUUID());
                } else if (attacker.getPersistentData().getBoolean("df_boss_minion")) {
                    isBossAttack = true;
                }
            } else {
                ArenaZone az = getActiveArena(player.serverLevel().dimension(), player.blockPosition());
                if (az != null) {
                    isBossAttack = true;
                    sourceBoss = activeBosses.get(az.bossUuid);
                }
            }

            if (isBossAttack) {
                float maxHp = player.getMaxHealth();
                boolean isUltimate = (sourceBoss != null && sourceBoss.ultimateCharging) || event.getAmount() >= 75.0f;

                if (isUltimate) {
                    // สกิลมหาประลัย: หากหลบไม่พ้น โดน 75% ของเลือดสูงสุด
                    float wipeDmg = maxHp * 0.75f;
                    event.setAmount(Math.min(event.getAmount(), wipeDmg));
                } else {
                    // การโจมตีทั่วไป: จำกัดไม่เกิน 5% ของ Max HP ต่อฮิต
                    float normalCap = Math.max(1.0f, maxHp * 0.05f);
                    event.setAmount(Math.min(event.getAmount(), normalCap));
                }
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /* ม็อบเกิดตามธรรมชาติ -> ยกระดับเป็น World Boss เฉพาะจุด Anchor          */
    /* ------------------------------------------------------------------ */
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

        // 1. ตรวจสอบโควต้ารวม: ป้องกันเกิด World Boss ล้นเซิร์ฟเวอร์ (สูงสุด 2 ตัวพร้อมกัน)
        if (activeBosses.size() >= 2) return;

        // 2. ตรวจสอบบอสซ้ำซ้อน: ห้ามให้ Entity ชนิดเดียวกันนี้เกิดเป็น Boss ซ้อนกันเด็ดขาด (ต้องรอตัวเดิมตายก่อน)
        boolean alreadyActive = activeBosses.values().stream()
                .anyMatch(ab -> ab.profile.entityId.equalsIgnoreCase(profile.entityId));
        if (alreadyActive) return;

        // 3. ตรวจสอบ Cooldown: บอสชนิดเดียวกันต้องรออย่างน้อยเท่ากับ spawnIntervalMinutes (เช่น 30 นาที) ถึงจะโปรโมตได้อีก
        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(15, profile.spawnIntervalMinutes) * 60_000L;
        Long lastPromote = lastPromoteTimeByProfile.get(profile.entityId);
        if (lastPromote != null && (now - lastPromote) < cooldownMs) {
            return;
        }

        // 4. จุดสำคัญ: หากตั้งเป็น ANCHOR_LANDMARK จะต้องเกิดในรัศมีของ Anchor จุดนี้แห่งเดียวเท่านั้น!
        // จุดอื่นๆ ทั่วโลกจะยังคงเป็นม็อบปกติของ Mod ตามเดิม ไม่ถูกยกระดับ
        if ("ANCHOR_LANDMARK".equals(profile.spawnStrategy)) {
            BlockPos pos = living.blockPosition();
            double dx = pos.getX() - profile.anchorX;
            double dz = pos.getZ() - profile.anchorZ;
            double distSq = dx * dx + dz * dz;
            if (distSq > profile.anchorRadius * (double) profile.anchorRadius) {
                return; // อยู่นอกพื้นที่แลนด์มาร์ก -> ปล่อยเป็นม็อบปกติ
            }
        }

        int roll = random.nextInt(100) + 1;
        if (roll <= profile.promotionChancePercent) {
            lastPromoteTimeByProfile.put(profile.entityId, now);
            applyBossEnhancements(living, profile, event.getLevel());
            long expire = System.currentTimeMillis() + profile.fightDurationMinutes * 60_000L;
            BlockPos origin = living.blockPosition();
            activeBosses.put(living.getUUID(), new ActiveBoss(living.getUUID(), profile, event.getLevel().dimension(), expire, origin));

            // เปิดบาเรียสนามประลองห้ามขุด รัศมี 500 บล็อก
            if (profile.antiGriefEnabled && profile.protectRadius > 0) {
                activeArenas.put(living.getUUID(), new ArenaZone(living.getUUID(), event.getLevel().dimension(), origin, profile.protectRadius, expire));
            }
            LOGGER.info("[OmniCore] ⚔️ Auto-Promoted {} to World Boss '{}' at pos [{}, {}, {}] in {}",
                    key, profile.displayName, origin.getX(), origin.getY(), origin.getZ(), dimId);
            AnnouncementTriggerHandler.broadcastBossSpawn(profile.displayName, origin, profile.fightDurationMinutes);
        }
    }

    /* ------------------------------------------------------------------ */
    /* เสก World Boss สด (ตามเวลา หรือคำสั่ง) พร้อมระบบสแกนปลอดภัย 500 บล็อก */
    /* ------------------------------------------------------------------ */
    public boolean spawnWorldBossNow(MinecraftServer server, String requestedEntityId) {
        return spawnWorldBossNow(server, requestedEntityId, null);
    }

    public boolean spawnWorldBossNow(MinecraftServer server, String requestedEntityId, ServerPlayer relativePlayer) {
        BossProfile chosen = null;
        if (requestedEntityId != null && !requestedEntityId.isBlank()) {
            chosen = findBossProfile(requestedEntityId);
        } else {
            List<BossProfile> candidates = registry.values().stream()
                    .filter(p -> p.enabled && ("SCHEDULED_WORLD_BOSS".equals(p.bossMode) || "BOTH".equals(p.bossMode)))
                    .toList();
            if (!candidates.isEmpty()) chosen = candidates.get(random.nextInt(candidates.size()));
        }
        if (chosen == null) return false;

        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.tryParse(chosen.dimensionId.equals("*") ? "minecraft:overworld" : chosen.dimensionId));
        ServerLevel level = server.getLevel(dimKey);
        if (level == null) level = (relativePlayer != null) ? relativePlayer.serverLevel() : server.overworld();

        BlockPos targetPos = null;

        // หากระบุ relativePlayer ให้ค้นหาจุดเกิดปลอดภัยห่าง 500 บล็อก
        if (relativePlayer != null) {
            level = relativePlayer.serverLevel();
            targetPos = findSafeSurfaceAroundPlayer(level, relativePlayer, 500);
        }

        // กลยุทธ์ที่ 1: ล็อกพิกัดแลนด์มาร์กแน่นอน (Anchor Landmark)
        if (targetPos == null && "ANCHOR_LANDMARK".equals(chosen.spawnStrategy) && (chosen.anchorX != 0 || chosen.anchorZ != 0)) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, chosen.anchorX, chosen.anchorZ);
            targetPos = new BlockPos(chosen.anchorX, chosen.anchorY > 0 ? chosen.anchorY : y, chosen.anchorZ);
        }

        // กลยุทธ์ที่ 2: สุ่มเกิดห่างจากผู้เล่น 500 บล็อก ในจุดปลอดภัย (Player-Relative Surface)
        if (targetPos == null && "PLAYER_RELATIVE_500M".equals(chosen.spawnStrategy)) {
            List<ServerPlayer> players = level.players();
            if (!players.isEmpty()) {
                ServerPlayer anchorPlayer = players.get(random.nextInt(players.size()));
                targetPos = findSafeSurfaceAroundPlayer(level, anchorPlayer, 500);
            }
        }

        // กลยุทธ์สำรอง: สุ่มตามระยะห่างจาก Spawn โลก
        if (targetPos == null) {
            BlockPos spawnCenter = level.getSharedSpawnPos();
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dist = chosen.minRadius + random.nextInt(Math.max(50, chosen.maxRadius - chosen.minRadius));
            int x = spawnCenter.getX() + (int) (Math.cos(angle) * dist);
            int z = spawnCenter.getZ() + (int) (Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            targetPos = new BlockPos(x, y, z);
        }

        return spawnWorldBossAt(server, chosen, level, targetPos);
    }

    public boolean spawnWorldBossAt(MinecraftServer server, BossProfile chosen, ServerLevel level, BlockPos targetPos) {
        if (chosen == null) return false;

        ResourceLocation entityRl = ResourceLocation.tryParse(chosen.entityId);
        if (entityRl == null) return false;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityRl);
        if (type == null) {
            LOGGER.error("[OmniCore] EntityType not found in Forge registry: {}", chosen.entityId);
            return false;
        }

        if (level == null) level = server.overworld();

        Entity entity = type.create(level);
        if (!(entity instanceof LivingEntity living)) {
            LOGGER.error("[OmniCore] Entity is not LivingEntity: {}", chosen.entityId);
            return false;
        }

        living.moveTo(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        applyBossEnhancements(living, chosen, level);
        level.addFreshEntity(living);

        long expire = System.currentTimeMillis() + chosen.fightDurationMinutes * 60_000L;
        activeBosses.put(living.getUUID(), new ActiveBoss(living.getUUID(), chosen, level.dimension(), expire, targetPos));

        // เปิดบาเรีย Anti-Grief 500 บล็อก
        if (chosen.antiGriefEnabled && chosen.protectRadius > 0) {
            activeArenas.put(living.getUUID(), new ArenaZone(living.getUUID(), level.dimension(), targetPos, chosen.protectRadius, expire));
        }

        AnnouncementTriggerHandler.broadcastBossSpawn(chosen.displayName, targetPos, chosen.fightDurationMinutes);
        GasHotReloadManager.INSTANCE.logEventAsync("BOSS_SPAWN", "SERVER", chosen.displayName + " (" + chosen.entityId + ") at " + targetPos.toShortString());
        return true;
    }

    public boolean spawnWorldBossAt(MinecraftServer server, String query, ServerLevel level, BlockPos targetPos) {
        BossProfile profile = findBossProfile(query);
        return spawnWorldBossAt(server, profile, level, targetPos);
    }

    /**
     * ค้นหาพื้นผิวโลกที่ปลอดภัยห่างจากผู้เล่น 500 บล็อก (ระบบกรอง 5 ชั้น — ไม่เกิดในน้ำ ลาวา ถ้ำ หรือหน้าผา)
     */
    private BlockPos findSafeSurfaceAroundPlayer(ServerLevel level, ServerPlayer player, int targetDist) {
        BlockPos pPos = player.blockPosition();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int x = pPos.getX() + (int) (Math.cos(angle) * targetDist);
            int z = pPos.getZ() + (int) (Math.sin(angle) * targetDist);
            level.getChunk(x >> 4, z >> 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos testPos = new BlockPos(x, y, z);

            BlockState surface = level.getBlockState(testPos);
            BlockState above1 = level.getBlockState(testPos.above(1));
            BlockState above2 = level.getBlockState(testPos.above(2));
            BlockState above3 = level.getBlockState(testPos.above(3));

            // กรอง: ห้ามเป็นของเหลว (น้ำ/ลาวา) และด้านบนต้องเป็นอากาศโปร่ง
            if (!surface.liquid() && above1.isAir() && above2.isAir() && above3.isAir()) {
                return testPos;
            }
        }
        int fallbackY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pPos.getX() + targetDist, pPos.getZ());
        return new BlockPos(pPos.getX() + targetDist, fallbackY, pPos.getZ());
    }

    private void applyBossEnhancements(LivingEntity boss, BossProfile profile, Level level) {
        boss.getPersistentData().putBoolean(NBT_BOSS_TAG, true);
        boss.getPersistentData().putString(NBT_PROFILE_ID, profile.entityId);
        boss.setCustomName(Component.literal("§c§l☠ " + profile.displayName));
        boss.setCustomNameVisible(true);

        // Raid Dynamic Scaling: เลือดบอสขยายสเกล +15% ต่อผู้เล่นในระยะ 40 บล็อก
        double raidScale = 1.0;
        if (profile.raidScaling && level instanceof ServerLevel sl) {
            AABB area = new AABB(boss.blockPosition()).inflate(40.0);
            int nearbyPlayers = sl.getEntitiesOfClass(ServerPlayer.class, area).size();
            if (nearbyPlayers > 1) {
                raidScale = 1.0 + (nearbyPlayers - 1) * 0.15;
            }
        }

        AttributeInstance maxHp = boss.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            double scaled = maxHp.getBaseValue() * profile.hpMult * globalHpScale * raidScale;
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

        // ม็อบธรรมดาที่ตั้งเป็น World Boss -> สวมใส่ชุดเกราะเทพและอาวุธเรืองแสง
        if (!profile.nativeBossBar && boss instanceof Mob mob) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
            mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            mob.setDropChance(EquipmentSlot.HEAD, 0.0f);
            mob.setDropChance(EquipmentSlot.CHEST, 0.0f);
            mob.setDropChance(EquipmentSlot.LEGS, 0.0f);
            mob.setDropChance(EquipmentSlot.FEET, 0.0f);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        }
    }

    private void cleanupArena(ServerLevel level, ActiveBoss ab) {
        if (level == null || ab == null) return;
        // 1. สลายสมุนที่เหลือทั้งหมด
        for (UUID minionId : ab.activeMinionUuids) {
            Entity m = level.getEntity(minionId);
            if (m != null) m.discard();
        }
        ab.activeMinionUuids.clear();

        // 2. ดับไฟในรัศมี 32 บล็อก
        BlockPos center = ab.spawnOrigin;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-32, -8, -32), center.offset(32, 16, 32))) {
            BlockState bs = level.getBlockState(p);
            if (bs.is(Blocks.FIRE) || bs.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(p, false);
            }
        }
    }

    private void summonZombieMinions(ServerLevel level, ActiveBoss ab, LivingEntity boss, int count) {
        if (level == null || ab == null || boss == null) return;
        for (int i = 0; i < count; i++) {
            Zombie minion = EntityType.ZOMBIE.create(level);
            if (minion == null) continue;
            double offsetX = (random.nextDouble() - 0.5) * 6.0;
            double offsetZ = (random.nextDouble() - 0.5) * 6.0;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) (boss.getX() + offsetX), (int) (boss.getZ() + offsetZ));
            minion.moveTo(boss.getX() + offsetX, y, boss.getZ() + offsetZ, random.nextFloat() * 360f, 0f);
            minion.getPersistentData().putBoolean("df_boss_minion", true);
            minion.getPersistentData().putUUID("df_master_boss", ab.uuid);
            minion.setCustomName(Component.literal("§c☠ สมุน World Boss"));
            minion.setCustomNameVisible(true);

            minion.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            minion.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            minion.setDropChance(EquipmentSlot.HEAD, 0.0f);
            minion.setDropChance(EquipmentSlot.MAINHAND, 0.0f);

            level.addFreshEntity(minion);
            ab.activeMinionUuids.add(minion.getUUID());
        }
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, boss.getX(), boss.getY() + 1.0, boss.getZ(), 20, 1.0, 1.0, 1.0, 0.05);
    }

    private void deliverReward(MinecraftServer server, ServerPlayer player, String itemWithNbt, int count) {
        if (server == null || player == null) return;
        String cmd = String.format("give %s %s %d", player.getName().getString(), itemWithNbt, count);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
    }

    private void distributeDpsRewards(MinecraftServer server, ActiveBoss ab) {
        if (server == null || ab == null || ab.damageContribution.isEmpty()) return;

        List<Map.Entry<UUID, Float>> ranked = new ArrayList<>(ab.damageContribution.entrySet());
        ranked.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("§6👑 === [สรุปผลงาน World Boss DPS Leaderboard] ===§r"), false
        );

        int rank = 1;
        for (Map.Entry<UUID, Float> entry : ranked) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            String pName = p != null ? p.getName().getString() : "นักผจญภัย";
            float dmg = entry.getValue();

            if (rank <= 3) {
                String medal = rank == 1 ? "🥇 MVP" : (rank == 2 ? "🥈 อันดับ 2" : "🥉 อันดับ 3");
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(String.format(" %s §f%s §7- ดาเมจสะสม: §e%.0f", medal, pName, dmg)), false
                );
            }

            if (p != null) {
                if (rank == 1) {
                    deliverReward(server, p, "ftbquests:lootcrate{type:\"legendary_crate\"}", 1);
                    deliverReward(server, p, "cisco_mod:champion_coin", 3);
                    p.giveExperienceLevels(30);
                    p.displayClientMessage(Component.literal("§6👑 คุณได้อันดับ 1 (MVP)! รับ 1x Legendary Crate + 3x Champion Coins + 30 EXP Levels!"), false);
                } else if (rank <= 3) {
                    deliverReward(server, p, "ftbquests:lootcrate{type:\"epic_crate\"}", 1);
                    deliverReward(server, p, "cisco_mod:champion_coin", 2);
                    p.giveExperienceLevels(20);
                    p.displayClientMessage(Component.literal("§d🥈 คุณได้อันดับ " + rank + "! รับ 1x Epic Crate + 2x Champion Coins + 20 EXP Levels!"), false);
                } else if (dmg >= 100.0f) {
                    deliverReward(server, p, "ftbquests:lootcrate{type:\"rare_lootcrate\"}", 1);
                    deliverReward(server, p, "cisco_mod:champion_coin", 1);
                    p.giveExperienceLevels(10);
                    p.displayClientMessage(Component.literal("§b🥉 รางวัลผู้ร่วมสู้! รับ 1x Rare Crate + 1x Champion Coin + 10 EXP Levels!"), false);
                }
            }
            rank++;
        }
    }

    public void despawnAllActiveBosses(MinecraftServer server) {
        for (ActiveBoss ab : new ArrayList<>(activeBosses.values())) {
            ServerLevel level = server.getLevel(ab.dimension);
            if (level != null) {
                cleanupArena(level, ab);
                Entity e = level.getEntity(ab.uuid);
                if (e != null) e.discard();
            }
            if (ab.vanillaBossBar != null) {
                ab.vanillaBossBar.removeAllPlayers();
                ab.vanillaBossBar.setVisible(false);
            }
            sendRemovePacket(ab);
        }
        activeBosses.clear();
        activeArenas.clear();

        // กวาดล้าง entity ในแมพทั้งหมดที่ติดแท็ก NBT_BOSS_TAG ป้องกันตัวที่หลุดจาก memory ค้างอยู่ในโลก
        if (server != null) {
            for (ServerLevel sl : server.getAllLevels()) {
                for (Entity ent : sl.getAllEntities()) {
                    if (ent.getPersistentData().getBoolean(NBT_BOSS_TAG) || ent.getPersistentData().getBoolean("df_boss_minion")) {
                        ent.discard();
                    }
                }
            }
        }
        LOGGER.info("[UniversalBossManager] 🧹 เคลียร์ World Boss และถอนบาเรียทั้งหมดเรียบร้อยแล้ว");
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 1. ผู้เล่นตายในสนามประลอง World Boss -> เซฟของ + เซฟเลเวล + บันทึกจุดเกิดขอบสนาม
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaZone az = getActiveArena(player.serverLevel().dimension(), player.blockPosition());
            if (az != null) {
                List<ItemStack> inv = new ArrayList<>();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    inv.add(player.getInventory().getItem(i).copy());
                }
                savedInventories.put(player.getUUID(), inv);
                savedXp.put(player.getUUID(), player.totalExperience);

                BlockPos bCenter = az.center;
                double angle = Math.atan2(player.getZ() - bCenter.getZ(), player.getX() - bCenter.getX());
                int rx = bCenter.getX() + (int) (Math.cos(angle) * 35.0);
                int rz = bCenter.getZ() + (int) (Math.sin(angle) * 35.0);
                ServerLevel sl = player.serverLevel();
                int ry = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rx, rz);
                arenaDeathRespawns.put(player.getUUID(), new BlockPos(rx, ry, rz));

                player.displayClientMessage(Component.literal("§e🛡️ [ระบบคุ้มครอง] คุณเสียชีวิตในเขต World Boss: ไอเทมและเลเวลจะไม่สูญหาย!"), false);
            }
            return;
        }

        // 2. World Boss ถูกกำจัด
        UUID id = event.getEntity().getUUID();
        ActiveBoss ab = activeBosses.remove(id);
        activeArenas.remove(id); // ถอนบาเรีย Anti-Grief 500 บล็อกทันที
        if (ab == null) return;

        // บันทึกเวลาเกิดใหม่ของ Landmark Area รอบถัดไป
        if ("ANCHOR_LANDMARK".equals(ab.profile.spawnStrategy)) {
            long cooldownMs = Math.max(5, ab.profile.spawnIntervalMinutes) * 60_000L;
            nextRespawnTimeByProfile.put(ab.profile.entityId.toLowerCase(Locale.ROOT), System.currentTimeMillis() + cooldownMs);
            LOGGER.info("[UniversalBossManager] ⏳ Landmark World Boss '{}' will respawn at ({}, {}, {}) in {} minutes",
                    ab.profile.displayName, ab.profile.anchorX, ab.profile.anchorY, ab.profile.anchorZ, ab.profile.spawnIntervalMinutes);
        }

        sendRemovePacket(ab);
        MinecraftServer server = event.getEntity().getServer();
        if (server != null && event.getEntity().level() instanceof ServerLevel sl) {
            cleanupArena(sl, ab);

            String killer = event.getSource().getEntity() instanceof ServerPlayer sp ? sp.getName().getString() : "นักผจญภัย";
            AnnouncementTriggerHandler.broadcastBossDefeated(killer, ab.profile.displayName);
            GasHotReloadManager.INSTANCE.logEventAsync("BOSS_DEFEATED", killer, "Defeated " + ab.profile.displayName);

            // แจกโบนัส Last-Hit
            if (event.getSource().getEntity() instanceof ServerPlayer killerPlayer) {
                deliverReward(server, killerPlayer, "cisco_mod:champion_coin", 1);
                deliverReward(server, killerPlayer, "minecraft:netherite_scrap", 2);
                killerPlayer.displayClientMessage(Component.literal("§e🗡️ โบนัสสังหารปิดฉากบอส (Last-Hit): รับ 1x Champion Coin + 2x Netherite Scrap!"), false);
            }

            // คำนวณและแจกของรางวัลตามอันดับ DPS Leaderboard
            distributeDpsRewards(server, ab);

            if (!ab.profile.rewardCommand.isBlank()) {
                String cmd = ab.profile.rewardCommand.replace("@p", killer);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), cmd);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        tickCounter++;
        MinecraftServer server = event.getServer();

        // ตรวจสอบตารางเวลาจริง (Real Clock Hours เช่น 18:00, 21:00)
        if (worldBossSystemEnabled && tickCounter % 1200 == 0) { // ตรวจทุกๆ 1 นาที
            String nowMinute = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            if (!nowMinute.equals(lastExecutedClockMinute)) {
                for (BossProfile bp : registry.values()) {
                    if (bp.enabled && !bp.scheduleHours.isBlank() && bp.scheduleHours.contains(nowMinute)) {
                        spawnWorldBossNow(server, bp.entityId, null);
                        lastExecutedClockMinute = nowMinute;
                        break;
                    }
                }
            }
        }

        // สุ่มเกิด World Boss ตามรอบนาที (Interval)
        if (worldBossSystemEnabled && tickCounter % 200 == 0 && System.currentTimeMillis() >= nextScheduledSpawnMs && activeBosses.isEmpty()) {
            spawnWorldBossNow(server, null, null);
            int intervalMin = registry.values().stream().filter(p -> p.enabled).mapToInt(p -> p.spawnIntervalMinutes).min().orElse(45);
            nextScheduledSpawnMs = System.currentTimeMillis() + intervalMin * 60_000L;
        }

        // ตรวจสอบการเกิดซ้ำอัตโนมัติของ Landmark Area แต่ละโซน (Independent Landmark Respawns)
        if (worldBossSystemEnabled && tickCounter % 200 == 0) { // ทุก 10 วินาที
            long nowMs = System.currentTimeMillis();
            for (BossProfile bp : registry.values()) {
                if (bp.enabled && "ANCHOR_LANDMARK".equals(bp.spawnStrategy) && (bp.anchorX != 0 || bp.anchorZ != 0)) {
                    String pKey = bp.entityId.toLowerCase(Locale.ROOT);
                    boolean isAlive = activeBosses.values().stream()
                            .anyMatch(active -> active.profile.entityId.equalsIgnoreCase(bp.entityId));
                    if (!isAlive) {
                        Long nextSpawn = nextRespawnTimeByProfile.get(pKey);
                        if (nextSpawn == null) {
                            long cooldownMs = Math.max(5, bp.spawnIntervalMinutes) * 60_000L;
                            nextRespawnTimeByProfile.put(pKey, nowMs + cooldownMs);
                        } else if (nowMs >= nextSpawn) {
                            nextRespawnTimeByProfile.remove(pKey);
                            spawnWorldBossNow(server, bp.entityId, null);
                            LOGGER.info("[UniversalBossManager] ⚔️ Landmark World Boss '{}' auto-respawned at ({}, {}, {})!",
                                    bp.displayName, bp.anchorX, bp.anchorY, bp.anchorZ);
                        }
                    }
                }
            }
        }

        // ตรวจสอบผู้เล่นเข้า/ออกจากสนามประลอง World Boss (ทุก 20 ticks / 1 วินาที)
        if (tickCounter % 20 == 0 && !activeArenas.isEmpty()) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ArenaZone az = getActiveArena(player.serverLevel().dimension(), player.blockPosition());
                if (az != null) {
                    if (playersInArena.add(player.getUUID())) {
                        ActiveBoss ab = activeBosses.get(az.bossUuid);
                        String bossName = ab != null ? ab.profile.displayName : "World Boss";
                        player.displayClientMessage(Component.literal("§6⚔️ เข้าสู่สนามประลอง: §c" + bossName + " §e(ของไม่ตกเมื่อตาย)"), true);
                        try {
                            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
                            player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§c§l⚔ BOSS ARENA ⚔")));
                            player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("§eของไม่ตกเมื่อตายในเขตนี้")));
                        } catch (Exception ignored) {}
                    }
                } else {
                    if (playersInArena.remove(player.getUUID())) {
                        player.displayClientMessage(Component.literal("§7ออกจากเขตสนามประลอง World Boss แล้ว"), true);
                    }
                }
            }
        }

        // ซิงก์ Custom Boss Bar + Xaero Red Pin ทุกๆ 10 ticks
        if (tickCounter % 10 != 0 || activeBosses.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (ActiveBoss ab : new ArrayList<>(activeBosses.values())) {
            ServerLevel level = server.getLevel(ab.dimension);
            Entity entity = level != null ? level.getEntity(ab.uuid) : null;
            LivingEntity living = entity instanceof LivingEntity le ? le : null;
            int remainingSec = (int) ((ab.expireAtMillis - now) / 1000L);

            if (living == null || !living.isAlive() || remainingSec <= 0) {
                if (living != null && living.isAlive() && remainingSec <= 0) {
                    living.discard();
                    AnnouncementTriggerHandler.broadcastCustom("⌛ " + ab.profile.displayName + " หายตัวไปเนื่องจากหมดเวลา!", "เตรียมพร้อมสำหรับ World Boss รอบถัดไป", 0x94A3B8, "SLIDE_DOWN", "minecraft:entity.enderman.teleport", 100);
                }
                if (level != null) cleanupArena(level, ab);
                sendRemovePacket(ab);
                activeBosses.remove(ab.uuid);
                activeArenas.remove(ab.uuid);

                if ("ANCHOR_LANDMARK".equals(ab.profile.spawnStrategy)) {
                    long cooldownMs = Math.max(5, ab.profile.spawnIntervalMinutes) * 60_000L;
                    nextRespawnTimeByProfile.put(ab.profile.entityId.toLowerCase(Locale.ROOT), now + cooldownMs);
                    LOGGER.info("[UniversalBossManager] ⏳ Landmark World Boss '{}' expired. Will respawn in {} minutes",
                            ab.profile.displayName, ab.profile.spawnIntervalMinutes);
                }
                continue;
            }

            BlockPos pos = living.blockPosition();

            // ----------------------------------------------------
            // 🔮 Universal Boss Skills Engine
            // ----------------------------------------------------
            float hpRatio = living.getHealth() / Math.max(1.0f, living.getMaxHealth());

            // 1. Ender Shift (Teleport): เมื่อผู้เล่นเป้าหมายอยู่ไกลเกิน 16 บล็อก
            if ((ab.profile.affixTraits.contains("ENDER_SHIFT") || !ab.profile.nativeBossBar) && living instanceof Mob mob && level != null) {
                if (mob.getTarget() instanceof ServerPlayer target && (now - ab.lastTeleportMs > 15_000L)) {
                    if (mob.distanceToSqr(target) > 16.0 * 16.0) {
                        ab.lastTeleportMs = now;
                        double tx = target.getX() - target.getLookAngle().x * 2.5;
                        double tz = target.getZ() - target.getLookAngle().z * 2.5;
                        int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) tx, (int) tz);
                        mob.teleportTo(tx, ty, tz);
                        level.sendParticles(ParticleTypes.PORTAL, tx, ty + 1.0, tz, 25, 0.5, 1.0, 0.5, 0.1);
                        level.playSound(null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
                        target.displayClientMessage(Component.literal("§5⚡ " + ab.profile.displayName + " เทเลพอร์ตประชิดตัวคุณ!"), true);
                    }
                }
            }

            // 2. Zombie Horde (Minion Summoning): Cooldown 60s, Max 4 minions, Phase triggers (75%, 50%, 25%)
            if (level != null) {
                ab.activeMinionUuids.removeIf(mid -> {
                    Entity m = level.getEntity(mid);
                    return m == null || !m.isAlive();
                });
                boolean phaseTrigger = false;
                if (hpRatio <= 0.75f && ab.triggeredPhases.add(75)) phaseTrigger = true;
                if (hpRatio <= 0.50f && ab.triggeredPhases.add(50)) phaseTrigger = true;
                if (hpRatio <= 0.25f && ab.triggeredPhases.add(25)) phaseTrigger = true;

                if ((phaseTrigger || (now - ab.lastMinionSummonMs > 60_000L)) && ab.activeMinionUuids.size() < 4) {
                    ab.lastMinionSummonMs = now;
                    summonZombieMinions(level, ab, living, Math.min(3, 4 - ab.activeMinionUuids.size()));
                }

                // 3. Telegraphed Ultimate Wipe Skill: Warning 2.5s -> 75% Max HP wipe if not dodged
                if (!ab.ultimateCharging && (now - ab.lastUltimateCastMs > 80_000L)) {
                    AABB area = new AABB(living.blockPosition()).inflate(20.0);
                    if (!level.getEntitiesOfClass(ServerPlayer.class, area).isEmpty()) {
                        ab.ultimateCharging = true;
                        ab.lastUltimateCastMs = now;
                        ab.ultimateDetonateAtMs = now + 2500L;
                        AnnouncementTriggerHandler.broadcastCustom(
                                "⚠️ [ULTIMATE] " + ab.profile.displayName + " กำลังชาร์จพลังทำลายล้าง!",
                                "รีบหลบออกจากระยะ 14 บล็อกด่วน! (2.5 วินาที)",
                                0xEF4444, "SCALE_POP", "minecraft:entity.warden.sonic_charge", 50
                        );
                    }
                }

                // จุดระเบิด Ultimate
                if (ab.ultimateCharging && now >= ab.ultimateDetonateAtMs) {
                    ab.ultimateCharging = false;
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, living.getX(), living.getY() + 1.0, living.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                    level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.8f);
                    for (ServerPlayer player : level.players()) {
                        if (player.distanceToSqr(living) <= 14.0 * 14.0 && !player.isCreative() && !player.isSpectator()) {
                            float wipeDmg = player.getMaxHealth() * 0.75f;
                            player.hurt(level.damageSources().mobAttack(living), wipeDmg);
                            player.setDeltaMovement(player.getDeltaMovement().add(0, 0.6, 0));
                            player.displayClientMessage(Component.literal("§c💥 คุณหลบไม่พ้นสกิลมหาประลัย โดนความเสียหายหนัก!"), true);
                        }
                    }
                }
            }

            // AI Boss Guarding Behavior:
            // หากบอสไม่มีผู้เล่นโจมตีหรือยังไม่พบเป้าหมาย (getTarget() == null)
            // ให้คุมตำแหน่งให้อยู่ในอาณาเขตจุดเกิด 30 บล็อก ไม่เดินเพ่นพ่านมั่ว
            if (living instanceof Mob mob && level != null) {
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    double distSq = mob.distanceToSqr(ab.spawnOrigin.getX(), ab.spawnOrigin.getY(), ab.spawnOrigin.getZ());
                    if (distSq > 30.0 * 30.0) {
                        mob.getNavigation().moveTo(ab.spawnOrigin.getX(), ab.spawnOrigin.getY(), ab.spawnOrigin.getZ(), 1.0);
                    }
                    // สแกนค้นหาผู้เล่นเอาชีวิตรอดในระยะ 40 บล็อกเพื่อเปิดฉากโจมตี
                    Player nearestPlayer = level.getNearestPlayer(mob, 40.0);
                    if (nearestPlayer != null && nearestPlayer.isAlive() && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                        mob.setTarget(nearestPlayer);
                    }
                }
            }

            // 100% Server-side Boss Bar Sync: แสดงผลหลอดเลือดให้ผู้เล่น Vanilla ในระยะ 128 บล็อก
            if (ab.vanillaBossBar != null && level != null) {
                float ratio = Math.max(0f, Math.min(1f, living.getHealth() / Math.max(1f, living.getMaxHealth())));
                ab.vanillaBossBar.setProgress(ratio);
                for (ServerPlayer player : level.players()) {
                    if (player.distanceToSqr(living) <= 128 * 128) {
                        ab.vanillaBossBar.addPlayer(player);
                    } else {
                        ab.vanillaBossBar.removePlayer(player);
                    }
                }
            }

            ModNetwork.sendToAll(new WorldBossSyncPacket(
                    ab.uuid, ab.profile.displayName, ab.profile.modPackTag,
                    living.getHealth(), living.getMaxHealth(),
                    pos.getX(), pos.getY(), pos.getZ(),
                    ab.dimension.location().toString(), remainingSec,
                    ab.profile.affixTraits, activeBarTheme,
                    ab.profile.xaeroRedMarker, ab.profile.nativeBossBar, true
            ));
        }
    }

    private void sendRemovePacket(ActiveBoss ab) {
        if (ab.vanillaBossBar != null) {
            ab.vanillaBossBar.removeAllPlayers();
            ab.vanillaBossBar.setVisible(false);
        }
        ModNetwork.sendToAll(new WorldBossSyncPacket(
                ab.uuid, ab.profile.displayName, ab.profile.modPackTag,
                0f, 1f, 0, 0, 0, ab.dimension.location().toString(),
                0, "", activeBarTheme, ab.profile.xaeroRedMarker, ab.profile.nativeBossBar, false
        ));
    }

    /**
     * ดึง Entity ทั้งหมดของทุก Mod ที่ลงทะเบียนในเกม แล้วบันทึกลง config/dragonfyre_bridge/registered_mobs.json
     * รองรับระบบ Global Modpack Scanner จัดหมวดหมู่ม็อบสิ่งมีชีวิต (Living/Monsters)
     */
    public void exportRegisteredMobsJson() {
        try {
            File dir = new File(FMLPaths.CONFIGDIR.get().toFile(), "dragonfyre_bridge");
            if (!dir.exists()) dir.mkdirs();
            File outFile = new File(dir, "registered_mobs.json");
            File rootOutFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "registered_mobs.json");

            JsonArray arr = new JsonArray();
            ForgeRegistries.ENTITY_TYPES.getEntries().forEach(entry -> {
                ResourceLocation rl = entry.getKey().location();
                EntityType<?> type = entry.getValue();
                String category = type.getCategory().getName();
                boolean isLiving = type.getCategory() != net.minecraft.world.entity.MobCategory.MISC;

                JsonObject obj = new JsonObject();
                obj.addProperty("id", rl.toString());
                obj.addProperty("namespace", rl.getNamespace());
                obj.addProperty("name", type.getDescription().getString());
                obj.addProperty("category", category);
                obj.addProperty("living", isLiving);
                obj.addProperty("summonable", type.canSummon());
                arr.add(obj);
            });

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonStr = gson.toJson(arr);
            Files.writeString(outFile.toPath(), jsonStr, java.nio.charset.StandardCharsets.UTF_8);
            Files.writeString(rootOutFile.toPath(), jsonStr, java.nio.charset.StandardCharsets.UTF_8);
            LOGGER.info("[UniversalBossManager] 📋 Global Modpack Mob Scan: Exported {} entities to registered_mobs.json", arr.size());
        } catch (Exception ignored) {
        }
    }
}
