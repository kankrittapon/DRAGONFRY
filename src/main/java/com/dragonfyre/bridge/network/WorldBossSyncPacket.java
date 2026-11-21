package com.dragonfyre.bridge.network;

import com.dragonfyre.bridge.client.CustomBossBarOverlay;
import com.dragonfyre.bridge.client.XaeroWaypointBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record WorldBossSyncPacket(
        UUID bossUuid,
        String displayName,
        String modSourceTag,
        float currentHp,
        float maxHp,
        int x,
        int y,
        int z,
        String dimensionId,
        int remainingSeconds,
        String affixCsv,
        String barTheme,
        boolean xaeroRedMarker,
        boolean active
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(bossUuid);
        buf.writeUtf(displayName, 128);
        buf.writeUtf(modSourceTag, 64);
        buf.writeFloat(currentHp);
        buf.writeFloat(maxHp);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeUtf(dimensionId, 96);
        buf.writeInt(remainingSeconds);
        buf.writeUtf(affixCsv, 128);
        buf.writeUtf(barTheme, 32);
        buf.writeBoolean(xaeroRedMarker);
        buf.writeBoolean(active);
    }

    public static WorldBossSyncPacket decode(FriendlyByteBuf buf) {
        return new WorldBossSyncPacket(
                buf.readUUID(),
                buf.readUtf(128),
                buf.readUtf(64),
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf(96),
                buf.readInt(),
                buf.readUtf(128),
                buf.readUtf(32),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            CustomBossBarOverlay.updateBoss(this);
            if (xaeroRedMarker) {
                if (active) {
                    XaeroWaypointBridge.upsertRedBossWaypoint(bossUuid, displayName, x, y, z, dimensionId);
                } else {
                    XaeroWaypointBridge.removeBossWaypoint(bossUuid);
                }
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
