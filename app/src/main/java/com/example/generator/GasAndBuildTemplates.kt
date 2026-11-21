package com.example.generator

import com.example.data.local.AnnouncementRuleEntity
import com.example.data.local.StudioConfigEntity
import com.example.data.local.WorldBossProfileEntity

object GasAndBuildTemplates {

    fun generateCodeGs(
        config: StudioConfigEntity,
        rules: List<AnnouncementRuleEntity>,
        bosses: List<WorldBossProfileEntity>
    ): GeneratedFile {
        val secret = config.gasSecretToken.replace("\"", "\\\"")
        val content = """
/**
 * Dragonfyre Forge 1.20.1 <-> Google Apps Script Bridge (Code.gs)
 * รองรับ: Cisco's Fantasy Medieval RPG [Dragonfyre] / [Ultimate] และ Forge Mod 1.20.1 ทุกตัว
 * สถาปัตยกรรม: Auto-Polling (ไม่ต้องเปิดพอร์ตเซิร์ฟเวอร์) + Hot-Reload ไม่ต้อง Restart
 *
 * วิธีติดตั้ง:
 * 1. สร้าง Google Sheet ใหม่ -> Extensions -> Apps Script -> วางโค้ดนี้แทนที่ทั้งหมด
 * 2. กด Deploy -> New deployment -> Web app -> Execute as: Me -> Who has access: Anyone
 * 3. คัดลอก Web App URL ไปวางในแอป Dragonfyre Forge Studio และไฟล์ config ของ Mod
 */

const SECRET_TOKEN = "$secret";
const SHEET_CONFIG = "Config";
const SHEET_BOSSES = "BossRegistry";
const SHEET_RULES = "AnnouncementRules";
const SHEET_QUEUE = "CommandQueue";
const SHEET_LOGS = "EventLog";

function ensureSheets_() {
  const ss = SpreadsheetApp.getActiveSpreadsheet();
  const defs = [
    { name: SHEET_CONFIG, headers: ["key", "value", "updatedAt"] },
    { name: SHEET_BOSSES, headers: ["id", "displayName", "entityId", "modPackTag", "bossMode", "promotionChancePercent", "hpMultiplier", "attackMultiplier", "armorBonus", "speedMultiplier", "spawnIntervalMinutes", "fightDurationMinutes", "minSpawnRadius", "maxSpawnRadius", "dimensionId", "glowingAura", "xaeroRedMarker", "affixTraits", "rewardCommand", "enabled"] },
    { name: SHEET_RULES, headers: ["id", "title", "triggerType", "matchFilter", "messageTemplate", "subtitleTemplate", "accentHex", "animationStyle", "soundId", "durationSeconds", "cooldownSeconds", "enabled"] },
    { name: SHEET_QUEUE, headers: ["commandId", "commandType", "payloadJson", "status", "createdAt", "executedAt"] },
    { name: SHEET_LOGS, headers: ["timestamp", "eventType", "player", "detail"] }
  ];
  defs.forEach(d => {
    let sh = ss.getSheetByName(d.name);
    if (!sh) {
      sh = ss.insertSheet(d.name);
      sh.appendRow(d.headers);
      sh.setFrozenRows(1);
    }
  });
  return ss;
}

function jsonOut_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}

function checkAuth_(token) {
  return SECRET_TOKEN === "" || String(token || "") === SECRET_TOKEN;
}

function getConfigMap_(ss) {
  const sh = ss.getSheetByName(SHEET_CONFIG);
  const rows = sh.getDataRange().getValues();
  const map = {};
  for (let i = 1; i < rows.length; i++) {
    if (rows[i][0]) map[String(rows[i][0])] = rows[i][1];
  }
  return map;
}

function readTableObjects_(ss, sheetName) {
  const sh = ss.getSheetByName(sheetName);
  const rows = sh.getDataRange().getValues();
  if (rows.length <= 1) return [];
  const headers = rows[0];
  const out = [];
  for (let i = 1; i < rows.length; i++) {
    const obj = {};
    for (let c = 0; c < headers.length; c++) {
      obj[headers[c]] = rows[i][c];
    }
    out.push(obj);
  }
  return out;
}

function doGet(e) {
  const params = (e && e.parameter) ? e.parameter : {};
  if (!checkAuth_(params.secret)) {
    return jsonOut_({ ok: false, error: "Invalid Secret Token" });
  }
  const ss = ensureSheets_();
  const action = params.action || "poll";
  const cfg = getConfigMap_(ss);
  const version = String(cfg.configVersion || "1");

  const queueSheet = ss.getSheetByName(SHEET_QUEUE);
  const qRows = queueSheet.getDataRange().getValues();
  const pendingCommands = [];
  for (let i = 1; i < qRows.length; i++) {
    if (String(qRows[i][3]) === "PENDING") {
      pendingCommands.push({
        commandId: String(qRows[i][0]),
        commandType: String(qRows[i][1]),
        payload: JSON.parse(String(qRows[i][2] || "{}"))
      });
    }
  }

  if (action === "ping") {
    return jsonOut_({
      ok: true,
      configVersion: version,
      pendingCommands: pendingCommands.length,
      serverTime: new Date().toISOString()
    });
  }

  // Forge Mod Polling endpoint (GET ?action=poll&secret=...&lastVersion=...)
  const lastVersion = String(params.lastVersion || "");
  const includeFullConfig = (lastVersion !== version);

  return jsonOut_({
    ok: true,
    configVersion: version,
    configChanged: includeFullConfig,
    settings: includeFullConfig ? cfg : null,
    bossRegistry: includeFullConfig ? readTableObjects_(ss, SHEET_BOSSES) : null,
    announcementRules: includeFullConfig ? readTableObjects_(ss, SHEET_RULES) : null,
    pendingCommands: pendingCommands
  });
}

function doPost(e) {
  try {
    const body = JSON.parse((e && e.postData && e.postData.contents) ? e.postData.contents : "{}");
    if (!checkAuth_(body.secret)) {
      return jsonOut_({ ok: false, error: "Unauthorized: Secret Token ไม่ถูกต้อง" });
    }
    const ss = ensureSheets_();
    const action = body.action || "";

    if (action === "update_config") {
      const cfgSheet = ss.getSheetByName(SHEET_CONFIG);
      const nextVersion = String(Date.now());
      const settings = body.settings || {};
      settings.configVersion = nextVersion;

      cfgSheet.clearContents();
      cfgSheet.appendRow(["key", "value", "updatedAt"]);
      const now = new Date().toISOString();
      Object.keys(settings).forEach(k => cfgSheet.appendRow([k, settings[k], now]));

      if (Array.isArray(body.bossRegistry)) {
        const bSheet = ss.getSheetByName(SHEET_BOSSES);
        const headers = ["id", "displayName", "entityId", "modPackTag", "bossMode", "promotionChancePercent", "hpMultiplier", "attackMultiplier", "armorBonus", "speedMultiplier", "spawnIntervalMinutes", "fightDurationMinutes", "minSpawnRadius", "maxSpawnRadius", "dimensionId", "glowingAura", "xaeroRedMarker", "affixTraits", "rewardCommand", "enabled"];
        bSheet.clearContents();
        bSheet.appendRow(headers);
        body.bossRegistry.forEach(b => bSheet.appendRow(headers.map(h => b[h] !== undefined ? b[h] : "")));
      }

      if (Array.isArray(body.announcementRules)) {
        const rSheet = ss.getSheetByName(SHEET_RULES);
        const headers = ["id", "title", "triggerType", "matchFilter", "messageTemplate", "subtitleTemplate", "accentHex", "animationStyle", "soundId", "durationSeconds", "cooldownSeconds", "enabled"];
        rSheet.clearContents();
        rSheet.appendRow(headers);
        body.announcementRules.forEach(r => rSheet.appendRow(headers.map(h => r[h] !== undefined ? r[h] : "")));
      }

      return jsonOut_({ ok: true, configVersion: nextVersion });
    }

    if (action === "enqueue_command") {
      const qSheet = ss.getSheetByName(SHEET_QUEUE);
      const cmdId = "CMD-" + Utilities.getUuid().slice(0, 8).toUpperCase();
      qSheet.appendRow([
        cmdId,
        String(body.commandType || "BROADCAST_ANNOUNCEMENT"),
        JSON.stringify(body.payload || {}),
        "PENDING",
        new Date().toISOString(),
        ""
      ]);
      return jsonOut_({ ok: true, commandId: cmdId });
    }

    if (action === "ack_commands") {
      const ids = Array.isArray(body.commandIds) ? body.commandIds : [];
      const qSheet = ss.getSheetByName(SHEET_QUEUE);
      const rows = qSheet.getDataRange().getValues();
      const now = new Date().toISOString();
      for (let i = 1; i < rows.length; i++) {
        if (ids.indexOf(String(rows[i][0])) !== -1) {
          qSheet.getRange(i + 1, 4).setValue("DONE");
          qSheet.getRange(i + 1, 6).setValue(now);
        }
      }
      return jsonOut_({ ok: true, acknowledged: ids.length });
    }

    if (action === "log_event") {
      const logSheet = ss.getSheetByName(SHEET_LOGS);
      logSheet.appendRow([
        new Date().toISOString(),
        String(body.eventType || "INFO"),
        String(body.player || "SERVER"),
        String(body.detail || "")
      ]);
      return jsonOut_({ ok: true });
    }

    return jsonOut_({ ok: false, error: "Unknown action: " + action });
  } catch (err) {
    return jsonOut_({ ok: false, error: String(err) });
  }
}
        """.trimIndent()

        return GeneratedFile(
            path = "google_apps_script/Code.gs",
            displayName = "Code.gs (Google Apps Script)",
            category = GeneratedFileCategory.GOOGLE_SCRIPT,
            language = "javascript",
            descriptionTh = "สคริปต์ฝั่ง Google Sheets ทำหน้าที่รับส่งค่าตั้งค่า Hot-Reload, คลังม็อบ/บอส, คิวคำสั่งด่วน และบันทึก EventLog",
            content = content
        )
    }

    fun generateBuildGradle(config: StudioConfigEntity): GeneratedFile {
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val pkg = config.packageName.trim().ifBlank { "com.dragonfyre.bridge" }
        val content = """
plugins {
    id 'eclipse'
    id 'idea'
    id 'maven-publish'
    id 'net.minecraftforge.gradle' version '[6.0,6.2)'
}

version = '${config.modVersion}'
group = '$pkg'

base {
    archivesName = '$modId'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

minecraft {
    mappings channel: 'official', version: '1.20.1'
    copyIdeResources = true

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'info'
            mods {
                "$modId" {
                    source sourceSets.main
                }
            }
        }
        client {
            property 'forge.enabledGameTestNamespaces', '$modId'
        }
        server {
            property 'forge.enabledGameTestNamespaces', '$modId'
            args '--nogui'
        }
    }
}

dependencies {
    minecraft 'net.minecraftforge:forge:1.20.1-47.2.20'
    // Xaero's Minimap & World Map ใช้ Soft-Dependency Reflection Bridge ใน Mod ไม่จำเป็นต้องผูก Compile Jar
    // ทำให้ไม่ Crash แม้อัปเดตเวอร์ชัน Xaero ใน Cisco Dragonfyre / Ultimate หรือ Modpack อื่นๆ
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
        """.trimIndent()

        return GeneratedFile(
            path = "build.gradle",
            displayName = "build.gradle (Forge 1.20.1)",
            category = GeneratedFileCategory.GRADLE_CONFIG,
            language = "groovy",
            descriptionTh = "ไฟล์ตั้งค่า Gradle สำหรับคอมไพล์ Minecraft Forge 1.20.1 (47.2.20 / Java 17)",
            content = content
        )
    }

    fun generateModsToml(config: StudioConfigEntity): GeneratedFile {
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val content = """
modLoader="javafml"
loaderVersion="[47,)"
license="MIT"

[[mods]]
modId="$modId"
version="${config.modVersion}"
displayName="${config.modName}"
description='''
Universal Minecraft Forge 1.20.1 Mod สำหรับ Cisco's Fantasy Medieval RPG [Dragonfyre] / [Ultimate] และ Modpack อื่นๆ
- ระบบป้ายประกาศกลางจอ (Center-Screen RPG HUD) รองรับ Apotheosis Rarity & Multi-Mod Boss Kills
- ระบบ Universal World Boss & Mob-to-Boss Promoter อัปเกรดม็อบจาก Mod ใดก็ได้เป็นบอส พร้อมหลอดเลือด Custom และจุดแดง Xaero's World Map
- ระบบเชื่อมต่อ Google Apps Script Auto-Polling จัดการ Server Settings แบบไม่ต้อง Restart
'''

[[dependencies.$modId]]
    modId="forge"
    mandatory=true
    versionRange="[47,)"
    ordering="NONE"
    side="BOTH"

[[dependencies.$modId]]
    modId="minecraft"
    mandatory=true
    versionRange="[1.20.1,1.21)"
    ordering="NONE"
    side="BOTH"
        """.trimIndent()

        return GeneratedFile(
            path = "src/main/resources/META-INF/mods.toml",
            displayName = "mods.toml",
            category = GeneratedFileCategory.GRADLE_CONFIG,
            language = "toml",
            descriptionTh = "ไฟล์ Metadata ประกาศ Mod สำหรับ Forge 1.20.1",
            content = content
        )
    }

    fun generateReadmeTh(config: StudioConfigEntity): GeneratedFile {
        val modId = config.modId.lowercase().replace(Regex("[^a-z0-9_]"), "_")
        val content = """
# คู่มือการติดตั้งและใช้งาน ${config.modName} (Forge 1.20.1)

Mod นี้รองรับทั้ง **Cisco's Fantasy Medieval RPG [Dragonfyre]**, **Cisco [Ultimate]** และ **Modpack Forge 1.20.1 อื่นๆ ทุกตัว**

## ขั้นที่ 1: ติดตั้ง Google Apps Script (Backend ไม่ต้องเปิดพอร์ต)
1. สร้าง Google Sheet ใหม่ใน Google Drive ของคุณ
2. ไปที่เมนู **Extensions (ส่วนขยาย) -> Apps Script**
3. นำโค้ดจากไฟล์ `google_apps_script/Code.gs` ไปวางแทนที่ทั้งหมดแล้วกด Save
4. กด **Deploy -> New deployment -> เลือกชนิดเป็น Web app**
   - Execute as: **Me**
   - Who has access: **Anyone**
5. คัดลอก **Web App URL** มาใส่ในแอป **Dragonfyre Forge Studio** แท็บ `รีโมต GAS` แล้วกด **ทดสอบเชื่อมต่อ** และ **ซิงก์ขึ้น Google Script**

## ขั้นที่ 2: คอมไพล์ไฟล์ Mod (.jar)
1. แตกไฟล์ `.zip` ที่ Export จากแอปนี้ทับลงในโฟลเดอร์ Forge 1.20.1 MDK (ต้องมี Java 17 JDK)
2. รันคำสั่ง:
   - Windows: `gradlew.bat build`
   - Linux/macOS: `./gradlew build`
3. นำไฟล์ `.jar` ใน `build/libs/$modId-${config.modVersion}.jar` ไปวางในโฟลเดอร์ `mods/` ทั้งฝั่ง **Server** และ **Client**

## ขั้นที่ 3: คำสั่งแอดมินในเกม (Hot-Reload & Universal Mob-to-Boss)
- `/dfboss list` — ดูรายชื่อม็อบ/บอสทั้งหมดที่ลงทะเบียนไว้ในระบบ
- `/dfboss spawn <entity_id>` — เสก World Boss ทันทีจาก Entity ID ของ Mod ใดก็ได้ (เช่น `cataclysm:ignis`, `twilightforest:naga`, `minecraft:warden`)
- `/dfboss promote <entity_id> <hpMult> <atkMult> <chance%>` — เพิ่มหรืออัปเกรดม็อบตัวใดก็ได้จาก Mod ใดก็ได้ให้มีโอกาสเกิดเป็น Custom Boss สดๆ โดยไม่ต้องรีสตาร์ทเซิร์ฟเวอร์!
- `/dfboss demote <entity_id>` — ถอดม็อบออกจากระบบบอส
- `/dfboss announce <ข้อความ>` — ยิงป้ายประกาศกลางจอให้ผู้เล่นทั้งเซิร์ฟเวอร์
- `/dfboss sync` — บังคับดึงค่าตั้งค่าล่าสุดจาก Google Apps Script ทันที
        """.trimIndent()

        return GeneratedFile(
            path = "README_SETUP_TH.md",
            displayName = "README_SETUP_TH.md (คู่มือ)",
            category = GeneratedFileCategory.GRADLE_CONFIG,
            language = "markdown",
            descriptionTh = "ขั้นตอน Deploy Google Apps Script, คอมไพล์ .jar และคำสั่ง /dfboss ทั้งหมด",
            content = content
        )
    }
}
