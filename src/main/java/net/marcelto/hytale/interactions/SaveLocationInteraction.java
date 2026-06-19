package net.marcelto.hytale.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import net.marcelto.hytale.components.PocketPortalDataComponent;
import net.marcelto.hytale.gui.SaveLocationGUI;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.joml.Vector3d;

public class SaveLocationInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<SaveLocationInteraction> CODEC =
      BuilderCodec.builder(
              SaveLocationInteraction.class,
              SaveLocationInteraction::new,
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
    Store<EntityStore> store = ref.getStore();

    // Ensure PlayerDataComponent exists
    PocketPortalDataComponent portaldata = new PocketPortalDataComponent();
    if (commandBuffer.getComponent(ref, PocketPortalDataComponent.getComponentType()) != null) {
      portaldata = commandBuffer.getComponent(ref, PocketPortalDataComponent.getComponentType());
    } else {
      commandBuffer.putComponent(ref, PocketPortalDataComponent.getComponentType(), portaldata);
    }

    if (portaldata == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("Pocket portal data component is null");
      return;
    }

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    UUIDComponent playerUid = commandBuffer.getComponent(ref, UUIDComponent.getComponentType());
    TransformComponent transformComponent =
        commandBuffer.getComponent(ref, TransformComponent.getComponentType());
    if (player == null || playerRef == null || playerUid == null || transformComponent == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("Player is null");
      return;
    }

    // Get player's current position
    SaveLocationGUI page = getSaveLocationGUI(transformComponent, playerRef);
    player.getPageManager().openCustomPage(ref, store, page);
  }

  @NonNullDecl
  private static SaveLocationGUI getSaveLocationGUI(
      TransformComponent transformComponent, PlayerRef playerRef) {
    Vector3d currentPlayerPosition = transformComponent.getPosition();
    Vector3d newPosition =
        new Vector3d(
            currentPlayerPosition.x(), currentPlayerPosition.y(), currentPlayerPosition.z());

    // Open the save location GUI and pass the current location to it
    return new SaveLocationGUI(
        playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, newPosition);
  }
}
