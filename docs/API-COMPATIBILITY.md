# API compatibility

This table tracks every public type in the official Ponder 1.21.1 `net.createmod.ponder.api`
source set, excluding the three `package-info` files. Coverage is 42/42 types. "Retained" means
the class name and purpose are preserved; signatures use the 1.12.2 types listed below and are not
binary-compatible with a 1.21.1 jar.

Coverage here is a type-level migration inventory, not a claim of method-for-method binary identity.
Modern overloads whose parameter systems do not exist in 1.12.2 are represented by the functional
adapter named in the table or are recorded as merged/not applicable. Scene source must replace modern
Minecraft types and may need to replace overloads; a compiled 1.21.1 addon cannot be loaded by this jar.
Catnip is included as a support API, but its adapter surface is grouped by subsystem below rather than
being presented as a stable binary contract with a particular modern Catnip release.

## Type mapping

| Modern type/system | Minecraft 1.12.2 adapter |
| --- | --- |
| `Direction`, `BlockState`, `BlockEntity`, `Level` | `EnumFacing`, `IBlockState`, `TileEntity`, `World` / `PonderLevel` |
| `Vec3`, `AABB`, `Component` | `Vec3d`, `AxisAlignedBB`, `ITextComponent` |
| `ParticleOptions`, `RenderType` | `EnumParticleTypes`, `BlockRenderLayer` and Catnip render phases |
| Mojang `Codec`, `StreamCodec`, DataComponent | Catnip NBT/Gson codec, bounded `PacketBuffer` codec, validated `ItemStack` NBT |
| Brigadier, `ForgeConfigSpec` | `CommandBase`, Forge `Configuration` / `GuiConfig` |
| JOML/Flywheel transforms | Java 8 Catnip pose stack using LWJGL2 and `javax.vecmath` |

## Public Ponder API inventory

| Official public type | 1.12.2 status |
| --- | --- |
| `api.ParticleEmitter` | Retained; emits bounded 1.12 `EnumParticleTypes` events into `PonderWorld`. |
| `api.PonderPalette` | Retained; palette colors use Catnip's integer color implementation. |
| `api.VirtualBlockEntity` | Retained for virtual-aware 1.12 `TileEntity` implementations. |
| `api.level.PonderLevel` | Retained as an in-memory `World` with backup/restore callbacks. |
| `api.element.AnimatedOverlayElement` | Retained with the fixed-pipeline overlay render signature. |
| `api.element.AnimatedSceneElement` | Retained; fade vectors use `Vec3d`. |
| `api.element.ElementLink` | Retained as a typed UUID link restored by keyframes. |
| `api.element.EntityElement` | Retained for 1.12 `Entity` instances. |
| `api.element.InputElementBuilder` | Retained; items use `ItemStack` and icons use Catnip screen elements. |
| `api.element.MinecartElement` | Retained with 1.12 minecart constructors. |
| `api.element.ParrotElement` | Retained with `Vec3d` animation state. |
| `api.element.ParrotPose` | Retained; poses target 1.12 parrot models. |
| `api.element.PonderElement` | Retained; adds functional memento hooks for deterministic replay. |
| `api.element.PonderOverlayElement` | Retained with the 1.12 scene render context. |
| `api.element.PonderSceneElement` | Retained with three fixed-pipeline render phases; transformed section ray tracing remains on `WorldSectionElement`. |
| `api.element.TextElementBuilder` | Retained; localized text resolves through 1.12 language keys. |
| `api.element.TrackedElement` | Retained for linked scene elements. |
| `api.element.WorldSectionElement` | Retained with `Selection`, `Vec3d`, movement, rotation, stabilization and ray tracing. |
| `api.registration.IndexExclusionHelper` | Retained for 1.12 item/block registry objects. |
| `api.registration.LangRegistryAccess` | Retained; emits `.lang` keys and formatted shared/tag/scene text. |
| `api.registration.MultiSceneBuilder` | Retained with 1.12 `ResourceLocation`. |
| `api.registration.MultiTagBuilder` | Retained with deterministic registry order. |
| `api.registration.PonderPlugin` | Retained; direct, `ServiceLoader` and Forge IMC discovery are supported. |
| `api.registration.PonderSceneRegistrationHelper` | Retained; storyboards remain Java scene programs. |
| `api.registration.PonderTagRegistrationHelper` | Retained for 1.12 component IDs. |
| `api.registration.SceneRegistryAccess` | Retained; compiles structure NBT into isolated scenes. |
| `api.registration.SharedTextRegistrationHelper` | Retained with namespaced language keys. |
| `api.registration.StoryBoardEntry` | Retained, including before/after ordering metadata. |
| `api.registration.TagBuilder` | Retained; icons use 1.12 items/blocks. |
| `api.registration.TagRegistryAccess` | Retained with immutable result views. |
| `api.scene.DebugInstructions` | Retained for development callbacks and schematic display. |
| `api.scene.EffectInstructions` | Retained with `EnumParticleTypes`. |
| `api.scene.OverlayInstructions` | Retained with `AxisAlignedBB`, `Vec3d` and 1.12 input icons. |
| `api.scene.PonderStoryBoard` | Retained as the Java functional scene entry point. |
| `api.scene.PositionUtil` | Retained with `BlockPos`. |
| `api.scene.SceneBuilder` | Retained with absolute-tick scheduling and deterministic keyframes. |
| `api.scene.SceneBuildingUtil` | Retained; supplies selection, position and vector helpers. |
| `api.scene.Selection` | Retained as a deterministic finite set of `BlockPos`. |
| `api.scene.SelectionUtil` | Retained for layer, position and range selections. |
| `api.scene.SpecialInstructions` | Retained for parrots, carts and point-of-interest animation. |
| `api.scene.VectorUtil` | Retained with `Vec3d`. |
| `api.scene.WorldInstructions` | Retained with 1.12 blocks, properties, entities and tile entities. |

All 42 rows above have a concrete 1.12.2 source type. None of these entries means "empty class retained
only for compilation"; behavior that has no 1.12 equivalent is redirected to the explicit adapters in
the type-mapping and Catnip sections.

## Catnip adapter groups

| Area | Status |
| --- | --- |
| Data, animation, math, NBT, language, theme and layout | Rewritten for Java 8; mutable views and codecs are bounded and validated. |
| Network | `SimpleNetworkWrapper`; packets are length-limited and dispatched on the game thread. |
| Config and commands | Forge `Configuration`/`GuiConfig` and `CommandBase`; no modern config or Brigadier dependency. |
| World wrappers, placement, ghost blocks and outliner | Functional 1.12 world/event adapters; no persistence or network side effects in Ponder worlds. |
| Mesh, pose and buffered rendering | LWJGL2/`javax.vecmath`, `BufferBuilder.State`, four `BlockRenderLayer` passes; no Flywheel or JOML. |

## Modern accessors and mixins

The modern project has 12 mixin/accessor classes. They are not copied mechanically because most
target APIs do not exist in 1.12.2. Each modern hook is accounted for below.

| Modern hook | 1.12.2 disposition |
| --- | --- |
| `accessor.TimerAccessor` | Merged into the public 1.12 `Timer.elapsedPartialTicks` and `renderPartialTicks` fields. |
| `client.WindowResizeMixin` | Replaced by `MinecraftResizeMixin`, which publishes the equivalent resize/fullscreen callback. |
| `client.accessor.BufferBuilderAccessor` | Replaced by the focused 1.12 `BufferBuilderAccessor` for byte buffer, vertex count and vertex format state. |
| `client.accessor.ClientPacketListenerAccessor` | No accessor is needed: the isolated chunk provider treats scene chunks as generated and does not consume the server view radius. |
| `client.accessor.GameRendererAccessor` | Merged into `PonderUI`'s explicit 45-degree fixed-pipeline projection; no private game FOV invocation is used. |
| `client.accessor.ItemRendererAccessor` | Merged into public `Minecraft#getTextureManager`, `RenderItem` and texture binding entry points. |
| `client.accessor.RenderSystemAccessor` | Modern shader-light state is absent; replaced by fixed-function lightmap setup, `GlStateGuard` and world light sampling. |
| `client.accessor.RenderTypeAccessor` | Modern render-type construction is absent; replaced by the four `BlockRenderLayer` values and Catnip buffer phases. |
| `client.accessor.ScreenAccessor` | Ponder screens own their 1.12 `buttonList` directly; container hovered-slot access is provided separately by `GuiContainerAccessor`. |
| `accessor.BiomeManagerAccessor` | Modern biome zoom sampling is not used; `PonderWorld` and selection views supply the 1.12 biome/world access required by renderers. |
| `accessor.EntityAccessor` | Replaced by the public 1.12 `Entity#setWorld` path used when entities enter or leave the virtual world. |
| `accessor.MinecraftServerAccessor` | Server storage access is intentionally absent; `PonderWorld` uses an in-memory `ISaveHandler` and never opens a real save. |

`ParticleManagerAccessor` is an additional 1.12-only client hook. It exposes particle factories,
layers and the current particle world because 1.12 has no public equivalent. It is not a port of a
modern accessor.

All four shipped mixins are client-only and required. No CatServer-specific injection or overwrite is
included.

## Known visual differences

- Particle events belong to the virtual scene world, not to an individual `WorldSectionElement`.
  They follow scene camera transforms and deterministic replay/seek, but a particle does not inherit
  an independent section's animated translation or rotation after it has spawned.
- Translucent quads are sorted in each section's local camera space and sections are drawn back to
  front. Two independently transformed translucent sections that geometrically intersect can still
  show the ordering limitations of Minecraft 1.12's fixed translucent pipeline.
- The UI uses Minecraft 1.12 fonts, GUI scaling and fixed-function lighting. Text metrics, antialiasing,
  light falloff and fluid faces can differ from the modern renderer even when layout and behavior match.
- Modern shader render types and post-processing do not exist. Addon renderers that require those
  systems need a 1.12 fixed-pipeline implementation; a failing third-party renderer is isolated and
  logged instead of being allowed to corrupt the remainder of the scene.
- The 1.12 fixed rendering pipeline targets equivalent function, layout and appearance; it is not a
  pixel-identical reproduction of the modern shader pipeline.

The release demo is a newly authored vanilla 1.12 structure. The nine modern debug structures remain
development-only mapping fixtures and are excluded from the release jar, so they must not be used as
evidence that a release contains modern sample content.

## Loader boundary

Only Forge 1.12.2 is implemented. Fabric, NeoForge, Cleanroom-specific behavior, third-party JSON
scene formats and third-party backport source compatibility are outside this artifact.

Runtime qualification is separate from this source inventory. See [TESTING.md](TESTING.md) for the
exact Forge, MixinBooter and CatServer versions and for the current client/server verification state.
