package net.marcelto.hytale.events;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.marcelto.hytale.PortalHome;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PocketPortalInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<PocketPortalInteraction> CODEC = BuilderCodec.builder(
            PocketPortalInteraction.class, PocketPortalInteraction::new, SimpleInstantInteraction.CODEC).build();

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType,
            @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
        if (commandBuffer == null) {
            interactionContext.getState().state = InteractionState.Failed;
            LOGGER.atInfo().log("CommandBuffer is null");
            return;
        }

        World world = commandBuffer.getExternalData().getWorld();
        Store<EntityStore> store = commandBuffer.getExternalData().getStore();
        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
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
        Vector3d newPosition = new Vector3d(currentPlayerPosition.getX(), currentPlayerPosition.getY(),
                currentPlayerPosition.getZ());
        // Save player's current position before teleporting
        PortalHome.LastPortalPositions.put(player.getDisplayName(), newPosition);
        player.sendMessage(Message.raw("Saved current position: " + newPosition.toString()));

        // Prepare Vector
        PlayerRespawnPointData[] respawnPoints = player.getPlayerConfigData().getPerWorldData(world.getName())
                .getRespawnPoints();

        if (respawnPoints.length == 0) {
            interactionContext.getState().state = InteractionState.Failed;
            player.sendMessage(Message.raw("No respawn points found."));
            LOGGER.atInfo().log("No respawn points found for player: " + player.getDisplayName());
            return;
        }
        player.sendMessage(Message.raw(respawnPoints[0].toString()));
        Vector3d spawnPointPosition = respawnPoints[0].getRespawnPosition();
        Vector3f newRotation = new Vector3f(player.getPlayerConfigData().lastSavedRotation.getX(),
                player.getPlayerConfigData().lastSavedRotation.getY(),
                player.getPlayerConfigData().lastSavedRotation.getZ());

        // Teleport Player
        world.execute(() -> {
            if (player.getReference() == null)
                return;
            Teleport teleport = new Teleport(
                    new Vector3d(spawnPointPosition.getX(), spawnPointPosition.getY(), spawnPointPosition.getZ()),
                    newRotation);
            store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
    }
}
