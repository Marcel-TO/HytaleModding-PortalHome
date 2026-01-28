package net.marcelto.hytale.events;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import net.marcelto.hytale.PortalHome;
import net.marcelto.hytale.components.PocketPortalDataComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PocketPortalSwitching extends SimpleInstantInteraction {
    public static final BuilderCodec<PocketPortalSwitching> CODEC = BuilderCodec.builder(
            PocketPortalSwitching.class, PocketPortalSwitching::new, SimpleInstantInteraction.CODEC).build();

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
        Ref<EntityStore> ref = interactionContext.getEntity();

        // Ensure PlayerDataComponent exists
        PocketPortalDataComponent portaldata = new PocketPortalDataComponent();
        if (commandBuffer.getComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT) != null) {
            portaldata = commandBuffer.getComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT);
        } else {
            commandBuffer.putComponent(ref, PortalHome.POCKETPORTAL_DATA_COMPONENT, portaldata);
        }

        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
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

        // Get last portal position for this player
        Map<String, Vector3d> LastPortalPositions = new HashMap<>(portaldata.getLastPortalPositions());
        Vector3d lastPortalPosition = LastPortalPositions.get(player.getDisplayName());
        if (lastPortalPosition == null) {
            interactionContext.getState().state = InteractionState.Failed;
            player.sendMessage(Message.raw("No last portal position found."));
            LOGGER.atInfo().log("No last portal position found for player: " + player.getDisplayName());
            return;
        }

        Vector3d newPosition = new Vector3d(lastPortalPosition.getX(), lastPortalPosition.getY(),
                lastPortalPosition.getZ());
        Vector3f newRotation = new Vector3f(player.getPlayerConfigData().lastSavedRotation.getX(),
                player.getPlayerConfigData().lastSavedRotation.getY(),
                player.getPlayerConfigData().lastSavedRotation.getZ());
        sendNotificationToPlayer(playerRef.getUuid(), "Teleporting to last portal location",
                lastPortalPosition.toString());

        // Teleport Player
        world.execute(() -> {
            if (player.getReference() == null)
                return;
            Teleport teleport = new Teleport(
                    newPosition,
                    newRotation);
            commandBuffer.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
    }

    private void sendNotificationToPlayer(UUID uuid, String message, String submessage) {
        String color = "#c300ff";
        String secondaryColor = "#c29cf3";
        var playerRef = Universe.get().getPlayer(uuid);
        var packetHandler = playerRef.getPacketHandler();
        var primaryMessage = Message.raw(message).color(color);
        var secondaryMessage = Message.raw(submessage).color(secondaryColor);
        var icon = new ItemStack("PocketPortal", 1).toPacket();
        NotificationUtil.sendNotification(
                packetHandler,
                primaryMessage,
                secondaryMessage,
                (ItemWithAllMetadata) icon);
    }
}
