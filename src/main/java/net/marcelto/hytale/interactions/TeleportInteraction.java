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
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import net.marcelto.hytale.gui.TeleportationGUI;

public class TeleportInteraction extends SimpleInstantInteraction {
  public static final BuilderCodec<TeleportInteraction> CODEC =
      BuilderCodec.builder(
              TeleportInteraction.class, TeleportInteraction::new, SimpleInstantInteraction.CODEC)
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

    Ref<EntityStore> ref = interactionContext.getEntity();
    Store<EntityStore> store = ref.getStore();

    Player player = commandBuffer.getComponent(ref, Player.getComponentType());
    PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    if (player == null || playerRef == null) {
      interactionContext.getState().state = InteractionState.Failed;
      LOGGER.atInfo().log("Player is null");
      return;
    }

    TeleportationGUI page =
        new TeleportationGUI(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction);
    player.getPageManager().openCustomPage(ref, store, page);
  }
}
