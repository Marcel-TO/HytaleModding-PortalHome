package net.marcelto.hytale.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.components.TeleportLocationComponent;
import org.joml.Vector3d;

public class SaveLocationGUI extends InteractiveCustomUIPage<SaveLocationGUI.SaveData> {
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final String UI_PATH = "Pages/TeleportLocations.ui";

  private UICommandBuilder command;
  private UIEventBuilder events;
  private String nameInput = "";
  private Vector3d lastStoredPosition;

  public SaveLocationGUI(
      @Nonnull PlayerRef playerRef,
      @Nonnull CustomPageLifetime lifetime,
      Vector3d lastStoredPosition) {
    super(playerRef, lifetime, SaveLocationGUI.SaveData.CODEC);
    this.lastStoredPosition = lastStoredPosition;
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder uiCommandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {
    this.command = uiCommandBuilder;
    this.events = eventBuilder;
    this.command.append("Pages/SaveLocations.ui");

    if (this.lastStoredPosition != null) {
      this.command.set(
          "#CurrentPosition.TextSpans",
          Message.raw(
              String.format(
                  "X: %d  Y: %d  Z: %d",
                  (int) this.lastStoredPosition.x(),
                  (int) this.lastStoredPosition.y(),
                  (int) this.lastStoredPosition.z())));
    }

    this.events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SaveButton",
        EventData.of("Button", "Save").append("@NameValue", "#NameInput.Value"),
        false);
  }

  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull SaveLocationGUI.SaveData data) {
    super.handleDataEvent(ref, store, data);

    if (data.button != null && !data.button.isEmpty()) {
      PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
      assert playerRef != null;

      if (data.button.equals("Save")) {
        this.nameInput = data.nameValue != null ? data.nameValue : "";
        PocketPortalDataComponent portalDataComponent =
            store.getComponent(ref, PocketPortalDataComponent.getComponentType());
        if (this.nameInput.isEmpty()) {
          playerRef.sendMessage(Message.raw("Please enter a name for the location before saving."));
          return;
        } else if (this.lastStoredPosition == null) {
          playerRef.sendMessage(
              Message.raw(
                  "No location to save. Please use a portal to set a location before saving."));
          return;
        } else if (portalDataComponent == null) {
          playerRef.sendMessage(Message.raw("Error: Player data not found. Cannot save location."));
          return;
        }

        portalDataComponent.addPortalPosition(
            new TeleportLocationComponent(this.nameInput, this.lastStoredPosition));
        this.sendNotificationToPlayer(
            playerRef,
            "Location Saved!",
            "Your location '" + this.nameInput + "' has been saved successfully.");
        this.close();
      }
    }
  }

  public static class SaveData {
    static final String KEY_BUTTON = "Button";
    static final String KEY_NAME = "@NameValue";
    public static final BuilderCodec<SaveLocationGUI.SaveData> CODEC =
        BuilderCodec.builder(SaveLocationGUI.SaveData.class, SaveLocationGUI.SaveData::new)
            .append(
                new KeyedCodec<>(KEY_BUTTON, Codec.STRING), (d, s) -> d.button = s, d -> d.button)
            .add()
            .append(
                new KeyedCodec<>(KEY_NAME, Codec.STRING),
                (d, s) -> d.nameValue = s,
                d -> d.nameValue)
            .add()
            .build();
    private String button;
    private String nameValue;
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
}
