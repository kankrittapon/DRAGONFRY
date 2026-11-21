package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnouncementRuleDao {
    @Query("SELECT * FROM announcement_rules ORDER BY id ASC")
    fun observeAll(): Flow<List<AnnouncementRuleEntity>>

    @Query("SELECT COUNT(*) FROM announcement_rules")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: AnnouncementRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<AnnouncementRuleEntity>)

    @Update
    suspend fun update(rule: AnnouncementRuleEntity)

    @Delete
    suspend fun delete(rule: AnnouncementRuleEntity)
}

@Dao
interface WorldBossProfileDao {
    @Query("SELECT * FROM world_boss_profiles ORDER BY id ASC")
    fun observeAll(): Flow<List<WorldBossProfileEntity>>

    @Query("SELECT COUNT(*) FROM world_boss_profiles")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: WorldBossProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<WorldBossProfileEntity>)

    @Update
    suspend fun update(profile: WorldBossProfileEntity)

    @Delete
    suspend fun delete(profile: WorldBossProfileEntity)
}

@Dao
interface StudioConfigDao {
    @Query("SELECT * FROM studio_config WHERE id = 1")
    fun observeConfig(): Flow<StudioConfigEntity?>

    @Query("SELECT * FROM studio_config WHERE id = 1")
    suspend fun getConfigOnce(): StudioConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: StudioConfigEntity)
}

@Dao
interface RemoteActionLogDao {
    @Query("SELECT * FROM remote_action_logs ORDER BY timestampMs DESC LIMIT 50")
    fun observeRecent(): Flow<List<RemoteActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: RemoteActionLogEntity)

    @Query("DELETE FROM remote_action_logs")
    suspend fun clearAll()
}
