package com.example.data.remote

import com.example.data.local.AnnouncementRuleEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class GasResult {
    data class Success(val message: String, val rawBody: String = "") : GasResult()
    data class Error(val message: String) : GasResult()
}

class GasRepository {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun validateUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
        return trimmed
    }

    suspend fun pingGas(webAppUrl: String, secretToken: String): GasResult = withContext(Dispatchers.IO) {
        val cleanUrl = validateUrl(webAppUrl)
            ?: return@withContext GasResult.Error("กรุณากรอก Google Apps Script Web App URL (ขึ้นต้นด้วย https://)")
        try {
            val sep = if (cleanUrl.contains("?")) "&" else "?"
            val encodedSecret = URLEncoder.encode(secretToken.trim(), "UTF-8")
            val fullUrl = "${cleanUrl}${sep}action=ping&secret=${encodedSecret}"
            val req = Request.Builder().url(fullUrl).get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext GasResult.Error("HTTP ${res.code}: ตรวจสอบสิทธิ์การ Deploy (Anyone) ของ Web App")
                }
                val parsed = runCatching { JSONObject(body) }.getOrNull()
                if (parsed?.optBoolean("ok", false) == true) {
                    val ver = parsed.optString("configVersion", "1")
                    val pending = parsed.optInt("pendingCommands", 0)
                    GasResult.Success("เชื่อมต่อสำเร็จ (Config v$ver • คิวคำสั่งค้าง $pending)", body)
                } else {
                    val err = parsed?.optString("error")?.takeIf { it.isNotBlank() }
                        ?: "ตอบกลับไม่ใช่ JSON จาก Dragonfyre Code.gs"
                    GasResult.Error(err)
                }
            }
        } catch (e: Exception) {
            GasResult.Error("เชื่อมต่อไม่สำเร็จ: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    suspend fun pushHotReloadConfig(
        config: StudioConfigEntity,
        rules: List<AnnouncementRuleEntity>,
        bosses: List<WorldBossProfileEntity>
    ): GasResult = withContext(Dispatchers.IO) {
        val cleanUrl = validateUrl(config.gasWebAppUrl)
            ?: return@withContext GasResult.Error("ยังไม่ได้ตั้งค่า Google Apps Script Web App URL")
        try {
            val payload = JSONObject().apply {
                put("action", "update_config")
                put("secret", config.gasSecretToken.trim())
                put("settings", JSONObject().apply {
                    put("worldBossSystemEnabled", config.worldBossSystemEnabled)
                    put("autoPromoteMobSystemEnabled", config.autoPromoteMobSystemEnabled)
                    put("announcementsEnabled", config.announcementsEnabled)
                    put("globalBossHpScale", config.globalBossHpScale.toDouble())
                    put("globalBossAtkScale", config.globalBossAtkScale.toDouble())
                    put("minLootRarity", config.minLootRarity)
                    put("keepInventory", config.keepInventory)
                    put("mobGriefing", config.mobGriefing)
                    put("doDaylightCycle", config.doDaylightCycle)
                    put("pvpEnabled", config.pvpEnabled)
                    put("serverDifficulty", config.serverDifficulty)
                    put("welcomeBannerText", config.welcomeBannerText)
                    put("maintenanceMode", config.maintenanceMode)
                    put("xpMultiplier", config.xpMultiplier.toDouble())
                    put("bossBarTheme", config.bossBarTheme)
                    put("xaeroWaypointPrefix", config.xaeroWaypointPrefix)
                    put("pollIntervalSeconds", config.pollIntervalSeconds)
                })
                put("bossRegistry", JSONArray().apply {
                    bosses.forEach { b ->
                        put(JSONObject().apply {
                            put("id", b.id)
                            put("displayName", b.displayName)
                            put("entityId", b.entityId)
                            put("modPackTag", b.modPackTag)
                            put("bossMode", b.bossMode)
                            put("promotionChancePercent", b.promotionChancePercent)
                            put("hpMultiplier", b.hpMultiplier.toDouble())
                            put("attackMultiplier", b.attackMultiplier.toDouble())
                            put("armorBonus", b.armorBonus.toDouble())
                            put("speedMultiplier", b.speedMultiplier.toDouble())
                            put("spawnIntervalMinutes", b.spawnIntervalMinutes)
                            put("fightDurationMinutes", b.fightDurationMinutes)
                            put("minSpawnRadius", b.minSpawnRadius)
                            put("maxSpawnRadius", b.maxSpawnRadius)
                            put("dimensionId", b.dimensionId)
                            put("glowingAura", b.glowingAura)
                            put("xaeroRedMarker", b.xaeroRedMarker)
                            put("affixTraits", b.affixTraits)
                            put("rewardCommand", b.rewardCommand)
                            put("enabled", b.enabled)
                        })
                    }
                })
                put("announcementRules", JSONArray().apply {
                    rules.forEach { r ->
                        put(JSONObject().apply {
                            put("id", r.id)
                            put("title", r.title)
                            put("triggerType", r.triggerType)
                            put("matchFilter", r.matchFilter)
                            put("messageTemplate", r.messageTemplate)
                            put("subtitleTemplate", r.subtitleTemplate)
                            put("accentHex", r.accentHex)
                            put("animationStyle", r.animationStyle)
                            put("soundId", r.soundId)
                            put("durationSeconds", r.durationSeconds)
                            put("cooldownSeconds", r.cooldownSeconds)
                            put("enabled", r.enabled)
                        })
                    }
                })
            }

            val req = Request.Builder()
                .url(cleanUrl)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext GasResult.Error("HTTP ${res.code}: ส่งค่าไปยัง Google Script ไม่สำเร็จ")
                }
                val parsed = runCatching { JSONObject(body) }.getOrNull()
                if (parsed?.optBoolean("ok", false) == true) {
                    val ver = parsed.optString("configVersion", "latest")
                    GasResult.Success("อัปเดตตั้งค่าสดขึ้น Google Sheets แล้ว (v$ver) • Mod จะ Hot-Reload ในรอบ Polling ถัดไป", body)
                } else {
                    GasResult.Error(parsed?.optString("error") ?: "Google Script ปฏิเสธคำขอ (ตรวจสอบ Secret Token)")
                }
            }
        } catch (e: Exception) {
            GasResult.Error("เกิดข้อผิดพลาดเครือข่าย: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }

    suspend fun enqueueRemoteCommand(
        webAppUrl: String,
        secretToken: String,
        commandType: String,
        payloadJson: JSONObject
    ): GasResult = withContext(Dispatchers.IO) {
        val cleanUrl = validateUrl(webAppUrl)
            ?: return@withContext GasResult.Error("ยังไม่ได้ตั้งค่า Google Apps Script Web App URL")
        try {
            val bodyJson = JSONObject().apply {
                put("action", "enqueue_command")
                put("secret", secretToken.trim())
                put("commandType", commandType)
                put("payload", payloadJson)
            }
            val req = Request.Builder()
                .url(cleanUrl)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext GasResult.Error("HTTP ${res.code}: ส่งคำสั่งด่วนไม่สำเร็จ")
                }
                val parsed = runCatching { JSONObject(body) }.getOrNull()
                if (parsed?.optBoolean("ok", false) == true) {
                    val cmdId = parsed.optString("commandId", "#")
                    GasResult.Success("เพิ่มคำสั่ง $commandType ($cmdId) เข้าคิวแล้ว เซิร์ฟเวอร์จะรันอัตโนมัติในไม่กี่วินาที", body)
                } else {
                    GasResult.Error(parsed?.optString("error") ?: "ไม่สามารถส่งคำสั่งเข้า Google Script ได้")
                }
            }
        } catch (e: Exception) {
            GasResult.Error("ส่งคำสั่งไม่สำเร็จ: ${e.localizedMessage ?: e.javaClass.simpleName}")
        }
    }
}
