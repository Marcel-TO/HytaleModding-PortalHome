package net.marcelto.hytale.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.marcelto.hytale.gui.PocketPortalHUD;

public class HudTickingSystem extends EntityTickingSystem<EntityStore> {

  @Override
  public void tick(
      float v,
      int index,
      @Nonnull ArchetypeChunk<EntityStore> chunk,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandBuffer<EntityStore> commandBuffer) {

    PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
    Player player = chunk.getComponent(index, Player.getComponentType());
    if (playerRef == null || player == null) return;

    UUID playerId = playerRef.getUuid();

    PocketPortalHUD hud = (PocketPortalHUD) player.getHudManager().getCustomHud();
    if (hud == null) return;

    // Determine item
    ItemStack heldItem = player.getInventory().getItemInHand();
    if (heldItem == null) return;

    boolean shouldVisible = heldItem.getItemId().equals("PocketPortal");
    hud.updateHUD(shouldVisible);
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
  }
}
