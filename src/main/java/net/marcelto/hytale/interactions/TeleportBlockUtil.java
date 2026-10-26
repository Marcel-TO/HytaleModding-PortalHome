package net.marcelto.hytale.interactions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.Axis;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.SetBlockSettings;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.GetChunkFlags;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;
import com.hypixel.hytale.server.core.util.thread.TickingThread;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.joml.Vector3d;

/** Shared block handling for the instance teleport interactions. */
final class TeleportBlockUtil {

  /** A set-block setting that informs the world to perform a block update. */
  private static final int SET_BLOCK_SETTINGS = SetBlockSettings.PERFORM_BLOCK_UPDATE;

  private TeleportBlockUtil() {}

  /**
   * Clears the block that triggered an instance teleport, after the given delay.
   *
   * <p>The section is loaded back if it unloaded during the delay. Generation stays off: the player
   * interacted with this block, so a section that worldgen must supply no longer holds it.
   *
   * @param world The world that holds the block.
   * @param x The X coordinate of the block.
   * @param y The Y coordinate of the block.
   * @param z The Z coordinate of the block.
   * @param delaySeconds The delay in seconds. A value of {@code 0} removes the block immediately.
   */
  static void removeTriggerBlock(
      @Nonnull final World world,
      final int x,
      final int y,
      final int z,
      final double delaySeconds) {
    final var chunkStore = world.getChunkStore();
    final var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
    if (sectionRef == null || !sectionRef.isValid()) return;

    if (delaySeconds == 0.0) {
      setEmpty(chunkStore, sectionRef, x, y, z);
      return;
    }

    final var blockSection =
        chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
    if (blockSection == null) return;

    final int blockId = blockSection.get(x, y, z);

    world.scheduleAfter(
        () ->
            chunkStore
                .getChunkSectionReferenceAtBlockAsync(x, y, z, GetChunkFlags.NO_GENERATE)
                .thenAcceptAsync(
                    delayedRef -> {
                      if (delayedRef == null || !delayedRef.isValid()) return;

                      final var delayedSection =
                          chunkStore
                              .getStore()
                              .getComponent(delayedRef, BlockSection.getComponentType());
                      if (delayedSection == null || delayedSection.get(x, y, z) != blockId) return;

                      setEmpty(chunkStore, delayedRef, x, y, z);
                    },
                    world),
        (long) (delaySeconds * TickingThread.NANOS_IN_ONE_SECOND),
        TimeUnit.NANOSECONDS);
  }

  /**
   * Builds a return point from the block that triggered an instance teleport. The point sits at the
   * middle of the block's hitbox, offset and rotated by the interaction's configuration.
   *
   * @param world The world that holds the block.
   * @param x The X coordinate of the block.
   * @param y The Y coordinate of the block.
   * @param z The Z coordinate of the block.
   * @param positionOffset The offset to apply to the block's middle, in block space.
   * @param rotation The rotation to face on return, or {@code null} to keep the current one.
   * @return the return point.
   * @throws IllegalArgumentException if the block cannot be read.
   */
  @Nonnull
  static Transform makeBlockReturnPoint(
      @Nonnull final World world,
      final int x,
      final int y,
      final int z,
      @Nullable final Vector3d positionOffset,
      @Nullable final Rotation3f rotation) {
    final var chunkStore = world.getChunkStore();
    final var sectionRef = chunkStore.getChunkSectionReferenceAtBlock(x, y, z);
    if (sectionRef == null || !sectionRef.isValid()) {
      throw new IllegalArgumentException("Chunk section not loaded");
    }

    final var blockSection =
        chunkStore.getStore().getComponent(sectionRef, BlockSection.getComponentType());
    if (blockSection == null) {
      throw new IllegalArgumentException("Block section not found");
    }

    final var blockType = BlockType.getAssetMap().getAsset(blockSection.get(x, y, z));
    if (blockType == null) {
      throw new IllegalArgumentException("Block type not found");
    }

    final int rotationIndex = blockSection.getRotationIndex(x, y, z);
    final var rotationTuple = RotationTuple.get(rotationIndex);

    final var hitboxAsset =
        BlockBoundingBoxes.getAssetMap().getAsset(blockType.getHitboxTypeIndex());
    if (hitboxAsset == null) {
      throw new IllegalArgumentException(
          "Hitbox asset not found for block type: " + blockType.getId());
    }

    final var hitbox = hitboxAsset.get(rotationIndex).getBoundingBox();

    final var position =
        positionOffset != null ? rotationTuple.rotatedVector(positionOffset) : new Vector3d();
    position.x += hitbox.middleX() + x;
    position.y += hitbox.middleY() + y;
    position.z += hitbox.middleZ() + z;

    final Rotation3f rotationOutput;
    if (rotation != null) {
      rotationOutput = new Rotation3f(rotation);
      rotationOutput.addRotationOnAxis(Axis.Y, rotationTuple.yaw().getDegrees());
      rotationOutput.addRotationOnAxis(Axis.X, rotationTuple.pitch().getDegrees());
    } else {
      rotationOutput = new Rotation3f(Rotation3f.NaN);
    }

    return new Transform(position, rotationOutput);
  }

  private static void setEmpty(
      @Nonnull final ChunkStore chunkStore,
      @Nonnull final Ref<ChunkStore> sectionRef,
      final int x,
      final int y,
      final int z) {
    BlockOperations.setBlock(
        chunkStore,
        sectionRef,
        x,
        y,
        z,
        BlockType.EMPTY_ID,
        BlockType.EMPTY,
        RotationTuple.NONE_INDEX,
        FillerBlockUtil.NO_FILLER,
        SET_BLOCK_SETTINGS);
  }
}
