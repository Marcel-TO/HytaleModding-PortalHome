package net.marcelto.hytale;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.events.PocketPortalInteraction;
import net.marcelto.hytale.events.PocketPortalSwitching;

import javax.annotation.Nonnull;

public class PortalHome extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static ComponentType<EntityStore, PocketPortalDataComponent> POCKETPORTAL_DATA_COMPONENT;

    public PortalHome(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initializing PortalHome...");
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Registering Components...");
        POCKETPORTAL_DATA_COMPONENT = this.getEntityStoreRegistry().registerComponent(PocketPortalDataComponent.class,
                "PocketPortalDataComponent", PocketPortalDataComponent.CODEC);
        LOGGER.atInfo().log("Registering PortalHome Interaction...");
        this.getCodecRegistry(Interaction.CODEC).register("PocketPortal", PocketPortalInteraction.class,
                PocketPortalInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("PocketPortal_Switching", PocketPortalSwitching.class,
                PocketPortalSwitching.CODEC);
    }
}
