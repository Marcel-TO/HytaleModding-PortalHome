package net.marcelto.hytale.interactions;

import com.hypixel.hytale.builtin.instances.InstanceValidator;
import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.instances.event.EnterInstanceTeleporterEvent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.TrigMathUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.OriginSource;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.marcelto.hytale.utils.NotifyPlayer;
import org.joml.Vector3d;

public class TeleportToGoblinInteraction extends SimpleInstantInteraction {
  @Nonnull
  public static final BuilderCodec<TeleportToGoblinInteraction> CODEC =
      BuilderCodec.builder(
              TeleportToGoblinInteraction.class,
              TeleportToGoblinInteraction::new,
              SimpleInstantInteraction.CODEC)
          .documentation("Teleports the **Player** to the named instance, creating it if required.")
          .appendInherited(
              new KeyedCodec<>("InstanceName", Codec.STRING),
              (o, i) -> o.instanceName = i,
              o -> o.instanceName,
              (o, p) -> o.instanceName = p.instanceName)
          .documentation("The name of the **instance** to teleport to.")
          .addValidator(Validators.nonNull())
          .addValidator(InstanceValidator.INSTANCE)
          .add()
          .appendInherited(
              new KeyedCodec<>("InstanceKey", Codec.STRING),
              (o, i) -> o.instanceKey = i,
              o -> o.instanceKey,
              (o, p) -> o.instanceKey = p.instanceKey)
          .documentation("The key to name the world. Random if not provided")
          .addValidator(Validators.nonNull())
          .add()
          .appendInherited(
              new KeyedCodec<>("PositionOffset", Vector3dUtil.CODEC),
              (o, i) -> o.positionOffset = i,
              o -> o.positionOffset,
              (o, p) -> o.positionOffset = p.positionOffset)
          .documentation(
              "The offset to apply to the return point.\n"
                  + "\n"
                  + "Used to prevent repeated interactions when returning from the instance.")
          .add()
          .appendInherited(
              new KeyedCodec<>("Arrival", Transform.CODEC),
              (o, i) -> o.arrival = i,
              o -> o.arrival,
              (o, p) -> o.arrival = p.arrival)
          .documentation("The arrival position return when teleporting to the instance.")
          .add()
          .appendInherited(
              new KeyedCodec<>("Rotation", Rotation3f.PARTIAL_TELEPORT_CODEC),
              (o, i) -> o.rotation = i,
              o -> o.rotation,
              (o, p) -> o.rotation = p.rotation)
          .documentation("The rotation to set the player to when returning from an instance.")
          .add()
          .appendInherited(
              new KeyedCodec<>("OriginSource", OriginSource.CODEC),
              (o, i) -> o.originSource = i,
              o -> o.originSource,
              (o, p) -> o.originSource = p.originSource)
          .documentation(
              "The source to use for the return position.\n"
                  + "\n"
                  + "Defaults to the player's position.")
          .addValidator(Validators.nonNull())
          .add()
          .appendInherited(
              new KeyedCodec<>("PersonalReturnPoint", Codec.BOOLEAN),
              (o, i) -> o.personalReturnPoint = i,
              o -> Boolean.valueOf(o.personalReturnPoint),
              (o, p) -> o.personalReturnPoint = p.personalReturnPoint)
          .documentation(
              "Whether the player entering the instance will have their own return point\n"
                  + "set to the current location. Overriding the world's return point.")
          .add()
          .appendInherited(
              new KeyedCodec<>("CloseOnBlockRemove", Codec.BOOLEAN),
              (o, i) -> o.closeOnBlockRemove = i,
              o -> Boolean.valueOf(o.closeOnBlockRemove),
              (o, p) -> o.closeOnBlockRemove = p.closeOnBlockRemove)
          .documentation("Whether to delete the instance when the portal block is removed.")
          .add()
          .appendInherited(
              new KeyedCodec<>("RemoveBlockAfter", Codec.DOUBLE),
              (o, i) -> o.removeBlockAfter = i,
              o -> Double.valueOf(o.removeBlockAfter),
              (o, p) -> o.removeBlockAfter = p.removeBlockAfter)
          .documentation(
              "The number of seconds to wait before removing the block that triggered\n"
                  + "the interaction. A negative value disables this.\n"
                  + "\n"
                  + "This is needed instead of using another interaction due to all interactions\n"
                  + "being stopped once teleporting to another world.")
          .add()
          .afterDecode(
              i -> {
                if (i.rotation != null) {
                  i.rotation.mul(TrigMathUtil.degToRad);
                }
              })
          .build();

  private String instanceName;
  private String instanceKey;
  private Vector3d positionOffset;
  private Rotation3f rotation;
  private Transform arrival;

  @Nonnull private OriginSource originSource = OriginSource.ENTITY;
  private boolean personalReturnPoint;
  private boolean closeOnBlockRemove = true;
  private double removeBlockAfter = -1;

  @Nonnull
  @Override
  public WaitForDataFrom getWaitForDataFrom() {
    return WaitForDataFrom.Server;
  }

  @Override
  protected void firstRun(
      @Nonnull InteractionType type,
      @Nonnull InteractionContext context,
      @Nonnull CooldownHandler cooldownHandler) {
    final CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
    assert commandBuffer != null;
    final Ref<EntityStore> ref = context.getEntity();

    final DeathComponent deathComponent =
        commandBuffer.getComponent(ref, DeathComponent.getComponentType());
    if (deathComponent != null) {
      return;
    }

    final Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
    final PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
    assert playerRef != null;
    if (playerComponent == null || playerComponent.isWaitingForClientReady()) {
      // The player has teleported to this location. Don't trigger another teleport
      return;
    }

    // Don't attempt to teleport when teleporting
    final var archetype = commandBuffer.getArchetype(ref);
    if (archetype.contains(Teleport.getComponentType())
        || archetype.contains(PendingTeleport.getComponentType())) return;

    final var world = commandBuffer.getExternalData().getWorld();
    final var module = InstancesPlugin.get();
    final var universe = Universe.get();

    if (world.getName().equals(instanceKey)) {
      NotifyPlayer.SendNotificationToPlayer(
          playerRef,
          "You are already here!",
          "You can not teleport to the same world you are currently in.",
          NotifyPlayer.ERRORCOLOR,
          NotifyPlayer.ERRORSECONDARY,
          NotifyPlayer.GOBLINPORTALITEM);
      return;
    }

    World targetWorld;
    CompletableFuture<World> targetWorldFuture = null;
    Transform returnPoint = null;

    if (instanceKey == null) return;

    // If we have a key then we don't need to use a block state to store the world name
    targetWorld = universe.getWorld(instanceKey);
    if (targetWorld == null) {
      returnPoint = makeReturnPoint(ref, context, commandBuffer);
      targetWorldFuture = module.spawnInstance(instanceName, instanceKey, world, returnPoint);
    }

    commandBuffer.invoke(EnterInstanceTeleporterEvent.of(context, targetWorld, targetWorldFuture));
    NotifyPlayer.SendNotificationToPlayer(
        playerRef,
        "Teleporting!",
        "Travelling to the Goblin Lab...",
        NotifyPlayer.GOBLINPORTALCOLOR,
        NotifyPlayer.GOBLINPORTALSECONDARY,
        NotifyPlayer.GOBLINPORTALITEM);

    if (targetWorldFuture != null) {
      final var personalReturnPoint =
          getPersonalReturnPoint(ref, context, returnPoint, commandBuffer);
      InstancesPlugin.teleportPlayerToLoadingInstance(
          ref, commandBuffer, targetWorldFuture, personalReturnPoint, arrival);
    } else if (targetWorld != null) {
      final var personalReturnPoint =
          getPersonalReturnPoint(ref, context, returnPoint, commandBuffer);
      InstancesPlugin.teleportPlayerToInstance(
          ref, commandBuffer, targetWorld, personalReturnPoint);
    }

    if (removeBlockAfter >= 0.0) {
      final var targetBlock = context.getTargetBlock();
      if (targetBlock != null) {
        TeleportBlockUtil.removeTriggerBlock(
            world, targetBlock.x, targetBlock.y, targetBlock.z, removeBlockAfter);
      }
    }
  }

  @Nullable
  private Transform getPersonalReturnPoint(
      @Nonnull final Ref<EntityStore> playerRef,
      @Nonnull final InteractionContext context,
      @Nullable final Transform returnPoint,
      @Nonnull ComponentAccessor<EntityStore> componentAccessor) {
    if (!personalReturnPoint) return null;
    if (returnPoint == null) return makeReturnPoint(playerRef, context, componentAccessor);
    return returnPoint;
  }

  @Nonnull
  private Transform makeReturnPoint(
      @Nonnull final Ref<EntityStore> playerRef,
      @Nonnull final InteractionContext context,
      @Nonnull final ComponentAccessor<EntityStore> componentAccessor) {
    Transform transform = null;
    switch (originSource) {
      case ENTITY -> {
        final var transformComponent =
            componentAccessor.getComponent(playerRef, TransformComponent.getComponentType());
        assert transformComponent != null;

        transform = new Transform(transformComponent.getTransform());
        transform.getPosition().add(positionOffset);
        transform.setRotation(rotation != null ? rotation : Rotation3f.NaN);
      }
      case BLOCK -> {
        final var targetBlock = context.getTargetBlock();
        if (targetBlock == null)
          throw new IllegalArgumentException("Can't use OriginSource.BLOCK without a target block");

        transform =
            TeleportBlockUtil.makeBlockReturnPoint(
                componentAccessor.getExternalData().getWorld(),
                targetBlock.x,
                targetBlock.y,
                targetBlock.z,
                positionOffset,
                rotation);
      }
    }
    return transform;
  }
}
