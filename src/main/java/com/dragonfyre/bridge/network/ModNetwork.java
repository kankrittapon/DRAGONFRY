package com.dragonfyre.bridge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("dragonfyre_bridge", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(AnnouncementPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnnouncementPacket::encode)
                .decoder(AnnouncementPacket::decode)
                .consumerMainThread(AnnouncementPacket::handle)
                .add();

        CHANNEL.messageBuilder(WorldBossSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WorldBossSyncPacket::encode)
                .decoder(WorldBossSyncPacket::decode)
                .consumerMainThread(WorldBossSyncPacket::handle)
                .add();
    }

    public static void sendToAll(Object packet) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
