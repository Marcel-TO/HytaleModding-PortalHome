package net.marcelto.hytale.utils;

import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import javax.annotation.Nonnull;

public class NotifyPlayer {
  public static void SendNotificationToPlayer(
      @Nonnull PlayerRef playerRef, String message, String submessage) {
    String color = "#c300ff";
    String secondaryColor = "#c29cf3";
    var packetHandler = playerRef.getPacketHandler();
    var primaryMessage = Message.raw(message).color(color);
    var secondaryMessage = Message.raw(submessage).color(secondaryColor);
    var icon = new ItemStack("PocketPortal", 1).toPacket();
    NotificationUtil.sendNotification(
        packetHandler, primaryMessage, secondaryMessage, (ItemWithAllMetadata) icon);
  }
}
