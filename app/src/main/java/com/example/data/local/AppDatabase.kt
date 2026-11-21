package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AnnouncementRuleEntity::class,
        WorldBossProfileEntity::class,
        StudioConfigEntity::class,
        RemoteActionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun announcementRuleDao(): AnnouncementRuleDao
    abstract fun worldBossProfileDao(): WorldBossProfileDao
    abstract fun studioConfigDao(): StudioConfigDao
    abstract fun remoteActionLogDao(): RemoteActionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dragonfyre_forge_studio.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object DefaultStudioPresets {
    fun initialRules(): List<AnnouncementRuleEntity> = listOf(
        AnnouncementRuleEntity(
            title = "ดรอปอาวุธ/เกราะระดับ Epic+",
            triggerType = "RARITY_LOOT",
            matchFilter = "apotheosis:epic,apotheosis:mythic,apotheosis:ancient,EPIC",
            messageTemplate = "⚔ {player} ได้รับไอเทมระดับ {rarity}: {item}",
            subtitleTemplate = "ตรวจพบพลัง Affix หายากใน Cisco RPG",
            accentHex = "#A855F7",
            animationStyle = "RUNE_GLOW",
            soundId = "minecraft:ui.toast.challenge_complete",
            durationSeconds = 6,
            cooldownSeconds = 8,
            enabled = true
        ),
        AnnouncementRuleEntity(
            title = "World Boss ถือกำเนิดบนแผนที่",
            triggerType = "BOSS_SPAWN",
            matchFilter = "*",
            messageTemplate = "🔥 {mob} ปรากฏตัวแล้วที่ ({x}, {y}, {z})",
            subtitleTemplate = "เปิด Xaero's World Map เพื่อดูจุดแดง • เหลือเวลา {time} นาที",
            accentHex = "#EF4444",
            animationStyle = "SLIDE_DOWN",
            soundId = "minecraft:entity.ender_dragon.growl",
            durationSeconds = 7,
            cooldownSeconds = 5,
            enabled = true
        ),
        AnnouncementRuleEntity(
            title = "โค่น World Boss สำเร็จ",
            triggerType = "BOSS_DEFEATED",
            matchFilter = "*",
            messageTemplate = "👑 {player} สังหาร World Boss {mob} สำเร็จ!",
            subtitleTemplate = "ผู้ร่วมปราบได้รับรางวัลพิเศษ • จุดแดงบนแผนที่ถูกนำออกแล้ว",
            accentHex = "#F59E0B",
            animationStyle = "SCALE_POP",
            soundId = "minecraft:ui.toast.challenge_complete",
            durationSeconds = 6,
            cooldownSeconds = 5,
            enabled = true
        ),
        AnnouncementRuleEntity(
            title = "สังหารม็อบ/บอสพิเศษข้าม Mod",
            triggerType = "MOB_KILL",
            matchFilter = "cataclysm:*,iceandfire:*_dragon,twilightforest:*,mowziesmobs:*,bosses_of_mass_destruction:*",
            messageTemplate = "☠ {player} สังหารม็อบระดับตำนาน {mob}",
            subtitleTemplate = "บันทึกเกียรติประวัติขึ้น Google Sheets แล้ว",
            accentHex = "#38BDF8",
            animationStyle = "SLIDE_DOWN",
            soundId = "minecraft:entity.wither.death",
            durationSeconds = 5,
            cooldownSeconds = 10,
            enabled = true
        ),
        AnnouncementRuleEntity(
            title = "ประกาศด่วนจาก Google Script",
            triggerType = "REMOTE_CUSTOM",
            matchFilter = "GAS_BROADCAST",
            messageTemplate = "📢 {message}",
            subtitleTemplate = "ประกาศจากผู้ดูแลเซิร์ฟเวอร์ (Live Broadcast)",
            accentHex = "#10B981",
            animationStyle = "RUNE_GLOW",
            soundId = "minecraft:block.bell.use",
            durationSeconds = 6,
            cooldownSeconds = 3,
            enabled = true
        )
    )

    fun initialBossProfiles(): List<WorldBossProfileEntity> = listOf(
        WorldBossProfileEntity(
            displayName = "Ignis, Lord of Dragonfyre",
            entityId = "cataclysm:ignis",
            modPackTag = "Cisco Dragonfyre",
            bossMode = "BOTH",
            promotionChancePercent = 100,
            hpMultiplier = 3.5f,
            attackMultiplier = 2.0f,
            armorBonus = 14f,
            speedMultiplier = 1.15f,
            spawnIntervalMinutes = 60,
            fightDurationMinutes = 20,
            minSpawnRadius = 300,
            maxSpawnRadius = 1500,
            dimensionId = "minecraft:overworld",
            glowingAura = true,
            xaeroRedMarker = true,
            affixTraits = "FIRE_NOVA,ENRAGED",
            rewardCommand = "give @p minecraft:netherite_ingot 5",
            enabled = true
        ),
        WorldBossProfileEntity(
            displayName = "Netherite Monstrosity",
            entityId = "cataclysm:netherite_monstrosity",
            modPackTag = "Cisco Ultimate",
            bossMode = "SCHEDULED_WORLD_BOSS",
            promotionChancePercent = 100,
            hpMultiplier = 4.0f,
            attackMultiplier = 1.9f,
            armorBonus = 18f,
            speedMultiplier = 1.1f,
            spawnIntervalMinutes = 75,
            fightDurationMinutes = 25,
            minSpawnRadius = 350,
            maxSpawnRadius = 1600,
            dimensionId = "minecraft:overworld",
            glowingAura = true,
            xaeroRedMarker = true,
            affixTraits = "IRON_SKIN,FIRE_NOVA",
            rewardCommand = "give @p minecraft:diamond_block 3",
            enabled = true
        ),
        WorldBossProfileEntity(
            displayName = "Crimson Elder Fire Dragon",
            entityId = "iceandfire:fire_dragon",
            modPackTag = "Ice & Fire",
            bossMode = "SCHEDULED_WORLD_BOSS",
            promotionChancePercent = 100,
            hpMultiplier = 2.8f,
            attackMultiplier = 1.8f,
            armorBonus = 12f,
            speedMultiplier = 1.1f,
            spawnIntervalMinutes = 90,
            fightDurationMinutes = 25,
            minSpawnRadius = 400,
            maxSpawnRadius = 1800,
            dimensionId = "minecraft:overworld",
            glowingAura = true,
            xaeroRedMarker = true,
            affixTraits = "FIRE_NOVA,LIGHTNING_CALLER",
            rewardCommand = "give @p minecraft:enchanted_golden_apple 2",
            enabled = true
        ),
        WorldBossProfileEntity(
            displayName = "Ferrous Wroughtnaut Apex",
            entityId = "mowziesmobs:ferrous_wroughtnaut",
            modPackTag = "Mowzie's Mobs",
            bossMode = "AUTO_PROMOTE_NATURAL",
            promotionChancePercent = 100,
            hpMultiplier = 2.5f,
            attackMultiplier = 1.6f,
            armorBonus = 16f,
            speedMultiplier = 1.2f,
            spawnIntervalMinutes = 45,
            fightDurationMinutes = 15,
            minSpawnRadius = 200,
            maxSpawnRadius = 1000,
            dimensionId = "minecraft:overworld",
            glowingAura = true,
            xaeroRedMarker = true,
            affixTraits = "IRON_SKIN,ENRAGED",
            rewardCommand = "give @p minecraft:netherite_scrap 4",
            enabled = true
        ),
        WorldBossProfileEntity(
            displayName = "Cursed Zombie Warlord (Promoted)",
            entityId = "minecraft:zombie",
            modPackTag = "Universal Promoter",
            bossMode = "AUTO_PROMOTE_NATURAL",
            promotionChancePercent = 3,
            hpMultiplier = 15.0f,
            attackMultiplier = 3.2f,
            armorBonus = 12f,
            speedMultiplier = 1.3f,
            spawnIntervalMinutes = 30,
            fightDurationMinutes = 12,
            minSpawnRadius = 150,
            maxSpawnRadius = 900,
            dimensionId = "minecraft:overworld",
            glowingAura = true,
            xaeroRedMarker = true,
            affixTraits = "LIFE_STEAL,SPEED_SURGE,IRON_SKIN",
            rewardCommand = "xp add @p 500 points",
            enabled = true
        )
    )
}
