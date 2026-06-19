package net.marcelto.hytale.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.Map;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.components.TeleportLocationComponent;

public class TeleportationGUI extends InteractiveCustomUIPage<TeleportationGUI.TeleportationData> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private UICommandBuilder command;
  private UIEventBuilder events;

  public TeleportationGUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
    super(playerRef, lifetime, TeleportationData.CODEC);
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder uiCommandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {
    this.command = uiCommandBuilder;
    this.events = eventBuilder;
    this.command.append("Pages/TeleportLocations.ui");

    this.buildList(ref, store);
  }

  private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    PocketPortalDataComponent portalData =
        store.getComponent(ref, PocketPortalDataComponent.getComponentType());

    if (portalData == null || portalData.getportalPositions().isEmpty()) {
      this.command.set("#NoTeleportsMessage.Visible", true);
      return;
    }

    Map<String, TeleportLocationComponent> positions = portalData.getportalPositions();
    this.command.set("#TeleportCount.TextSpans", Message.raw(String.valueOf(positions.size())));

    int i = 0;
    for (TeleportLocationComponent loc : positions.values()) {
      this.command.append("#IndexCards", "Pages/LocationCard.ui");
      this.command.set(
          String.format("#IndexCards[%d] #LocationName.TextSpans", i), Message.raw(loc.getName()));
      this.command.set(
          String.format("#IndexCards[%d] #LocationCoords.TextSpans", i),
          Message.raw(loc.getLocationString()));

      this.events.addEventBinding(
          CustomUIEventBindingType.Activating,
          String.format("#IndexCards[%d] #TeleportButton", i),
          EventData.of("Button", "Teleport:" + loc.getName()),
          false);
      this.events.addEventBinding(
          CustomUIEventBindingType.Activating,
          String.format("#IndexCards[%d] #DeleteButton", i),
          EventData.of("Button", "Delete:" + loc.getName()),
          false);
      i++;
    }
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull TeleportationData data) {
    super.handleDataEvent(ref, store, data);

    if (data.button == null || data.button.isEmpty()) return;

    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
    if (playerRef == null) return;

    if (data.button.startsWith("Teleport:")) {
      String name = data.button.substring("Teleport:".length());
      PocketPortalDataComponent portalData =
          store.getComponent(ref, PocketPortalDataComponent.getComponentType());
      if (portalData == null) return;

      TeleportLocationComponent loc = portalData.getportalPositions().get(name);
      if (loc == null) return;

      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      Rotation3f rotation = transform != null ? transform.getRotation() : new Rotation3f(0, 0, 0);

      store.addComponent(
          ref, Teleport.getComponentType(), new Teleport(loc.getLocation(), rotation));
      this.sendNotificationToPlayer(playerRef, "Teleporting!", "Travelling to '" + name + "'...");
      this.close();
    } else if (data.button.startsWith("Delete:")) {
      String name = data.button.substring("Delete:".length());
      PocketPortalDataComponent portalData =
          store.getComponent(ref, PocketPortalDataComponent.getComponentType());
      if (portalData == null) return;
      portalData.removePortalPosition(name);
      sendNotificationToPlayer(playerRef, "Location Deleted", "'" + name + "' has been removed.");
      this.rebuild();
    }
  }

  private void sendNotificationToPlayer(
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

  public static class TeleportationData {
    static final String KEY_BUTTON = "Button";
    public static final BuilderCodec<TeleportationData> CODEC =
        BuilderCodec.builder(TeleportationData.class, TeleportationData::new)
            .append(
                new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .add()
            .build();
    private String button;
  }
}
