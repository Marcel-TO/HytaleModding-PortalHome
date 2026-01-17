package net.marcelto.hytale;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import net.marcelto.hytale.commands.*;
import net.marcelto.hytale.events.PocketPortalInteraction;
import net.marcelto.hytale.events.PocketPortalSwitching;

import javax.annotation.Nonnull;
import java.util.HashMap;

public class PortalHome extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PortalHome(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initializing PortalHome...");
    }

    public static final HashMap<String, Vector3d> LastPortalPositions = new HashMap<>();

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Registering PortalHome Commands...");
        this.getCommandRegistry().registerCommand(new PortalHomeCommand());
        LOGGER.atInfo().log("Registering PortalHome Interaction...");
        this.getCodecRegistry(Interaction.CODEC).register("PocketPortal", PocketPortalInteraction.class,
                PocketPortalInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("PocketPortal_Switching", PocketPortalSwitching.class,
                PocketPortalSwitching.CODEC);
    }
}
