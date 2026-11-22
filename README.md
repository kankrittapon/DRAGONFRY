# 🌌 OmniCore — Universal Minecraft RPG & Server Engine (Forge 1.20.1)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.20+-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**OmniCore** is a universal, all-in-one RPG mechanics and server control framework for **Minecraft Forge 1.20.1**. It is designed from the ground up to be **100% modpack-agnostic**, making it completely portable across any Forge modpack (such as *Cisco's Fantasy Medieval RPG*, *Prominence*, *DawnCraft*, *All The Mods*, or any custom vanilla+ pack).

> 💡 **100% Server-Side Compatible:** Clients without OmniCore installed can connect and play seamlessly with all server features active. Modded clients with OmniCore enjoy extra client features such as the `Alt + C` keybind, real-time Action Bar timers, obsidian HUD banners, and custom boss health bars with damage lag trails.

---

## 🏛️ The 4 Core Pillars of OmniCore

```
                     ┌─────────────────────────────────────────┐
                     │                OMNICORE                 │
                     │    Universal RPG & Server Framework     │
                     └────────────────────┬────────────────────┘
          ┌──────────────────┬────────────┴───────┬──────────────────┐
          ▼                  ▼                    ▼                  ▼
   ┌─────────────┐    ┌─────────────┐      ┌─────────────┐    ┌─────────────┐
   │  BossCore   │    │ CombatCore  │      │  PartyCore  │    │ ControlCore │
   │ World Boss  │    │  PvP & PK   │      │ Zero Damage │    │ Web & Cloud │
   │   Promote   │    │  Alt + C    │      │ FTB/Vanilla │    │ Hot-Reload  │
   └─────────────┘    └─────────────┘      └─────────────┘    └─────────────┘
```

---

### 1. ⚔️ BossCore (Universal World Boss & Mob Promoter)
* **Universal Mob-to-Boss Promoter:** Upgrade any existing living entity from **any mod** (e.g. Ice & Fire, Cataclysm, Aquamirae, Born in Chaos, or Vanilla) into a World Boss using `/omnicore promote [radius]`.
* **Permanent Landmark Boss Areas:** Automatically registers designated Landmark coordinates. World Bosses naturally respawn periodically upon defeat or expiration, with enforced limits: **1 mob type = 1 active boss = 1 area max** (no duplicate overlap).
* **2-Tier Damage Balance:**
  * *Standard Attacks:* Capped at a configurable maximum **5% of player Max HP per hit** — allowing players to duel 30,000+ HP bosses without being unfairly one-shot.
  * *Telegraphed Ultimate (Death Skill):* Bosses telegraph an area-wide destruction skill every ~80s with screen alerts and sounds (`⚠️ [ULTIMATE] Run out of 14 blocks!`). Failure to evade results in a punishing 75% Max HP hit.
* **Keep Inventory in Arena & Respawn Warp:**
  * When players perish inside a World Boss arena, their inventory, equipment, and experience levels are **100% protected** from dropping.
  * Clicking **Respawn** teleports the player directly back to the edge of the arena with 5 seconds of invulnerability (Resistance V) to jump back into action, **without resetting their home bed spawn point**.
* **Zombie Minions & Area Auto-Cleanup:**
  * Bosses summon armored zombie minions during phase transitions (75%, 50%, 25% HP) on a 60-second cooldown (maximum 4 active minions).
  * Upon boss defeat or expiration, all minions despawn immediately and environmental fire is extinguished within 32 blocks.
* **Instanced DPS Leaderboard Loot:**
  * Drops rewards directly into participants' inventories based on damage dealt:
    * 🥇 **Rank 1 (MVP):** Legendary Lootcrate + 3x Champion Coins + 30 EXP Levels.
    * 🥈 **Rank 2 - 3:** Epic Lootcrate + 2x Champion Coins + 20 EXP Levels.
    * 🥉 **Participants (>= 100 DMG):** Rare Lootcrate + 1x Champion Coin + 10 EXP Levels.
* **Custom BossBar & Xaero Map Pins:**
  * Features a custom RPG boss health bar with damage lag trails and real-time HP percentages.
  * Live red boss waypoints automatically appear on Xaero's World Map & Minimap.

---

### 2. 🗡️ CombatCore / PvPCore (PvP Toggle & Free PK Engine)
* **Client Keybind:** Press **`Alt + C`** (configurable in Controls) to toggle PvP mode instantly.
* **Player Name Visual Indicators:**
  * **Peaceful (PvP OFF):** Standard white player name.
  * **PvP Mode ON:** **`§c⚔ [PvP] §fPlayerName`** displayed above the player and in the Tab List.
  * **Free PK (Criminal):** **`§4🗡️ [PK] §cPlayerName`** displayed when attacking another player.
* **Free PK Mechanics:**
  * Anyone can attack a Free PK player without penalty or becoming flagged themselves.
  * Real-time countdown on the Action Bar HUD: `🗡️ [FREE PK] Status expires in: 45s`.
  * **Combat-Lock:** Players flagged as Free PK are blocked from toggling PvP OFF until the 45-second timer elapses.

---

### 3. 🛡️ PartyCore (Friendly Fire Protection)
* Full reflection-based compatibility with **FTB Teams** and **Vanilla Teams**.
* **100% Damage Interception:** Party members cannot damage each other under any circumstances (melee attacks, arrows, magic, splash potions, or summoned minions are canceled).
* Works even when players have PvP or Free PK enabled.

---

### 4. 🖥️ ControlCore (Remote Control & Web Studio)
* **OmniCore Web Studio (PC GUI):**
  * Accessible via web browser at `/studio` or through the server management panel.
  * Create, edit, and fine-tune boss profiles, HP/ATK multipliers, custom loot tables, and announcement banners.
* **Google Apps Script (GAS) Bridge:**
  * Background auto-polling poller syncing server configs and live telemetry to Google Sheets.
  * Perform hot-reloads and execute server commands remotely without server restarts.

---

## 🎮 Command Reference

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/omnicore status` | Everyone | View active World Bosses and Landmark respawn cooldowns |
| `/omnicore list` | Everyone | List all registered boss profiles in the system |
| `/omnicore promote [radius]` | OP Level 2 | Promote target mob into a permanent Landmark World Boss |
| `/omnicore respawnnow <id>` | OP Level 2 | Force spawn a Landmark Boss immediately (testing) |
| `/omnicore system status` | OP Level 2 | Check status of random and natural spawn systems |
| `/omnicore system random <enable\|disable>` | OP Level 2 | Toggle random boss spawning globally |
| `/omnicore system promote <enable\|disable>` | OP Level 2 | Toggle natural mob promotion globally |
| `/omnicore mobs` | OP Level 2 | Scan and dump all registered entities across all mods |
| `/omnicore reload` | OP Level 2 | Hot-reload boss profiles from `config/omnicore_bosses.json` |
| `/omnicore clear` | OP Level 2 | Discard all active bosses and arenas immediately |
| `/pvp [on\|off\|toggle]` | Everyone | Toggle PvP mode (or press `Alt + C`) |

> 📌 **Backward Compatibility Aliases:** The commands `/core`, `/wboss`, and `/dfboss` can be used interchangeably with `/omnicore`.

---

## 📁 Project Directory Structure

```
omnicore/
├── build.gradle                          # ForgeGradle 6.0 build script
├── settings.gradle                       # Gradle settings (rootProject.name = 'omnicore')
├── gradle.properties                     # Mod properties & Forge/MC version mappings
├── gradlew / gradlew.bat                 # Gradle Wrapper scripts
├── src/main/java/com/omnicore/
│   ├── OmniCoreMod.java                  # Main Mod Class & Command Registrations
│   ├── config/
│   │   └── OmniConfig.java               # Forge Common Config Spec
│   ├── server/
│   │   ├── UniversalBossManager.java     # World Boss, Mob Promoter & Landmark Arena Engine
│   │   ├── AnnouncementTriggerHandler.java # Loot & Boss Kill Event Interceptor
│   │   ├── PvPManager.java               # PvP Mode, Free PK & Party Friendly Fire Engine
│   │   ├── GasHotReloadManager.java      # Google Apps Script Polling Engine
│   │   └── EmbeddedStudioServer.java     # Embedded Local Web Server (optional)
│   ├── network/
│   │   ├── ModNetwork.java               # SimpleChannel Network ("omnicore:main")
│   │   ├── AnnouncementPacket.java       # Center Banner Packet
│   │   ├── TogglePvPPacket.java          # Client C2S PvP Toggle Packet (Alt + C)
│   │   └── WorldBossSyncPacket.java      # Boss HP & Waypoint Sync Packet
│   └── client/
│       ├── CenterAnnouncementOverlay.java# Obsidian Banner HUD Overlay
│       ├── CustomBossBarOverlay.java     # Custom Boss Bar Overlay with Lag Trail
│       ├── PvPKeyHandler.java            # Client Keybind Alt + C Handler
│       └── XaeroWaypointBridge.java      # Xaero's World Map & Minimap Integration
├── web-studio/                           # OmniCore Web Studio frontend (HTML/CSS/JS)
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── studio-config/                        # Default JSON configuration profiles
│   ├── config.json
│   ├── bosses.json
│   └── announcement_rules.json
└── google_apps_script/                   # Google Sheets backend code
    ├── Code.gs
    └── README_GAS.md
```

---

## 🛠️ Building & Compiling from Source

### Prerequisites
* **Java Development Kit (JDK) 17** (e.g. Eclipse Adoptium Temurin 17)
* Internet access (for first-time ForgeGradle dependency resolution)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/kankrittapon/DRAGONFRY.git omnicore
cd omnicore

# Build the mod JAR (Windows PowerShell)
$env:JAVA_HOME = "C:\Path\To\jdk-17"
.\gradlew.bat build -x test

# Build the mod JAR (Linux / macOS)
export JAVA_HOME="/path/to/jdk-17"
./gradlew build -x test
```

The compiled mod JAR will be generated in `build/libs/omnicore-1.2.0.jar`. Copy this file into your Minecraft server or client `mods/` directory.

---

## 📄 License
This project is licensed under the **MIT License** — feel free to use, modify, and distribute it in your own modpacks and servers.
