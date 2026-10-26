package net.marcelto.hytale.components;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;
import org.joml.Vector3d;

public class TeleportLocationComponent implements Component<EntityStore> {
  private static ComponentType<EntityStore, TeleportLocationComponent> TYPE;
  public static final BuilderCodec<TeleportLocationComponent> CODEC = buildCodec();
  private Vector3d location;
  private String name;
  private String worldName;

  public TeleportLocationComponent() {}

  public TeleportLocationComponent(String name, Vector3d location, String worldName) {
    this.name = name;
    this.location = location;
    this.worldName = worldName;
  }

  public TeleportLocationComponent(TeleportLocationComponent other) {
    this.location = other.location;
    this.name = other.name;
    this.worldName = other.worldName;
  }

  public String getName() {
    return this.name;
  }

  public Vector3d getLocation() {
    return this.location;
  }

  public String getWorldName() {
    return this.worldName;
  }

  public String getLocationString() {
    return String.format(
        "X: %d, Y: %d, Z: %d",
        (int) this.location.x(), (int) this.location.y(), (int) this.location.z());
  }

  public static ComponentType<EntityStore, TeleportLocationComponent> getComponentType() {
    return TYPE;
  }

  public static BuilderCodec<TeleportLocationComponent> buildCodec() {
    var teleportLocation = new KeyedCodec<>("TeleportLocation", Vector3dUtil.CODEC);
    KeyedCodec<String> teleportName = new KeyedCodec<>("TeleportName", Codec.STRING);
    KeyedCodec<String> teleportWorldName = new KeyedCodec<>("TeleportWorldName", Codec.STRING);

    return BuilderCodec.builder(TeleportLocationComponent.class, TeleportLocationComponent::new)
        .append(teleportLocation, (data, value) -> data.location = value, (data) -> data.location)
        .addValidator(Validators.nonNull())
        .add()
        .append(teleportName, (data, value) -> data.name = value, (data) -> data.name)
        .addValidator(Validators.nonNull())
        .add()
        .append(
            teleportWorldName, (data, value) -> data.worldName = value, (data) -> data.worldName)
        .add()
        .build();
  }

  @Nullable
  @Override
  public Component<EntityStore> clone() {
    return new TeleportLocationComponent(this);
  }
}
