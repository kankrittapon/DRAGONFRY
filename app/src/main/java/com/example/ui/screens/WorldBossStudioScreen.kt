package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity
import com.example.ui.components.ForgeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.UNIVERSAL_MOD_PRESETS
import com.example.ui.components.bossModeColor
import com.example.ui.components.bossModeLabelTh
import com.example.ui.components.bossThemeColors
import com.example.ui.theme.ArcaneAmethyst
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.DragonGold
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentIvory
import java.util.Locale

@Composable
fun WorldBossStudioScreen(
    config: StudioConfigEntity,
    bosses: List<WorldBossProfileEntity>,
    onUpdateConfig: ((StudioConfigEntity) -> StudioConfigEntity) -> Unit,
    onSaveBoss: (WorldBossProfileEntity) -> Unit,
    onToggleBoss: (WorldBossProfileEntity, Boolean) -> Unit,
    onDeleteBoss: (WorldBossProfileEntity) -> Unit
) {
    var previewBossId by remember { mutableStateOf<Long?>(null) }
    var simulatedHpRatio by remember { mutableFloatStateOf(0.76f) }
    var editingBoss by remember { mutableStateOf<WorldBossProfileEntity?>(null) }
    var isNewBoss by remember { mutableStateOf(false) }

    val activeBoss = remember(bosses, previewBossId) {
        bosses.firstOrNull { it.id == previewBossId } ?: bosses.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("world_boss_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Filled.Whatshot,
                title = "หลอดเลือดบอส Custom & จุดแดง Xaero",
                subtitle = "จำลองแถบเลือด Damage Trail พร้อมหมุดแดงบนแผนที่",
                tint = EmberCrimson
            )
        }

        // Interactive Custom Boss Bar + Xaero Minimap Red Pin Preview
        item {
            LiveBossBarAndXaeroPreview(
                boss = activeBoss,
                config = config,
                hpRatio = simulatedHpRatio,
                onDealDamage = { simulatedHpRatio = (simulatedHpRatio - 0.16f).coerceAtLeast(0.08f) },
                onResetHp = { simulatedHpRatio = 1.0f }
            )
        }

        // Boss Bar Style Customizer Card
        item {
            ForgeCard {
                Text("ปรับแต่งสไตล์หลอดเลือดบอส (Client HUD)", style = MaterialTheme.typography.titleSmall, color = DragonGold)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "DRAGON_GOLD" to "Dragon Gold",
                        "CRIMSON_OBSIDIAN" to "Crimson Void",
                        "ARCANE_RUNE" to "Arcane Rune",
                        "EMERALD_VOID" to "Emerald Abyss"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = config.bossBarTheme == key,
                            onClick = { onUpdateConfig { it.copy(bossBarTheme = key) } },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("แสดงตัวเลข HP จริง & ขีดแบ่งเฟส (75/50/25%)", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = config.showExactHp && config.showPhaseNotches,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(showExactHp = v, showPhaseNotches = v) } }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("แถบ Damage Lag Trail ไหลตามตอนโดนตี", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = config.enableDamageLagTrail,
                        onCheckedChange = { v -> onUpdateConfig { it.copy(enableDamageLagTrail = v) } }
                    )
                }
            }
        }

        // Universal Multi-Mod Mob-to-Boss Registry Header
        item {
            SectionHeader(
                icon = Icons.Filled.Public,
                title = "ระบบจัดการม็อบข้าม Mod (${bosses.size} ตัว)",
                subtitle = "เพิ่ม Entity ID ของ Mod ใดก็ได้ หรือตั้งโอกาสอัปเกรดม็อบทั่วไปเป็น Boss",
                tint = DragonGold,
                trailing = {
                    Button(
                        onClick = {
                            isNewBoss = true
                            editingBoss = WorldBossProfileEntity(
                                displayName = "Custom Mod Boss",
                                entityId = "modid:custom_mob",
                                modPackTag = "Custom Mod",
                                bossMode = "BOTH",
                                promotionChancePercent = 25
                            )
                        },
                        modifier = Modifier.testTag("add_boss_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_item), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("เพิ่มม็อบ")
                    }
                }
            )
        }

        // Quick-Add Universal Mod Presets Row (Cisco + Other Popular Mods + Vanilla Promoter)
        item {
            ForgeCard(borderColor = ArcaneAmethyst.copy(alpha = 0.55f)) {
                Text(
                    "เพิ่มด่วนจาก Mod ยอดนิยม (รองรับทั้ง Cisco และ Mod อื่นๆ):",
                    style = MaterialTheme.typography.labelLarge,
                    color = ArcaneAmethyst
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UNIVERSAL_MOD_PRESETS.forEach { preset ->
                        AssistChip(
                            onClick = {
                                onSaveBoss(
                                    WorldBossProfileEntity(
                                        displayName = preset.displayName,
                                        entityId = preset.entityId,
                                        modPackTag = preset.modPackTag,
                                        bossMode = preset.defaultMode,
                                        promotionChancePercent = preset.defaultChance,
                                        hpMultiplier = preset.hpMult,
                                        attackMultiplier = preset.atkMult,
                                        affixTraits = preset.affixes
                                    )
                                )
                            },
                            label = { Text("+ ${preset.displayName} (${preset.modPackTag})") }
                        )
                    }
                }
            }
        }

        // Registered Boss Cards
        items(bosses, key = { it.id }) { boss ->
            BossProfileCard(
                boss = boss,
                isPreviewing = boss.id == activeBoss?.id,
                onSelectPreview = {
                    previewBossId = boss.id
                    simulatedHpRatio = 0.78f
                },
                onToggle = { onToggleBoss(boss, it) },
                onEdit = {
                    isNewBoss = false
                    editingBoss = boss
                },
                onDelete = { onDeleteBoss(boss) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    editingBoss?.let { target ->
        BossEditorDialog(
            initial = target,
            isNew = isNewBoss,
            onDismiss = { editingBoss = null },
            onSave = { updated ->
                onSaveBoss(updated)
                editingBoss = null
            }
        )
    }
}

@Composable
private fun LiveBossBarAndXaeroPreview(
    boss: WorldBossProfileEntity?,
    config: StudioConfigEntity,
    hpRatio: Float,
    onDealDamage: () -> Unit,
    onResetHp: () -> Unit
) {
    val (borderColor, baseFill) = bossThemeColors(config.bossBarTheme)
    val fillColor = if (hpRatio < 0.25f) EmberCrimson else baseFill
    val animatedLag by animateFloatAsState(
        targetValue = hpRatio,
        animationSpec = tween(durationMillis = 750),
        label = "damage_lag"
    )
    val pulseTransition = rememberInfiniteTransition(label = "xaero_pin")
    val pulseRadius by pulseTransition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "pulse_r"
    )

    val maxHpDisplay = ((boss?.hpMultiplier ?: 3.5f) * 45000).toInt()
    val currentHpDisplay = (maxHpDisplay * hpRatio).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, borderColor.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_rpg_hud_backdrop),
            contentDescription = stringResource(R.string.cd_rpg_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xAA0A0910))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Custom Boss Bar HUD
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = boss?.displayName ?: "Ignis, Lord of Dragonfyre",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = CinzelFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ParchmentIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((config.bossBarHeightDp + 6).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianVoid)
                        .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                ) {
                    if (config.enableDamageLagTrail) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedLag)
                                .background(Color(0xFFFDE68A))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hpRatio)
                            .background(
                                Brush.horizontalGradient(listOf(fillColor.copy(alpha = 0.85f), fillColor))
                            )
                    )
                    if (config.showPhaseNotches) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            repeat(3) {
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .background(ObsidianVoid.copy(alpha = 0.8f))
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (config.showExactHp) {
                        Text(
                            text = String.format(Locale.US, "%,d / %,d HP", currentHpDisplay, maxHpDisplay),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⏳ 18:42  •  📍 (420, 72, -815) 340m  •  ${boss?.modPackTag ?: "Cisco Dragonfyre"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DragonGold
                )
            }

            // Bottom Row: Xaero's Red Boss Pin Radar + Interactive Hit Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Xaero's Minimap Red Boss Pin Radar
                Surface(
                    color = ObsidianVoid.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, EmberCrimson.copy(alpha = 0.65f)),
                    modifier = Modifier.size(86.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(Color(0x33FFFFFF), radius = size.minDimension * 0.40f, center = center, style = Stroke(1f))
                            drawCircle(Color(0x22FFFFFF), radius = size.minDimension * 0.22f, center = center, style = Stroke(1f))
                            drawLine(Color(0x22FFFFFF), Offset(center.x, 4f), Offset(center.x, size.height - 4f), 1f)
                            drawLine(Color(0x22FFFFFF), Offset(4f, center.y), Offset(size.width - 4f, center.y), 1f)
                            // Player dot in center
                            drawCircle(FrostCyan, radius = 3.5f, center = center)
                            // Pulsing Xaero Red Boss Marker
                            val bossPos = Offset(size.width * 0.72f, size.height * 0.30f)
                            drawCircle(EmberCrimson.copy(alpha = 0.32f), radius = pulseRadius, center = bossPos)
                            drawCircle(EmberCrimson, radius = 5.5f, center = bossPos)
                            drawCircle(Color.White, radius = 2f, center = bossPos)
                        }
                        Text(
                            text = "Xaero Pin",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmberCrimson,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 3.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "จุดแดงอัตโนมัติบน Xaero's World Map",
                        style = MaterialTheme.typography.titleSmall,
                        color = ParchmentIvory
                    )
                    Text(
                        text = "สร้างจุดแดง ${config.xaeroWaypointPrefix} ทันทีที่บอสเกิด และลบอัตโนมัติเมื่อปราบได้",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onDealDamage,
                            modifier = Modifier.testTag("simulate_boss_hit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmberCrimson),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ตี -16%")
                        }
                        OutlinedButton(
                            onClick = onResetHp,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("รีเซ็ต")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BossProfileCard(
    boss: WorldBossProfileEntity,
    isPreviewing: Boolean,
    onSelectPreview: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val modeCol = bossModeColor(boss.bossMode)
    ForgeCard(
        borderColor = if (isPreviewing) DragonGold else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .clickable(onClick = onSelectPreview)
            .testTag("boss_card_${boss.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = boss.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = ParchmentIvory,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(boss.entityId, FrostCyan, monospace = true)
                    StatusPill(boss.modPackTag, DragonGold)
                    StatusPill(
                        if (boss.bossMode == "SCHEDULED_WORLD_BOSS") bossModeLabelTh(boss.bossMode)
                        else "${bossModeLabelTh(boss.bossMode)} (${boss.promotionChancePercent}%)",
                        modeCol
                    )
                }
            }
            Switch(checked = boss.enabled, onCheckedChange = onToggle)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format(Locale.US, "HP x%.1f  •  ATK x%.1f  •  เวลาตี %dน.", boss.hpMultiplier, boss.attackMultiplier, boss.fightDurationMinutes),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                color = ParchmentIvory.copy(alpha = 0.85f)
            )
            if (boss.xaeroRedMarker) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = EmberCrimson, modifier = Modifier.size(14.dp))
                    Text("Xaero", style = MaterialTheme.typography.labelSmall, color = EmberCrimson)
                }
            }
        }

        if (boss.affixTraits.isNotBlank()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                boss.affixTraits.split(",").filter { it.isNotBlank() }.forEach { affix ->
                    StatusPill(affix.trim(), EmeraldSignal, monospace = true)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_item), tint = DragonGold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_item), tint = EmberCrimson)
            }
        }
    }
}
