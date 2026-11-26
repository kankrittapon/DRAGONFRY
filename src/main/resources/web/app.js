// OmniCore Forge Studio - Web Edition (Universal Non-Mobile)
const DEFAULT_BOSSES = [
  {
    id: 1,
    displayName: "Ignis, Lord of Dragonfyre",
    entityId: "cataclysm:ignis",
    modPackTag: "Cisco Dragonfyre",
    bossMode: "BOTH",
    promotionChancePercent: 100,
    hpMultiplier: 3.5,
    attackMultiplier: 2.0,
    armorBonus: 14,
    speedMultiplier: 1.15,
    spawnIntervalMinutes: 60,
    fightDurationMinutes: 20,
    minSpawnRadius: 300,
    maxSpawnRadius: 1500,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: "FIRE_NOVA,ENRAGED",
    rewardCommand: "give @p minecraft:netherite_ingot 5",
    enabled: true,
    spawnStrategy: "ANCHOR_LANDMARK",
    anchorX: 0,
    anchorY: 0,
    anchorZ: 0,
    anchorRadius: 80,
    antiGriefEnabled: true,
    protectRadius: 500,
    scheduleHours: "18:00,21:00",
    nativeBossBar: true,
    raidScaling: true
  },
  {
    id: 2,
    displayName: "Netherite Monstrosity",
    entityId: "cataclysm:netherite_monstrosity",
    modPackTag: "Cisco Ultimate",
    bossMode: "SCHEDULED_WORLD_BOSS",
    promotionChancePercent: 100,
    hpMultiplier: 4.0,
    attackMultiplier: 1.9,
    armorBonus: 18,
    speedMultiplier: 1.1,
    spawnIntervalMinutes: 75,
    fightDurationMinutes: 25,
    minSpawnRadius: 350,
    maxSpawnRadius: 1600,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: "IRON_SKIN,FIRE_NOVA",
    rewardCommand: "give @p minecraft:diamond_block 3",
    enabled: true,
    spawnStrategy: "PLAYER_RELATIVE_500M",
    anchorX: 0,
    anchorY: 0,
    anchorZ: 0,
    anchorRadius: 60,
    antiGriefEnabled: true,
    protectRadius: 500,
    scheduleHours: "20:00",
    nativeBossBar: true,
    raidScaling: true
  },
  {
    id: 3,
    displayName: "Crimson Elder Fire Dragon",
    entityId: "iceandfire:fire_dragon",
    modPackTag: "Ice & Fire",
    bossMode: "SCHEDULED_WORLD_BOSS",
    promotionChancePercent: 100,
    hpMultiplier: 2.8,
    attackMultiplier: 1.8,
    armorBonus: 12,
    speedMultiplier: 1.1,
    spawnIntervalMinutes: 90,
    fightDurationMinutes: 25,
    minSpawnRadius: 400,
    maxSpawnRadius: 1800,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: "FIRE_NOVA,LIGHTNING_CALLER",
    rewardCommand: "give @p minecraft:enchanted_golden_apple 2",
    enabled: true,
    spawnStrategy: "DYNAMIC_BIOME",
    anchorX: 0,
    anchorY: 0,
    anchorZ: 0,
    anchorRadius: 100,
    antiGriefEnabled: true,
    protectRadius: 500,
    scheduleHours: "15:00,22:00",
    nativeBossBar: true,
    raidScaling: true
  },
  {
    id: 4,
    displayName: "Ferrous Wroughtnaut Apex",
    entityId: "mowziesmobs:ferrous_wroughtnaut",
    modPackTag: "Mowzie's Mobs",
    bossMode: "AUTO_PROMOTE_NATURAL",
    promotionChancePercent: 100,
    hpMultiplier: 2.5,
    attackMultiplier: 1.6,
    armorBonus: 16,
    speedMultiplier: 1.2,
    spawnIntervalMinutes: 45,
    fightDurationMinutes: 15,
    minSpawnRadius: 200,
    maxSpawnRadius: 1000,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: "IRON_SKIN,ENRAGED",
    rewardCommand: "give @p minecraft:netherite_scrap 4",
    enabled: true,
    spawnStrategy: "ANCHOR_LANDMARK",
    anchorX: 0,
    anchorY: 0,
    anchorZ: 0,
    anchorRadius: 60,
    antiGriefEnabled: true,
    protectRadius: 500,
    scheduleHours: "",
    nativeBossBar: true,
    raidScaling: true
  },
  {
    id: 5,
    displayName: "Cursed Zombie Warlord (Promoted)",
    entityId: "minecraft:zombie",
    modPackTag: "Universal Promoter",
    bossMode: "AUTO_PROMOTE_NATURAL",
    promotionChancePercent: 3,
    hpMultiplier: 15.0,
    attackMultiplier: 3.2,
    armorBonus: 12,
    speedMultiplier: 1.3,
    spawnIntervalMinutes: 30,
    fightDurationMinutes: 12,
    minSpawnRadius: 150,
    maxSpawnRadius: 900,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: "LIFE_STEAL,SPEED_SURGE,IRON_SKIN",
    rewardCommand: "xp add @p 500 points",
    enabled: true,
    spawnStrategy: "PLAYER_RELATIVE_500M",
    anchorX: 0,
    anchorY: 0,
    anchorZ: 0,
    anchorRadius: 50,
    antiGriefEnabled: false,
    protectRadius: 0,
    scheduleHours: "",
    nativeBossBar: false,
    raidScaling: false
  }
];

const DEFAULT_RULES = [
  {
    id: 1,
    title: "ดรอปอาวุธ/เกราะระดับ Epic+",
    triggerType: "RARITY_LOOT",
    matchFilter: "apotheosis:epic,apotheosis:mythic,apotheosis:ancient,EPIC",
    messageTemplate: "⚔ {player} ได้รับไอเทมระดับ {rarity}: {item}",
    subtitleTemplate: "ตรวจพบพลัง Affix หายากใน Cisco RPG",
    accentHex: "#A855F7",
    animationStyle: "RUNE_GLOW",
    soundId: "minecraft:ui.toast.challenge_complete",
    durationSeconds: 6,
    enabled: true
  },
  {
    id: 2,
    title: "World Boss ถือกำเนิดบนแผนที่",
    triggerType: "BOSS_SPAWN",
    matchFilter: "*",
    messageTemplate: "🔥 {mob} ปรากฏตัวแล้วที่ ({x}, {y}, {z})",
    subtitleTemplate: "เปิด Xaero's World Map เพื่อดูจุดแดง • เหลือเวลา {time} นาที",
    accentHex: "#EF4444",
    animationStyle: "SLIDE_DOWN",
    soundId: "minecraft:entity.ender_dragon.growl",
    durationSeconds: 7,
    enabled: true
  },
  {
    id: 3,
    title: "โค่น World Boss สำเร็จ",
    triggerType: "BOSS_DEFEATED",
    matchFilter: "*",
    messageTemplate: "👑 {player} สังหาร World Boss {mob} สำเร็จ!",
    subtitleTemplate: "ผู้ร่วมปราบได้รับรางวัลพิเศษ • จุดแดงบนแผนที่ถูกนำออกแล้ว",
    accentHex: "#F59E0B",
    animationStyle: "SCALE_POP",
    soundId: "minecraft:ui.toast.challenge_complete",
    durationSeconds: 6,
    enabled: true
  },
  {
    id: 4,
    title: "สังหารม็อบ/บอสพิเศษข้าม Mod",
    triggerType: "MOB_KILL",
    matchFilter: "cataclysm:*,iceandfire:*_dragon,twilightforest:*,mowziesmobs:*",
    messageTemplate: "☠ {player} สังหารม็อบระดับตำนาน {mob}",
    subtitleTemplate: "บันทึกเกียรติประวัติขึ้น Google Sheets แล้ว",
    accentHex: "#38BDF8",
    animationStyle: "SLIDE_DOWN",
    soundId: "minecraft:entity.wither.death",
    durationSeconds: 5,
    enabled: true
  },
  {
    id: 5,
    title: "ประกาศด่วนจาก Google Script",
    triggerType: "REMOTE_CUSTOM",
    matchFilter: "GAS_BROADCAST",
    messageTemplate: "📢 {message}",
    subtitleTemplate: "ประกาศจากผู้ดูแลเซิร์ฟเวอร์ (Live Broadcast)",
    accentHex: "#10B981",
    animationStyle: "RUNE_GLOW",
    soundId: "minecraft:block.bell.use",
    durationSeconds: 6,
    enabled: true
  }
];

const DEFAULT_CONFIG = {
  modId: "omnicore",
  modName: "OmniCore: Universal RPG Engine",
  packageName: "com.omnicore",
  modVersion: "1.2.0",
  bossBarTheme: "DRAGON_GOLD",
  gasWebAppUrl: "",
  gasSecretToken: "DF_SECRET_2026",
  pollIntervalSeconds: 20,
  worldBossSystemEnabled: true,
  autoPromoteMobSystemEnabled: true,
  announcementsEnabled: true,
  globalBossHpScale: 1.0,
  globalBossAtkScale: 1.0,
  keepInventory: true,
  mobGriefing: false,
  doDaylightCycle: true,
  pvpEnabled: false,
  serverDifficulty: "HARD"
};

let state = {
  bosses: JSON.parse(localStorage.getItem("df_bosses") || "null") || DEFAULT_BOSSES,
  rules: JSON.parse(localStorage.getItem("df_rules") || "null") || DEFAULT_RULES,
  config: JSON.parse(localStorage.getItem("df_config") || "null") || DEFAULT_CONFIG
};

function saveState() {
  localStorage.setItem("df_bosses", JSON.stringify(state.bosses));
  localStorage.setItem("df_rules", JSON.stringify(state.rules));
  localStorage.setItem("df_config", JSON.stringify(state.config));
}

// Navigation
document.querySelectorAll(".tab-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));
    btn.classList.add("active");
    const target = document.getElementById(btn.dataset.tab);
    if (target) target.classList.add("active");
    if (btn.dataset.tab === "stackmob-tab") {
      refreshStackMobStatus();
    }
  });
});

// Render Boss Cards
function renderBossCards() {
  const container = document.getElementById("boss-grid");
  if (!container) return;
  container.innerHTML = state.bosses.map(b => `
    <div class="card">
      <div class="card-header">
        <div class="card-title">
          <h3>${escapeHtml(b.displayName)}</h3>
          <span class="tag">${escapeHtml(b.entityId)}</span>
        </div>
        <span style="font-size: 11px; font-weight: 700; color: ${b.enabled ? 'var(--green)' : 'var(--crimson)'}">
          ${b.enabled ? '● เปิดใช้งาน' : '○ ปิด'}
        </span>
      </div>
      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-label">พลังชีวิต (HP)</span>
          <span class="stat-value" style="color: var(--crimson)">x${b.hpMultiplier}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">พลังโจมตี (ATK)</span>
          <span class="stat-value" style="color: var(--gold)">x${b.attackMultiplier}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">โหมด / จุดเกิด</span>
          <span class="stat-value">${b.spawnStrategy === 'ANCHOR_LANDMARK' ? '📍 Anchor (' + (b.anchorX||0) + ',' + (b.anchorY||0) + ',' + (b.anchorZ||0) + ')' : (b.spawnStrategy === 'PLAYER_RELATIVE_500M' ? '🏃 ห่างผู้เล่น 500m' : b.bossMode)}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">รอบเกิด / เวลาสู้</span>
          <span class="stat-value">${b.scheduleHours ? b.scheduleHours : b.spawnIntervalMinutes + 'm'} / ${b.fightDurationMinutes}m</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">ระบบความปลอดภัย</span>
          <span class="stat-value" style="font-size: 11px;">
            ${b.antiGriefEnabled ? '🛡️ บาเรีย ' + (b.protectRadius||500) + 'm' : '❌ ไม่มีบาเรีย'}
            ${b.raidScaling ? ' • 👥 Raid Scaling' : ''}
          </span>
        </div>
        <div class="stat-item">
          <span class="stat-label">บัฟต่อสู้ (Affixes)</span>
          <span class="stat-value" style="color: var(--purple)">${escapeHtml(b.affixTraits || 'NONE')}</span>
        </div>
      </div>
      <div class="card-actions" style="flex-wrap: wrap; gap: 6px;">
        <button class="btn btn-secondary" style="flex: 1;" onclick="editBoss(${b.id})">✏️ แก้ไข</button>
        <button class="btn btn-secondary" style="padding: 6px 10px;" title="เสกห่างผู้เล่น 500m" onclick="sendSpawnNear(${b.id})">🏃 เสก</button>
        <button class="btn btn-danger" onclick="deleteBoss(${b.id})">🗑️ ลบ</button>
      </div>
    </div>
  `).join("");
}

// Boss CRUD Modal
function openBossModal(boss = null) {
  const isEdit = !!boss;
  document.getElementById("modal-title").innerText = isEdit ? "แก้ไข World Boss Profile" : "เพิ่ม World Boss Profile ใหม่";
  document.getElementById("boss-id").value = isEdit ? boss.id : 0;
  document.getElementById("boss-name").value = isEdit ? boss.displayName : "";
  document.getElementById("boss-entity").value = isEdit ? boss.entityId : "";
  document.getElementById("boss-tag").value = isEdit ? boss.modPackTag : "Custom Mod";
  document.getElementById("boss-mode").value = isEdit ? boss.bossMode : "BOTH";
  document.getElementById("boss-hp").value = isEdit ? boss.hpMultiplier : 3.0;
  document.getElementById("boss-atk").value = isEdit ? boss.attackMultiplier : 1.8;
  document.getElementById("boss-chance").value = isEdit ? boss.promotionChancePercent : 100;
  document.getElementById("boss-interval").value = isEdit ? boss.spawnIntervalMinutes : 60;
  document.getElementById("boss-duration").value = isEdit ? boss.fightDurationMinutes : 20;
  document.getElementById("boss-affix").value = isEdit ? boss.affixTraits : "FIRE_NOVA,ENRAGED";
  document.getElementById("boss-reward").value = isEdit ? boss.rewardCommand : "give @p minecraft:netherite_ingot 3";
  document.getElementById("boss-strategy").value = isEdit ? (boss.spawnStrategy || "ANCHOR_LANDMARK") : "ANCHOR_LANDMARK";
  document.getElementById("boss-schedule-hours").value = isEdit ? (boss.scheduleHours || "") : "";
  document.getElementById("boss-anchor-x").value = isEdit ? (boss.anchorX || 0) : 0;
  document.getElementById("boss-anchor-y").value = isEdit ? (boss.anchorY || 0) : 0;
  document.getElementById("boss-anchor-z").value = isEdit ? (boss.anchorZ || 0) : 0;
  document.getElementById("boss-anchor-rad").value = isEdit ? (boss.anchorRadius || 80) : 80;
  document.getElementById("boss-protect-radius").value = isEdit ? (boss.protectRadius !== undefined ? boss.protectRadius : 500) : 500;
  document.getElementById("boss-antigrief").checked = isEdit ? (boss.antiGriefEnabled !== false) : true;
  document.getElementById("boss-native-bar").checked = isEdit ? (boss.nativeBossBar !== false) : true;
  document.getElementById("boss-raid-scaling").checked = isEdit ? (boss.raidScaling !== false) : true;
  document.getElementById("boss-enabled").checked = isEdit ? boss.enabled : true;
  document.getElementById("boss-modal").classList.add("active");
}

function closeBossModal() {
  document.getElementById("boss-modal").classList.remove("active");
}

function saveBossFromModal() {
  const id = parseInt(document.getElementById("boss-id").value) || Date.now();
  const profile = {
    id: id,
    displayName: document.getElementById("boss-name").value.trim() || "Custom Boss",
    entityId: document.getElementById("boss-entity").value.trim() || "minecraft:zombie",
    modPackTag: document.getElementById("boss-tag").value.trim() || "Custom",
    bossMode: document.getElementById("boss-mode").value,
    promotionChancePercent: parseInt(document.getElementById("boss-chance").value) || 100,
    hpMultiplier: parseFloat(document.getElementById("boss-hp").value) || 3.0,
    attackMultiplier: parseFloat(document.getElementById("boss-atk").value) || 1.8,
    armorBonus: 10.0,
    speedMultiplier: 1.15,
    spawnIntervalMinutes: parseInt(document.getElementById("boss-interval").value) || 45,
    fightDurationMinutes: parseInt(document.getElementById("boss-duration").value) || 20,
    minSpawnRadius: 250,
    maxSpawnRadius: 1400,
    dimensionId: "minecraft:overworld",
    glowingAura: true,
    xaeroRedMarker: true,
    affixTraits: document.getElementById("boss-affix").value.trim() || "ENRAGED",
    rewardCommand: document.getElementById("boss-reward").value.trim(),
    enabled: document.getElementById("boss-enabled").checked,
    spawnStrategy: document.getElementById("boss-strategy").value,
    scheduleHours: document.getElementById("boss-schedule-hours").value.trim(),
    anchorX: parseInt(document.getElementById("boss-anchor-x").value) || 0,
    anchorY: parseInt(document.getElementById("boss-anchor-y").value) || 0,
    anchorZ: parseInt(document.getElementById("boss-anchor-z").value) || 0,
    anchorRadius: parseInt(document.getElementById("boss-anchor-rad").value) || 80,
    protectRadius: parseInt(document.getElementById("boss-protect-radius").value) || 500,
    antiGriefEnabled: document.getElementById("boss-antigrief").checked,
    nativeBossBar: document.getElementById("boss-native-bar").checked,
    raidScaling: document.getElementById("boss-raid-scaling").checked
  };

  const existingIdx = state.bosses.findIndex(b => b.id === id);
  if (existingIdx >= 0) {
    state.bosses[existingIdx] = profile;
  } else {
    state.bosses.push(profile);
  }
  saveState();
  renderBossCards();
  closeBossModal();
}

window.editBoss = (id) => {
  const boss = state.bosses.find(b => b.id === id);
  if (boss) openBossModal(boss);
};

window.deleteBoss = (id) => {
  if (confirm("ต้องการลบโปรไฟล์บอสตัวนี้หรือไม่?")) {
    state.bosses = state.bosses.filter(b => b.id !== id);
    saveState();
    renderBossCards();
  }
};

// Announcement Preview
function updateHudPreview() {
  const headline = document.getElementById("hud-headline").value;
  const subtitle = document.getElementById("hud-subtitle").value;
  const color = document.getElementById("hud-color").value;
  const banner = document.getElementById("preview-banner");
  if (!banner) return;

  banner.style.borderColor = color;
  banner.style.boxShadow = `0 0 25px ${color}55`;
  document.getElementById("preview-headline").innerText = headline || "ข้อความตัวอย่าง";
  document.getElementById("preview-subtitle").innerText = subtitle || "";
}

function triggerTestAnimation() {
  const banner = document.getElementById("preview-banner");
  if (!banner) return;
  banner.style.animation = "none";
  banner.offsetHeight; // trigger reflow
  banner.style.animation = "bannerSlide 0.35s ease-out";
}

// Remote GAS Functions
async function testGasConnection() {
  const url = document.getElementById("gas-url").value.trim();
  const secret = document.getElementById("gas-secret").value.trim();
  const statusBox = document.getElementById("gas-status");
  if (!url) {
    alert("กรุณากรอก Web App URL ก่อน");
    return;
  }
  statusBox.innerText = "⏳ กำลังทดสอบเชื่อมต่อ Google Apps Script...";
  statusBox.style.color = "var(--gold)";

  try {
    const sep = url.includes("?") ? "&" : "?";
    const res = await fetch(`${url}${sep}action=get_all&secret=${encodeURIComponent(secret)}`);
    const data = await res.json();
    if (data.ok) {
      statusBox.innerText = `✅ เชื่อมต่อสำเร็จ! Config Version: ${data.configVersion} (บอสในระบบ: ${data.bossRegistry ? data.bossRegistry.length : 0} ตัว)`;
      statusBox.style.color = "var(--green)";
      state.config.gasWebAppUrl = url;
      state.config.gasSecretToken = secret;
      saveState();
    } else {
      statusBox.innerText = `❌ Google Apps Script ปฏิเสธ: ${data.error || 'Unknown error'}`;
      statusBox.style.color = "var(--crimson)";
    }
  } catch (err) {
    statusBox.innerText = `❌ ไม่สามารถเข้าถึง URL ได้ (ตรวจสอบสิทธิ์ Deploy Anyone): ${err.message}`;
    statusBox.style.color = "var(--crimson)";
  }
}

async function syncToGas() {
  const url = document.getElementById("gas-url").value.trim();
  const secret = document.getElementById("gas-secret").value.trim();
  if (!url) {
    alert("กรุณากรอก Web App URL ก่อนกดซิงก์");
    return;
  }

  const payload = {
    action: "update_all",
    secret: secret,
    settings: state.config,
    bossRegistry: state.bosses,
    announcementRules: state.rules
  };

  const statusBox = document.getElementById("gas-status");
  statusBox.innerText = "⏳ กำลังอัปโหลดข้อมูลขึ้น Google Sheets...";
  try {
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "text/plain;charset=utf-8" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (data.ok) {
      statusBox.innerText = `✅ ซิงก์ขึ้น Google Sheets สำเร็จ! อัปเดต Version เป็น ${data.configVersion}`;
      statusBox.style.color = "var(--green)";
    } else {
      statusBox.innerText = `❌ ซิงก์ไม่สำเร็จ: ${data.error}`;
      statusBox.style.color = "var(--crimson)";
    }
  } catch (err) {
    statusBox.innerText = `❌ ซิงก์ไม่สำเร็จ: ${err.message}`;
    statusBox.style.color = "var(--crimson)";
  }
}

function escapeHtml(str) {
  return String(str || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// Embedded Server Integration
let isEmbedded = false;

async function checkEmbeddedServer() {
  try {
    const res = await fetch("/api/status");
    if (res.ok) {
      const data = await res.json();
      if (data.ok) {
        isEmbedded = true;
        const badge = document.getElementById("server-badge");
        if (badge) {
          badge.innerText = `🟢 IN-GAME SERVER (PORT: ${data.port || 8088} • ผู้เล่น: ${data.playerCount || 0})`;
          badge.style.color = "var(--green)";
        }
        loadFromEmbeddedServer();
      }
    }
  } catch (ignored) {}
}

async function loadFromEmbeddedServer() {
  try {
    const res = await fetch("/api/bosses");
    if (res.ok) {
      const list = await res.json();
      if (Array.isArray(list) && list.length > 0) {
        state.bosses = list;
        renderBossCards();
      }
    }
  } catch (ignored) {}
}

async function syncToEmbeddedGame() {
  const btn = document.getElementById("btn-sync-game");
  if (btn) btn.innerText = "⏳ กำลังซิงก์ลงเกม...";
  try {
    const token = state.config.gasSecretToken || "DF_SECRET_2026";
    const res = await fetch("/api/bosses", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
      },
      body: JSON.stringify({ token: token, bosses: state.bosses })
    });
    const data = await res.json();
    if (data.ok) {
      alert("✅ " + (data.message || "ซิงก์และ Hot-Reload ในเกมสำเร็จ!"));
    } else {
      alert("❌ ซิงก์ไม่สำเร็จ: " + (data.error || "Unknown error"));
    }
  } catch (e) {
    alert("❌ ไม่สามารถเชื่อมต่อ Embedded Web Server: " + e.message);
  } finally {
    if (btn) btn.innerText = "⚡ ซิงก์ลงเกมสด (In-Game Hot-Reload)";
  }
}

async function sendSpawnNear(id) {
  const token = state.config.gasSecretToken || "DF_SECRET_2026";
  try {
    const res = await fetch("/api/command", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + token
      },
      body: JSON.stringify({ token: token, command: "/dfboss spawnnear " + id })
    });
    const data = await res.json();
    if (data.ok) alert("✅ ส่งคำสั่งเสกบอสใกล้ผู้เล่น 500m เรียบร้อย!");
    else alert("❌ เกิดข้อผิดพลาด: " + data.error);
  } catch (e) {
    alert("❌ ไม่สามารถส่งคำสั่งได้: " + e.message);
  }
}

// ==========================================
// Merge'n'Feel (Mob Stacker) Integration
// ==========================================
async function refreshStackMobStatus() {
  const badge = document.getElementById("stackmob-status-badge");
  const modeBadge = document.getElementById("stackmob-mode-badge");
  const controls = document.getElementById("stackmob-controls");
  const notInstalled = document.getElementById("stackmob-not-installed-msg");
  const select = document.getElementById("stackmob-mode-select");

  if (!badge) return;
  badge.innerText = "⏳ กำลังดึงข้อมูล...";
  badge.style.color = "var(--gold)";

  try {
    const res = await fetch("/api/stackmob");
    if (!res.ok) throw new Error("API returned " + res.status);
    const data = await res.json();

    if (data.installed) {
      badge.innerText = "🟢 ติดตั้งแล้ว (พร้อมทำงาน)";
      badge.style.color = "var(--green)";
      if (controls) controls.style.display = "block";
      if (notInstalled) notInstalled.style.display = "none";

      const mode = data.mode || "NORMAL";
      if (modeBadge) {
        modeBadge.innerText = "MODE: " + mode;
        modeBadge.style.color = mode === "ONE_HIT_KILL" ? "var(--green)" : mode === "NORMAL" ? "var(--gold)" : "var(--crimson)";
      }
      if (select) select.value = mode;
    } else {
      badge.innerText = "⚪ ยังไม่ได้ติดตั้งม็อด";
      badge.style.color = "#94a3b8";
      if (controls) controls.style.display = "none";
      if (notInstalled) notInstalled.style.display = "block";
      if (modeBadge) {
        modeBadge.innerText = "NOT INSTALLED";
        modeBadge.style.color = "#94a3b8";
      }
    }
  } catch (err) {
    badge.innerText = "❌ เกิดข้อผิดพลาด: " + err.message;
    badge.style.color = "var(--crimson)";
  }
}

async function cycleStackMobMode() {
  const token = state.config.gasSecretToken || "DF_SECRET_2026";
  try {
    const res = await fetch("/api/stackmob", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
      body: JSON.stringify({ token: token, action: "cycle" })
    });
    const data = await res.json();
    if (data.ok) {
      alert("✅ " + (data.message || "เปลี่ยนโหมดสำเร็จ"));
      refreshStackMobStatus();
    } else {
      alert("❌ " + (data.error || "เกิดข้อผิดพลาด"));
    }
  } catch (e) {
    alert("❌ ไม่สามารถเชื่อมต่อ API: " + e.message);
  }
}

async function toggleStackMobEnabled() {
  const token = state.config.gasSecretToken || "DF_SECRET_2026";
  try {
    const res = await fetch("/api/stackmob", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
      body: JSON.stringify({ token: token, action: "toggle" })
    });
    const data = await res.json();
    if (data.ok) {
      alert("✅ " + (data.message || "สลับสถานะสำเร็จ"));
      refreshStackMobStatus();
    } else {
      alert("❌ " + (data.error || "เกิดข้อผิดพลาด"));
    }
  } catch (e) {
    alert("❌ ไม่สามารถเชื่อมต่อ API: " + e.message);
  }
}

async function changeStackMobMode() {
  const select = document.getElementById("stackmob-mode-select");
  if (!select) return;
  const targetMode = select.value;
  const token = state.config.gasSecretToken || "DF_SECRET_2026";
  try {
    const res = await fetch("/api/stackmob", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
      body: JSON.stringify({ token: token, action: "setmode", mode: targetMode })
    });
    const data = await res.json();
    if (data.ok) {
      refreshStackMobStatus();
    } else {
      alert("❌ " + (data.error || "ไม่สามารถตั้งค่าโหมดได้"));
    }
  } catch (e) {
    alert("❌ ไม่สามารถเชื่อมต่อ API: " + e.message);
  }
}

async function unstackMobsNearby() {
  const token = state.config.gasSecretToken || "DF_SECRET_2026";
  try {
    const res = await fetch("/api/command", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": "Bearer " + token },
      body: JSON.stringify({ token: token, command: "stackmob toggle" })
    });
    const data = await res.json();
    if (data.ok) {
      alert("✅ ส่งคำสั่งสลับการรวมม็อบเรียบร้อย!");
    } else {
      alert("❌ " + data.error);
    }
  } catch (e) {
    alert("❌ ไม่สามารถส่งคำสั่งได้: " + e.message);
  }
}

window.addEventListener("DOMContentLoaded", () => {
  renderBossCards();
  renderRulesTable();
  updateHudPreview();
  checkEmbeddedServer();
  refreshStackMobStatus();
});
