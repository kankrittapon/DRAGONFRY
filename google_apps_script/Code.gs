/**
 * Dragonfyre Forge 1.20.1 <-> Google Apps Script Bridge (Code.gs)
 * รองรับ: Cisco's Fantasy Medieval RPG [Dragonfyre] / [Ultimate] และ Forge Mod 1.20.1 ทุกตัว
 * สถาปัตยกรรม: Auto-Polling (ไม่ต้องเปิดพอร์ตเซิร์ฟเวอร์) + Hot-Reload ไม่ต้อง Restart
 *
 * วิธีติดตั้ง:
 * 1. สร้าง Google Sheet ใหม่ -> Extensions -> Apps Script -> วางโค้ดนี้แทนที่ทั้งหมด
 * 2. กด Deploy -> New deployment -> Web app -> Execute as: Me -> Who has access: Anyone
 * 3. คัดลอก Web App URL ไปวางใน Web Studio และไฟล์ config ของ Mod
 */

const SECRET_TOKEN = "DF_SECRET_2026";
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
    if (qRows[i][3] === "PENDING") {
      let payload = {};
      try { payload = JSON.parse(qRows[i][2] || "{}"); } catch (e) {}
      pendingCommands.push({
        commandId: String(qRows[i][0]),
        commandType: String(qRows[i][1]),
        payload: payload
      });
    }
  }

  if (action === "poll") {
    const clientVer = params.lastVersion || "";
    const changed = clientVer !== version;
    return jsonOut_({
      ok: true,
      configVersion: version,
      configChanged: changed,
      settings: changed ? cfg : null,
      bossRegistry: changed ? readTableObjects_(ss, SHEET_BOSSES) : null,
      announcementRules: changed ? readTableObjects_(ss, SHEET_RULES) : null,
      pendingCommands: pendingCommands
    });
  }

  if (action === "get_all") {
    return jsonOut_({
      ok: true,
      configVersion: version,
      settings: cfg,
      bossRegistry: readTableObjects_(ss, SHEET_BOSSES),
      announcementRules: readTableObjects_(ss, SHEET_RULES),
      pendingCommands: pendingCommands
    });
  }

  return jsonOut_({ ok: false, error: "Unknown action" });
}

function doPost(e) {
  try {
    const body = JSON.parse(e.postData.contents || "{}");
    if (!checkAuth_(body.secret)) {
      return jsonOut_({ ok: false, error: "Invalid Secret Token" });
    }
    const ss = ensureSheets_();
    const action = body.action || "";

    if (action === "update_all") {
      const cfgSheet = ss.getSheetByName(SHEET_CONFIG);
      const settings = body.settings || {};
      const nextVersion = String(Date.now());
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
