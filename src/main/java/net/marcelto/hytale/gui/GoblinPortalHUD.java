package net.marcelto.hytale.gui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public class GoblinPortalHUD extends CustomUIHud {
  public GoblinPortalHUD(@Nonnull PlayerRef playerRef) {
    super(playerRef, HUD_ID);
  }

  public static final String HUD_ID = "goblinportal_hud";

  @Override
  protected void build(@Nonnull UICommandBuilder ui) {
    ui.append("HUD/GoblinPortalHUD.ui");
  }

  public void updateHUD(boolean visible) {
    UICommandBuilder patch = new UICommandBuilder();
    patch.set("#GoblinPortalContent.Visible", visible);
    update(false, patch);
  }
}
