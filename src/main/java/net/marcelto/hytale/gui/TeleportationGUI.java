package net.marcelto.hytale.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.components.TeleportLocationComponent;
import net.marcelto.hytale.utils.NotifyPlayer;
import org.joml.Vector3d;

public class TeleportationGUI extends InteractiveCustomUIPage<TeleportationGUI.TeleportationData> {
  private UICommandBuilder command;
  private UIEventBuilder events;

  public TeleportationGUI(@Nonnull PlayerRef playerRef, @Nonnull CustomPageLifetime lifetime) {
    super(playerRef, lifetime, TeleportationData.CODEC);
  }

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
    this.buildSpawnCard(ref, store);

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
      this.command.set(
          String.format("#IndexCards[%d] #LocationWorld.TextSpans", i),
          Message.raw(loc.getWorldName()));

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

  private void buildSpawnCard(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return;

    World world = player.getWorld();
    if (world == null) return;

    PlayerRespawnPointData[] respawnPoints =
        player.getPlayerConfigData().getPerWorldData(world.getName()).getRespawnPoints();

    if (respawnPoints == null || respawnPoints.length == 0) return;

    Vector3d spawnPos = respawnPoints[0].getRespawnPosition();
    String coordString =
        String.format(
            "X: %d, Y: %d, Z: %d", (int) spawnPos.x(), (int) spawnPos.y(), (int) spawnPos.z());

    this.command.append("#SpawnCards", "Pages/SpawnCard.ui");
    this.command.set("#SpawnCards[0] #SpawnCoords.TextSpans", Message.raw(coordString));
    this.events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#SpawnCards[0] #TeleportToSpawnButton",
        EventData.of("Button", "Teleport:__spawn__"),
        false);
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

    if (data.button.equals("Teleport:__spawn__")) {
      Player player = store.getComponent(ref, Player.getComponentType());
      if (player == null) return;

      World world = player.getWorld();
      if (world == null) return;

      PlayerRespawnPointData[] respawnPoints =
          player.getPlayerConfigData().getPerWorldData(world.getName()).getRespawnPoints();

      if (respawnPoints == null || respawnPoints.length == 0) {
        NotifyPlayer.SendNotificationToPlayer(
            playerRef,
            "No Spawn Found",
            "Could not find your spawn point.",
            NotifyPlayer.ERRORCOLOR,
            NotifyPlayer.ERRORSECONDARY,
            NotifyPlayer.POCKETPORTALITEM);
        return;
      }

      Vector3d spawnPos = respawnPoints[0].getRespawnPosition();
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      Rotation3f rotation = transform != null ? transform.getRotation() : new Rotation3f(0, 0, 0);

      store.addComponent(ref, Teleport.getComponentType(), new Teleport(spawnPos, rotation));
      NotifyPlayer.SendNotificationToPlayer(
          playerRef,
          "Teleporting Home!",
          "Travelling to your spawn point...",
          NotifyPlayer.POCKETPORTALCOLOR,
          NotifyPlayer.POCKETPORTALSECONDARY,
          NotifyPlayer.POCKETPORTALITEM);
      this.close();
    } else if (data.button.startsWith("Teleport:")) {
      String name = data.button.substring("Teleport:".length());
      PocketPortalDataComponent portalData =
          store.getComponent(ref, PocketPortalDataComponent.getComponentType());
      if (portalData == null) return;

      TeleportLocationComponent loc = portalData.getportalPositions().get(name);
      if (loc == null) return;

      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      Rotation3f rotation = transform != null ? transform.getRotation() : new Rotation3f(0, 0, 0);
      String targetWorldName = loc.getWorldName();
      Teleport teleport;
      if (targetWorldName == null) {
        // Locations saved before world tracking retain their current-world behavior.
        teleport = Teleport.createForPlayer(loc.getLocation(), rotation);
      } else {
        World world = Universe.get().getWorld(targetWorldName);
        if (world == null) {
          loadWorldAndTeleport(ref, store, playerRef, loc, rotation);
          return;
        }
        if (!world.isAlive()) {
          NotifyPlayer.SendNotificationToPlayer(
              playerRef,
              "World Unavailable",
              "The destination world '" + targetWorldName + "' is shutting down.",
              NotifyPlayer.ERRORCOLOR,
              NotifyPlayer.ERRORSECONDARY,
              NotifyPlayer.POCKETPORTALITEM);
          return;
        }
        teleport = Teleport.createForPlayer(world, loc.getLocation(), rotation);
      }

      store.addComponent(ref, Teleport.getComponentType(), teleport);
      NotifyPlayer.SendNotificationToPlayer(
          playerRef,
          "Teleporting!",
          "Travelling to '" + name + "'...",
          NotifyPlayer.POCKETPORTALCOLOR,
          NotifyPlayer.POCKETPORTALSECONDARY,
          NotifyPlayer.POCKETPORTALITEM);
      this.close();
    } else if (data.button.startsWith("Delete:")) {
      String name = data.button.substring("Delete:".length());
      PocketPortalDataComponent portalData =
          store.getComponent(ref, PocketPortalDataComponent.getComponentType());
      if (portalData == null) return;
      portalData.removePortalPosition(name);
      NotifyPlayer.SendNotificationToPlayer(
          playerRef,
          "Location Deleted",
          "'" + name + "' has been removed.",
          NotifyPlayer.POCKETPORTALCOLOR,
          NotifyPlayer.POCKETPORTALSECONDARY,
          NotifyPlayer.POCKETPORTALITEM);
      this.rebuild();
    }
  }

  private void loadWorldAndTeleport(
      Ref<EntityStore> ref,
      Store<EntityStore> store,
      PlayerRef playerRef,
      TeleportLocationComponent location,
      Rotation3f rotation) {
    World sourceWorld = store.getExternalData().getWorld();
    String worldName = location.getWorldName();
    String locationName = location.getName();
    Vector3d destination = new Vector3d(location.getLocation());
    Rotation3f destinationRotation = new Rotation3f(rotation);
    CompletableFuture<World> loading;
    try {
      loading = Universe.get().loadWorld(worldName);
    } catch (IllegalArgumentException error) {
      LOGGER.atSevere().log("Cannot load world '%s': %s", worldName, error);
      NotifyPlayer.SendNotificationToPlayer(
          playerRef,
          "Teleport Failed",
          "The destination world could not be loaded.",
          NotifyPlayer.ERRORCOLOR,
          NotifyPlayer.ERRORSECONDARY,
          NotifyPlayer.POCKETPORTALITEM);
      return;
    }

    NotifyPlayer.SendNotificationToPlayer(
        playerRef,
        "Loading World",
        "Loading the destination world '" + worldName + "'...",
        NotifyPlayer.POCKETPORTALCOLOR,
        NotifyPlayer.POCKETPORTALSECONDARY,
        NotifyPlayer.POCKETPORTALITEM);
    this.close();

    loading
        .whenCompleteAsync(
            (loadedWorld, error) -> {
              if (error != null) {
                LOGGER.atSevere().log("Failed to load world '%s': %s", worldName, error);
              }

              // Loading may finish after the player disconnects or changes worlds.
              if (!ref.isValid() || !ref.equals(playerRef.getReference())) {
                return;
              }
              if (error != null) {
                NotifyPlayer.SendNotificationToPlayer(
                    playerRef,
                    "Teleport Failed",
                    "Could not load the destination world.",
                    NotifyPlayer.ERRORCOLOR,
                    NotifyPlayer.ERRORSECONDARY,
                    NotifyPlayer.POCKETPORTALITEM);
                return;
              }
              if (loadedWorld == null || !loadedWorld.isAlive()) {
                NotifyPlayer.SendNotificationToPlayer(
                    playerRef,
                    "Teleport Failed",
                    "The destination world is unavailable.",
                    NotifyPlayer.ERRORCOLOR,
                    NotifyPlayer.ERRORSECONDARY,
                    NotifyPlayer.POCKETPORTALITEM);
                return;
              }

              store.addComponent(
                  ref,
                  Teleport.getComponentType(),
                  Teleport.createForPlayer(loadedWorld, destination, destinationRotation));
              NotifyPlayer.SendNotificationToPlayer(
                  playerRef,
                  "Teleporting!",
                  "Travelling to '" + locationName + "'...",
                  NotifyPlayer.POCKETPORTALCOLOR,
                  NotifyPlayer.POCKETPORTALSECONDARY,
                  NotifyPlayer.POCKETPORTALITEM);
            },
            sourceWorld)
        .exceptionally(
            error -> {
              LOGGER.atSevere().log(
                  "Failed to complete teleport to world '%s': %s", worldName, error);
              return null;
            });
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
