package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.WorldBossProfileEntity
import com.example.ui.components.ALL_BOSS_AFFIXES
import com.example.ui.components.bossModeLabelTh
import com.example.ui.theme.SlateElevated
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BossEditorDialog(
    initial: WorldBossProfileEntity,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (WorldBossProfileEntity) -> Unit
) {
    var displayName by remember { mutableStateOf(initial.displayName) }
    var entityId by remember { mutableStateOf(initial.entityId) }
    var modPackTag by remember { mutableStateOf(initial.modPackTag) }
    var bossMode by remember { mutableStateOf(initial.bossMode) }
    var promotionChance by remember { mutableIntStateOf(initial.promotionChancePercent) }
    var hpMult by remember { mutableFloatStateOf(initial.hpMultiplier) }
    var atkMult by remember { mutableFloatStateOf(initial.attackMultiplier) }
    var armorBonus by remember { mutableFloatStateOf(initial.armorBonus) }
    var spawnInterval by remember { mutableIntStateOf(initial.spawnIntervalMinutes) }
    var fightDuration by remember { mutableIntStateOf(initial.fightDurationMinutes) }
    var maxRadius by remember { mutableIntStateOf(initial.maxSpawnRadius) }
    var dimensionId by remember { mutableStateOf(initial.dimensionId) }
    var glowingAura by remember { mutableStateOf(initial.glowingAura) }
    var xaeroRedMarker by remember { mutableStateOf(initial.xaeroRedMarker) }
    var rewardCommand by remember { mutableStateOf(initial.rewardCommand) }
    var selectedAffixes by remember {
        mutableStateOf(
            initial.affixTraits.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }

    val modes = listOf("BOTH", "SCHEDULED_WORLD_BOSS", "AUTO_PROMOTE_NATURAL")
    val dims = listOf("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end", "*")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateElevated,
        title = { Text(if (isNew) "เพิ่ม / อัปเกรดม็อบเป็น Boss" else "ตั้งค่า World Boss") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("ชื่อบอสที่แสดงบนหลอดเลือดและแผนที่") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = entityId,
                    onValueChange = { entityId = it },
                    label = { Text("Entity ID (ใช้ได้กับทุก Mod เช่น modid:mob_name)") },
                    placeholder = { Text("เช่น cataclysm:ignis หรือ twilightforest:naga") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modPackTag,
                    onValueChange = { modPackTag = it },
                    label = { Text("ชื่อ Mod / หมวดหมู่ (เช่น Cisco, Twilight Forest)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("โหมดการจัดการบอส (Universal Boss Mode)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    modes.forEach { m ->
                        FilterChip(
                            selected = bossMode == m,
                            onClick = { bossMode = m },
                            label = { Text(bossModeLabelTh(m)) }
                        )
                    }
                }

                if (bossMode == "AUTO_PROMOTE_NATURAL" || bossMode == "BOTH") {
                    Text(
                        "โอกาสอัปเกรดม็อบที่เกิดธรรมชาติเป็น Boss: $promotionChance%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = promotionChance.toFloat(),
                        onValueChange = { promotionChance = it.toInt() },
                        valueRange = 1f..100f
                    )
                }

                Text(
                    String.format(Locale.US, "ตัวคูณเลือด (HP): x%.1f • พลังโจมตี (ATK): x%.1f • เกราะ +%.0f", hpMult, atkMult, armorBonus),
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(value = hpMult, onValueChange = { hpMult = it }, valueRange = 1.0f..20.0f)
                Slider(value = atkMult, onValueChange = { atkMult = it }, valueRange = 1.0f..8.0f)
                Slider(value = armorBonus, onValueChange = { armorBonus = it }, valueRange = 0f..30f)

                Text(
                    "สุ่มเกิดทุก $spawnInterval นาที • เวลาตี $fightDuration นาที • รัศมีสูงสุด $maxRadius บล็อก",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(value = spawnInterval.toFloat(), onValueChange = { spawnInterval = it.toInt() }, valueRange = 10f..180f)
                Slider(value = fightDuration.toFloat(), onValueChange = { fightDuration = it.toInt() }, valueRange = 5f..60f)
                Slider(value = maxRadius.toFloat(), onValueChange = { maxRadius = it.toInt() }, valueRange = 200f..3000f)

                Text("มิติที่อนุญาต (Dimension)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dims.forEach { d ->
                        FilterChip(
                            selected = dimensionId == d,
                            onClick = { dimensionId = d },
                            label = { Text(if (d == "*") "ทุกมิติ (*)" else d.removePrefix("minecraft:")) }
                        )
                    }
                }

                Text("ความสามารถพิเศษ (Boss Affix Traits)", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ALL_BOSS_AFFIXES.forEach { (code, label) ->
                        val active = code in selectedAffixes
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedAffixes = if (active) selectedAffixes - code else selectedAffixes + code
                            },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ออร่าเรืองแสง (Glowing Aura)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = glowingAura, onCheckedChange = { glowingAura = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ปักจุดแดงบน Xaero's World Map", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = xaeroRedMarker, onCheckedChange = { xaeroRedMarker = it })
                }

                OutlinedTextField(
                    value = rewardCommand,
                    onValueChange = { rewardCommand = it },
                    label = { Text("คำสั่งรางวัลเมื่อปราบสำเร็จ (ใช้ @p แทนผู้สังหาร)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEntity = entityId.trim().ifBlank { "minecraft:warden" }
                    onSave(
                        initial.copy(
                            displayName = displayName.trim().ifBlank { cleanEntity },
                            entityId = cleanEntity,
                            modPackTag = modPackTag.trim().ifBlank { cleanEntity.substringBefore(":", "Custom Mod") },
                            bossMode = bossMode,
                            promotionChancePercent = promotionChance,
                            hpMultiplier = hpMult,
                            attackMultiplier = atkMult,
                            armorBonus = armorBonus,
                            spawnIntervalMinutes = spawnInterval,
                            fightDurationMinutes = fightDuration,
                            maxSpawnRadius = maxRadius,
                            dimensionId = dimensionId,
                            glowingAura = glowingAura,
                            xaeroRedMarker = xaeroRedMarker,
                            affixTraits = selectedAffixes.joinToString(","),
                            rewardCommand = rewardCommand.trim()
                        )
                    )
                },
                modifier = Modifier.testTag("save_boss_confirm")
            ) { Text("บันทึกบอส") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ยกเลิก") }
        }
    )
}
