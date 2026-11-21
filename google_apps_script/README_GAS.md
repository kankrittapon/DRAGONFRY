# คู่มือติดตั้ง Google Apps Script Bridge สำหรับ Minecraft Server

สถาปัตยกรรมนี้ใช้ระบบ **Auto-Polling ขาออก (Outbound Polling)** ทำให้เซิร์ฟเวอร์สามารถคุยกับ Google Sheets ได้โดย:
1. **ไม่ต้องเปิดพอร์ตเซิร์ฟเวอร์ (Zero Port-Forwarding)**
2. **ทำงานผ่าน Tailscale หรือเน็ตบ้านได้ 100%**
3. **Hot-Reload ค่าคอนฟิกเซิร์ฟเวอร์ได้ทันทีโดยไม่ต้อง Restart Server**

---

## ขั้นตอนการติดตั้ง (ใช้เวลา 2 นาที)

### 1. สร้าง Google Sheet ใหม่
1. ไปที่ [Google Sheets](https://sheets.new) และสร้าง Spreadsheet ใหม่ (ตั้งชื่อว่า `Dragonfyre Server Bridge`)
2. ไม่ต้องสร้างตารางล่วงหน้า สคริปต์จะสร้างแท็บเหล่านี้ให้อัตโนมัติเมื่อรันครั้งแรก:
   - `Config` — ค่า GameRules, สถานะบอส, Theme, Multipliers
   - `BossRegistry` — รายชื่อ World Boss และม็อบที่โปรโมต
   - `AnnouncementRules` — กฎการยิงป้ายประกาศกลางจอ
   - `CommandQueue` — คิวคำสั่งรีโมต (เสกบอส, ยิงประกาศ, รันคำสั่ง)
   - `EventLog` — ประวัติการได้ของ Epic, การฆ่าบอส, สถานะเซิร์ฟเวอร์

### 2. วางโค้ด Google Apps Script
1. ที่เมนูด้านบนของ Google Sheet ให้คลิก **Extensions (ส่วนขยาย) -> Apps Script**
2. ลบโค้ดเริ่มต้นทั้งหมดในไฟล์ `Code.gs` ออก
3. คัดลอกโค้ดจากไฟล์ `google_apps_script/Code.gs` ไปวางแทนที่ทั้งหมด
4. กดบันทึก (Ctrl + S หรือไอคอนแผ่นดิสก์)

### 3. Deploy เป็น Web App
1. กดปุ่มสีน้ำเงินมุมขวาบน: **Deploy (การทำให้ใช้งานได้) -> New deployment (การทำให้ใช้งานได้ใหม่)**
2. คลิกไอคอนฟันเฟือง เลือกประเภทเป็น **Web app**
3. ตั้งค่าดังนี้:
   - **Description:** `Dragonfyre Server Bridge v1.2.0`
   - **Execute as (ดำเนินการในฐานะ):** `Me (ฉัน / บัญชี Google ของคุณ)`
   - **Who has access (ผู้มีสิทธิ์เข้าถึง):** `Anyone (ทุกคน)` *(สำคัญมาก เพื่อให้ Minecraft Poller ยิงเข้ามาได้)*
4. กด **Deploy** และกดยอมรับสิทธิ์ (Authorize access)
5. คัดลอก **Web App URL** (ตัวอย่าง: `https://script.google.com/macros/s/AKfycb.../exec`)

### 4. นำ URL มาใช้งาน
- นำ URL ไปใส่ใน **Web Studio** (`web-studio/index.html`) แท็บ Remote GAS เพื่อซิงก์ข้อมูล
- นำ URL ไปตั้งในตัวแปร `gasWebAppUrl` ของ Mod หรือส่งผ่านคำสั่ง `/dfboss sync`
