# 🌌 OmniCore: Universal RPG Engine (Forge 1.20.1)

> **The all-in-one RPG & Server Framework for Minecraft.**  
> Turn any mob into an epic World Boss, switch between PvE and PvP with a modern MMORPG HUD, protect party members from friendly fire, and manage your server via an in-game Web Dashboard!

---

### 🌟 Key Features

* **⚔️ Universal World Boss (BossCore):**
  * Promote **any entity from any mod** (or Vanilla) into an epic World Boss (`/omnicore promote`).
  * Features an **8-block Boss Leash Engine** to prevent fleeing, **30-block arena boundary**, and **100% Keep Inventory + Curios protection** inside boss arenas.
  * Two-tier damage cap (5% Max HP cap on regular attacks + 75% telegraphed ultimate) and automatic DPS leaderboard rewards.

* **🗡️ MMORPG Combat & Free PK (CombatCore):**
  * Toggle between **🛡️ SAFE [PVE]** and **⚔️ PVP [ON]** with a single hotkey (`Alt + C`) or command (`/pvp`).
  * **Interactive HUD & Cursor Screen (`Ctrl`):** Hold `Ctrl` to unlock your mouse cursor and click HUD elements directly on-screen.
  * **Free PK System:** Attacking players triggers a 45-second Free PK status where anyone can retaliate without penalty.
  * Full AOE/magic damage filtering (Iron's Spells, Origins, splash potions, summoned minions).

* **🛡️ Zero Friendly Fire (PartyCore):**
  * Native integration with **FTB Teams** and **Vanilla Scoreboard Teams**.
  * Complete protection against accidental party damage from sweeping edge, arrows, projectile spells, and summon minions.

* **🖥️ Embedded Web Studio (ControlCore):**
  * Built-in browser-based admin dashboard accessible directly via local web port.
  * Real-time server telemetry, live boss tracking, and instant toggle for Mob Stacker / Merge'n'Feel.

---

### 💡 100% Server-Side Compatible (Dual-Mode)
* **Vanilla Friendly:** Players can join using a standard Vanilla client or any client without OmniCore installed.
* **Client Features (Optional):** Installing OmniCore on the client unlocks the on-screen PvP HUD, `Ctrl` mouse cursor unlock, and animated RPG boss health bars.

---

### ⌨️ Keybinds & Commands

| Hotkey / Command | Description |
| :--- | :--- |
| **`Alt + C`** | Quick toggle between PvE (Safe) and PvP mode |
| **`Ctrl`** | Hold to unlock mouse cursor for interactive HUD clicking |
| `/pvp` | Toggle PvP status |
| `/omnicore promote [radius]` | Promote targeted entity into a World Boss (Aliases: `/dfboss`, `/wboss`) |
| `/omnicore studio [port]` | Start or inspect the in-game Web Studio control panel |
