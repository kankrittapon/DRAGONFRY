# 🐉 DRAGONFRY — Forge 1.20.1 Mod & Server Remote Studio

ระบบจัดการ World Boss ข้าม Mod, ป้ายประกาศกลางจอ RPG HUD และ Google Apps Script Auto-Polling Remote สำหรับ Minecraft Forge 1.20.1 (รองรับ Cisco's Fantasy Medieval RPG [Dragonfyre] / [Ultimate] และ Modpack อื่นๆ ทุกตัว)

> **หมายเหตุ:** โปรเจกต์นี้ได้รับการแปลงจากแอปมือถือเดิมให้เป็น **Forge 1.20.1 Mod Project + Web Studio บน PC** เต็มรูปแบบเรียบร้อยแล้ว ไม่ต้องใช้ Android Studio หรือโทรศัพท์มือถืออีกต่อไป

---

## 📁 โครงสร้างโปรเจกต์ (Project Structure)

```
DRAGONFRY/
├── build.gradle                          # สคริปต์คอมไพล์ Forge 1.20.1 (ForgeGradle 6.0 / Java 17)
├── settings.gradle                       # Gradle Settings
├── gradle.properties                     # ตัวแปรเวอร์ชันและชื่อ Mod
├── gradlew.bat / gradlew                 # Gradle Wrapper สำหรับคอมไพล์ (.jar)
├── src/main/java/com/dragonfyre/bridge/
│   ├── DragonfyreBridgeMod.java          # Main Mod Class & คำสั่ง /dfboss
│   ├── server/
│   │   ├── UniversalBossManager.java     # ระบบ World Boss & โปรโมตม็อบธรรมชาติ
│   │   ├── AnnouncementTriggerHandler.java # ระบบตรวจจับ Loot Epic & Mob Kills
│   │   └── GasHotReloadManager.java      # ระบบคุยกับ Google Sheets แบบ Auto-Polling
│   ├── network/
│   │   ├── ModNetwork.java               # Network SimpleChannel (Server <-> Client)
│   │   ├── AnnouncementPacket.java       # แพ็กเก็ตป้ายประกาศกลางจอ
│   │   └── WorldBossSyncPacket.java      # แพ็กเก็ตเลือดบอส & จุดแดง Xaero
│   └── client/
│       ├── CenterAnnouncementOverlay.java# วาดป้ายประกาศกลางจอ Obsidian + แอนิเมชัน
│       ├── CustomBossBarOverlay.java     # วาดหลอดเลือดบอส Custom + Damage Lag Trail
│       └── XaeroWaypointBridge.java      # เชื่อมต่อ Xaero's World Map ปักจุดแดงอัตโนมัติ
├── google_apps_script/
│   ├── Code.gs                           # สคริปต์ Backend ฝั่ง Google Sheets
│   └── README_GAS.md                     # คู่มือติดตั้ง Google Apps Script แบบละเอียด
├── web-studio/                           # Web Studio แทนที่แอปมือถือ (เปิดได้ทันทีบน PC)
│   ├── index.html                        # หน้า Studio จัดการบอส & ประกาศ
│   ├── styles.css                        # ธีม Dark Fantasy RPG
│   └── app.js                            # ระบบบันทึก, พรีวิว และซิงก์ GAS
└── studio-config/                        # ค่าคอนฟิกเริ่มต้น (JSON)
    ├── config.json                       # ค่าตั้งค่าทั่วไป & GameRules
    ├── bosses.json                       # ข้อมูล World Boss ทั้ง 5 โปรไฟล์
    └── announcement_rules.json           # กฎการยิงป้ายประกาศ
```

---

## 🚀 วิธีใช้งาน 1: ใช้งาน Web Studio บน PC (แทนแอปมือถือเดิม)

คุณสามารถเปิดใช้งาน Web Studio ได้ทันทีบนคอมพิวเตอร์ของคุณ โดย:
1. ดับเบิลคลิกเปิดไฟล์ `web-studio/index.html` ด้วยเบราว์เซอร์ใดก็ได้ (Chrome, Edge, Firefox)
2. **ฟังก์ชันที่ใช้งานได้ทันทีในหน้าเว็บ:**
   - 🛡️ **World Boss Studio:** เพิ่ม/แก้ไขม็อบจาก Mod ใดก็ได้ ปรับตัวคูณ HP, ATK, โอกาสเกิด, บัฟ Affixes, และของรางวัล
   - 📢 **Announcement Studio:** ทดลองยิงป้ายประกาศกลางจอแบบสดๆ ปรับสี ขอบออร่า และเสียงประกอบ
   - 🌐 **Google Script Remote:** วาง Web App URL เพื่อทดสอบการเชื่อมต่อ และกดปุ่ม **ซิงก์ขึ้น Google Sheets** ทันที
   - 📦 **Code Exporter:** ดูโค้ดและดาวน์โหลดไฟล์ Mod ล่าสุดได้ในคลิกเดียว

---

## 🛠️ วิธีใช้งาน 2: คอมไพล์ไฟล์ Mod (.jar)

หากต้องการบิลด์เป็นไฟล์ `.jar` เพื่อนำไปใส่ในโฟลเดอร์ `mods/` ของเซิร์ฟเวอร์และเครื่องผู้เล่น:
1. เปิด Command Prompt หรือ PowerShell ในโฟลเดอร์ `DRAGONFRY`
2. รันคำสั่งคอมไพล์:
   ```powershell
   .\gradlew.bat build
   ```
3. ไฟล์ Mod จะถูกสร้างขึ้นที่:
   ```
   build/libs/dragonfyre_bridge-1.2.0.jar
   ```
4. คัดลอกไฟล์ `.jar` ไปใส่ในโฟลเดอร์ `mods/` ของทั้ง **Server** และ **Client**

---

## 🌐 วิธีใช้งาน 3: ติดตั้ง Google Apps Script (Server Remote)

1. เปิด [Google Sheets](https://sheets.new) ใหม่
2. ไปที่ **Extensions (ส่วนขยาย) -> Apps Script**
3. วางโค้ดจาก `google_apps_script/Code.gs` ลงไปทั้งหมด
4. กด **Deploy -> New deployment -> เลือก Web app**
   - Execute as: **Me**
   - Who has access: **Anyone**
5. คัดลอก **Web App URL** มาใส่ใน **Web Studio** แล้วกด **ซิงก์ขึ้น Google Sheets**
*(ดูรายละเอียดเพิ่มเติมได้ที่ [google_apps_script/README_GAS.md](google_apps_script/README_GAS.md))*

---

## 🎮 คำสั่งแอดมินในเกม Minecraft (`/dfboss`)

เมื่อติดตั้ง Mod แล้ว แอดมินสามารถใช้คำสั่งในเกมได้ทันที:
- `/dfboss list` — แสดงรายชื่อม็อบและบอสทั้งหมดที่อยู่ในระบบ
- `/dfboss spawn <entity_id>` — เสก World Boss ทันที (เช่น `/dfboss spawn cataclysm:ignis` หรือ `/dfboss spawn minecraft:warden`)
- `/dfboss promote <entity_id> <hpMult> <atkMult> <chance%>` — อัปเกรดม็อบตัวใดก็ได้ให้มีโอกาสเกิดเป็น Custom Boss สดๆ โดยไม่ต้องรีสตาร์ทเซิร์ฟเวอร์
- `/dfboss demote <entity_id>` — ถอดม็อบออกจากระบบบอส
- `/dfboss announce <ข้อความ>` — ส่งป้ายประกาศกลางจอให้ผู้เล่นทั้งเซิร์ฟเวอร์
- `/dfboss sync` — บังคับดึงการตั้งค่าล่าสุดจาก Google Sheets ทันที
