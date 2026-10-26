package net.marcelto.hytale.interactions;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import net.marcelto.hytale.utils.NotifyPlayer;

import javax.annotation.Nonnull;

public class TeleportBackGoblinInteraction extends SimpleInstantInteraction {
    @Nonnull
    public static final BuilderCodec<TeleportBackGoblinInteraction> CODEC =
            BuilderCodec.builder(
                            TeleportBackGoblinInteraction.class,
                            TeleportBackGoblinInteraction::new,
                            SimpleInstantInteraction.CODEC)
                    .documentation("Teleports the Player out of the Goblin Lab")
                    .appendInherited(
                            new KeyedCodec<>("InstanceKey", Codec.STRING),
                            (o, i) -> o.instanceKey = i,
                            o -> o.instanceKey,
                            (o, p) -> o.instanceKey = p.instanceKey)
                    .documentation("The key to name the world. Random if not provided")
                    .addValidator(Validators.nonNull())
                    .add()
                    .build();

    private String instanceKey;

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
        final var commandBuffer = context.getCommandBuffer();
        assert commandBuffer != null;

        final var ref = context.getEntity();

        final Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        final PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        assert player != null;
        assert playerRef != null;
        if (player.isWaitingForClientReady()) {
            // The player has teleported to this location. Don't trigger another teleport
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            NotifyPlayer.SendNotificationToPlayer(
                    playerRef,
                    "Teleport Failed",
                    "Current world not found. Cannot teleport back.",
                    NotifyPlayer.ERRORCOLOR,
                    NotifyPlayer.ERRORSECONDARY,
                    NotifyPlayer.GOBLINPORTALITEM);
            return;
        }

        if (!world.getName().equals(instanceKey)) {
            NotifyPlayer.SendNotificationToPlayer(
                    playerRef,
                    "Teleport Failed",
                    "Not in correct World for teleporting back.",
                    NotifyPlayer.ERRORCOLOR,
                    NotifyPlayer.ERRORSECONDARY,
                    NotifyPlayer.GOBLINPORTALITEM);
            return;
        }

        // Don't attempt to teleport when teleporting
        final var archetype = commandBuffer.getArchetype(ref);
        if (archetype.contains(Teleport.getComponentType())
                || archetype.contains(PendingTeleport.getComponentType())) return;

        NotifyPlayer.SendNotificationToPlayer(
                playerRef,
                "Teleporting!",
                "Teleporting back to original location.",
                NotifyPlayer.GOBLINPORTALCOLOR,
                NotifyPlayer.GOBLINPORTALSECONDARY,
                NotifyPlayer.GOBLINPORTALITEM);
        InstancesPlugin.exitInstance(ref, commandBuffer);
    }
}
