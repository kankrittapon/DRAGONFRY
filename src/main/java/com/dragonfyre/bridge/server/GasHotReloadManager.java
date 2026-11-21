package com.dragonfyre.bridge.server;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/**
 * ระบบ Google Apps Script Auto-Polling & Hot-Reload Server Settings (ไม่ต้อง Restart Server!)
 * - ทำงานบน Background Thread ไม่ดึง TPS เซิร์ฟเวอร์
 * - นำค่าที่เปลี่ยนไปอัปเดต GameRules, Difficulty, Boss Scaling, Universal Boss Registry และคิวคำสั่งบน Main Thread อย่างปลอดภัย
 */
public class GasHotReloadManager {
    public static final GasHotReloadManager INSTANCE = new GasHotReloadManager();

    private String gasWebAppUrl = "";
    private String secretToken = "DF_SECRET_2026";
    private int pollIntervalSeconds = 20;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private ScheduledExecutorService scheduler;
    private String lastConfigVersion = "";

    public void start(MinecraftServer server) {
        stop();
        if (gasWebAppUrl == null || gasWebAppUrl.isBlank() || gasWebAppUrl.contains("REPLACE_WITH_YOUR_DEPLOY_ID")) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Dragonfyre-GAS-Poller");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(() -> pollNow(server), 5, pollIntervalSeconds, TimeUnit.SECONDS);
    }

    public void triggerImmediatePoll(MinecraftServer server) {
        CompletableFuture.runAsync(() -> pollNow(server));
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void configure(String url, String secret, int interval) {
        this.gasWebAppUrl = url != null ? url.trim() : "";
        this.secretToken = secret != null ? secret.trim() : "DF_SECRET_2026";
        this.pollIntervalSeconds = Math.max(10, Math.min(300, interval));
    }

    private void pollNow(MinecraftServer server) {
        if (gasWebAppUrl == null || gasWebAppUrl.isBlank()) return;
        try {
            String sep = gasWebAppUrl.contains("?") ? "&" : "?";
            String url = gasWebAppUrl + sep + "action=poll&secret=" + URLEncoder.encode(secretToken, StandardCharsets.UTF_8)
                    + "&lastVersion=" + URLEncoder.encode(lastConfigVersion, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return;

            JsonObject json = JsonParser.parseString(res.body()).getAsJsonObject();
            if (!json.has("ok") || !json.get("ok").getAsBoolean()) return;

            boolean changed = json.has("configChanged") && json.get("configChanged").getAsBoolean();
            String newVersion = json.has("configVersion") ? json.get("configVersion").getAsString() : lastConfigVersion;
            JsonObject settings = changed && json.has("settings") && json.get("settings").isJsonObject()
                    ? json.getAsJsonObject("settings") : null;
            JsonArray bossArr = changed && json.has("bossRegistry") && json.get("bossRegistry").isJsonArray()
                    ? json.getAsJsonArray("bossRegistry") : null;
            JsonArray commands = json.has("pendingCommands") && json.get("pendingCommands").isJsonArray()
                    ? json.getAsJsonArray("pendingCommands") : null;

            server.execute(() -> {
                if (settings != null) {
                    applyHotReloadSettings(server, settings);
                    lastConfigVersion = newVersion;
                }
                if (bossArr != null) {
                    applyBossRegistryHotReload(bossArr);
                }
                if (commands != null && !commands.isEmpty()) {
                    List<String> executedIds = executeCommandQueue(server, commands);
                    if (!executedIds.isEmpty()) {
                        ackCommandsAsync(executedIds);
                    }
                }
            });
        } catch (Exception ignored) {
            // Silent retry on next polling cycle
        }
    }

    private void applyHotReloadSettings(MinecraftServer server, JsonObject s) {
        GameRules rules = server.getGameRules();
        if (s.has("keepInventory")) rules.getRule(GameRules.RULE_KEEPINVENTORY).set(s.get("keepInventory").getAsBoolean(), server);
        if (s.has("mobGriefing")) rules.getRule(GameRules.RULE_MOBGRIEFING).set(s.get("mobGriefing").getAsBoolean(), server);
        if (s.has("doDaylightCycle")) rules.getRule(GameRules.RULE_DAYLIGHT).set(s.get("doDaylightCycle").getAsBoolean(), server);
        if (s.has("pvpEnabled")) server.setPvpAllowed(s.get("pvpEnabled").getAsBoolean());
        if (s.has("serverDifficulty")) {
            Difficulty diff = Difficulty.byName(s.get("serverDifficulty").getAsString().toLowerCase(Locale.ROOT));
            if (diff != null) server.setDifficulty(diff, true);
        }
        if (s.has("worldBossSystemEnabled")) UniversalBossManager.INSTANCE.worldBossSystemEnabled = s.get("worldBossSystemEnabled").getAsBoolean();
        if (s.has("autoPromoteMobSystemEnabled")) UniversalBossManager.INSTANCE.autoPromoteSystemEnabled = s.get("autoPromoteMobSystemEnabled").getAsBoolean();
        if (s.has("globalBossHpScale")) UniversalBossManager.INSTANCE.globalHpScale = s.get("globalBossHpScale").getAsFloat();
        if (s.has("globalBossAtkScale")) UniversalBossManager.INSTANCE.globalAtkScale = s.get("globalBossAtkScale").getAsFloat();
        if (s.has("bossBarTheme")) UniversalBossManager.INSTANCE.activeBarTheme = s.get("bossBarTheme").getAsString();
        if (s.has("announcementsEnabled")) AnnouncementTriggerHandler.INSTANCE.announcementsEnabled = s.get("announcementsEnabled").getAsBoolean();
        if (s.has("minLootRarity")) AnnouncementTriggerHandler.INSTANCE.minLootRarity = s.get("minLootRarity").getAsString();
    }

    private void applyBossRegistryHotReload(JsonArray arr) {
        List<UniversalBossManager.BossProfile> list = new ArrayList<>();
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            JsonObject o = el.getAsJsonObject();
            list.add(new UniversalBossManager.BossProfile(
                    str(o, "displayName", "World Boss"),
                    str(o, "entityId", "minecraft:warden"),
                    str(o, "modPackTag", "Universal"),
                    str(o, "bossMode", "BOTH"),
                    numInt(o, "promotionChancePercent", 100),
                    numFloat(o, "hpMultiplier", 3.0f),
                    numFloat(o, "attackMultiplier", 1.8f),
                    numFloat(o, "armorBonus", 10f),
                    numFloat(o, "speedMultiplier", 1.1f),
                    numInt(o, "spawnIntervalMinutes", 45),
                    numInt(o, "fightDurationMinutes", 20),
                    numInt(o, "minSpawnRadius", 200),
                    numInt(o, "maxSpawnRadius", 1200),
                    str(o, "dimensionId", "minecraft:overworld"),
                    bool(o, "glowingAura", true),
                    bool(o, "xaeroRedMarker", true),
                    str(o, "affixTraits", "ENRAGED"),
                    str(o, "rewardCommand", ""),
                    bool(o, "enabled", true)
            ));
        }
        UniversalBossManager.INSTANCE.replaceRegistryFromGas(list);
    }

    private List<String> executeCommandQueue(MinecraftServer server, JsonArray commands) {
        List<String> done = new ArrayList<>();
        for (JsonElement el : commands) {
            if (!el.isJsonObject()) continue;
            JsonObject cmd = el.getAsJsonObject();
            String id = str(cmd, "commandId", "");
            String type = str(cmd, "commandType", "");
            JsonObject payload = cmd.has("payload") && cmd.get("payload").isJsonObject()
                    ? cmd.getAsJsonObject("payload") : new JsonObject();

            switch (type) {
                case "BROADCAST_ANNOUNCEMENT" -> AnnouncementTriggerHandler.broadcastCustom(
                        str(payload, "headline", "📢 ประกาศจากเซิร์ฟเวอร์"),
                        str(payload, "subtitle", ""),
                        0xF59E0B, "RUNE_GLOW", "minecraft:block.bell.use", 120
                );
                case "SPAWN_BOSS_NOW" -> UniversalBossManager.INSTANCE.spawnWorldBossNow(server, str(payload, "entityId", ""));
                case "DESPAWN_BOSS" -> UniversalBossManager.INSTANCE.despawnAllActiveBosses(server);
                case "RUN_SERVER_COMMAND" -> {
                    String raw = str(payload, "command", "");
                    if (!raw.isBlank()) {
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), raw);
                    }
                }
            }
            if (!id.isBlank()) done.add(id);
        }
        return done;
    }

    private void ackCommandsAsync(List<String> ids) {
        JsonObject body = new JsonObject();
        body.addProperty("action", "ack_commands");
        body.addProperty("secret", secretToken);
        JsonArray arr = new JsonArray();
        ids.forEach(arr::add);
        body.add("commandIds", arr);
        postJsonAsync(body);
    }

    public void logEventAsync(String eventType, String player, String detail) {
        JsonObject body = new JsonObject();
        body.addProperty("action", "log_event");
        body.addProperty("secret", secretToken);
        body.addProperty("eventType", eventType);
        body.addProperty("player", player);
        body.addProperty("detail", detail);
        postJsonAsync(body);
    }

    private void postJsonAsync(JsonObject body) {
        if (gasWebAppUrl == null || gasWebAppUrl.isBlank() || gasWebAppUrl.contains("REPLACE_WITH_YOUR_DEPLOY_ID")) return;
        HttpRequest req = HttpRequest.newBuilder(URI.create(gasWebAppUrl))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        http.sendAsync(req, HttpResponse.BodyHandlers.discarding());
    }

    private static String str(JsonObject o, String k, String def) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def; }
    private static int numInt(JsonObject o, String k, int def) { try { return o.has(k) ? o.get(k).getAsInt() : def; } catch (Exception e) { return def; } }
    private static float numFloat(JsonObject o, String k, float def) { try { return o.has(k) ? o.get(k).getAsFloat() : def; } catch (Exception e) { return def; } }
    private static boolean bool(JsonObject o, String k, boolean def) { try { return o.has(k) ? o.get(k).getAsBoolean() : def; } catch (Exception e) { return def; } }
}
