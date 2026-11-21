package com.dragonfyre.bridge.network;

import com.dragonfyre.bridge.client.CenterAnnouncementOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record AnnouncementPacket(
        String headline,
        String subtitle,
        int accentColorRgb,
        String animationStyle,
        String soundId,
        int durationTicks
) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(headline, 256);
        buf.writeUtf(subtitle, 256);
        buf.writeInt(accentColorRgb);
        buf.writeUtf(animationStyle, 32);
        buf.writeUtf(soundId, 128);
        buf.writeInt(durationTicks);
    }

    public static AnnouncementPacket decode(FriendlyByteBuf buf) {
        return new AnnouncementPacket(
                buf.readUtf(256),
                buf.readUtf(256),
                buf.readInt(),
                buf.readUtf(32),
                buf.readUtf(128),
                buf.readInt()
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                CenterAnnouncementOverlay.pushAnnouncement(this)
        ));
        ctx.get().setPacketHandled(true);
    }
}
