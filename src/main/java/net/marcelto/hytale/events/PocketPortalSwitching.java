package net.marcelto.hytale.events;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
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
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementConfig;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.marcelto.hytale.PortalHome;
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

        // Get last portal position for this player
        Vector3d lastPortalPosition = PortalHome.LastPortalPositions.get(player.getDisplayName());
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
        player.sendMessage(Message.raw("Teleporting to last portal location: " + lastPortalPosition.toString()));

        PageManager pageManager = player.getPageManager();
        Inventory inventory = player.getInventory();
        ItemContainer itemContainer = inventory.getStorage();
        itemContainer.addItemStack(itemStack);
        itemContainer.addItemStackToSlot((short) 0, itemStack);
        itemContainer.removeItemStack(itemStack);

        // Teleport Player
        world.execute(() -> {
            if (player.getReference() == null)
                return;
            Teleport teleport = new Teleport(
                    newPosition,
                    newRotation);
            store.addComponent(player.getReference(), Teleport.getComponentType(), teleport);
        });
    }
}
