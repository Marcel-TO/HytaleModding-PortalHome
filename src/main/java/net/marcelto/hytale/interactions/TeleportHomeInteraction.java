package net.marcelto.hytale.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.marcelto.hytale.PortalHome;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import org.joml.Vector3d;

public class TeleportHomeInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<TeleportHomeInteraction> CODEC =
      BuilderCodec.builder(
              TeleportHomeInteraction.class,
              TeleportHomeInteraction::new,
              SimpleInstantInteraction.CODEC)
          .build();

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  @Override
  protected void firstRun(
      @Nonnull InteractionType interactionType,
      @Nonnull InteractionContext interactionContext,
      @Nonnull CooldownHandler cooldownHandler) {
    CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
    if (commandBuffer == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("CommandBuffer is null");
      return;
    }

    World world = commandBuffer.getExternalData().getWorld();
    Ref<EntityStore> ref = interactionContext.getEntity();

    // Ensure PlayerDataComponent exists
    PocketPortalDataComponent portaldata = new PocketPortalDataComponent();
    if (commandBuffer.getComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT) != null) {
      portaldata = commandBuffer.getComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT);
    } else {
      commandBuffer.putComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT, portaldata);
    }

    if (portaldata == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("Pocket portal data component is null");
      return;
    }

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    UUIDComponent playerUid = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
    if (player == null || playerRef == null || playerUid == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("Player is null");
      return;
    }

    ItemStack itemStack = interactionContext.getHeldItem();
    if (itemStack == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("ItemStack is null");
      return;
    }

    // Get player's current position
    Vector3d currentPlayerPosition = player.getPlayerConfigData().lastSavedPosition;
    Vector3d newPosition =
        new Vector3d(
            currentPlayerPosition.x(), currentPlayerPosition.y(), currentPlayerPosition.z());

    // Save player's current position before teleporting
    Map<String, Vector3d> LastPortalPositions = new HashMap<>(portaldata.getLastPortalPositions());
    LastPortalPositions.put(playerUid.getUuid().toString(), newPosition);
    portaldata.setLastPortalPositions(LastPortalPositions);

    sendNotificationToPlayer(playerRef, "Saved last position", newPosition.toString());

    // Prepare Vector
    PlayerRespawnPointData[] respawnPoints =
        player.getPlayerConfigData().getPerWorldData(world.getName()).getRespawnPoints();

    if (respawnPoints == null) {
      respawnPoints = new PlayerRespawnPointData[0];
    }

    if (respawnPoints.length == 0) {
      interactionContext.getState().state = InteractionState.Failed;
      playerRef.sendMessage(Message.raw("No respawn points found."));
      LOGGER.atInfo().log("No respawn points found for player: " + playerRef.getUsername());
      return;
    }
    playerRef.sendMessage(Message.raw(respawnPoints[0].toString()));
    Vector3d spawnPointPosition = respawnPoints[0].getRespawnPosition();
    Rotation3f newRotation =
        new Rotation3f(
            player.getPlayerConfigData().lastSavedRotation.x(),
            player.getPlayerConfigData().lastSavedRotation.y(),
            player.getPlayerConfigData().lastSavedRotation.z());

    // Teleport Player
    world.execute(
        () -> {
          if (player.getReference() == null) return;
          Teleport teleport =
              new Teleport(
                  new Vector3d(
                      spawnPointPosition.x(), spawnPointPosition.y(), spawnPointPosition.z()),
                  newRotation);
          commandBuffer.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
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
