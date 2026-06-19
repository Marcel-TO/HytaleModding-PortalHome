package net.marcelto.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.interactions.SaveLocationInteraction;
import net.marcelto.hytale.interactions.TeleportInteraction;
import net.marcelto.hytale.systems.HudTickingSystem;
import net.marcelto.hytale.systems.PlayerJoinSystem;

public class PortalHome extends JavaPlugin {

  public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  public PortalHome(@Nonnull JavaPluginInit init) {
    super(init);
    LOGGER.atInfo().log("Initializing PortalHome...");
  }

  @Override
  protected void setup() {
    // Components
    LOGGER.atInfo().log("Registering Components...");
    var pocketPortalComponent =
        this.getEntityStoreRegistry()
            .registerComponent(
                PocketPortalDataComponent.class,
                "PocketPortalDataComponent",
                PocketPortalDataComponent.CODEC);
    PocketPortalDataComponent.setComponentType(pocketPortalComponent);

    // Interactions
    LOGGER.atInfo().log("Registering PortalHome Interaction...");
    this.getCodecRegistry(Interaction.CODEC)
        .register(
            "PocketPortal_SaveLocation",
            SaveLocationInteraction.class,
            SaveLocationInteraction.CODEC);
    this.getCodecRegistry(Interaction.CODEC)
        .register(
            "PocketPortal_TeleportInteraction",
            TeleportInteraction.class,
            TeleportInteraction.CODEC);

    // Systems
    this.getEntityStoreRegistry().registerSystem(new HudTickingSystem());
    this.getEntityStoreRegistry().registerSystem(new PlayerJoinSystem());
  }
}
