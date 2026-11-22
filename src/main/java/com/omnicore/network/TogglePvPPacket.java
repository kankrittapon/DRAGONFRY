package com.omnicore.network;

import com.omnicore.server.PvPManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TogglePvPPacket {
    public TogglePvPPacket() {}

    public static void encode(TogglePvPPacket pkt, FriendlyByteBuf buf) {}

    public static TogglePvPPacket decode(FriendlyByteBuf buf) {
        return new TogglePvPPacket();
    }

    public static void handle(TogglePvPPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PvPManager.INSTANCE.togglePvP(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
