# 🌌 OmniCore — Universal Minecraft RPG & Server Framework (Forge 1.20.1)

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.20+-orange.svg)](https://files.minecraftforge.net/)
[![Java](https://img.shields.io/badge/Java-17-red.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> 🌐 **Language / ภาษา:**  
> **[ 🇹🇭 ภาษาไทย (Thai Documentation) ](#-ภาษาไทย--thai-documentation)** &nbsp;|&nbsp; **[ 🇬🇧 English (English Documentation) ](#-english-documentation)**

---

<a name="-ภาษาไทย--thai-documentation"></a>
# 🇹🇭 ภาษาไทย — คู่มือการใช้งาน OmniCore

**OmniCore** คือ Framework และ Mod ควบคุมเซิร์ฟเวอร์แนว MMORPG / RPG แบบครบวงจรสำหรับ **Minecraft Forge 1.20.1**  
ถูกออกแบบให้เป็น **100% Modpack-Agnostic** (ทำงานได้กับทุก Modpack บนโลก ไม่ว่าจะเป็น *Cisco's Medieval RPG*, *Prominence*, *DawnCraft*, *All The Mods* หรือ Vanilla+ Custom)

> 💡 **100% Server-Side Compatible (Dual-Mode):**  
> ผู้เล่นสามารถเข้าเล่นเซิร์ฟเวอร์ได้ด้วย **Vanilla Client** หรือ Client ที่ไม่มีตัวม็อด OmniCore ได้ 100% โดยไม่หลุดและไม่ติด Error ใดๆ ทั้งสิ้น!  
> หากผู้เล่นติดตั้ง OmniCore ในเครื่อง Client จะได้รับฟังก์ชันเสริมระดับพรีเมียมทันที: **Interactive PvP HUD Overlay**, ปปลดล็อกเมาส์คลิก UI ด้วยปุ่ม **`Ctrl`**, ปุ่มลัด **`Alt + C`**, และหลอดเลือดบอสแบบแอนิเมชัน RPG

---

## 🏛️ สถาปัตยกรรม 4 เสาหลัก (The 4 Core Pillars)

```
                     ┌─────────────────────────────────────────┐
                     │                OMNICORE                 │
                     │    Universal RPG & Server Framework     │
                     └────────────────────┬────────────────────┘
          ┌──────────────────┬────────────┴───────┬──────────────────┐
          ▼                  ▼                    ▼                  ▼
   ┌─────────────┐    ┌─────────────┐      ┌─────────────┐    ┌─────────────┐
   │  BossCore   │    │ CombatCore  │      │  PartyCore  │    │ ControlCore │
   │ World Boss  │    │  PvP & PK   │      │ Zero Damage │    │ Web Studio  │
   │  Leash & KG │    │ HUD & Cursor│      │ FTB/Vanilla │    │ GAS Cloud   │
   └─────────────┘    └─────────────┘      └─────────────┘    └─────────────┘
```

---

### 1. ⚔️ BossCore (ระบบบอสโลกสากล & เลื่อนขั้นม็อบ)

* **Universal Mob-to-Boss Promoter:**  
  เลื่อนขั้นสิ่งมีชีวิต (Living Entity) ทุกชนิดจาก **ม็อดใดก็ได้** ในเซิร์ฟเวอร์ (เช่น *Cataclysm*, *Ice and Fire*, *Aquamirae*, *Born in Chaos* หรือ Vanilla) ให้กลายเป็น World Boss ประจำพื้นที่ด้วยคำสั่ง `/omnicore promote [radius]` (หรือใช้ Alias ย่อ `/dfboss`, `/wboss`, `/core`)
* **ระบบคัดกรองบอสอัจฉริยะ (Smart Boss Validation & Anti-Spam):**  
  * กรองและตัดม็อบลูกน้อง (Minions / Adds), สัตว์อัญเชิญ, นก, และสัตว์ป่า (เช่น Umvuthana Crane, Twilight Birds, Minion spawns) ออกจากระบบประกาศกลางจออย่างเด็ดขาด
  * กำหนดเกณฑ์เลือดขั้นต่ำ (**Minimum 80 HP**) และตรวจสอบ Blacklist/Whitelist คำนำหน้า เพื่อให้มั่นใจว่าจะประกาศเฉพาะ "บอสตัวจริง" ไม่สแปมหน้าจอผู้เล่น
* **ข้อจำกัดความปลอดภัยระดับสากล (Global Anti-Overlap & Duplicate Guard):**  
  * **1 ม็อบ 1 ชนิด = สามารถเลื่อนขั้นเป็นบอสได้ 1 ตัวเท่านั้น** และ **1 พื้นที่ (Area) = มีบอสได้ 1 ตัวเท่านั้น** ไม่สามารถเกิดซ้ำหรือซ้อนทับกันได้
* **ระบบเชือกพันธนาการบอส (8-Block Boss Leash Engine):**  
  * ป้องกันบอสเดินเตลิดหนีออกจากพื้นที่ต่อสู้! หากบอสเคลื่อนที่ห่างจากจุดเกิดเกิน **8 บล็อก** (ปรับได้ใน config) ตัวบอสจะถูกแรงโน้มถ่วงลากและเทเลพอร์ตกลับสู่ใจกลางจุดเกิด พร้อมทั้งได้รับ Speed Boost ชั่วคราวเพื่อกลับมาสู้ต่อทันที
* **อาณาเขตสังเวียนบอส (30-Block Arena Bounds):**  
  * รัศมีพื้นที่ต่อสู้ถูกจำกัดให้อยู่ในระยะ **30 บล็อก** รอบตัวบอส เพื่อให้การต่อสู้กระชับและดุเดือด
* **การปกป้องไอเทม 100% (Keep Inventory & Curios Protection):**  
  * เมื่อผู้เล่นพลาดท่าเสียชีวิตในเขตสังเวียนบอส ไอเทมในตัว, ชุดเกราะ, อาวุธ, ค่าประสบการณ์ (EXP), รวมถึง **ไอเทมในช่อง Curios / Accessories ทั้งหมดจะไม่ตก 100%**
  * เมื่อกดปุ่ม **เกิดใหม่ (Respawn)** ระบบจะวาร์ปผู้เล่นกลับมายังขอบสนามต่อสู้ทันที พร้อมมอบสถานะอมตะ (Resistance V) เป็นเวลา 5 วินาที โดยที่ **ไม่ทับจุดเกิดเตียงนอน (Home Bed)** เดิมของผู้เล่น
* **ระบบสมดุลดาเมจ 2 ชั้น (Two-Tier Damage Cap):**  
  * *การโจมตีปกติ:* ดาเมจถูกล็อกเพดานไว้ที่สูงสุด **5% ของ Max HP ผู้เล่น** ทำให้สามารถต่อสู้กับบอสเลือดหลักแสนได้อย่างสมศักดิ์ศรีโดยไม่โดน One-shot ตายในทีเดียว
  * *ท่าไม้ตายสะเทือนโลก (Telegraphed Ultimate):* บอสจะร่ายท่าระเบิดพื้นที่ทุกๆ ~80 วินาที พร้อมข้อความเตือนกลางจอและเสียงเอฟเฟกต์นับถอยหลัง หากผู้เล่นไม่วิ่งหนีออกจากระยะ 14 บล็อก จะได้รับความเสียหายรุนแรง 75% Max HP
* **ระบบสแกนพื้นผิวอัจฉริยะ (Dimension-Aware Surface Scanner):**  
  * สแกนจุดเกิดบอสอย่างปลอดภัย:
    * **Nether:** สแกนถ้ำด้านล่างระหว่าง Y=95 ถึง Y=32 เพื่อป้องกันไม่ให้บอสเกิดบนหลังคาหิน Bedrock
    * **The End:** ตรวจสอบพื้นผิวเกาะ ป้องกันไม่ให้บอสหรือผู้เล่นตกลงไปใน Void
    * **Overworld:** กรองไม่ให้เกิดในน้ำ ลาวา หรือใต้ถ้ำทึบ
* **ระบบของรางวัล DPS Leaderboard & Fallback อัตโนมัติ:**  
  * แจกของรางวัลเข้าตัวผู้เล่นโดยตรงตามสัดส่วนดาเมจที่ทำได้ (MVP อันดับ 1, 2-3, และผู้ร่วมต่อสู้) พร้อมโบนัส Last-Hit
  * **Dynamic Fallback:** หากเซิร์ฟเวอร์ไม่มีม็อด `cisco_mod` หรือ `ftbquests` ระบบจะสลับมาแจกไอเทม Vanilla ที่มีค่าสูง (`Netherite Scrap`, `Diamond`, `Emerald`) ให้อัตโนมัติ 100%

---

### 2. 🗡️ CombatCore (ระบบสลับโหมด PvP & Free PK สไตล์ MMORPG)

* **Interactive HUD Badge & Cursor Screen:**  
  * แสดงสัญลักษณ์สถานะมุมจอตลอดเวลา:
    * 🛡️ **SAFE [PVE]** (สีเขียว): โหมดสงบ ไม่สามารถตีผู้เล่นอื่นได้ และไม่ถูกผู้เล่นอื่นโจมตี
    * ⚔️ **PVP [ON]** (สีแดง): เปิดโหมดต่อสู้ ชื่อกลายเป็น `§c⚔ [PvP] PlayerName`
    * 🗡️ **FREE PK** (สีแดงเข้มกะพริบ): ติดสถานะ Free PK พร้อมเวลานับถอยหลัง `🗡 PK [45s]`
  * **ปุ่มปลดล็อกเคอร์เซอร์เมาส์ (`Ctrl`):**  
    * กดปุ่ม `Ctrl` (หรือปุ่มที่ตั้งไว้) เพื่อเปิด **Transparent Cursor Screen** หน้าจอจะโปร่งใส 100% ให้คุณใช้เมาส์คลิกที่ปุ่ม PvP บน HUD ได้ทันทีเหมือนเกม MMORPG
    * เมื่อปล่อยปุ่ม `Ctrl` หน้าจอจะปิดลงและกลับสู่มุมมองสายตาปกติทันที!
  * **ปุ่มลัดสลับด่วน:** สามารถกด `Alt + C` หรือพิมพ์คำสั่ง `/pvp` ได้ตลอดเวลา
* **ระบบเจาะลึกที่มาของดาเมจ (Recursive Damage Source Resolution):**  
  * ป้องกันและยกเลิกความเสียหาย 100% ครอบคลุมทุกม็อดเวทมนตร์และพลังพิเศษ เช่น **Iron's Spells 'n Spellbooks**, **Origins Mod**, ธนูเวทมนตร์, ยาพิษตกค้าง, และละอองไอเวท (AOE / Lingering Clouds)
  * ตรวจสอบแหล่งที่มาแท้จริงของผู้เล่นที่ร่ายเวทหรือเสกดาเมจ แม้จะส่งผ่านม็อบอัญเชิญ (Vex, Zombie, Golem, Minions) หรือโปรเจกไทล์หลายชั้น
* **กลไกปลดความแค้นม็อบ (DropMobAggro Mechanism):**  
  * เมื่อสัตว์เลี้ยง, โกเลม, หรือมินเนี่ยนพยายามเข้าตีผู้เล่นที่อยู่ในโหมดปลอดภัย (SAFE PVE) ตัวระบบจะสั่งล้างเป้าหมาย (`setTarget(null)`) ทันที ป้องกันปัญหาม็อบยืนตอดหรือรุมไล่ตีไม่ยอมหยุด
* **การปกป้องสัตว์เลี้ยงและมินเนี่ยน (Pet & Summon Protection):**  
  * ผู้เล่นในโหมดสงบจะไม่สามารถทำร้ายสัตว์เลี้ยงของผู้เล่นอื่นได้ และสัตว์เลี้ยงจะไม่ทำร้ายผู้เล่นโหมดสงบ
* **กลไก Free PK (45 วินาที):**  
  * เมื่อผู้เล่นที่เปิด PvP เริ่มโจมตีผู้เล่นอื่น จะได้รับสถานะ **🗡️ Free PK เป็นเวลา 45 วินาที** (มีสัญลักษณ์ดาบนำหน้าชื่อ)
  * ในช่วง 45 วินาทีนี้ **ผู้เล่นทุกคนในเซิร์ฟเวอร์สามารถรุมโจมตีผู้เล่นคนนี้ได้ฟรี** โดยผู้ที่ตีจะไม่ติดโทษ
  * **Combat Lock:** ผู้ที่ติดสถานะ Free PK จะไม่สามารถกดปิด PvP หนีได้จนกว่าจะหมดเวลา 45 วินาที

---

### 3. 🛡️ PartyCore (ป้องกัน Friendly Fire สมาชิกปาร์ตี้ 100%)

* รองรับการเชื่อมโยงอัตโนมัติผ่าน Reflection กับ:
  * **FTB Teams API**
  * **Vanilla Scoreboard Teams**
* **ยกเลิกดาเมจระหว่างเพื่อนร่วมทีม 100%:**  
  สมาชิกในปาร์ตี้เดียวกันจะไม่สามารถสร้างความเสียหายให้แก่กันได้ ไม่ว่าจะเกิดจากการฟัน, ธนู, เวทมนตร์ลูกโซ่, คลื่นพลัง Origins, ยาพิษ, หรือสมุนที่เรียกออกมา แม้ว่าจะเปิดโหมด PvP ค้างไว้ก็ตาม

---

### 4. 🖥️ ControlCore (ระบบประกาศกลางจอ, Web Studio & Cloud Sync)

* **การแจ้งเตือนกลางจออัจฉริยะ (Smart Banner Announcements):**  
  * **Apotheosis Top-2 Tier Rule:** กรองการประกาศไอเทมดรอป จะแจ้งเตือนเฉพาะ 2 ระดับสูงสุดเท่านั้น (**Mythic** และ **Ancient** / **Heirloom**) ไอเทมระดับ Common, Uncommon, Rare จะไม่ส่งเสียงหรือขึ้นบังหน้าจอ
  * **Artifacts & Relics First Discovery:** เมื่อผู้เล่นค้นพบ Artifact ชิ้นใหม่เป็นครั้งแรก จะมีการประกาศยินดีกลางจอ แต่เมื่อเก็บได้ชิ้นเดิมซ้ำเป็นครั้งที่สอง จะไม่มีการประกาศซ้ำ (บันทึกข้อมูลแยกรายคนใน NBT)
  * **Granular Announcement Toggles:** คำสั่งเปิด/ปิดการประกาศการเกิดของบอสโลกแยกตามต้องการ (`/omnicore announce mob <enable|disable|toggle>`)
* **ระบบปิดเซิร์ฟเวอร์คลีนหมดจด (Clean JVM Shutdown & Daemon Threads):**  
  * Thread Pool ภายใน `EmbeddedStudioServer` ทำงานบน Daemon Thread 100% พร้อมคำสั่ง `shutdownNow()` เมื่อเซิร์ฟเวอร์สั่งปิด ป้องกันปัญหา Stuck Thread หรือเซิร์ฟเวอร์ค้างจาก `AllTheLeaks` (`pool-19-thread-*`)
* **OmniCore Web Studio:** หน้าเว็บควบคุมเซิร์ฟเวอร์ผ่าน Browser จัดการโปรไฟล์บอส, อัตราคูณ HP/ATK, และกฎการประกาศ
* **Google Apps Script (GAS) Cloud Sync:** เชื่อมต่อฐานข้อมูล Google Sheets อัปเดตและ Hot-Reload การตั้งค่าโดยไม่ต้องรีสตาร์ตเซิร์ฟเวอร์

---

## 🎮 ตารางคำสั่งทั้งหมด (Commands Reference)

> 💡 **Command Aliases:** คุณสามารถใช้คำสั่งย่อ `/dfboss`, `/wboss`, หรือ `/core` แทนคำสั่ง `/omnicore` ได้ทุกคำสั่ง!

| คำสั่ง (Command) | ระดับสิทธิ์ | คำอธิบาย |
| :--- | :--- | :--- |
| `/omnicore status` | ผู้เล่นทุกคน | ตรวจสอบรายชื่อ World Boss ที่ยังมีชีวิต และเวลานับถอยหลังของ Landmark Boss |
| `/omnicore list` | ผู้เล่นทุกคน | ดูรายชื่อโปรไฟล์บอสทั้งหมดที่มีในระบบ |
| `/omnicore artifacts list` | ผู้เล่นทุกคน | ดูรายชื่อ Artifacts และ Relics ทั้งหมดที่คุณเคยค้นพบแล้ว |
| `/omnicore artifacts reset` | OP Level 2 | รีเซ็ตประวัติการค้นพบ Artifacts ของตัวเอง (สำหรับทดสอบ) |
| `/omnicore promote [radius]` | OP Level 2 | ส่องเป้าหมายม็อบที่มองอยู่ แล้วเลื่อนขั้นเป็น Landmark World Boss ถาวร |
| `/omnicore respawnnow <id>` | OP Level 2 | บังคับเกิดบอส Landmark ตัวที่ระบุทันที |
| `/omnicore announce mob <enable\|disable\|toggle>` | OP Level 2 | เปิด/ปิด/สลับ การประกาศข้อความกลางจอเมื่อมีบอสหรือม็อบเกิด |
| `/omnicore announce toggle` | OP Level 2 | เปิด/ปิดการแจ้งเตือนกลางจอทั้งหมด |
| `/omnicore system status` | OP Level 2 | ตรวจสอบสถานะการเปิด/ปิดของระบบบอสสุ่ม และระบบเลื่อนขั้นอัตโนมัติ |
| `/omnicore system random <enable\|disable>` | OP Level 2 | เปิด/ปิด ระบบสุ่มเกิดบอสโลกอัตโนมัติ |
| `/omnicore system promote <enable\|disable>` | OP Level 2 | เปิด/ปิด ระบบเลื่อนขั้นม็อบตามธรรมชาติ |
| `/omnicore mobs` | OP Level 2 | สแกนและส่งออกรายชื่อม็อบทั้งหมดในเซิร์ฟเวอร์ลงไฟล์ JSON |
| `/omnicore reload` | OP Level 2 | รีโหลดการตั้งค่าและโปรไฟล์บอสจาก `config/omnicore_bosses.json` ทันที |
| `/omnicore clear` | OP Level 2 | ลบบอสโลกและอารีน่าที่เปิดใช้งานอยู่ทั้งหมดทันที |
| `/pvp [on\|off\|toggle]` | ผู้เล่นทุกคน | สลับโหมดต่อสู้ PvP (หรือใช้ปุ่ม `Ctrl` คลิก HUD หรือกด `Alt + C`) |

---
---

<a name="-english-documentation"></a>
# 🇬🇧 English — OmniCore Complete Documentation

**OmniCore** is a universal, all-in-one MMORPG-style server mechanics and gameplay control framework built for **Minecraft Forge 1.20.1**.  
Engineered from the ground up to be **100% Modpack-Agnostic**, it is plug-and-play across any Forge modpack (*Cisco's Fantasy Medieval RPG*, *Prominence*, *DawnCraft*, *All The Mods*, or custom vanilla+ packs).

> 💡 **100% Server-Side Compatible (Dual-Mode):**  
> Vanilla clients and modded clients without OmniCore can connect to the server without any crashes, errors, or disconnection issues!  
> Clients that have the `omnicore.jar` installed in their `mods/` directory unlock client-side features: **Interactive PvP HUD Overlay**, mouse cursor unlock via **`Left Ctrl`**, quick-toggle **`Alt + C`**, and animated RPG boss bars with lag damage trails.

---

## 🏛️ The 4 Core Pillars

### 1. ⚔️ BossCore (Universal World Boss & Mob Promoter)

* **Universal Mob-to-Boss Promoter:**  
  Promote any living mob from **any installed mod** (Cataclysm, Ice and Fire, Aquamirae, Born in Chaos, Vanilla, etc.) into an instanced World Boss using `/omnicore promote [radius]` (or shorthand aliases `/dfboss`, `/wboss`, `/core`).
* **Smart Boss Validation & Anti-Spam Noise Reduction:**  
  * Filters out minions, adds, summoned creatures, critters, and decorative mobs (e.g., Umvuthana Cranes, Twilight Forest birds, minion adds) from center-screen announcements.
  * Enforces an **80 HP minimum threshold** and targeted regex/tag whitelists (`bosses`, `cataclysm`, `boss`, `dragon`, `titan`, `elder`, `wither`, `warden`, etc.) so only legitimate world bosses broadcast globally.
* **Global Anti-Overlap & Duplicate Guard:**  
  * **1 mob type = 1 active boss max**, and **1 designated area = 1 active boss max**. Prevents duplicate spawns and stacked bosses.
* **8-Block Leash Tethering Engine:**  
  * Bosses cannot stray far from their spawn location. Moving beyond **8 blocks** (configurable) triggers a tether pull and instant reposition back to the arena center with a brief recovery speed boost.
* **30-Block Arena Bounds:**  
  * Combat is focused within a 30-block circular zone around the boss.
* **100% Keep-Inventory & Curios Protection:**  
  * Dying within a World Boss arena guarantees zero item drops: main inventory, armor, offhand, experience levels, and **all Curios / Accessories slots** are 100% protected.
  * Clicking **Respawn** teleports players directly to the edge of the arena with Resistance V (5 seconds), **without overwriting their home bed spawn point**.
* **Two-Tier Damage Scaling:**  
  * *Standard Attacks:* Capped at **5% of player Max HP per hit**, preventing unfair one-shots against high-tier bosses.
  * *Telegraphed Ultimate:* Boss charges an arena-wide shockwave every ~80s with warning audio and title alerts (`⚠️ [ULTIMATE] Run out of 14 blocks!`). Failure to evade results in a devastating 75% Max HP hit.
* **Dimension-Aware Surface Scanner:**  
  * Prevents Nether roof bedrock spawns (scans cavern floors between Y=95 and Y=32).
  * Prevents The End void drops (verifies solid island surface above Y=20).
  * Prevents Overworld water/lava and buried underground suffocation spawns.
* **DPS Leaderboard & Dynamic Item Fallback:**  
  * Instanced rewards delivered directly to players' inventories based on damage dealt (MVP, Top 2-3, participants >= 100 DMG) and Last-Hit bonus.
  * **Dynamic Fallback:** If `cisco_mod` or `ftbquests` are not present, automatically awards high-tier Vanilla items (`Netherite Scrap`, `Diamond`, `Emerald`).

---

### 2. 🗡️ CombatCore (PvP Toggle & Free PK Engine)

* **Interactive PvP HUD Badge & Cursor Screen:**  
  * Always-visible HUD badge displaying current combat state:
    * 🛡️ **SAFE [PVE]** (Emerald Green): Peaceful mode. Cannot attack players; cannot be damaged by players.
    * ⚔️ **PVP [ON]** (Crimson Red): PvP enabled. Name displays `§c⚔ [PvP] PlayerName`.
    * 🗡️ **FREE PK** (Flashing Amber/Red): Free PK flag active with countdown `🗡 PK [45s]`.
  * **Mouse Cursor Unlock (`Ctrl`):**  
    * Press and hold `Left Ctrl` (configurable) to summon a **100% transparent screen** that frees your mouse cursor to click the PvP badge directly, exactly like modern MMORPGs.
    * Release `Ctrl` to immediately resume camera control.
  * **Quick Shortcuts:** Instant toggle with `Alt + C` or `/pvp`.
* **Recursive Damage Source Resolution:**  
  * Complete 100% damage interception across complex third-party magic and ability mods, including **Iron's Spells 'n Spellbooks**, **Origins Mod**, summoned minions, indirect AOE magic, and lingering potion clouds.
  * Recursively unwraps direct entities to identify the true player attacker regardless of nested projectile or summon layers.
* **DropMobAggro Mechanism:**  
  * Summons, tamed pets, and minions instantly lose aggro (`setTarget(null)`) when attempting to target a peaceful player or teammate, preventing annoying target-locking loops.
* **Pet & Minion Grief Protection:**  
  * Peaceful players cannot harm or be harmed by other players' tamed pets, summons, or familiars.
* **Free PK Mechanics (45-Second Penalty):**  
  * Striking another player immediately inflicts the **Free PK flag for 45 seconds**.
  * Any player on the server may attack a Free PK player without receiving penalties.
  * **Combat Lock:** Players cannot disable PvP while their Free PK timer is active.

---

### 3. 🛡️ PartyCore (Friendly Fire Protection)

* Seamless reflection-based integration with:
  * **FTB Teams API**
  * **Vanilla Scoreboard Teams**
* **100% Damage Interception:**  
  Party members cannot harm each other under any circumstance (swords, bows, magic, Origins abilities, area-of-effect spells, potions, or minion summons), even if both players have PvP mode enabled.

---

### 4. 🖥️ ControlCore (Remote Control, Announcements & Web Studio)

* **Smart Center-Screen Announcements:**  
  * **Apotheosis Top-2 Tier Rule:** Only the top 2 highest gear rarities (**Mythic** and **Ancient** / **Heirloom**) trigger center-screen alerts.
  * **Artifact & Relic First Discovery:** Celebrating unique artifact drops on their first discovery; duplicate finds remain completely silent.
  * **Granular Announcement Controls:** Administrators can toggle boss and mob broadcast banners on or off via `/omnicore announce mob <enable|disable|toggle>`.
* **Clean JVM Shutdown Architecture:**  
  * Embedded Web Studio HTTP executor runs exclusively on daemon threads with explicit `shutdownNow()` triggers upon server shutdown, completely eliminating `AllTheLeaks` stuck thread warnings (`pool-19-thread-*`).
* **OmniCore Web Studio:** Embedded and external web dashboard for managing profiles, HP/ATK multipliers, and rules.
* **Google Apps Script Cloud Sync:** Remote synchronization with Google Sheets for telemetry and hot-reloads without server restarts.

---

## 🎮 Commands Reference

> 💡 **Command Aliases:** You can use `/dfboss`, `/wboss`, or `/core` in place of `/omnicore` for any command!

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/omnicore status` | Everyone | View active World Bosses and landmark countdowns |
| `/omnicore list` | Everyone | List all configured boss profiles |
| `/omnicore artifacts list` | Everyone | View all artifacts/relics discovered by you |
| `/omnicore artifacts reset` | OP Level 2 | Reset your artifact discovery history (for testing) |
| `/omnicore promote [radius]` | OP Level 2 | Promote targeted living entity into a permanent Landmark World Boss |
| `/omnicore respawnnow <id>` | OP Level 2 | Immediately force-spawn a designated Landmark Boss |
| `/omnicore announce mob <enable\|disable\|toggle>` | OP Level 2 | Enable/disable/toggle center-screen banners for mob & boss spawns |
| `/omnicore announce toggle` | OP Level 2 | Toggle all center-screen announcement banners |
| `/omnicore system status` | OP Level 2 | Check status of random boss & auto-promote systems |
| `/omnicore system random <enable\|disable>` | OP Level 2 | Toggle automatic world boss random spawn system |
| `/omnicore system promote <enable\|disable>` | OP Level 2 | Toggle natural mob-to-boss promotion |
| `/omnicore mobs` | OP Level 2 | Scan and export all server entity IDs into a JSON file |
| `/omnicore reload` | OP Level 2 | Hot-reload boss configs and rules from `config/omnicore_bosses.json` |
| `/omnicore clear` | OP Level 2 | Clear and despawn all active world bosses and arenas |
| `/pvp [on\|off\|toggle]` | Everyone | Toggle PvP mode (or press `Ctrl` to click HUD, or `Alt + C`) |

---

## 🛠️ Compilation & Installation

```bash
# Clone repository
git clone https://github.com/kankrittapon/DRAGONFRY.git omnicore
cd omnicore

# Build with Gradle (Java 17)
$env:JAVA_HOME = "C:\Path\To\jdk-17"
.\gradlew.bat build -x test
```

Place the compiled `build/libs/omnicore-1.2.0.jar` into your server's `mods/` directory.

---

## 📄 License
Licensed under the **MIT License**. Free for public and private servers and modpacks.
