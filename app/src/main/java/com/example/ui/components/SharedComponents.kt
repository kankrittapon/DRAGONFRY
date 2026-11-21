package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArcaneAmethyst
import com.example.ui.theme.DragonGold
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetallicBorder
import com.example.ui.theme.SlateElevated
import com.example.ui.theme.SlateIron

fun parseHexColor(hex: String, fallback: Color = DragonGold): Color {
    return runCatching {
        val clean = hex.trim().removePrefix("#")
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MetallicBorder,
    containerColor: Color = SlateIron,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = DragonGold,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(listOf(tint.copy(alpha = 0.28f), tint.copy(alpha = 0.08f))),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    monospace: Boolean = false
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = if (monospace) {
                MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily)
            } else {
                MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
            },
            color = color
        )
    }
}

fun triggerTypeLabelTh(type: String): String = when (type) {
    "RARITY_LOOT" -> "ดรอปไอเทมหายาก"
    "MOB_KILL" -> "สังหารม็อบพิเศษ"
    "BOSS_SPAWN" -> "World Boss เกิด"
    "BOSS_DEFEATED" -> "ปราบ World Boss"
    "MILESTONE" -> "ความสำเร็จ/เลเวล"
    "REMOTE_CUSTOM" -> "ประกาศจาก GAS"
    else -> type
}

fun bossModeLabelTh(mode: String): String = when (mode) {
    "SCHEDULED_WORLD_BOSS" -> "สุ่มเกิดบนแผนที่"
    "AUTO_PROMOTE_NATURAL" -> "อัปเกรดม็อบธรรมชาติ"
    "BOTH" -> "ทั้ง 2 โหมด (สุ่มเกิด+อัปเกรด)"
    else -> mode
}

fun bossModeColor(mode: String): Color = when (mode) {
    "SCHEDULED_WORLD_BOSS" -> EmberCrimson
    "AUTO_PROMOTE_NATURAL" -> ArcaneAmethyst
    "BOTH" -> DragonGold
    else -> FrostCyan
}

data class UniversalModMobPreset(
    val displayName: String,
    val entityId: String,
    val modPackTag: String,
    val defaultMode: String,
    val defaultChance: Int,
    val hpMult: Float,
    val atkMult: Float,
    val affixes: String
)

val UNIVERSAL_MOD_PRESETS = listOf(
    UniversalModMobPreset("Ignis (Cataclysm)", "cataclysm:ignis", "Cisco Dragonfyre", "BOTH", 100, 3.5f, 2.0f, "FIRE_NOVA,ENRAGED"),
    UniversalModMobPreset("Ender Guardian", "cataclysm:ender_guardian", "Cisco Ultimate", "SCHEDULED_WORLD_BOSS", 100, 3.8f, 2.0f, "IRON_SKIN,ENRAGED"),
    UniversalModMobPreset("Ancient Remnant", "cataclysm:ancient_remnant", "Cataclysm", "SCHEDULED_WORLD_BOSS", 100, 4.2f, 2.1f, "FIRE_NOVA,IRON_SKIN"),
    UniversalModMobPreset("Ice Dragon", "iceandfire:ice_dragon", "Ice & Fire", "SCHEDULED_WORLD_BOSS", 100, 2.8f, 1.8f, "LIGHTNING_CALLER,IRON_SKIN"),
    UniversalModMobPreset("Twilight Naga", "twilightforest:naga", "Twilight Forest", "AUTO_PROMOTE_NATURAL", 100, 3.0f, 1.8f, "SPEED_SURGE,ENRAGED"),
    UniversalModMobPreset("Void Worm", "alexsmobs:void_worm", "Alex's Mobs", "BOTH", 100, 3.2f, 1.9f, "LIFE_STEAL,ENRAGED"),
    UniversalModMobPreset("Lord Pumpkinhead", "born_in_chaos_v1:lord_pumpkinhead", "Born in Chaos", "BOTH", 100, 3.0f, 1.7f, "FIRE_NOVA,LIFE_STEAL"),
    UniversalModMobPreset("Void Blossom", "bosses_of_mass_destruction:void_blossom", "Bosses of MD", "SCHEDULED_WORLD_BOSS", 100, 3.0f, 1.6f, "LIFE_STEAL,IRON_SKIN"),
    UniversalModMobPreset("Apex Skeleton King", "minecraft:skeleton", "Universal Promoter", "AUTO_PROMOTE_NATURAL", 5, 14.0f, 3.0f, "SPEED_SURGE,LIGHTNING_CALLER"),
    UniversalModMobPreset("Abyssal Warden", "minecraft:warden", "Vanilla → Boss", "BOTH", 50, 3.0f, 1.8f, "IRON_SKIN,ENRAGED")
)

val ALL_BOSS_AFFIXES = listOf(
    "FIRE_NOVA" to "ไฟนรกเผาผลาญ",
    "LIFE_STEAL" to "ดูดเลือด 25%",
    "IRON_SKIN" to "เกราะเหล็กต้านดาเมจ",
    "LIGHTNING_CALLER" to "สายฟ้าฟาด 20%",
    "SPEED_SURGE" to "ความเร็วคลั่ง",
    "ENRAGED" to "คลั่งเฟสสุดท้าย"
)

fun bossThemeColors(theme: String): Pair<Color, Color> = when (theme) {
    "CRIMSON_OBSIDIAN" -> EmberCrimson to Color(0xFFB91C1C)
    "ARCANE_RUNE" -> ArcaneAmethyst to Color(0xFF7E22CE)
    "EMERALD_VOID" -> EmeraldSignal to Color(0xFF047857)
    else -> DragonGold to Color(0xFFDC2626)
}
