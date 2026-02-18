package net.marcelto.hytale.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PocketPortalDataComponent implements Component<EntityStore> {
    private Map<String, Vector3d> LastPortalPositions;

    public static final BuilderCodec<PocketPortalDataComponent> CODEC = PocketPortalDataComponent.buildCodec();

    public PocketPortalDataComponent() {
        this.LastPortalPositions = new HashMap<>();
    }

    public PocketPortalDataComponent(PocketPortalDataComponent other) {
        this.LastPortalPositions = new HashMap<>(other.LastPortalPositions);
    }

    public static BuilderCodec<PocketPortalDataComponent> buildCodec() {
        var lastPortalPositionsCodec = new KeyedCodec<>("LastPortalPositions",
                new MapCodec<>(Vector3d.CODEC, HashMap<String, Vector3d>::new));

        return BuilderCodec.builder(PocketPortalDataComponent.class, PocketPortalDataComponent::new)
                .append(
                        lastPortalPositionsCodec,
                        (data, value) -> data.LastPortalPositions = value,
                        (data) -> data.LastPortalPositions)
                .addValidator(Validators.nonNull())
                .add()
                .build();
    }

    public Map<String, Vector3d> getLastPortalPositions() {
        if (LastPortalPositions == null) {
            LastPortalPositions = new HashMap<>();
        }
        return LastPortalPositions;
    }

    public void setLastPortalPositions(Map<String, Vector3d> lastPortalPositions) {
        this.LastPortalPositions = lastPortalPositions;
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PocketPortalDataComponent(this);
    }
}
