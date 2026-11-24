package com.omnicore.server;

import com.omnicore.config.OmniConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Embedded Web Studio Server (All-in-One Server Manager)
 * รัน HTTP Server ตรงจากภายในตัว Mod โดยไม่ต้องพึ่ง Node.js หรือแผงควบคุมภายนอก
 * ใช้พอร์ตที่กำหนดใน dragonfyre_bridge-common.toml (ค่าเริ่มต้น 8088)
 */
public class EmbeddedStudioServer {
    private static final Logger LOGGER = LogManager.getLogger("DragonfyreEmbeddedServer");
    public static final EmbeddedStudioServer INSTANCE = new EmbeddedStudioServer();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HttpServer server;
    private MinecraftServer minecraftServer;
    private int port = 8088;

    private EmbeddedStudioServer() {}

    public synchronized void start(MinecraftServer mcServer) {
        if (!OmniConfig.INSTANCE.webServerEnabled.get()) {
            LOGGER.info("[OmniCore] Embedded Web Studio is disabled in config (.toml).");
            return;
        }

        this.minecraftServer = mcServer;
        this.port = OmniConfig.INSTANCE.webServerPort.get();
        String bindAddr = OmniConfig.INSTANCE.webServerBindAddress.get();

        try {
            stop(); // Ensure any existing instance is stopped

            InetSocketAddress address = new InetSocketAddress(bindAddr, port);
            server = HttpServer.create(address, 0);
            server.setExecutor(Executors.newFixedThreadPool(4));

            // REST API Handlers
            server.createContext("/api/status", this::handleStatus);
            server.createContext("/api/bosses", this::handleBosses);
            server.createContext("/api/mobs", this::handleMobs);
            server.createContext("/api/command", this::handleCommand);

            // Static Web Resource Handler (Serves /web/* from inside jar)
            server.createContext("/", this::handleStatic);

            server.start();
            LOGGER.info("==================================================================");
            LOGGER.info(" [OmniCore] 🐉 Embedded Web Studio is LIVE!");
            LOGGER.info("  URL: http://127.0.0.1:{}/ (or your server IP:{})", port, port);
            LOGGER.info("  Token: {}", OmniConfig.INSTANCE.webServerToken.get());
            LOGGER.info("==================================================================");
        } catch (Exception e) {
            LOGGER.error("[OmniCore] Failed to start Embedded Web Studio on port {}: {}", port, e.getMessage());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            try {
                server.stop(1);
                LOGGER.info("[OmniCore] Embedded Web Studio stopped.");
            } catch (Exception ignored) {}
            server = null;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Security Check                                                      */
    /* ------------------------------------------------------------------ */
    private boolean isAuthorized(HttpExchange exchange, JsonObject body) {
        String configuredToken = OmniConfig.INSTANCE.webServerToken.get();
        if (configuredToken == null || configuredToken.isBlank()) return true;

        // 1. Check Header Authorization: Bearer <token>
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            if (configuredToken.equals(authHeader.substring(7).trim())) return true;
        }

        // 2. Check X-Secret-Token Header
        String secretHeader = exchange.getRequestHeaders().getFirst("X-Secret-Token");
        if (configuredToken.equals(secretHeader)) return true;

        // 3. Check query param ?token=...
        String query = exchange.getRequestURI().getQuery();
        if (query != null && query.contains("token=")) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).equals("token")) {
                    if (configuredToken.equals(pair.substring(eq + 1))) return true;
                }
            }
        }

        // 4. Check JSON body token
        if (body != null && body.has("token") && configuredToken.equals(body.get("token").getAsString())) {
            return true;
        }

        return false;
    }

    /* ------------------------------------------------------------------ */
    /* API: /api/status                                                   */
    /* ------------------------------------------------------------------ */
    private void handleStatus(HttpExchange exchange) throws IOException {
        setCORSHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        JsonObject out = new JsonObject();
        out.addProperty("ok", true);
        out.addProperty("modVersion", "1.2.0");
        out.addProperty("port", port);

        if (minecraftServer != null) {
            out.addProperty("serverMotd", minecraftServer.getMotd());
            out.addProperty("playerCount", minecraftServer.getPlayerCount());
            out.addProperty("maxPlayers", minecraftServer.getMaxPlayers());

            JsonArray playerArray = new JsonArray();
            for (ServerPlayer sp : minecraftServer.getPlayerList().getPlayers()) {
                playerArray.add(sp.getName().getString());
            }
            out.add("players", playerArray);

            // Active Bosses Count
            out.addProperty("activeBossesCount", UniversalBossManager.INSTANCE.getActiveBossCount());
            out.addProperty("registeredBossProfilesCount", UniversalBossManager.INSTANCE.getRegisteredBossCount());
        }

        sendJsonResponse(exchange, 200, out);
    }

    /* ------------------------------------------------------------------ */
    /* API: /api/bosses (GET / POST)                                       */
    /* ------------------------------------------------------------------ */
    private void handleBosses(HttpExchange exchange) throws IOException {
        setCORSHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            File configFile = new File("config/omnicore_bosses.json");
        if (!configFile.exists() && new File("config/dragonfyre_bosses.json").exists()) configFile = new File("config/dragonfyre_bosses.json");
            String jsonContent = "[]";
            if (configFile.exists()) {
                jsonContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            }
            sendRawResponse(exchange, 200, "application/json; charset=utf-8", jsonContent.getBytes(StandardCharsets.UTF_8));
            return;
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonObject body = parseJsonBody(exchange);
            if (!isAuthorized(exchange, body)) {
                sendJsonResponse(exchange, 401, errorJson("Unauthorized - Invalid Security Token"));
                return;
            }

            JsonArray bossesArray = body.has("bosses") && body.get("bosses").isJsonArray()
                    ? body.getAsJsonArray("bosses") : null;

            if (bossesArray == null) {
                sendJsonResponse(exchange, 400, errorJson("bosses array is required"));
                return;
            }

            File configFile = new File("config/omnicore_bosses.json");
        if (!configFile.exists() && new File("config/dragonfyre_bosses.json").exists()) configFile = new File("config/dragonfyre_bosses.json");
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            Files.writeString(configFile.toPath(), GSON.toJson(bossesArray), StandardCharsets.UTF_8);

            // Hot reload in game!
            UniversalBossManager.INSTANCE.loadRegistryFromFile();

            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("message", "บันทึกและ Hot-Reload บอสในเกมเรียบร้อยแล้ว!");
            res.addProperty("count", bossesArray.size());
            sendJsonResponse(exchange, 200, res);
            return;
        }

        sendJsonResponse(exchange, 405, errorJson("Method Not Allowed"));
    }

    /* ------------------------------------------------------------------ */
    /* API: /api/mobs (GET)                                               */
    /* ------------------------------------------------------------------ */
    private void handleMobs(HttpExchange exchange) throws IOException {
        setCORSHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        JsonArray arr = new JsonArray();
        for (Map.Entry<net.minecraft.resources.ResourceKey<net.minecraft.world.entity.EntityType<?>>, net.minecraft.world.entity.EntityType<?>> entry : ForgeRegistries.ENTITY_TYPES.getEntries()) {
            ResourceLocation rl = entry.getKey().location();
            JsonObject obj = new JsonObject();
            obj.addProperty("id", rl.toString());
            obj.addProperty("mod", rl.getNamespace());
            obj.addProperty("name", entry.getValue().getDescription().getString());
            arr.add(obj);
        }

        JsonObject res = new JsonObject();
        res.addProperty("ok", true);
        res.add("mobs", arr);
        sendJsonResponse(exchange, 200, res);
    }

    /* ------------------------------------------------------------------ */
    /* API: /api/command (POST)                                           */
    /* ------------------------------------------------------------------ */
    private void handleCommand(HttpExchange exchange) throws IOException {
        setCORSHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJsonResponse(exchange, 405, errorJson("Method Not Allowed"));
            return;
        }

        JsonObject body = parseJsonBody(exchange);
        if (!isAuthorized(exchange, body)) {
            sendJsonResponse(exchange, 401, errorJson("Unauthorized - Invalid Security Token"));
            return;
        }

        String cmd = body.has("command") ? body.get("command").getAsString().trim() : "";
        if (cmd.isBlank()) {
            sendJsonResponse(exchange, 400, errorJson("command is empty"));
            return;
        }

        if (minecraftServer != null) {
            final String execCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
            minecraftServer.execute(() -> {
                minecraftServer.getCommands().performPrefixedCommand(
                        minecraftServer.createCommandSourceStack().withSuppressedOutput(),
                        execCmd
                );
            });
            JsonObject res = new JsonObject();
            res.addProperty("ok", true);
            res.addProperty("executed", execCmd);
            sendJsonResponse(exchange, 200, res);
        } else {
            sendJsonResponse(exchange, 503, errorJson("Minecraft Server is not ready yet"));
        }
    }

    /* ------------------------------------------------------------------ */
    /* Static Web Handler (Serves /web/ files from Mod Jar or Filesystem)  */
    /* ------------------------------------------------------------------ */
    private InputStream findWebResource(String relativePath) {
        String clean = relativePath.replace("\\", "/").replaceAll("^/+", "");
        if (clean.startsWith("web/")) {
            clean = clean.substring("web/".length());
        }
        if (clean.isEmpty()) clean = "index.html";

        String[] pathsToTry = new String[] {
                "web/" + clean,
                clean
        };

        for (String p : pathsToTry) {
            // Strategy 1: Forge Native ModFile Resource (100% reliable inside Forge 1.20.1 JAR)
            try {
                var modFileInfo = net.minecraftforge.fml.ModList.get().getModFileById("omnicore");
                if (modFileInfo != null && modFileInfo.getFile() != null) {
                    java.nio.file.Path resPath = modFileInfo.getFile().findResource(p.split("/"));
                    if (resPath != null && java.nio.file.Files.exists(resPath)) {
                        return java.nio.file.Files.newInputStream(resPath);
                    }
                }
            } catch (Throwable ignored) {}

            // Strategy 2: ClassLoader without leading slash
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream(p);
                if (is != null) return is;
            } catch (Throwable ignored) {}

            // Strategy 3: Class getResourceAsStream with leading slash
            try {
                InputStream is = getClass().getResourceAsStream("/" + p);
                if (is != null) return is;
            } catch (Throwable ignored) {}

            // Strategy 4: Thread Context ClassLoader
            try {
                InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(p);
                if (is != null) return is;
            } catch (Throwable ignored) {}

            // Strategy 5: Local server / project filesystem fallback
            try {
                String sub = p.startsWith("web/") ? p.substring(4) : p;
                java.io.File[] dirs = new java.io.File[] {
                        new java.io.File("web-studio"),
                        new java.io.File("config/omnicore/web"),
                        new java.io.File("web"),
                        new java.io.File("omnicore/web-studio"),
                        new java.io.File("E:/Minecraft-Server/omnicore/web-studio")
                };
                for (java.io.File dir : dirs) {
                    java.io.File file = new java.io.File(dir, sub);
                    if (file.exists() && file.isFile()) {
                        return new java.io.FileInputStream(file);
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path == null) path = "/";

        // Aliases support: strip /studio or /web
        if (path.startsWith("/studio")) {
            path = path.substring("/studio".length());
        }
        if (path.startsWith("/web")) {
            path = path.substring("/web".length());
        }

        path = path.replace("\\", "/").replaceAll("/+", "/");
        if (path.contains("..")) {
            sendRawResponse(exchange, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }

        InputStream is = findWebResource(path);
        if (is == null && !path.endsWith(".js") && !path.endsWith(".css") && !path.endsWith(".png") && !path.endsWith(".ico")) {
            // SPA fallback
            is = findWebResource("/index.html");
            path = "/index.html";
        }

        if (is == null) {
            sendRawResponse(exchange, 404, "text/plain; charset=utf-8", "Web Studio resource not found in jar".getBytes(StandardCharsets.UTF_8));
            return;
        }

        byte[] bytes = readAllBytes(is);
        String mime = getMimeType(path);
        sendRawResponse(exchange, 200, mime, bytes);
    }

    /* ------------------------------------------------------------------ */
    /* Utility Methods                                                     */
    /* ------------------------------------------------------------------ */
    private void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Secret-Token");
    }

    private void sendJsonResponse(HttpExchange exchange, int status, JsonObject json) throws IOException {
        byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
        sendRawResponse(exchange, status, "application/json; charset=utf-8", bytes);
    }

    private void sendRawResponse(HttpExchange exchange, int status, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private JsonObject parseJsonBody(HttpExchange exchange) {
        try (InputStream is = exchange.getRequestBody()) {
            String text = new String(readAllBytes(is), StandardCharsets.UTF_8);
            if (text.isBlank()) return new JsonObject();
            JsonElement el = JsonParser.parseString(text);
            return el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private JsonObject errorJson(String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("ok", false);
        obj.addProperty("error", message);
        return obj;
    }

    private byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (path.endsWith(".json")) return "application/json; charset=utf-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
