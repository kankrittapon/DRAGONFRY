package com.omnicore.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Soft-Dependency Bridge สำหรับ Curios API (Forge 1.20.1)
 * ป้องกันไอเทมเครื่องประดับในช่อง Curios หายเมื่อผู้เล่นเสียชีวิตในเขต World Boss Arena
 * โดยมีระบบป้องกันแบบ 2 ชั้น (Double-Layer Protection):
 * 1. ดัก DropRulesEvent เพื่อ Override ทุกช่อง Curios ให้เป็น ALWAYS_KEEP
 * 2. บันทึกและกู้คืน CompoundTag ในช่วง Clone และ Respawn ผ่าน NBT Serialization
 */
public class CuriosBridge {
    private static final Logger LOGGER = LogManager.getLogger("OmniCoreCurios");
    private static boolean initialized = false;

    public static boolean isCuriosLoaded() {
        return ModList.get().isLoaded("curios");
    }

    public static void init() {
        if (!isCuriosLoaded()) {
            LOGGER.info("[OmniCore] Curios mod not detected; Curios bridge disabled.");
            return;
        }

        if (initialized) return;
        initialized = true;

        try {
            @SuppressWarnings("unchecked")
            Class<Event> dropRulesClass = (Class<Event>) Class.forName("top.theillusivec4.curios.api.event.DropRulesEvent");
            MinecraftForge.EVENT_BUS.addListener(
                    EventPriority.HIGHEST,
                    false,
                    dropRulesClass,
                    CuriosBridge::onDropRules
            );
            LOGGER.info("[OmniCore] Curios DropRulesEvent listener registered successfully.");
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Failed to register Curios DropRulesEvent listener: {}", t.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static void onDropRules(Event event) {
        try {
            Method getEntityMethod = event.getClass().getMethod("getEntity");
            Object entity = getEntityMethod.invoke(event);
            if (entity instanceof ServerPlayer player) {
                if (UniversalBossManager.INSTANCE.hasSavedInventory(player.getUUID())
                        || UniversalBossManager.INSTANCE.isInsideActiveArena(player.serverLevel().dimension(), player.blockPosition())) {
                    Class<?> dropRuleClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICurio$DropRule");
                    Object alwaysKeep = Enum.valueOf((Class<Enum>) dropRuleClass, "ALWAYS_KEEP");
                    Method addOverrideMethod = event.getClass().getMethod("addOverride", Predicate.class, dropRuleClass);
                    addOverrideMethod.invoke(event, (Predicate<ItemStack>) (stack -> true), alwaysKeep);
                    LOGGER.info("[OmniCore] Overrode Curios DropRule to ALWAYS_KEEP for player {}", player.getName().getString());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Error in Curios onDropRules: {}", t.getMessage());
        }
    }

    public static CompoundTag saveCurios(ServerPlayer player) {
        if (!isCuriosLoaded()) return null;

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInvMethod = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazyOpt = getCuriosInvMethod.invoke(null, player);
            if (lazyOpt instanceof LazyOptional<?> opt) {
                Object handler = opt.orElse(null);
                if (handler != null) {
                    Method writeTagMethod = handler.getClass().getMethod("writeTag");
                    Object tag = writeTagMethod.invoke(handler);
                    if (tag instanceof CompoundTag compound) {
                        LOGGER.info("[OmniCore] Saved Curios inventory snapshot for player {}", player.getName().getString());
                        return compound;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Failed to save Curios for player {}: {}", player.getName().getString(), t.getMessage());
        }
        return null;
    }

    public static void restoreCurios(ServerPlayer player, CompoundTag tag) {
        if (!isCuriosLoaded() || tag == null) return;

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInvMethod = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazyOpt = getCuriosInvMethod.invoke(null, player);
            if (lazyOpt instanceof LazyOptional<?> opt) {
                Object handler = opt.orElse(null);
                if (handler != null) {
                    Method readTagMethod = handler.getClass().getMethod("readTag", Tag.class);
                    readTagMethod.invoke(handler, tag);
                    LOGGER.info("[OmniCore] Restored Curios inventory snapshot for player {}", player.getName().getString());
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Failed to restore Curios for player {}: {}", player.getName().getString(), t.getMessage());
        }
    }

    public static void syncCurios(ServerPlayer player) {
        if (!isCuriosLoaded()) return;

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Method getCuriosInvMethod = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazyOpt = getCuriosInvMethod.invoke(null, player);
            if (lazyOpt instanceof LazyOptional<?> opt) {
                Object handler = opt.orElse(null);
                if (handler != null) {
                    Method getCuriosMap = handler.getClass().getMethod("getCurios");
                    Object curiosMap = getCuriosMap.invoke(handler);

                    Class<?> sPacketSyncCurios = Class.forName("top.theillusivec4.curios.common.network.server.sync.SPacketSyncCurios");
                    Constructor<?> ctor = sPacketSyncCurios.getConstructor(int.class, Map.class);
                    Object packet = ctor.newInstance(player.getId(), curiosMap);

                    Class<?> netHandler = Class.forName("top.theillusivec4.curios.common.network.NetworkHandler");
                    Field instField = netHandler.getField("INSTANCE");
                    SimpleChannel channel = (SimpleChannel) instField.get(null);
                    if (channel != null) {
                        channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet);
                        LOGGER.info("[OmniCore] Synced Curios inventory to client for player {}", player.getName().getString());
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.warn("[OmniCore] Failed to sync Curios for player {}: {}", player.getName().getString(), t.getMessage());
        }
    }
}
