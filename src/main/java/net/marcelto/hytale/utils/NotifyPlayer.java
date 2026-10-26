package net.marcelto.hytale.utils;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import javax.annotation.Nonnull;

public class NotifyPlayer {
  public static final String POCKETPORTALCOLOR = "#c300ff";
  public static final String POCKETPORTALSECONDARY = "#c29cf3";
  public static final String POCKETPORTALITEM = "PocketPortal";

  public static final String GOBLINPORTALCOLOR = "#7ef02c";
  public static final String GOBLINPORTALSECONDARY = "#45910f";
  public static final String GOBLINPORTALITEM = "GoblinPortal";

  public static final String ERRORCOLOR = "#ff0000";
  public static final String ERRORSECONDARY = "#e00000";

  public static void SendNotificationToPlayer(
      @Nonnull PlayerRef playerRef,
      String message,
      String submessage,
      String color,
      String secondaryColor,
      String itemId) {
    var packetHandler = playerRef.getPacketHandler();
    var primaryMessage = Message.raw(message).color(color);
    var secondaryMessage = Message.raw(submessage).color(secondaryColor);
    var icon = new ItemStack(itemId, 1).toPacket();
    NotificationUtil.sendNotification(packetHandler, primaryMessage, secondaryMessage, icon);
  }
}
