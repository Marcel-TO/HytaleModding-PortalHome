package net.marcelto.hytale.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import net.marcelto.hytale.gui.GoblinPortalHUD;

public class HudGoblinPortalSystem extends EntityTickingSystem<EntityStore> {

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

    GoblinPortalHUD hud =
        (GoblinPortalHUD) player.getHudManager().getCustomHud(GoblinPortalHUD.HUD_ID);
    if (hud == null) return;

    // Determine item in hand
    ItemStack heldItem =
        InventoryComponent.getItemInHand(commandBuffer, chunk.getReferenceTo(index));
    if (heldItem == null) {
      hud.updateHUD(false);
      return;
    }

    boolean shouldVisible = heldItem.getItemId().equals("GoblinPortal");
    hud.updateHUD(shouldVisible);
  }

  @Override
  public Query<EntityStore> getQuery() {
    return Query.and(Player.getComponentType(), PlayerRef.getComponentType());
  }
}
