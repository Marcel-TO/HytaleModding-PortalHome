package net.marcelto.hytale.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import net.marcelto.hytale.PortalHome;

import javax.annotation.Nonnull;

public class PortalHomeCommand extends AbstractPlayerCommand {

    private Vector3d lastPortalPosition;

    public PortalHomeCommand() {
        super("portal-home", "PortalHomeCommand lets you warp home to your spawnpoint.");
    }

    public Vector3d getLastPortalPosition() {
        return lastPortalPosition;
    }

    public void setLastPortalPosition(Vector3d lastPortalPosition) {
        this.lastPortalPosition = lastPortalPosition;
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player player = store.getComponent(ref, Player.getComponentType()); // also a component
        assert player != null;

        // Get player's current position
        Vector3d currentPlayerPosition = player.getPlayerConfigData().lastSavedPosition;
        Vector3d newPosition = new Vector3d(currentPlayerPosition.getX(), currentPlayerPosition.getY(),
                currentPlayerPosition.getZ());
        // Save player's current position before teleporting
        PortalHome.LastPortalPositions.put(player.getDisplayName(), newPosition);
        player.sendMessage(Message.raw("Saved current position: " + newPosition.toString()));

        // Prepare Vector
        var spawnPoint = Player.getRespawnPosition(ref, world.getName(), store);
        player.sendMessage(Message.raw(spawnPoint.toString()));
        Vector3d spawnPointPosition = spawnPoint.getPosition();
        Vector3f spawnPointRotation = spawnPoint.getRotation();
        // Teleport Player
        world.execute(() -> {
            if (player.getReference() == null)
                return;
            Teleport teleport = new Teleport(
                    new Vector3d(spawnPointPosition.getX(), spawnPointPosition.getY(), spawnPointPosition.getZ()),
                    new Vector3f(spawnPointRotation.getX(), spawnPointRotation.getY(), spawnPointRotation.getZ()));
            store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
    }
}
