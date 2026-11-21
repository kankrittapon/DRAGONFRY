package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AnnouncementRuleEntity
import com.example.ui.components.ForgeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.parseHexColor
import com.example.ui.components.triggerTypeLabelTh
import com.example.ui.theme.ArcaneAmethyst
import com.example.ui.theme.DragonGold
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentIvory
import com.example.ui.theme.SlateElevated
import kotlinx.coroutines.delay

@Composable
fun AnnouncementStudioScreen(
    rules: List<AnnouncementRuleEntity>,
    onSaveRule: (AnnouncementRuleEntity) -> Unit,
    onToggleRule: (AnnouncementRuleEntity, Boolean) -> Unit,
    onDeleteRule: (AnnouncementRuleEntity) -> Unit
) {
    var selectedRuleId by remember { mutableStateOf<Long?>(null) }
    var bannerVisible by remember { mutableStateOf(true) }
    var replayCounter by remember { mutableIntStateOf(0) }
    var editingRule by remember { mutableStateOf<AnnouncementRuleEntity?>(null) }
    var isNewRuleDialog by remember { mutableStateOf(false) }

    val activeRule = remember(rules, selectedRuleId) {
        rules.firstOrNull { it.id == selectedRuleId } ?: rules.firstOrNull()
    }

    LaunchedEffect(replayCounter, activeRule?.id) {
        bannerVisible = false
        delay(120)
        bannerVisible = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("announcement_studio_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Filled.Campaign,
                title = "พรีวิวป้ายประกาศกลางจอ (In-Game HUD)",
                subtitle = "จำลองการแสดงผลจริงบน Minecraft Forge 1.20.1",
                tint = DragonGold,
                trailing = {
                    Button(
                        onClick = { replayCounter++ },
                        modifier = Modifier.testTag("test_banner_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("ทดสอบ")
                    }
                }
            )
        }

        // Live In-Game Viewport Preview
        item {
            LiveHudAnnouncementViewport(
                rule = activeRule,
                visible = bannerVisible
            )
        }

        // Quick selector row for previewing rules
        if (rules.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rules.forEach { rule ->
                        val isSelected = rule.id == activeRule?.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedRuleId = rule.id
                                replayCounter++
                            },
                            label = { Text(triggerTypeLabelTh(rule.triggerType), maxLines = 1) }
                        )
                    }
                }
            }
        }

        // Rule list header
        item {
            SectionHeader(
                icon = Icons.Filled.AutoAwesome,
                title = "โครงสร้างกฎการประกาศ (${rules.size})",
                subtitle = "รองรับ Apotheosis Rarity, บอสข้าม Mod และคำสั่งจาก Google Script",
                tint = ArcaneAmethyst,
                trailing = {
                    Button(
                        onClick = {
                            isNewRuleDialog = true
                            editingRule = AnnouncementRuleEntity(
                                title = "ประกาศใหม่",
                                triggerType = "MOB_KILL",
                                matchFilter = "cataclysm:*,twilightforest:*",
                                messageTemplate = "⚔ {player} สังหาร {mob} สำเร็จ!",
                                subtitleTemplate = "ได้รับรางวัลพิเศษจากระบบเซิร์ฟเวอร์",
                                accentHex = "#F59E0B",
                                animationStyle = "RUNE_GLOW",
                                soundId = "minecraft:ui.toast.challenge_complete"
                            )
                        },
                        modifier = Modifier.testTag("add_rule_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcaneAmethyst),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_item), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("เพิ่มกฎ")
                    }
                }
            )
        }

        items(rules, key = { it.id }) { rule ->
            AnnouncementRuleCard(
                rule = rule,
                isPreviewing = rule.id == activeRule?.id,
                onPreviewClick = {
                    selectedRuleId = rule.id
                    replayCounter++
                },
                onToggle = { onToggleRule(rule, it) },
                onEdit = {
                    isNewRuleDialog = false
                    editingRule = rule
                },
                onDelete = { onDeleteRule(rule) }
            )
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    editingRule?.let { target ->
        RuleEditorDialog(
            initial = target,
            isNew = isNewRuleDialog,
            onDismiss = { editingRule = null },
            onSave = { updated ->
                onSaveRule(updated)
                editingRule = null
                replayCounter++
            }
        )
    }
}

@Composable
private fun LiveHudAnnouncementViewport(
    rule: AnnouncementRuleEntity?,
    visible: Boolean
) {
    val accent = parseHexColor(rule?.accentHex ?: "#A855F7", ArcaneAmethyst)
    val previewHeadline = remember(rule) {
        (rule?.messageTemplate ?: "⚔ KanDragon ได้รับไอเทมระดับ MYTHIC: Sylvi's Blade")
            .replace("{player}", "KanDragon")
            .replace("{rarity}", "MYTHIC")
            .replace("{item}", "Dragonfyre Greatsword")
            .replace("{mob}", "Ignis, Lord of Dragonfyre")
            .replace("{x}", "420")
            .replace("{y}", "72")
            .replace("{z}", "-815")
            .replace("{time}", "20")
            .replace("{message}", "กิจกรรม World Boss คูณ 2 เริ่มขึ้นแล้ว!")
    }
    val previewSubtitle = remember(rule) {
        (rule?.subtitleTemplate ?: "ตรวจพบพลัง Affix หายากใน Cisco RPG")
            .replace("{time}", "20")
            .replace("{x}", "420")
            .replace("{y}", "72")
            .replace("{z}", "-815")
    }

    val glowAlpha by animateFloatAsState(
        targetValue = if (visible) 0.9f else 0.1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "banner_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(206.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_rpg_hud_backdrop),
            contentDescription = stringResource(R.string.cd_rpg_backdrop),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x8809080E), Color(0x5509080E), Color(0xBB09080E))
                    )
                )
        )

        // Subtle mock hotbar indicator at bottom so user feels true Minecraft HUD scale
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(9) { idx ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(0x8814121B), RoundedCornerShape(3.dp))
                        .border(
                            width = if (idx == 0) 1.5.dp else 0.7.dp,
                            color = if (idx == 0) DragonGold else Color(0x66FFFFFF),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }

        // Animated Center-Screen RPG Banner
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 28.dp, start = 14.dp, end = 14.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = when (rule?.animationStyle) {
                    "SCALE_POP" -> scaleIn(spring(dampingRatio = 0.65f)) + fadeIn()
                    "RUNE_GLOW" -> fadeIn() + scaleIn(initialScale = 0.92f)
                    else -> slideInVertically(initialOffsetY = { -it }) + fadeIn()
                },
                exit = fadeOut()
            ) {
                Surface(
                    color = ObsidianVoid.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = glowAlpha)),
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.22f),
                                        Color.Transparent,
                                        accent.copy(alpha = 0.22f)
                                    )
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color.Transparent, accent, Color.Transparent)
                                    )
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = previewHeadline,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ParchmentIvory,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (previewSubtitle.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = previewSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = accent.copy(alpha = 0.95f),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Top-left live HUD indicator chip
        Surface(
            color = ObsidianVoid.copy(alpha = 0.72f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                Text(
                    text = "${rule?.animationStyle ?: "RUNE_GLOW"} • ${rule?.durationSeconds ?: 5}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = ParchmentIvory
                )
            }
        }
    }
}

@Composable
private fun AnnouncementRuleCard(
    rule: AnnouncementRuleEntity,
    isPreviewing: Boolean,
    onPreviewClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseHexColor(rule.accentHex)
    ForgeCard(
        borderColor = if (isPreviewing) accent else MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .clickable(onClick = onPreviewClick)
            .testTag("rule_card_${rule.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = ParchmentIvory,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusPill(triggerTypeLabelTh(rule.triggerType), accent)
                    StatusPill(rule.animationStyle, FrostCyan, monospace = true)
                }
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = onToggle
            )
        }

        Text(
            text = rule.messageTemplate,
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentIvory.copy(alpha = 0.92f)
        )
        Text(
            text = "Filter: ${rule.matchFilter}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onPreviewClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("พรีวิว")
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.cd_edit_item), tint = DragonGold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_item), tint = EmberCrimson)
            }
        }
    }
}

@Composable
private fun RuleEditorDialog(
    initial: AnnouncementRuleEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (AnnouncementRuleEntity) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var triggerType by remember { mutableStateOf(initial.triggerType) }
    var matchFilter by remember { mutableStateOf(initial.matchFilter) }
    var messageTemplate by remember { mutableStateOf(initial.messageTemplate) }
    var subtitleTemplate by remember { mutableStateOf(initial.subtitleTemplate) }
    var accentHex by remember { mutableStateOf(initial.accentHex) }
    var animStyle by remember { mutableStateOf(initial.animationStyle) }
    var soundId by remember { mutableStateOf(initial.soundId) }
    var duration by remember { mutableIntStateOf(initial.durationSeconds) }

    val triggerOptions = listOf("RARITY_LOOT", "MOB_KILL", "BOSS_SPAWN", "BOSS_DEFEATED", "MILESTONE", "REMOTE_CUSTOM")
    val animOptions = listOf("RUNE_GLOW", "SLIDE_DOWN", "SCALE_POP")
    val colorSwatches = listOf("#A855F7", "#F59E0B", "#EF4444", "#38BDF8", "#10B981")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateElevated,
        title = { Text(if (isNew) "เพิ่มกฎป้ายประกาศใหม่" else "แก้ไขกฎป้ายประกาศ") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ชื่อกฎประกาศ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("ประเภทเหตุการณ์ (Trigger)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    triggerOptions.forEach { t ->
                        FilterChip(
                            selected = triggerType == t,
                            onClick = { triggerType = t },
                            label = { Text(triggerTypeLabelTh(t)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = matchFilter,
                    onValueChange = { matchFilter = it },
                    label = { Text("เงื่อนไขจับคู่ (Rarity / modid:*)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = messageTemplate,
                    onValueChange = { messageTemplate = it },
                    label = { Text("ข้อความหลัก ({player} {item} {rarity} {mob})") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitleTemplate,
                    onValueChange = { subtitleTemplate = it },
                    label = { Text("ข้อความรอง (Subtitle)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("สีขอบป้ายประกาศ & แอนิเมชัน", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colorSwatches.forEach { hex ->
                        val col = parseHexColor(hex)
                        val selected = accentHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { accentHex = hex }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    animOptions.forEach { anim ->
                        FilterChip(
                            selected = animStyle == anim,
                            onClick = { animStyle = anim },
                            label = { Text(anim) }
                        )
                    }
                }
                OutlinedTextField(
                    value = soundId,
                    onValueChange = { soundId = it },
                    label = { Text("Sound Event ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("ระยะเวลาแสดงผล: $duration วินาที", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = duration.toFloat(),
                    onValueChange = { duration = it.toInt() },
                    valueRange = 2f..12f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.ifBlank { "กฎประกาศ" },
                            triggerType = triggerType,
                            matchFilter = matchFilter.ifBlank { "*" },
                            messageTemplate = messageTemplate.ifBlank { "⚔ {player} ทำภารกิจสำเร็จ!" },
                            subtitleTemplate = subtitleTemplate,
                            accentHex = accentHex,
                            animationStyle = animStyle,
                            soundId = soundId.ifBlank { "minecraft:ui.toast.challenge_complete" },
                            durationSeconds = duration
                        )
                    )
                },
                modifier = Modifier.testTag("save_rule_confirm")
            ) { Text("บันทึก") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ยกเลิก") }
        }
    )
}
