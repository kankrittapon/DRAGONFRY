package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.StudioConfigEntity
import com.example.generator.GeneratedFile
import com.example.generator.GeneratedFileCategory
import com.example.ui.components.ForgeCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.ArcaneAmethyst
import com.example.ui.theme.DragonGold
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.FrostCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MetallicBorder
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.ParchmentIvory
import com.example.ui.theme.SlateElevated

@Composable
fun CodeExportScreen(
    config: StudioConfigEntity,
    files: List<GeneratedFile>,
    onUpdateConfig: ((StudioConfigEntity) -> StudioConfigEntity) -> Unit,
    onRequestExportZip: () -> Unit,
    onCopiedNotice: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var selectedCategory by remember { mutableStateOf(GeneratedFileCategory.ALL) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var showMetaDialog by remember { mutableStateOf(false) }

    val filteredFiles = remember(files, selectedCategory) {
        if (selectedCategory == GeneratedFileCategory.ALL) files
        else files.filter { it.category == selectedCategory }
    }
    val activeFile = remember(filteredFiles, selectedPath) {
        filteredFiles.firstOrNull { it.path == selectedPath } ?: filteredFiles.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("code_export_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeader(
                icon = Icons.Filled.Code,
                title = "โค้ด Forge 1.20.1 Mod & Google Script",
                subtitle = "สร้างอัตโนมัติจากกฎประกาศ บอสข้าม Mod และตั้งค่าของคุณ (${files.size} ไฟล์)",
                tint = FrostCyan,
                trailing = {
                    IconButton(onClick = { showMetaDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "ตั้งค่าชื่อ Mod", tint = DragonGold)
                    }
                }
            )
        }

        // Export & Mod Info Card
        item {
            ForgeCard(borderColor = DragonGold.copy(alpha = 0.6f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(config.modName, style = MaterialTheme.typography.titleMedium, color = ParchmentIvory)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill("Forge 1.20.1 (47.2.20)", DragonGold, monospace = true)
                            StatusPill(config.modId, FrostCyan, monospace = true)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestExportZip,
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("export_zip_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.cd_export_zip), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export ทั้งหมด (.ZIP)")
                    }
                    OutlinedButton(
                        onClick = {
                            activeFile?.let { file ->
                                clipboard.setText(AnnotatedString(file.content))
                                onCopiedNotice("คัดลอกไฟล์ ${file.displayName} ลงคลิปบอร์ดแล้ว")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_code_button")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.cd_copy_code), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("คัดลอกไฟล์นี้")
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GeneratedFileCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.labelTh) }
                    )
                }
            }
        }

        // File Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredFiles.forEach { file ->
                    FilterChip(
                        selected = file.path == activeFile?.path,
                        onClick = { selectedPath = file.path },
                        label = { Text(file.displayName) }
                    )
                }
            }
        }

        // Code Viewer
        if (activeFile != null) {
            item {
                ForgeCard(borderColor = MetallicBorder) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(activeFile.path, style = MaterialTheme.typography.labelMedium, color = DragonGold)
                            Spacer(Modifier.height(2.dp))
                            Text(activeFile.descriptionTh, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(activeFile.language.uppercase(), EmeraldSignal, monospace = true)
                    }

                    Surface(
                        color = ObsidianVoid,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MetallicBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val numberedCode = remember(activeFile.content) {
                            activeFile.content.lines().mapIndexed { idx, line ->
                                String.format("%3d │ %s", idx + 1, line)
                            }.joinToString("\n")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                text = numberedCode,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                ),
                                color = ParchmentIvory.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showMetaDialog) {
        var modName by remember { mutableStateOf(config.modName) }
        var modId by remember { mutableStateOf(config.modId) }
        var pkgName by remember { mutableStateOf(config.packageName) }
        var modVer by remember { mutableStateOf(config.modVersion) }

        AlertDialog(
            onDismissRequest = { showMetaDialog = false },
            containerColor = SlateElevated,
            title = { Text("ตั้งค่าข้อมูลโปรเจกต์ Forge Mod") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = modName, onValueChange = { modName = it }, label = { Text("ชื่อ Mod (Display Name)") }, singleLine = true)
                    OutlinedTextField(value = modId, onValueChange = { modId = it }, label = { Text("Mod ID (ตัวพิมพ์เล็ก a-z _)") }, singleLine = true)
                    OutlinedTextField(value = pkgName, onValueChange = { pkgName = it }, label = { Text("Java Package Name") }, singleLine = true)
                    OutlinedTextField(value = modVer, onValueChange = { modVer = it }, label = { Text("เวอร์ชัน Mod") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateConfig {
                        it.copy(
                            modName = modName.ifBlank { it.modName },
                            modId = modId.lowercase().replace(Regex("[^a-z0-9_]"), "_").ifBlank { "dragonfyre_bridge" },
                            packageName = pkgName.ifBlank { "com.dragonfyre.bridge" },
                            modVersion = modVer.ifBlank { "1.0.0" }
                        )
                    }
                    showMetaDialog = false
                }) { Text("บันทึก") }
            },
            dismissButton = {
                TextButton(onClick = { showMetaDialog = false }) { Text("ยกเลิก") }
            }
        )
    }
}
