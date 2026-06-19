package net.marcelto.hytale.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

public class PocketPortalDataComponent implements Component<EntityStore> {
  private Map<String, TeleportLocationComponent> portalPositions;

  private static ComponentType<EntityStore, PocketPortalDataComponent> TYPE;
  public static final BuilderCodec<PocketPortalDataComponent> CODEC =
      PocketPortalDataComponent.buildCodec();

  public PocketPortalDataComponent() {
    this.portalPositions = new HashMap<>();
  }

  public PocketPortalDataComponent(PocketPortalDataComponent other) {
    this.portalPositions = new HashMap<>(other.portalPositions);
  }

  public static ComponentType<EntityStore, PocketPortalDataComponent> getComponentType() {
    return TYPE;
  }

  public static void setComponentType(ComponentType<EntityStore, PocketPortalDataComponent> type) {
    TYPE = type;
  }

  public static BuilderCodec<PocketPortalDataComponent> buildCodec() {
    var portalPositionsCodec =
        new KeyedCodec<>(
            "PortalPositions",
            new MapCodec<>(
                TeleportLocationComponent.CODEC, HashMap<String, TeleportLocationComponent>::new));

    return BuilderCodec.builder(PocketPortalDataComponent.class, PocketPortalDataComponent::new)
        .append(
            portalPositionsCodec,
            (data, value) -> data.portalPositions = value,
            (data) -> data.portalPositions)
        .addValidator(Validators.nonNull())
        .add()
        .build();
  }

  public Map<String, TeleportLocationComponent> getportalPositions() {
    if (portalPositions == null) {
      portalPositions = new HashMap<>();
    }
    return portalPositions;
  }

  public void setportalPositions(Map<String, TeleportLocationComponent> portalPositions) {
    this.portalPositions = portalPositions;
  }

  public boolean addPortalPosition(TeleportLocationComponent portalPosition) {
    if (portalPosition == null) {
      return false;
    }

    if (!(this.portalPositions instanceof HashMap)) {
      this.portalPositions = new HashMap<>(this.portalPositions);
    }

    this.portalPositions.put(portalPosition.getName(), portalPosition);
    return true;
  }

  public boolean renamePortalPosition(String oldName, String newName) {
    if (oldName == null || newName == null || newName.isBlank()) {
      return false;
    }
    if (!(this.portalPositions instanceof HashMap)) {
      this.portalPositions = new HashMap<>(this.portalPositions);
    }
    TeleportLocationComponent existing = this.portalPositions.remove(oldName);
    if (existing == null) {
      return false;
    }
    this.portalPositions.put(
        newName, new TeleportLocationComponent(newName, existing.getLocation()));
    return true;
  }

  public boolean removePortalPosition(String name) {
    if (name == null || this.portalPositions == null) {
      return false;
    }

    if (!(this.portalPositions instanceof HashMap)) {
      this.portalPositions = new HashMap<>(this.portalPositions);
    }

    return this.portalPositions.remove(name) != null;
  }

  @Nullable
  @Override
  public Component<EntityStore> clone() {
    return new PocketPortalDataComponent(this);
  }
}
