package com.omnicore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * ซิงค์สถานะ PvP และ Free PK Countdown จาก Server ไปยัง Client
 * ทำงานแบบ Dual-Mode (Vanilla Client เชื่อมต่อได้ 100% โดยไม่เกิด Error)
 */
public class SyncPvPStatePacket {
    private final boolean pvpEnabled;
    private final int pkRemainingSeconds;

    public SyncPvPStatePacket(boolean pvpEnabled, int pkRemainingSeconds) {
        this.pvpEnabled = pvpEnabled;
        this.pkRemainingSeconds = pkRemainingSeconds;
    }

    public boolean pvpEnabled() {
        return pvpEnabled;
    }

    public int pkRemainingSeconds() {
        return pkRemainingSeconds;
    }

    public static void encode(SyncPvPStatePacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.pvpEnabled);
        buf.writeInt(pkt.pkRemainingSeconds);
    }

    public static SyncPvPStatePacket decode(FriendlyByteBuf buf) {
        return new SyncPvPStatePacket(buf.readBoolean(), buf.readInt());
    }

    public static void handle(SyncPvPStatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                com.omnicore.client.ClientPvPState.update(pkt.pvpEnabled, pkt.pkRemainingSeconds);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
