package net.marcelto.hytale.gui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class PocketPortalHUD extends CustomUIHud {
  public PocketPortalHUD(@Nonnull PlayerRef playerRef) {
    super(playerRef);
  }

  @Override
  protected void build(@Nonnull UICommandBuilder ui) {
    ui.append("HUD/PocketPortalHUD.ui");
  }

  public void updateHUD(boolean visible) {
    UICommandBuilder patch = new UICommandBuilder();
    patch.set("#Content.Visible", visible);
    update(false, patch);
  }
}
