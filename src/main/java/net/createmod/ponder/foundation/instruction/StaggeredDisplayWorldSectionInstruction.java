package net.createmod.ponder.foundation.instruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderElementFactories;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.SelectionImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** Internal implementation for deterministic, block-by-block section reveals. */
public final class StaggeredDisplayWorldSectionInstruction extends TickingInstruction {
    private static final int TOTAL_TICKS = 15;
    private static final int MULTI_BLOCK_FADE_TICKS = 6;
    private static final int LAST_START_TICK = TOTAL_TICKS - MULTI_BLOCK_FADE_TICKS;
    private static final Vec3d FADE_FROM_BELOW = new Vec3d(EnumFacing.DOWN.getDirectionVec());

    private final Selection selection;
    private final Supplier<WorldSectionElement> targetSupplier;
    private final BlockPos explicitOrigin;

    private List<BlockPos> orderedPositions = Collections.emptyList();
    private List<WorldSectionElement> temporarySections = Collections.emptyList();
    private int[] startTicks = new int[0];
    private boolean[] merged = new boolean[0];
    private WorldSectionElement target;
    private Object targetInitialState;

    public StaggeredDisplayWorldSectionInstruction(Selection selection,
                                                    Supplier<WorldSectionElement> targetSupplier) {
        this(selection, targetSupplier, null);
    }

    public StaggeredDisplayWorldSectionInstruction(Selection selection,
                                                    Supplier<WorldSectionElement> targetSupplier,
                                                    BlockPos origin) {
        super(false, TOTAL_TICKS);
        if (selection == null)
            throw new IllegalArgumentException("Selection is required");
        if (targetSupplier == null)
            throw new IllegalArgumentException("Target section supplier is required");
        this.selection = selection.copy();
        this.targetSupplier = targetSupplier;
        this.explicitOrigin = origin == null ? null : origin.toImmutable();
    }

    @Override
    protected void firstTick(PonderScene scene) {
        target = targetSupplier.get();
        if (target == null)
            target = scene.getBaseWorldSection();
        targetInitialState = target.captureState();

        orderedPositions = collectAndSort(scene);
        int count = orderedPositions.size();
        Selection animatedSelection = SelectionImpl.empty();
        for (BlockPos position : orderedPositions)
            animatedSelection.add(SelectionImpl.of(position));
        target.erase(animatedSelection);
        startTicks = new int[count];
        merged = new boolean[count];
        temporarySections = new ArrayList<WorldSectionElement>(count);
        for (int index = 0; index < count; index++) {
            startTicks[index] = calculateStartTick(index, count);
            WorldSectionElement section = PonderElementFactories.get()
                .createWorldSection(SelectionImpl.of(orderedPositions.get(index)));
            section.setFadeVec(FADE_FROM_BELOW);
            section.forceApplyFade(0);
            section.setVisible(false);
            section.queueRedraw();
            temporarySections.add(section);
            scene.addElement(section);
        }
    }

    @Override
    protected void tickRunning(PonderScene scene, int elapsed, float progress) {
        int count = temporarySections.size();
        int fadeTicks = count == 1 ? TOTAL_TICKS : MULTI_BLOCK_FADE_TICKS;
        for (int index = 0; index < count; index++) {
            if (merged[index])
                continue;
            int localElapsed = elapsed - startTicks[index];
            if (localElapsed <= 0)
                continue;

            WorldSectionElement section = temporarySections.get(index);
            section.setVisible(true);
            float localProgress = Math.min(1, localElapsed / (float) Math.max(1, fadeTicks - 1));
            section.setFade(smooth(localProgress));
            if (localElapsed >= fadeTicks)
                merge(scene, index);
        }
    }

    @Override
    protected void finish(PonderScene scene) {
        for (int index = 0; index < temporarySections.size(); index++)
            if (!merged[index])
                merge(scene, index);
    }

    @Override
    public void reset(PonderScene scene) {
        for (WorldSectionElement section : temporarySections)
            scene.removeElement(section);
        if (target != null && targetInitialState != null)
            target.restoreState(targetInitialState);
        clearRuntimeState();
        super.reset(scene);
    }

    @Override
    public Object captureState() {
        return new State(super.captureState(), orderedPositions, temporarySections, startTicks, merged,
            target, targetInitialState);
    }

    @Override
    public void restoreState(Object value) {
        if (!(value instanceof State)) {
            super.restoreState(value);
            return;
        }
        State state = (State) value;
        super.restoreState(state.tickingState);
        orderedPositions = new ArrayList<BlockPos>(state.orderedPositions);
        temporarySections = new ArrayList<WorldSectionElement>(state.temporarySections);
        startTicks = state.startTicks.clone();
        merged = state.merged.clone();
        target = state.target;
        targetInitialState = state.targetInitialState;
    }

    private List<BlockPos> collectAndSort(PonderScene scene) {
        List<BlockPos> positions = new ArrayList<BlockPos>();
        for (BlockPos pos : selection) {
            if (scene.getWorld() != null) {
                IBlockState state = scene.getWorld().getBlockState(pos);
                if (state.getBlock().isAir(state, scene.getWorld(), pos))
                    continue;
            }
            positions.add(pos.toImmutable());
        }

        final Vec3d origin = explicitOrigin == null
            ? selection.getCenter()
            : new Vec3d(explicitOrigin).add(.5, .5, .5);
        Collections.sort(positions, new Comparator<BlockPos>() {
            @Override
            public int compare(BlockPos first, BlockPos second) {
                int distance = Double.compare(manhattan(first, origin), manhattan(second, origin));
                if (distance != 0) return distance;
                int y = Integer.compare(first.getY(), second.getY());
                if (y != 0) return y;
                int z = Integer.compare(first.getZ(), second.getZ());
                if (z != 0) return z;
                return Integer.compare(first.getX(), second.getX());
            }
        });
        return positions;
    }

    private void merge(PonderScene scene, int index) {
        WorldSectionElement section = temporarySections.get(index);
        section.forceApplyFade(1);
        section.mergeOnto(target);
        target.setVisible(true);
        target.queueRedraw();
        scene.removeElement(section);
        merged[index] = true;
    }

    private void clearRuntimeState() {
        orderedPositions = Collections.emptyList();
        temporarySections = Collections.emptyList();
        startTicks = new int[0];
        merged = new boolean[0];
        target = null;
        targetInitialState = null;
    }

    private static int calculateStartTick(int index, int count) {
        if (count <= 1)
            return 0;
        return Math.round(index * LAST_START_TICK / (float) (count - 1));
    }

    private static double manhattan(BlockPos pos, Vec3d origin) {
        return Math.abs(pos.getX() + .5 - origin.x)
            + Math.abs(pos.getY() + .5 - origin.y)
            + Math.abs(pos.getZ() + .5 - origin.z);
    }

    private static float smooth(float value) {
        return value * value * (3 - 2 * value);
    }

    // Package-private probes keep client harnesses independent of concrete section implementations.
    List<BlockPos> getOrderedPositionsForTesting() {
        return Collections.unmodifiableList(new ArrayList<BlockPos>(orderedPositions));
    }

    int getStartTickForTesting(int index) {
        return startTicks[index];
    }

    int getTemporarySectionCountForTesting() {
        int count = 0;
        for (boolean value : merged)
            if (!value) count++;
        return count;
    }

    int getStartedBlockCountForTesting() {
        int elapsed = TOTAL_TICKS - remainingTicks;
        int count = 0;
        for (int startTick : startTicks)
            if (elapsed > startTick) count++;
        return count;
    }

    int getMergedBlockCountForTesting() {
        int count = 0;
        for (boolean value : merged)
            if (value) count++;
        return count;
    }

    private static final class State {
        final Object tickingState;
        final List<BlockPos> orderedPositions;
        final List<WorldSectionElement> temporarySections;
        final int[] startTicks;
        final boolean[] merged;
        final WorldSectionElement target;
        final Object targetInitialState;

        State(Object tickingState, List<BlockPos> orderedPositions,
              List<WorldSectionElement> temporarySections, int[] startTicks, boolean[] merged,
              WorldSectionElement target, Object targetInitialState) {
            this.tickingState = tickingState;
            this.orderedPositions = new ArrayList<BlockPos>(orderedPositions);
            this.temporarySections = new ArrayList<WorldSectionElement>(temporarySections);
            this.startTicks = startTicks.clone();
            this.merged = merged.clone();
            this.target = target;
            this.targetInitialState = targetInitialState;
        }
    }
}
