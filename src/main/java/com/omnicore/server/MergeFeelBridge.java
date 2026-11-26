package com.omnicore.server;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Method;

/**
 * Optional reflection-based bridge to Merge'n'Feel mod (modid: mergefeel).
 * Allows OmniCore Web Studio to inspect and control mob stacking safely.
 * Works seamlessly whether Merge'n'Feel is installed or not (100% soft dependency).
 */
public class MergeFeelBridge {
    private static final Logger LOGGER = LogManager.getLogger("OmniCore-MergeFeelBridge");
    public static final MergeFeelBridge INSTANCE = new MergeFeelBridge();

    private static final String MOD_ID = "mergefeel";
    private static final String RUNTIME_CLASS = "com.bokkchoy.mergefeel.mob.MobStackRuntime";
    private static final String CONFIG_CLASS = "com.bokkchoy.mergefeel.config.MergeConfig";
    private static final String MOB_STACKER_CLASS = "com.bokkchoy.mergefeel.mob.MobStacker";

    private MergeFeelBridge() {}

    public boolean isLoaded() {
        try {
            return ModList.get().isLoaded(MOD_ID);
        } catch (Throwable t) {
            return false;
        }
    }

    public JsonObject getStatusJson() {
        JsonObject json = new JsonObject();
        boolean loaded = isLoaded();
        json.addProperty("ok", true);
        json.addProperty("installed", loaded);

        if (!loaded) {
            json.addProperty("message", "Merge'n'Feel (mergefeel) is not installed on this server.");
            json.addProperty("enabled", false);
            json.addProperty("mode", "NOT_INSTALLED");
            return json;
        }

        try {
            Class<?> runtimeCls = Class.forName(RUNTIME_CLASS);
            Method isEnabledMethod = runtimeCls.getMethod("isEnabled");
            Method getModeMethod = runtimeCls.getMethod("getMode");

            boolean enabled = (boolean) isEnabledMethod.invoke(null);
            Object modeObj = getModeMethod.invoke(null);
            String modeStr = modeObj != null ? modeObj.toString() : "UNKNOWN";

            json.addProperty("enabled", enabled);
            json.addProperty("mode", modeStr);
            json.addProperty("message", "Merge'n'Feel is active.");
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Failed to query MergeFeel status via reflection: {}", t.getMessage());
            json.addProperty("enabled", false);
            json.addProperty("mode", "ERROR");
            json.addProperty("message", "Reflection error: " + t.getMessage());
        }

        return json;
    }

    public boolean setMode(String modeName) {
        if (!isLoaded()) return false;
        try {
            Class<?> runtimeCls = Class.forName(RUNTIME_CLASS);
            Class<?> configCls = Class.forName(CONFIG_CLASS);
            Class<?> modeEnumCls = null;
            for (Class<?> inner : configCls.getDeclaredClasses()) {
                if (inner.getSimpleName().equals("MobStackMode")) {
                    modeEnumCls = inner;
                    break;
                }
            }

            if (modeEnumCls != null) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object targetEnum = Enum.valueOf((Class<Enum>) modeEnumCls, modeName.toUpperCase());
                Method setModeMethod = runtimeCls.getMethod("setMode", modeEnumCls);
                setModeMethod.invoke(null, targetEnum);
                return true;
            }
        } catch (Throwable t) {
            LOGGER.error("[OmniCore] Failed to set MergeFeel mode to {}: {}", modeName, t.getMessage());
        }
        return false;
    }

    public String cycleMode() {
        if (!isLoaded()) return "NOT_INSTALLED";
        try {
            Class<?> runtimeCls = Class.forName(RUNTIME_CLASS);
            Method cycleMethod = runtimeCls.getMethod("cycle");
            Object res = cycleMethod.invoke(null);
            return res != null ? res.toString() : "UNKNOWN";
        } catch (Throwable t) {
            LOGGER.error("[OmniCore] Failed to cycle MergeFeel mode: {}", t.getMessage());
            return "ERROR";
        }
    }

    public boolean toggleEnabled() {
        if (!isLoaded()) return false;
        try {
            Class<?> runtimeCls = Class.forName(RUNTIME_CLASS);
            Method toggleMethod = runtimeCls.getMethod("toggle");
            return (boolean) toggleMethod.invoke(null);
        } catch (Throwable t) {
            LOGGER.error("[OmniCore] Failed to toggle MergeFeel: {}", t.getMessage());
            return false;
        }
    }

    public void triggerUnstackCommand(MinecraftServer mcServer) {
        if (mcServer != null) {
            mcServer.execute(() -> {
                mcServer.getCommands().performPrefixedCommand(
                        mcServer.createCommandSourceStack().withSuppressedOutput(),
                        "stackmob mode normal"
                );
            });
        }
    }
}
