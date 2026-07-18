package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.PonderSceneElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.registration.StoryBoardEntry.SceneOrderingEntry;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

/** Absolute-tick, resettable scene runtime shared by the UI and headless tests. */
public final class PonderScene {
    public static final String TITLE_KEY = "header";

    private final PonderLocalization localization;
    @Nullable private final PonderWorld world;
    private final String namespace;
    private final ResourceLocation component;
    private final List<PonderTag> tags;
    private final List<SceneOrderingEntry> orderingEntries;
    private final List<ScheduledInstruction> schedule = new ArrayList<ScheduledInstruction>();
    private final List<PonderInstruction> activeInstructions = new ArrayList<PonderInstruction>();
    private final LinkedHashSet<PonderElement> elements = new LinkedHashSet<PonderElement>();
    private final Map<UUID, PonderElement> linkedElements = new LinkedHashMap<UUID, PonderElement>();
    private final List<Integer> keyframes = new ArrayList<Integer>();
    private final NavigableMap<Integer, RuntimeSnapshot> snapshots = new TreeMap<Integer, RuntimeSnapshot>();
    private final WorldSectionElement baseWorldSection;
    private final SceneTransform transform;

    private ResourceLocation sceneId;
    private Vec3d pointOfInterest = new Vec3d(0, 4, 0);
    private Vec3d cursorPosition = pointOfInterest;
    private int textIndex = 1;
    private int buildCursor;
    private int currentTick;
    private int totalTicks;
    private boolean finished;
    private boolean paused;
    private boolean nextUpEnabled = true;
    private int basePlateOffsetX;
    private int basePlateOffsetZ;
    private int basePlateSize = 1;
    private float scale = 1;
    private float yOffset;
    private boolean basePlateShown;
    private boolean shadowShown = true;
    private float cameraYaw;
    private float cameraPitch;

    public PonderScene(@Nullable PonderWorld world, PonderLocalization localization, String namespace,
                       ResourceLocation component, Collection<ResourceLocation> tagIds,
                       Collection<SceneOrderingEntry> orderingEntries) {
        if (localization == null || namespace == null || component == null)
            throw new IllegalArgumentException("Scene identity and localization are required");
        this.world = world;
        this.localization = localization;
        this.namespace = namespace;
        this.component = component;
        this.sceneId = new ResourceLocation(namespace, "missing_title");
        List<PonderTag> resolvedTags = new ArrayList<PonderTag>();
        if (tagIds != null)
            for (ResourceLocation tag : tagIds)
                resolvedTags.add(PonderIndex.getTagAccess().getRegisteredTag(tag));
        this.tags = Collections.unmodifiableList(resolvedTags);
        this.orderingEntries = Collections.unmodifiableList(new ArrayList<SceneOrderingEntry>(orderingEntries));
        BlockPos min = world == null ? BlockPos.ORIGIN : world.getBoundsMin();
        BlockPos max = world == null ? BlockPos.ORIGIN : world.getBoundsMax();
        basePlateSize = Math.max(1, max.getX() - min.getX() + 1);
        baseWorldSection = PonderElementFactories.get().createWorldSection(SelectionImpl.empty());
        transform = new SceneTransform(world == null ? BlockPos.ORIGIN : world.getAnchor());
    }

    public SceneBuilder builder() { return new PonderSceneBuilder(this); }

    public SceneBuildingUtil getSceneBuildingUtil() {
        BlockPos min = world == null ? BlockPos.ORIGIN : world.getBoundsMin();
        BlockPos max = world == null ? BlockPos.ORIGIN : world.getBoundsMax();
        return new PonderSceneBuildingUtil(min, max);
    }

    public synchronized void schedule(PonderInstruction instruction) {
        if (instruction == null)
            throw new IllegalArgumentException("Instruction may not be null");
        instruction.onScheduled(this);
        schedule.add(new ScheduledInstruction(buildCursor, instruction));
        totalTicks = Math.max(totalTicks, buildCursor + instruction.getDuration());
    }

    public void advanceBuildCursor(int ticks) {
        if (ticks < 0)
            throw new IllegalArgumentException("Scene time may not move backwards while building");
        buildCursor += ticks;
        totalTicks = Math.max(totalTicks, buildCursor);
    }

    public int getBuildCursor() { return buildCursor; }

    public synchronized void declareKeyframe(int tick) {
        int safeTick = Math.max(0, tick);
        if (!keyframes.contains(safeTick)) {
            keyframes.add(safeTick);
            Collections.sort(keyframes);
        }
        totalTicks = Math.max(totalTicks, safeTick);
    }

    public synchronized void begin() {
        for (PonderElement element : new ArrayList<PonderElement>(elements))
            element.reset(this);
        currentTick = 0;
        finished = false;
        paused = false;
        activeInstructions.clear();
        linkedElements.clear();
        elements.clear();
        snapshots.clear();
        pointOfInterest = new Vec3d(0, 4, 0);
        cursorPosition = pointOfInterest;
        for (ScheduledInstruction scheduled : schedule)
            scheduled.instruction.reset(this);
        if (world != null)
            world.restore();
        baseWorldSection.reset(this);
        baseWorldSection.setEmpty();
        baseWorldSection.resetSelectedBlock();
        baseWorldSection.forceApplyFade(1);
        baseWorldSection.setVisible(true);
        elements.add(baseWorldSection);
        if (world != null) {
            for (Entity entity : world.getEntities()) {
                EntityElement initialEntity = PonderElementFactories.get().createEntity(entity);
                initialEntity.setVisible(true);
                elements.add(initialEntity);
            }
        }
        snapshots.put(0, captureSnapshot());
    }

    public void restart() { begin(); }

    public void deselect() {
        forEach(WorldSectionElement.class, WorldSectionElement::resetSelectedBlock);
    }

    /** Finds the nearest visible section hit and marks only that section for identification. */
    public Pair<ItemStack, BlockPos> rayTraceScene(Vec3d from, Vec3d to) {
        return rayTraceScene(from, to, 1);
    }

    public Pair<ItemStack, BlockPos> rayTraceScene(Vec3d from, Vec3d to, float partialTicks) {
        if (world == null || from == null || to == null) {
            deselect();
            return Pair.of(ItemStack.EMPTY, null);
        }
        WorldSectionElement nearestSection = null;
        RayTraceResult nearestHit = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (PonderElement element : new ArrayList<PonderElement>(elements)) {
            if (!(element instanceof WorldSectionElement)) continue;
            WorldSectionElement section = (WorldSectionElement) element;
            section.resetSelectedBlock();
            if (!section.isVisible()) continue;
            Pair<Vec3d, RayTraceResult> result = section.rayTrace(world, from, to, partialTicks);
            if (result == null || result.getFirst() == null || result.getSecond() == null) continue;
            double distance = result.getFirst().squareDistanceTo(from);
            if (distance >= nearestDistance) continue;
            nearestDistance = distance;
            nearestSection = section;
            nearestHit = result.getSecond();
        }
        if (nearestSection == null || nearestHit == null) return Pair.of(ItemStack.EMPTY, null);

        BlockPos selectedPos = nearestHit.getBlockPos();
        if (selectedPos == null || !world.getOccupiedPositions().contains(selectedPos))
            return Pair.of(ItemStack.EMPTY, null);
        boolean basePlate = selectedPos.getY() == 0
            && selectedPos.getX() >= basePlateOffsetX && selectedPos.getX() < basePlateOffsetX + basePlateSize
            && selectedPos.getZ() >= basePlateOffsetZ && selectedPos.getZ() < basePlateOffsetZ + basePlateSize;
        if (basePlate) {
            if (PonderIndex.editingModeActive()) nearestSection.selectBlock(selectedPos);
            return Pair.of(ItemStack.EMPTY, selectedPos);
        }

        nearestSection.selectBlock(selectedPos);
        net.minecraft.block.state.IBlockState state = world.getBlockState(selectedPos);
        if (state.getBlock() == Blocks.AIR) return Pair.of(ItemStack.EMPTY, selectedPos);
        ItemStack picked = ItemStack.EMPTY;
        try {
            picked = CatnipServices.HOOKS.getCloneItemFromBlockstate(state, nearestHit, world, selectedPos, null);
        } catch (Throwable ignored) {
            // Some third-party blocks require a real player for pick-block; the metadata fallback remains useful.
        }
        if (picked == null || picked.isEmpty()) {
            Item item = Item.getItemFromBlock(state.getBlock());
            if (item != null) {
                try {
                    picked = new ItemStack(item, 1, state.getBlock().getMetaFromState(state));
                } catch (RuntimeException ignored) {
                    picked = new ItemStack(item);
                }
            }
        }
        return Pair.of(picked == null ? ItemStack.EMPTY : picked, selectedPos);
    }

    public synchronized void tick() {
        if (paused || finished)
            return;
        if (world != null)
            world.tickVirtualWorld();
        for (PonderElement element : new ArrayList<PonderElement>(elements))
            element.tick(this);
        for (ScheduledInstruction scheduled : schedule)
            if (scheduled.tick == currentTick)
                activeInstructions.add(scheduled.instruction);
        List<PonderInstruction> running = new ArrayList<PonderInstruction>(activeInstructions);
        for (PonderInstruction instruction : running) {
            instruction.tick(this);
            if (instruction.isComplete())
                activeInstructions.remove(instruction);
        }
        int completedTick = currentTick;
        currentTick++;
        if (keyframes.contains(completedTick) || keyframes.contains(currentTick))
            snapshots.put(currentTick, captureSnapshot());
        if (currentTick >= totalTicks && activeInstructions.isEmpty())
            finished = true;
    }

    public synchronized void seek(int targetTick) { seekToTime(targetTick); }

    public synchronized void seekToTime(int targetTick) {
        int target = Math.max(0, Math.min(targetTick, totalTicks));
        if (target < currentTick) {
            Map.Entry<Integer, RuntimeSnapshot> nearest = snapshots.floorEntry(target);
            if (nearest == null) {
                begin();
            } else {
                restoreSnapshot(nearest.getValue());
            }
        }
        boolean wasPaused = paused;
        paused = false;
        while (currentTick < target && !finished) {
            for (PonderElement element : new ArrayList<PonderElement>(elements))
                element.whileSkipping(this);
            tick();
        }
        paused = wasPaused;
        forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
    }

    private RuntimeSnapshot captureSnapshot() {
        Map<PonderInstruction, Object> instructionStates = new IdentityHashMap<PonderInstruction, Object>();
        for (ScheduledInstruction entry : schedule)
            instructionStates.put(entry.instruction, entry.instruction.captureState());
        Map<PonderElement, Object> elementStates = new IdentityHashMap<PonderElement, Object>();
        for (PonderElement element : elements)
            elementStates.put(element, element.captureState());
        return new RuntimeSnapshot(currentTick, finished, pointOfInterest, cursorPosition, cameraYaw, cameraPitch,
            world == null ? null : world.createSnapshot(), new ArrayList<PonderInstruction>(activeInstructions),
            new LinkedHashSet<PonderElement>(elements), new LinkedHashMap<UUID, PonderElement>(linkedElements),
            instructionStates, elementStates);
    }

    private void restoreSnapshot(RuntimeSnapshot snapshot) {
        currentTick = snapshot.currentTick;
        finished = snapshot.finished;
        pointOfInterest = snapshot.pointOfInterest;
        cursorPosition = snapshot.cursorPosition;
        cameraYaw = snapshot.cameraYaw;
        cameraPitch = snapshot.cameraPitch;
        activeInstructions.clear();
        activeInstructions.addAll(snapshot.activeInstructions);
        elements.clear();
        elements.addAll(snapshot.elements);
        linkedElements.clear();
        linkedElements.putAll(snapshot.links);
        if (world != null && snapshot.world != null)
            world.restoreSnapshot(snapshot.world);
        for (Map.Entry<PonderInstruction, Object> state : snapshot.instructionStates.entrySet())
            state.getKey().restoreState(state.getValue());
        for (Map.Entry<PonderElement, Object> state : snapshot.elementStates.entrySet())
            state.getKey().restoreState(state.getValue());
        forEach(EntityElement.class, element -> element.tick(this));
    }

    public void addElement(PonderElement element) { if (element != null) elements.add(element); }
    public void removeElement(PonderElement element) { if (element != baseWorldSection) elements.remove(element); }

    public <E extends PonderElement> void linkElement(E element, ElementLink<E> link) {
        if (element == null || link == null)
            throw new IllegalArgumentException("Linked element and link are required");
        linkedElements.put(link.getId(), element);
    }

    public void unlinkElement(ElementLink<?> link) {
        if (link != null)
            linkedElements.remove(link.getId());
    }

    @Nullable public <E extends PonderElement> E resolve(ElementLink<E> link) {
        return link == null ? null : link.cast(linkedElements.get(link.getId()));
    }

    public <E extends PonderElement> Optional<E> resolveOptional(ElementLink<E> link) {
        return Optional.ofNullable(resolve(link));
    }

    public <E extends PonderElement> void runWith(ElementLink<E> link, Consumer<E> callback) {
        E element = resolve(link);
        if (element != null) callback.accept(element);
    }

    @Nullable public <E extends PonderElement, R> R applyTo(ElementLink<E> link, Function<E, R> function) {
        E element = resolve(link);
        return element == null ? null : function.apply(element);
    }

    public void forEach(Consumer<? super PonderElement> consumer) {
        for (PonderElement element : new ArrayList<PonderElement>(elements)) consumer.accept(element);
    }

    public <T extends PonderElement> void forEach(Class<T> type, Consumer<T> consumer) {
        for (PonderElement element : new ArrayList<PonderElement>(elements))
            if (type.isInstance(element)) consumer.accept(type.cast(element));
    }

    public <T extends Entity> void forEachWorldEntity(Class<T> type, Consumer<T> consumer) {
        if (world == null) return;
        for (Entity entity : world.getEntities())
            if (type.isInstance(entity)) consumer.accept(type.cast(entity));
    }

    public Supplier<String> registerText(String defaultText) {
        final String key = "text_" + textIndex++;
        localization.registerSpecific(sceneId, key, defaultText);
        return () -> localization.getSpecific(sceneId, key);
    }

    public Supplier<String> registerText(String defaultText, final Object... parameters) {
        final String key = "text_" + textIndex++;
        localization.registerSpecific(sceneId, key, defaultText);
        return () -> localization.getSpecific(sceneId, key, parameters);
    }

    public void setSceneTitle(String id, String title) {
        sceneId = id.indexOf(':') >= 0 ? new ResourceLocation(id) : new ResourceLocation(namespace, id);
        localization.registerSpecific(sceneId, TITLE_KEY, title);
    }

    public Collection<PonderSceneElement> getSceneElements() {
        List<PonderSceneElement> result = new ArrayList<PonderSceneElement>();
        for (PonderElement element : elements) if (element instanceof PonderSceneElement) result.add((PonderSceneElement) element);
        return Collections.unmodifiableList(result);
    }

    public Collection<PonderOverlayElement> getOverlayElements() {
        List<PonderOverlayElement> result = new ArrayList<PonderOverlayElement>();
        for (PonderElement element : elements) if (element instanceof PonderOverlayElement) result.add((PonderOverlayElement) element);
        return Collections.unmodifiableList(result);
    }

    public Set<PonderElement> getElements() { return Collections.unmodifiableSet(new LinkedHashSet<PonderElement>(elements)); }
    public PonderWorld getWorld() { return world; }
    public String getNamespace() { return namespace; }
    public ResourceLocation getComponent() { return component; }
    public ResourceLocation getLocation() { return component; }
    public ResourceLocation getId() { return sceneId; }
    public String getTitle() { return localization.getSpecific(sceneId, TITLE_KEY); }
    public List<PonderTag> getTags() { return tags; }
    public List<SceneOrderingEntry> getOrderingEntries() { return orderingEntries; }
    public WorldSectionElement getBaseWorldSection() { return baseWorldSection; }
    public Vec3d getPointOfInterest() { return pointOfInterest; }
    public void setPointOfInterest(Vec3d point) { if (point != null) pointOfInterest = point; }
    public Vec3d getCursorPosition() { return cursorPosition; }
    public void setCursorPosition(Vec3d point) { if (point != null) cursorPosition = point; }
    public int getCurrentTick() { return currentTick; }
    public int getCurrentTime() { return currentTick; }
    public int getTotalTicks() { return totalTicks; }
    public int getTotalTime() { return totalTicks; }
    public int getScheduledInstructionCount() { return schedule.size(); }
    public float getSceneProgress() { return totalTicks == 0 ? 1 : Math.min(1, currentTick / (float) totalTicks); }
    public boolean isFinished() { return finished; }
    public void setFinished(boolean finished) { this.finished = finished; }
    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = paused; }
    public boolean isNextUpEnabled() { return nextUpEnabled; }
    public void setNextUpEnabled(boolean value) { nextUpEnabled = value; }
    public List<Integer> getKeyframes() { return Collections.unmodifiableList(new ArrayList<Integer>(keyframes)); }
    public int getKeyframeCount() { return keyframes.size(); }
    public int getKeyframeTime(int index) { return keyframes.get(index); }
    public int getBasePlateOffsetX() { return basePlateOffsetX; }
    public int getBasePlateOffsetZ() { return basePlateOffsetZ; }
    public int getBasePlateSize() { return basePlateSize; }
    public float getScale() { return scale; }
    public float getScaleFactor() { return scale; }
    public float getYOffset() { return yOffset; }
    public boolean isBasePlateShown() { return basePlateShown; }
    public boolean isShadowShown() { return shadowShown; }
    public boolean shouldHidePlatformShadow() { return !shadowShown; }
    public SceneTransform getTransform() { return transform; }
    public float getCameraYaw() { return cameraYaw; }
    public void setCameraYaw(float cameraYaw) { this.cameraYaw = cameraYaw; }
    public float getCameraPitch() { return cameraPitch; }
    public void setCameraPitch(float cameraPitch) { this.cameraPitch = Math.max(-90, Math.min(90, cameraPitch)); }
    public Outliner getOutliner() { return Outliner.getInstance(); }

    void configureBasePlate(int x, int z, int size) {
        if (size <= 0) throw new IllegalArgumentException("Base plate size must be positive");
        basePlateOffsetX = x; basePlateOffsetZ = z; basePlateSize = size;
    }
    void setScale(float value) { if (!(value > 0)) throw new IllegalArgumentException("Scene scale must be positive"); scale = value; }
    void setYOffset(float value) { yOffset = value; }
    void setBasePlateShown(boolean value) { basePlateShown = value; }
    void setShadowShown(boolean value) { shadowShown = value; }

    public static final class SceneTransform {
        private final Matrix4f transform;
        private final Matrix4f inverse;

        SceneTransform(BlockPos anchor) {
            transform = new Matrix4f();
            transform.setIdentity();
            transform.setTranslation(new Vector3f(anchor.getX(), anchor.getY(), anchor.getZ()));
            inverse = new Matrix4f(transform);
            inverse.invert();
        }

        public Matrix4f getTransform(float partialTicks) { return new Matrix4f(transform); }
        public Matrix4f getInverseTransform(float partialTicks) { return new Matrix4f(inverse); }
        public Vec3d toWorld(Vec3d local) { return apply(transform, local); }
        public Vec3d toLocal(Vec3d world) { return apply(inverse, world); }

        private static Vec3d apply(Matrix4f matrix, Vec3d vector) {
            Point3f point = new Point3f((float) vector.x, (float) vector.y, (float) vector.z);
            matrix.transform(point);
            return new Vec3d(point.x, point.y, point.z);
        }
    }

    private static final class ScheduledInstruction {
        final int tick;
        final PonderInstruction instruction;
        ScheduledInstruction(int tick, PonderInstruction instruction) { this.tick = tick; this.instruction = instruction; }
    }

    private static final class RuntimeSnapshot {
        final int currentTick;
        final boolean finished;
        final Vec3d pointOfInterest;
        final Vec3d cursorPosition;
        final float cameraYaw;
        final float cameraPitch;
        final PonderWorld.Snapshot world;
        final List<PonderInstruction> activeInstructions;
        final Set<PonderElement> elements;
        final Map<UUID, PonderElement> links;
        final Map<PonderInstruction, Object> instructionStates;
        final Map<PonderElement, Object> elementStates;

        RuntimeSnapshot(int currentTick, boolean finished, Vec3d pointOfInterest, Vec3d cursorPosition,
                        float cameraYaw, float cameraPitch,
                        PonderWorld.Snapshot world, List<PonderInstruction> activeInstructions,
                        Set<PonderElement> elements, Map<UUID, PonderElement> links,
                        Map<PonderInstruction, Object> instructionStates, Map<PonderElement, Object> elementStates) {
            this.currentTick = currentTick;
            this.finished = finished;
            this.pointOfInterest = pointOfInterest;
            this.cursorPosition = cursorPosition;
            this.cameraYaw = cameraYaw;
            this.cameraPitch = cameraPitch;
            this.world = world;
            this.activeInstructions = activeInstructions;
            this.elements = elements;
            this.links = links;
            this.instructionStates = instructionStates;
            this.elementStates = elementStates;
        }
    }
}
