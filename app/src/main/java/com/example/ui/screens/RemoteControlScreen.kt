package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.RemoteActionLogEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity
import com.example.ui.components.ForgeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.ArcaneAmethyst
import com.example.ui.theme.DragonGold
import com.example.ui.theme.EmberCrimson
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.ParchmentIvory
import com.example.ui.viewmodel.ConnectionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemoteControlScreen(
    config: StudioConfigEntity,
    bosses: List<WorldBossProfileEntity>,
    logs: List<RemoteActionLogEntity>,
    connectionStatus: ConnectionStatus,
    isBusy: Boolean,
    onUpdateConfig: ((StudioConfigEntity) -> StudioConfigEntity) -> Unit,
    onPingGas: () -> Unit,
    onSyncHotReload: () -> Unit,
    onSendBroadcast: (String, String) -> Unit,
    onSpawnBossNow: (String, String) -> Unit,
    onDespawnBosses: () -> Unit,
    onSendCustomCommand: (String) -> Unit,
    onClearLogs: () -> Unit
) {
    var broadcastHeadline by remember { mutableStateOf("") }
    var broadcastSubtitle by remember { mutableStateOf("") }
    var customServerCmd by remember { mutableStateOf("") }
    var selectedBossId by remember { mutableStateOf(bosses.firstOrNull()?.entityId ?: "cataclysm:ignis") }

    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("remote_control_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Google Apps Script Bridge Connection
        item {
            SectionHeader(
                icon = Icons.Filled.CloudSync,
                title = "เชื่อมต่อ Google Apps Script (Polling)",
                subtitle = "Mod ดึงค่าอัตโนมัติไม่ต้องเปิดพอร์ต และไม่ต้อง Restart Server",
                tint = EmeraldSignal
            )
        }

        item {
            ForgeCard(borderColor = EmeraldSignal.copy(alpha = 0.55f)) {
                val (statusLabel, statusColor) = when (connectionStatus) {
                    is ConnectionStatus.Idle -> "รอเชื่อมต่อ (ใส่ URL จากการ Deploy Code.gs)" to FrostCyan
                    is ConnectionStatus.Loading -> "กำลังติดต่อ Google Apps Script..." to DragonGold
                    is ConnectionStatus.Connected -> connectionStatus.detail to EmeraldSignal
                    is ConnectionStatus.Failed -> connectionStatus.reason to EmberCrimson
                }
                StatusPill(statusLabel, statusColor)

                OutlinedTextField(
                    value = config.gasWebAppUrl,
                    onValueChange = { url -> onUpdateConfig { it.copy(gasWebAppUrl = url) } },
                    label = { Text("Google Apps Script Web App URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gas_url_input")
                )
                OutlinedTextField(
                    value = config.gasSecretToken,
                    onValueChange = { token -> onUpdateConfig { it.copy(gasSecretToken = token) } },
                    label = { Text("Secret Token (ตรงกับ SECRET_TOKEN ใน Code.gs)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gas_secret_input")
                )

                Text(
                    "รอบการ Polling ของ Forge Mod: ทุก ${config.pollIntervalSeconds} วินาที",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = config.pollIntervalSeconds.toFloat(),
                    onValueChange = { sec -> onUpdateConfig { it.copy(pollIntervalSeconds = sec.toInt()) } },
                    valueRange = 10f..120f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPingGas,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ping_gas_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("ทดสอบ Ping")
                    }
                    Button(
                        onClick = onSyncHotReload,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("sync_hot_reload_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSignal, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ซิงก์ตั้งค่าสด")
                    }
                }
            }
        }

        // 2. Live Hot-Reload Server Settings (No Restart!)
        item {
            SectionHeader(
                icon = Icons.Filled.Tune,
                title = "ตั้งค่า Server สด (Hot-Reload ไม่ต้องรีสตาร์ท)",
                subtitle = "แก้ค่าด้านล่างแล้วกด \"ซิงก์ตั้งค่าสด\" เซิร์ฟเวอร์จะอัปเดตทันที",
                tint = DragonGold
            )
        }

        item {
            ForgeCard {
                SettingToggleRow("เปิดระบบ World Boss สุ่มเกิดตามเวลา", config.worldBossSystemEnabled) { v ->
                    onUpdateConfig { it.copy(worldBossSystemEnabled = v) }
                }
                SettingToggleRow("เปิดระบบ Auto-Promote ม็อบข้าม Mod เป็นบอส", config.autoPromoteMobSystemEnabled) { v ->
                    onUpdateConfig { it.copy(autoPromoteMobSystemEnabled = v) }
                }
                SettingToggleRow("เปิดระบบป้ายประกาศกลางจอทั้งเซิร์ฟเวอร์", config.announcementsEnabled) { v ->
                    onUpdateConfig { it.copy(announcementsEnabled = v) }
                }
                SettingToggleRow("GameRule: keepInventory (ตายของไม่ตก)", config.keepInventory) { v ->
                    onUpdateConfig { it.copy(keepInventory = v) }
                }
                SettingToggleRow("GameRule: mobGriefing (ม็อบทำลายบล็อก)", config.mobGriefing) { v ->
                    onUpdateConfig { it.copy(mobGriefing = v) }
                }
                SettingToggleRow("GameRule: doDaylightCycle (กลางวัน-กลางคืน)", config.doDaylightCycle) { v ->
                    onUpdateConfig { it.copy(doDaylightCycle = v) }
                }
                SettingToggleRow("เปิด PvP ระหว่างผู้เล่น", config.pvpEnabled) { v ->
                    onUpdateConfig { it.copy(pvpEnabled = v) }
                }

                Text("ระดับความยากเซิร์ฟเวอร์ (Difficulty)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PEACEFUL", "EASY", "NORMAL", "HARD").forEach { diff ->
                        FilterChip(
                            selected = config.serverDifficulty == diff,
                            onClick = { onUpdateConfig { it.copy(serverDifficulty = diff) } },
                            label = { Text(diff) }
                        )
                    }
                }

                Text(
                    String.format(Locale.US, "ตัวคูณเลือดบอสทั้งเซิร์ฟเวอร์: x%.1f • พลังโจมตี: x%.1f", config.globalBossHpScale, config.globalBossAtkScale),
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = config.globalBossHpScale,
                    onValueChange = { v -> onUpdateConfig { it.copy(globalBossHpScale = v) } },
                    valueRange = 0.5f..5.0f
                )
                Slider(
                    value = config.globalBossAtkScale,
                    onValueChange = { v -> onUpdateConfig { it.copy(globalBossAtkScale = v) } },
                    valueRange = 0.5f..4.0f
                )

                Text("ระดับความหายากขั้นต่ำสำหรับประกาศอาวุธ (Apotheosis)", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("RARE", "EPIC", "MYTHIC", "ANCIENT").forEach { rarity ->
                        FilterChip(
                            selected = config.minLootRarity == rarity,
                            onClick = { onUpdateConfig { it.copy(minLootRarity = rarity) } },
                            label = { Text(rarity) }
                        )
                    }
                }
            }
        }

        // 3. Instant Remote Commands
        item {
            SectionHeader(
                icon = Icons.Filled.Bolt,
                title = "สั่งงานเซิร์ฟเวอร์ด่วน (Command Queue)",
                subtitle = "ยิงประกาศกลางจอ เสกบอส หรือรันคำสั่งทันทีผ่าน Google Script",
                tint = EmberCrimson
            )
        }

        item {
            ForgeCard(borderColor = EmberCrimson.copy(alpha = 0.55f)) {
                Text("1. ยิงป้ายประกาศกลางจอทันที", style = MaterialTheme.typography.titleSmall, color = DragonGold)
                OutlinedTextField(
                    value = broadcastHeadline,
                    onValueChange = { broadcastHeadline = it },
                    label = { Text("ข้อความประกาศหลัก") },
                    placeholder = { Text("เช่น กิจกรรมล่า World Boss คูณ 2 เริ่มแล้ว!") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = broadcastSubtitle,
                    onValueChange = { broadcastSubtitle = it },
                    label = { Text("ข้อความรอง (ไม่บังคับ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        onSendBroadcast(broadcastHeadline, broadcastSubtitle)
                        broadcastHeadline = ""
                        broadcastSubtitle = ""
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("send_live_broadcast_button")
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("ส่งป้ายประกาศเข้าเซิร์ฟเวอร์")
                }

                Spacer(Modifier.height(6.dp))
                Text("2. สั่งเสก / เคลียร์ World Boss ทันที", style = MaterialTheme.typography.titleSmall, color = EmberCrimson)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bosses.forEach { b ->
                        FilterChip(
                            selected = selectedBossId == b.entityId,
                            onClick = { selectedBossId = b.entityId },
                            label = { Text(b.displayName, maxLines = 1) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val chosen = bosses.firstOrNull { it.entityId == selectedBossId }
                            onSpawnBossNow(selectedBossId, chosen?.displayName ?: selectedBossId)
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = EmberCrimson),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("spawn_boss_now_button")
                    ) {
                        Icon(Icons.Filled.Whatshot, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("เสกบอสทันที")
                    }
                    OutlinedButton(
                        onClick = onDespawnBosses,
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("เคลียร์บอส")
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("3. รันคำสั่ง Console เซิร์ฟเวอร์", style = MaterialTheme.typography.titleSmall, color = ArcaneAmethyst)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customServerCmd,
                        onValueChange = { customServerCmd = it },
                        label = { Text("เช่น time set day หรือ weather clear") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onSendCustomCommand(customServerCmd)
                            customServerCmd = ""
                        },
                        enabled = !isBusy
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "ส่งคำสั่ง", tint = DragonGold)
                    }
                }
            }
        }

        // 4. Recent Remote Action Logs
        item {
            SectionHeader(
                icon = Icons.Filled.History,
                title = "ประวัติการซิงก์และคำสั่ง (${logs.size})",
                tint = FrostCyan,
                trailing = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "ล้างประวัติ", tint = EmberCrimson)
                        }
                    }
                }
            )
        }

        if (logs.isEmpty()) {
            item {
                ForgeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = FrostCyan)
                        Text(
                            "ยังไม่มีประวัติการส่งคำสั่ง — นำ Web App URL จาก Code.gs มาวางด้านบนแล้วกดทดสอบเชื่อมต่อ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id }) { log ->
                val col = when (log.status) {
                    "SUCCESS" -> EmeraldSignal
                    "QUEUED" -> DragonGold
                    else -> EmberCrimson
                }
                ForgeCard(borderColor = col.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill("${log.actionType} • ${log.status}", col, monospace = true)
                        Text(
                            timeFmt.format(Date(log.timestampMs)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(log.summary, style = MaterialTheme.typography.titleSmall, color = ParchmentIvory)
                    Text(
                        log.responseDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ParchmentIvory,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
