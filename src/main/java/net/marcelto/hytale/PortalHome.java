package net.marcelto.hytale;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.interactions.TeleportHomeInteraction;
import net.marcelto.hytale.interactions.TeleportLastPositionInteraction;
import net.marcelto.hytale.systems.HudTickingSystem;
import net.marcelto.hytale.systems.PlayerJoinSystem;

public class PortalHome extends JavaPlugin {

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public static ComponentType<EntityStore, PocketPortalDataComponent> POCKETPORTAL_DATA_COMPONENT;

  public PortalHome(@Nonnull JavaPluginInit init) {
    super(init);
    LOGGER.atInfo().log("Initializing PortalHome...");
  }

  @Override
  protected void setup() {
    // Components
    LOGGER.atInfo().log("Registering Components...");
    POCKETPORTAL_DATA_COMPONENT =
        this.getEntityStoreRegistry()
            .registerComponent(
                PocketPortalDataComponent.class,
                "PocketPortalDataComponent",
                PocketPortalDataComponent.CODEC);

    // Interactions
    LOGGER.atInfo().log("Registering PortalHome Interaction...");
    this.getCodecRegistry(Interaction.CODEC)
        .register(
            "PocketPortal_Home", TeleportHomeInteraction.class, TeleportHomeInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC)
        .register(
            "PocketPortal_LastPos",
            TeleportLastPositionInteraction.class,
            TeleportLastPositionInteraction.CODEC);

    // Systems
    this.getEntityStoreRegistry().registerSystem(new HudTickingSystem());
    this.getEntityStoreRegistry().registerSystem(new PlayerJoinSystem());
  }
}
