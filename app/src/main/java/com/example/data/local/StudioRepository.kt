package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StudioRepository(private val db: AppDatabase) {
    private val ruleDao = db.announcementRuleDao()
    private val bossDao = db.worldBossProfileDao()
    private val configDao = db.studioConfigDao()
    private val logDao = db.remoteActionLogDao()

    val rules: Flow<List<AnnouncementRuleEntity>> = ruleDao.observeAll()
    val bosses: Flow<List<WorldBossProfileEntity>> = bossDao.observeAll()
    val config: Flow<StudioConfigEntity> = configDao.observeConfig().map { it ?: StudioConfigEntity() }
    val logs: Flow<List<RemoteActionLogEntity>> = logDao.observeRecent()

    suspend fun ensureSeeded() {
        if (configDao.getConfigOnce() == null) {
            configDao.upsert(StudioConfigEntity())
        }
        if (ruleDao.count() == 0) {
            ruleDao.insertAll(DefaultStudioPresets.initialRules())
        }
        if (bossDao.count() == 0) {
            bossDao.insertAll(DefaultStudioPresets.initialBossProfiles())
        }
    }

    suspend fun saveRule(rule: AnnouncementRuleEntity) {
        if (rule.id == 0L) ruleDao.insert(rule) else ruleDao.update(rule)
    }

    suspend fun deleteRule(rule: AnnouncementRuleEntity) = ruleDao.delete(rule)

    suspend fun saveBoss(profile: WorldBossProfileEntity) {
        if (profile.id == 0L) bossDao.insert(profile) else bossDao.update(profile)
    }

    suspend fun deleteBoss(profile: WorldBossProfileEntity) = bossDao.delete(profile)

    suspend fun saveConfig(config: StudioConfigEntity) = configDao.upsert(config)

    suspend fun addLog(actionType: String, summary: String, status: String, responseDetail: String) {
        logDao.insert(
            RemoteActionLogEntity(
                actionType = actionType,
                summary = summary,
                status = status,
                responseDetail = responseDetail
            )
        )
    }

    suspend fun clearLogs() = logDao.clearAll()
}
