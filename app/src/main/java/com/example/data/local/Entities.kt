package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "announcement_rules")
data class AnnouncementRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val triggerType: String, // RARITY_LOOT, MOB_KILL, BOSS_SPAWN, BOSS_DEFEATED, MILESTONE, REMOTE_CUSTOM
    val matchFilter: String, // e.g., "apotheosis:epic,apotheosis:mythic,EPIC" or "cataclysm:*"
    val messageTemplate: String,
    val subtitleTemplate: String,
    val accentHex: String, // e.g. "#A855F7"
    val animationStyle: String, // SLIDE_DOWN, RUNE_GLOW, SCALE_POP
    val soundId: String, // e.g. "minecraft:ui.toast.challenge_complete"
    val durationSeconds: Int = 5,
    val cooldownSeconds: Int = 10,
    val enabled: Boolean = true
)

@Entity(tableName = "world_boss_profiles")
data class WorldBossProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val entityId: String, // Any Forge ResourceLocation e.g. "cataclysm:ignis", "mowziesmobs:ferrous_wroughtnaut", "minecraft:zombie"
    val modPackTag: String, // e.g. "Cisco Dragonfyre", "Ice & Fire", "Mowzie's Mobs", "Universal Promoter", "Custom Mod"
    val bossMode: String, // SCHEDULED_WORLD_BOSS, AUTO_PROMOTE_NATURAL, BOTH
    val promotionChancePercent: Int = 100, // 1..100 used when AUTO_PROMOTE_NATURAL or BOTH
    val hpMultiplier: Float = 3.0f,
    val attackMultiplier: Float = 1.8f,
    val armorBonus: Float = 10.0f,
    val speedMultiplier: Float = 1.15f,
    val spawnIntervalMinutes: Int = 45,
    val fightDurationMinutes: Int = 20,
    val minSpawnRadius: Int = 250,
    val maxSpawnRadius: Int = 1200,
    val dimensionId: String = "minecraft:overworld",
    val glowingAura: Boolean = true,
    val xaeroRedMarker: Boolean = true,
    val affixTraits: String = "FIRE_NOVA,ENRAGED", // Comma-separated: FIRE_NOVA, LIFE_STEAL, IRON_SKIN, LIGHTNING_CALLER, SPEED_SURGE, ENRAGED
    val rewardCommand: String = "give @p minecraft:netherite_ingot 4",
    val enabled: Boolean = true
)

@Entity(tableName = "studio_config")
data class StudioConfigEntity(
    @PrimaryKey val id: Int = 1,
    val modId: String = "dragonfyre_bridge",
    val modName: String = "Dragonfyre World Boss & GAS Bridge",
    val packageName: String = "com.dragonfyre.bridge",
    val modVersion: String = "1.2.0",
    // Custom Boss Bar styling
    val bossBarTheme: String = "DRAGON_GOLD", // DRAGON_GOLD, CRIMSON_OBSIDIAN, ARCANE_RUNE, EMERALD_VOID
    val bossBarHeightDp: Int = 16,
    val showExactHp: Boolean = true,
    val showPhaseNotches: Boolean = true,
    val showCountdownTimer: Boolean = true,
    val showDistanceAndCoords: Boolean = true,
    val enableDamageLagTrail: Boolean = true,
    val xaeroWaypointPrefix: String = "[BOSS]",
    // Google Apps Script Bridge
    val gasWebAppUrl: String = "",
    val gasSecretToken: String = "DF_SECRET_2026",
    val pollIntervalSeconds: Int = 20,
    val enableGasSync: Boolean = true,
    val enableEventLogging: Boolean = true,
    // Hot-Reload Server Settings (applied live without restart!)
    val worldBossSystemEnabled: Boolean = true,
    val autoPromoteMobSystemEnabled: Boolean = true,
    val announcementsEnabled: Boolean = true,
    val globalBossHpScale: Float = 1.0f,
    val globalBossAtkScale: Float = 1.0f,
    val minLootRarity: String = "EPIC", // UNCOMMON, RARE, EPIC, MYTHIC, ANCIENT
    val keepInventory: Boolean = true,
    val mobGriefing: Boolean = false,
    val doDaylightCycle: Boolean = true,
    val pvpEnabled: Boolean = false,
    val serverDifficulty: String = "HARD", // PEACEFUL, EASY, NORMAL, HARD
    val welcomeBannerText: String = "ยินดีต้อนรับสู่เซิร์ฟเวอร์ Cisco Medieval RPG!",
    val maintenanceMode: Boolean = false,
    val xpMultiplier: Float = 1.5f
)

@Entity(tableName = "remote_action_logs")
data class RemoteActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val actionType: String,
    val summary: String,
    val status: String, // SUCCESS, ERROR, QUEUED
    val responseDetail: String
)
