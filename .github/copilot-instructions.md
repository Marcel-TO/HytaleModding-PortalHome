# Copilot instructions for PortalHome

## Working preferences

- Make requested changes in the current checkout. Do not stage, commit, push, create branches, or amend commits without an explicit request.
- When a commit is requested, do not add Copilot as a collaborator or include a Copilot co-author trailer.
- Preserve unrelated changes and manual asset edits. Ask before overwriting work that differs from its generator.
- Use Windows paths and PowerShell commands for local work. Hytale asset identifiers inside JSON still use forward slashes.
- Implement the requested change completely, but keep its scope narrow. For an analysis or explanation request, inspect the code and explain it without changing behavior.
- Keep responses concise. State what changed and any remaining limitation. Do not claim in-game verification from a successful build or software render.

## Useful skills

Load an available matching skill before starting the relevant work. Read only the references needed for the task.

- `hytale-modder` for server plugins, ECS components and systems, interactions, commands, custom UI, inventory, and world handling.
- `hytale-blockbench-generator` for reproducible `.blockymodel` models, texture atlases, icons, previews, and `.blockyanim` animations. Use its model, animation, preview, and icon reference scripts instead of inventing a new format.
- `python-expert` when writing or changing Python generators or preview tools.
- `unslop` for user-facing writing and documentation.
- `changelog-generator` when release notes or a changelog are requested.

Skills may not be installed in every environment. If a needed skill is unavailable, say so and use repository examples and verified Hytale documentation. Do not pretend to have loaded it.

## Repository layout and plugin changes

- Java code lives in `src\main\java\net\marcelto\hytale`. `PortalHome.java` registers components, interaction codecs, and systems in `setup()`.
- Keep persistent data in `components`, gameplay actions in `interactions`, ECS logic in `systems`, and custom pages and HUD code in `gui`.
- Resource files live in `src\main\resources`. Item definitions are under `Server\Item\Items`, models and textures under `Common\Resources\Items`, icons under `Common\Icons\ItemsGenerated`, and UI under `Common\UI\Custom`.
- Discover actual paths from the current checkout and item definitions. Other Hytale projects may use `Common\Items` instead of `Common\Resources\Items`. Do not reuse an old path without checking.
- Follow existing helpers, including `TeleportBlockUtil` and `NotifyPlayer`, before adding duplicate behavior.
- Use the Hytale API version configured by `gradle.properties` and `build.gradle`. Verify API signatures against available sources or dependencies rather than assuming a skill's example matches the installed version.
- Use `Ref<EntityStore>` and the relevant store for entity access. Check reference validity, and run world/store mutations on the owning world's execution thread. Do not retain stale entity references across asynchronous callbacks.
- Keep codec registration names consistent with item interaction `Type` values. Treat persisted component names and codec fields as compatibility-sensitive.
- Avoid codec defaults that resolve game assets before those assets are available during plugin setup.
- Use existing Hytale logging and player notifications for failures. Do not turn a failed teleport or save into apparent success.
- Keep Java, item definitions, UI, effects, animations, and localization consistent when behavior changes. Reuse translation keys from `Server\Languages\en-US\server.lang`.
- Preserve Pocket Portal saved destinations and cross-world travel, Goblin Portal entry/return behavior, and charging cancellation unless the task explicitly changes them.
- Update the README when items, recipes, controls, or player-visible behavior change. Cover both portals, not only the Pocket Portal.
- Keep `IncludesAssetPack` enabled for packaged custom assets. Use `gradle.properties` and the manifest generation task for metadata changes rather than allowing generated metadata to drift.

## Gradle validation

Use the checked-in Gradle wrapper from the repository root. The Java toolchain is configured in `gradle.properties`; do not replace it with the machine's default Java version.

Choose the smallest command that covers the change.

```powershell
# Compile Java changes.
.\gradlew.bat --console=plain compileJava

# Check Java formatting and main-source Checkstyle rules.
.\gradlew.bat --console=plain spotlessCheck checkstyleMain

# Run configured verification tasks.
.\gradlew.bat --console=plain check

# Build and package the plugin, including resources.
.\gradlew.bat --console=plain build
```

- Java formatting uses Spotless with Google Java Format. Checkstyle uses `config\checkstyle.xml`. Do not hand-format against a different style.
- Use `.\gradlew.bat --console=plain spotlessApply` when formatting changes are needed. Review its diff and do not leave unrelated formatting edits.
- For Java changes, cover compilation and the applicable formatting and verification tasks. For asset changes, validate the changed assets and build the packaged plugin.
- A Gradle build does not establish valid Hytale asset references, correct attachment placement, or working gameplay. Check these separately.
- Documentation-only changes do not need a Gradle build.
- Do not start with `clean`, delete caches, upgrade dependencies, or run the broad `just reset` recipe to work around a failure. Diagnose the actual error and report unresolved blockers.

Development tasks, when runtime validation is needed:

```powershell
.\gradlew.bat --console=plain updatePluginManifest
.\gradlew.bat --console=plain prepareRunServer
.\gradlew.bat --console=plain hytaleJvmDoctor
.\gradlew.bat --console=plain runServer -Dhotswap=true
```

Manifest generation writes metadata. Server tasks may download files or start a long-running process. Use them only when relevant, keep temporary servers scoped to the task, and stop them when finished. Do not deploy a JAR to the user's game installation without a request.

## Reproducible asset generation

- Store generator and related Python source files in `generate_blockbench` at the repository root. Derive paths from the script location, never from an absolute developer-machine path.
- This repository currently ignores `generate_blockbench` in `.gitignore`. Keep requested source files there, but disclose that they are ignored when preparing them for version control. Do not force-add files or change ignore rules without a request.
- Use a uv-managed Python project and run scripts with `uv run`. Reuse the skill's dependency requirements; add dependencies only when needed.
- Generate the model and matching texture together. Keep UV packing synchronized, use deterministic seeded randomness, and fail explicitly when the atlas has no space.
- Before regeneration, compare the current asset with the generator. Preserve meaningful manual changes to positions, pivots, rotations, geometry, and UVs. Ignore only known harmless Blockbench serialization differences.
- For user-adjusted assets, update the generator to reproduce the current model instead of reverting the model to an older generated version.
- Preserve root/attachment transforms and all animated node names. Check attachment height and pivots against the original model, especially for held, equipped, projectile, and open/closed variants.
- Keep visible faces intact. Check quad normals, face omission, and backface culling when flat geometry disappears.
- Keep the existing model format, scale conventions, and texture dimensions unless a change is requested. Match Hytale's chunky shapes, readable silhouettes, hand-painted pixel textures, and consistent texel density.
- Only overwrite the intended model and texture. Inspect related item JSON, icons, animations, `Scale`, and `IconProperties`, but do not silently redesign or retune them for a model-only request.
- When an asset move or rename is requested, update every affected reference. Check whether an asset is shared before replacing it.

## Previews and asset verification

- Render front, three-quarter, back, and side views after model changes. Also inspect the enlarged texture with nearest-neighbor scaling.
- Open and visually inspect the generated images. Display or attach the preview for the user, rather than only printing a filesystem path.
- Check silhouette, clipping, floating parts, missing faces, decal readability, UV alignment, and attachment placement. Iterate on visible defects before presenting the result.
- When implementing a preview script's default output behavior, default to the generator directory containing that script, following the earlier preview-tool request. Keep an explicit output override.
- For temporary review images, use that override to write to the session artifact directory or a scoped temporary directory. Do not put scratch previews into packaged assets or overwrite curated README images.
- Generate icons when requested and inspect them at inventory size. Preserve existing icon framing settings unless their adjustment is part of the task.
- Validate model JSON, unique string node IDs, finite numbers, normalized quaternions, and UV rectangles within texture bounds. Check that referenced model, texture, icon, and animation assets resolve under Hytale's asset conventions.
- Validate animation target names against the model, keyframe order and duration, supported tracks, and quaternion values. Use the Hytale Blockbench plugin's format as the source of truth.
- A static preview does not verify animation playback or exact Hytale UV orientation. Confirm animations in Blockbench with the Hytale plugin and gameplay placement in-game when possible. State plainly when those checks remain unperformed.
