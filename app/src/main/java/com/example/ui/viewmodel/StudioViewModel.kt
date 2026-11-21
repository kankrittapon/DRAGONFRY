package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AnnouncementRuleEntity
import com.example.data.local.AppDatabase
import com.example.data.local.RemoteActionLogEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.StudioRepository
import com.example.data.local.WorldBossProfileEntity
import com.example.data.remote.GasRepository
import com.example.data.remote.GasResult
import com.example.generator.ForgeModCodeGenerator
import com.example.generator.GeneratedFile
import com.example.generator.ZipExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class ConnectionStatus {
    data object Idle : ConnectionStatus()
    data object Loading : ConnectionStatus()
    data class Connected(val detail: String) : ConnectionStatus()
    data class Failed(val reason: String) : ConnectionStatus()
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudioRepository(AppDatabase.getInstance(application))
    private val gasRepository = GasRepository()

    val rules: StateFlow<List<AnnouncementRuleEntity>> = repository.rules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bosses: StateFlow<List<WorldBossProfileEntity>> = repository.bosses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val config: StateFlow<StudioConfigEntity> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StudioConfigEntity())

    val logs: StateFlow<List<RemoteActionLogEntity>> = repository.logs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val generatedFiles: StateFlow<List<GeneratedFile>> = combine(config, rules, bosses) { cfg, r, b ->
        ForgeModCodeGenerator.generateAll(cfg, r, b)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Idle)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // --- Announcement Rules CRUD ---
    fun saveRule(rule: AnnouncementRuleEntity) {
        viewModelScope.launch {
            repository.saveRule(rule)
            _snackbarMessage.value = "บันทึกกฎประกาศ \"${rule.title}\" เรียบร้อยแล้ว"
        }
    }

    fun toggleRule(rule: AnnouncementRuleEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.saveRule(rule.copy(enabled = enabled))
        }
    }

    fun deleteRule(rule: AnnouncementRuleEntity) {
        viewModelScope.launch {
            repository.deleteRule(rule)
            _snackbarMessage.value = "ลบกฎประกาศ \"${rule.title}\" แล้ว"
        }
    }

    // --- Universal World Boss & Mob-to-Boss Registry CRUD ---
    fun saveBoss(profile: WorldBossProfileEntity) {
        viewModelScope.launch {
            repository.saveBoss(profile)
            _snackbarMessage.value = "บันทึกบอส \"${profile.displayName}\" (${profile.entityId}) แล้ว"
        }
    }

    fun toggleBoss(profile: WorldBossProfileEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.saveBoss(profile.copy(enabled = enabled))
        }
    }

    fun deleteBoss(profile: WorldBossProfileEntity) {
        viewModelScope.launch {
            repository.deleteBoss(profile)
            _snackbarMessage.value = "นำ \"${profile.displayName}\" ออกจากคลังบอสแล้ว"
        }
    }

    // --- Config Updates ---
    fun updateConfig(transform: (StudioConfigEntity) -> StudioConfigEntity) {
        viewModelScope.launch {
            val updated = transform(config.value)
            repository.saveConfig(updated)
        }
    }

    // --- Google Apps Script Live Operations ---
    fun pingGas() {
        val cfg = config.value
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Loading
            _isBusy.value = true
            when (val res = gasRepository.pingGas(cfg.gasWebAppUrl, cfg.gasSecretToken)) {
                is GasResult.Success -> {
                    _connectionStatus.value = ConnectionStatus.Connected(res.message)
                    repository.addLog("PING", "ทดสอบเชื่อมต่อ Google Apps Script", "SUCCESS", res.message)
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    _connectionStatus.value = ConnectionStatus.Failed(res.message)
                    repository.addLog("PING", "ทดสอบเชื่อมต่อ Google Apps Script", "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun syncHotReloadConfigToGas() {
        val cfg = config.value
        val currentRules = rules.value
        val currentBosses = bosses.value
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = gasRepository.pushHotReloadConfig(cfg, currentRules, currentBosses)) {
                is GasResult.Success -> {
                    _connectionStatus.value = ConnectionStatus.Connected(res.message)
                    repository.addLog(
                        "HOT_RELOAD_SYNC",
                        "ซิงก์ตั้งค่า + บอส ${currentBosses.size} ตัว + กฎ ${currentRules.size} ข้อ",
                        "SUCCESS",
                        res.message
                    )
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    _connectionStatus.value = ConnectionStatus.Failed(res.message)
                    repository.addLog("HOT_RELOAD_SYNC", "ซิงก์ตั้งค่า Hot-Reload", "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun sendLiveBroadcast(headline: String, subtitle: String) {
        if (headline.isBlank()) {
            _snackbarMessage.value = "กรุณากรอกข้อความป้ายประกาศก่อนส่ง"
            return
        }
        val cfg = config.value
        viewModelScope.launch {
            _isBusy.value = true
            val payload = JSONObject().apply {
                put("headline", headline.trim())
                put("subtitle", subtitle.trim())
            }
            when (val res = gasRepository.enqueueRemoteCommand(cfg.gasWebAppUrl, cfg.gasSecretToken, "BROADCAST_ANNOUNCEMENT", payload)) {
                is GasResult.Success -> {
                    repository.addLog("BROADCAST", headline.trim(), "QUEUED", res.message)
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    repository.addLog("BROADCAST", headline.trim(), "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun triggerSpawnBossNow(entityId: String, displayName: String) {
        val cfg = config.value
        viewModelScope.launch {
            _isBusy.value = true
            val payload = JSONObject().apply { put("entityId", entityId.trim()) }
            when (val res = gasRepository.enqueueRemoteCommand(cfg.gasWebAppUrl, cfg.gasSecretToken, "SPAWN_BOSS_NOW", payload)) {
                is GasResult.Success -> {
                    repository.addLog("SPAWN_BOSS", "เสก $displayName ($entityId)", "QUEUED", res.message)
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    repository.addLog("SPAWN_BOSS", "เสก $displayName", "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun triggerDespawnBosses() {
        val cfg = config.value
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = gasRepository.enqueueRemoteCommand(cfg.gasWebAppUrl, cfg.gasSecretToken, "DESPAWN_BOSS", JSONObject())) {
                is GasResult.Success -> {
                    repository.addLog("DESPAWN_BOSS", "สั่งเคลียร์ World Boss ทั้งหมด", "QUEUED", res.message)
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    repository.addLog("DESPAWN_BOSS", "สั่งเคลียร์ World Boss", "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun sendCustomServerCommand(rawCommand: String) {
        val cleaned = rawCommand.trim().removePrefix("/")
        if (cleaned.isBlank()) {
            _snackbarMessage.value = "กรุณากรอกคำสั่งเซิร์ฟเวอร์"
            return
        }
        val cfg = config.value
        viewModelScope.launch {
            _isBusy.value = true
            val payload = JSONObject().apply { put("command", cleaned) }
            when (val res = gasRepository.enqueueRemoteCommand(cfg.gasWebAppUrl, cfg.gasSecretToken, "RUN_SERVER_COMMAND", payload)) {
                is GasResult.Success -> {
                    repository.addLog("SERVER_CMD", "/$cleaned", "QUEUED", res.message)
                    _snackbarMessage.value = res.message
                }
                is GasResult.Error -> {
                    repository.addLog("SERVER_CMD", "/$cleaned", "ERROR", res.message)
                    _snackbarMessage.value = res.message
                }
            }
            _isBusy.value = false
        }
    }

    fun clearLogs() {
        viewModelScope.launch { repository.clearLogs() }
    }

    fun exportProjectZip(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val files = generatedFiles.value
            val result = ZipExporter.exportToUri(getApplication(), uri, files)
            result.fold(
                onSuccess = { count ->
                    _snackbarMessage.value = "Export สำเร็จ ($count ไฟล์: Forge 1.20.1 Mod + Code.gs)"
                },
                onFailure = { err ->
                    _snackbarMessage.value = "Export ล้มเหลว: ${err.localizedMessage}"
                }
            )
            _isBusy.value = false
        }
    }
}
