package net.createmod.ponder.foundation.instruction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.SelectionImpl;
import net.createmod.ponder.foundation.registration.PonderLocalization;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

public class StaggeredDisplayWorldSectionInstructionTest {
    private static final BlockPos FIRST = new BlockPos(0, 1, 0);
    private static final BlockPos SECOND = new BlockPos(2, 1, 0);
    private static final BlockPos THIRD = new BlockPos(4, 1, 0);
    private static final BlockPos FOURTH = new BlockPos(6, 1, 0);
    private static final BlockPos AIR = new BlockPos(8, 1, 0);

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void filtersAirAndDistributesMultiBlockStartsAcrossFifteenTicks() {
        Fixture fixture = fixture(selection(FIRST, SECOND, THIRD, FOURTH, AIR), FIRST);
        fixture.scene.begin();
        fixture.scene.tick();

        assertEquals(15, fixture.instruction.getDuration());
        assertEquals(Arrays.asList(FIRST, SECOND, THIRD, FOURTH),
            fixture.instruction.getOrderedPositionsForTesting());
        assertEquals(0, fixture.instruction.getStartTickForTesting(0));
        assertEquals(3, fixture.instruction.getStartTickForTesting(1));
        assertEquals(6, fixture.instruction.getStartTickForTesting(2));
        assertEquals(9, fixture.instruction.getStartTickForTesting(3));
        assertEquals(1, fixture.instruction.getStartedBlockCountForTesting());
        assertEquals(4, fixture.instruction.getTemporarySectionCountForTesting());
        assertEquals(1, visibleTemporarySections(fixture));

        int previousStarted = 0;
        int previousMerged = 0;
        int previousVisibleBlocks = 0;
        while (!fixture.scene.isFinished()) {
            fixture.scene.tick();
            int started = fixture.instruction.getStartedBlockCountForTesting();
            int merged = fixture.instruction.getMergedBlockCountForTesting();
            int visibleBlocks = merged + visibleTemporarySections(fixture);
            assertTrue(started >= previousStarted);
            assertTrue(merged >= previousMerged);
            assertTrue(visibleBlocks >= previousVisibleBlocks);
            previousStarted = started;
            previousMerged = merged;
            previousVisibleBlocks = visibleBlocks;
        }

        assertEquals(4, fixture.instruction.getStartedBlockCountForTesting());
        assertEquals(4, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(0, fixture.instruction.getTemporarySectionCountForTesting());
        assertEquals(1, fixture.scene.getElements().size());
        assertFalse(fixture.scene.getBaseWorldSection().isEmpty());
        assertEquals(FIRST, rayTrace(fixture, FIRST).getBlockPos());
        assertEquals(FOURTH, rayTrace(fixture, FOURTH).getBlockPos());
    }

    @Test
    public void singleBlockUsesTheFullFifteenTickFade() {
        Fixture fixture = fixture(SelectionImpl.of(FIRST), FIRST);
        fixture.scene.begin();
        fixture.scene.seek(14);

        assertEquals(1, fixture.instruction.getStartedBlockCountForTesting());
        assertEquals(0, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(1, fixture.instruction.getTemporarySectionCountForTesting());

        fixture.scene.seek(15);
        assertEquals(1, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(0, fixture.instruction.getTemporarySectionCountForTesting());
        assertEquals(1, fixture.scene.getElements().size());
    }

    @Test
    public void overlappingTargetBlockIsRemovedUntilItsRevealMerges() {
        Fixture fixture = fixture(SelectionImpl.of(FIRST), FIRST);
        fixture.scene.begin();
        fixture.scene.getBaseWorldSection().add(SelectionImpl.of(FIRST));
        fixture.scene.tick();

        assertTrue(fixture.scene.getBaseWorldSection().isEmpty());
        assertEquals(1, visibleTemporarySections(fixture));

        fixture.scene.seek(15);
        assertFalse(fixture.scene.getBaseWorldSection().isEmpty());
        assertEquals(1, fixture.scene.getElements().size());
    }

    @Test
    public void explicitOriginUsesStableManhattanThenYZXOrdering() {
        BlockPos origin = new BlockPos(1, 1, 1);
        BlockPos below = new BlockPos(1, 0, 1);
        BlockPos north = new BlockPos(1, 1, 0);
        BlockPos west = new BlockPos(0, 1, 1);
        BlockPos east = new BlockPos(2, 1, 1);
        BlockPos south = new BlockPos(1, 1, 2);
        BlockPos above = new BlockPos(1, 2, 1);
        Fixture fixture = fixture(selection(south, east, above, origin, west, north, below), origin);
        fixture.scene.begin();
        fixture.scene.tick();

        assertEquals(Arrays.asList(origin, below, north, west, east, south, above),
            fixture.instruction.getOrderedPositionsForTesting());
    }

    @Test
    public void defaultOriginUsesSelectionCenterAndStableTieBreakers() {
        BlockPos west = new BlockPos(0, 1, 0);
        BlockPos east = new BlockPos(2, 1, 0);
        BlockPos north = new BlockPos(1, 1, -1);
        BlockPos south = new BlockPos(1, 1, 1);
        Fixture fixture = fixtureWithoutOrigin(selection(east, south, west, north));
        fixture.scene.begin();
        fixture.scene.tick();

        assertEquals(Arrays.asList(north, west, east, south),
            fixture.instruction.getOrderedPositionsForTesting());
    }

    @Test
    public void keyframeRestoreAndReplayRecreateTheSameRuntimeState() {
        Fixture fixture = fixture(selection(FIRST, SECOND, THIRD, FOURTH), FIRST);
        fixture.scene.declareKeyframe(8);
        fixture.scene.begin();
        fixture.scene.seek(8);
        int elementsAtKeyframe = fixture.scene.getElements().size();
        int startedAtKeyframe = fixture.instruction.getStartedBlockCountForTesting();
        int mergedAtKeyframe = fixture.instruction.getMergedBlockCountForTesting();
        int temporaryAtKeyframe = fixture.instruction.getTemporarySectionCountForTesting();

        fixture.scene.seek(15);
        assertEquals(4, fixture.instruction.getMergedBlockCountForTesting());
        fixture.scene.seek(8);
        assertEquals(elementsAtKeyframe, fixture.scene.getElements().size());
        assertEquals(startedAtKeyframe, fixture.instruction.getStartedBlockCountForTesting());
        assertEquals(mergedAtKeyframe, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(temporaryAtKeyframe, fixture.instruction.getTemporarySectionCountForTesting());

        fixture.scene.seek(15);
        assertEquals(1, fixture.scene.getElements().size());
        fixture.scene.seek(0);
        assertTrue(fixture.instruction.getOrderedPositionsForTesting().isEmpty());
        assertTrue(fixture.scene.getBaseWorldSection().isEmpty());
        fixture.scene.seek(15);
        assertEquals(Arrays.asList(FIRST, SECOND, THIRD, FOURTH),
            fixture.instruction.getOrderedPositionsForTesting());
        assertEquals(4, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(1, fixture.scene.getElements().size());

        fixture.scene.restart();
        assertTrue(fixture.instruction.getOrderedPositionsForTesting().isEmpty());
        assertTrue(fixture.scene.getBaseWorldSection().isEmpty());
        fixture.scene.seek(15);
        assertEquals(Arrays.asList(FIRST, SECOND, THIRD, FOURTH),
            fixture.instruction.getOrderedPositionsForTesting());
        assertEquals(4, fixture.instruction.getMergedBlockCountForTesting());
        assertEquals(1, fixture.scene.getElements().size());
    }

    private static Fixture fixture(Selection selection, BlockPos origin) {
        return createFixture(selection, origin);
    }

    private static Fixture fixtureWithoutOrigin(Selection selection) {
        return createFixture(selection, null);
    }

    private static Fixture createFixture(Selection selection, BlockPos origin) {
        PonderLevel world = new PonderLevel(BlockPos.ORIGIN, null);
        for (BlockPos pos : selection)
            if (!AIR.equals(pos))
                world.setBlockState(pos, Blocks.STONE.getDefaultState(), 0);
        world.backup();
        PonderScene scene = new PonderScene(world, new PonderLocalization(), "test",
            new ResourceLocation("test", "staggered"), Collections.emptyList(), Collections.emptyList());
        StaggeredDisplayWorldSectionInstruction instruction = origin == null
            ? new StaggeredDisplayWorldSectionInstruction(selection, scene::getBaseWorldSection)
            : new StaggeredDisplayWorldSectionInstruction(selection, scene::getBaseWorldSection, origin);
        scene.schedule(instruction);
        return new Fixture(scene, instruction);
    }

    private static Selection selection(BlockPos... positions) {
        Selection selection = SelectionImpl.empty();
        for (BlockPos pos : positions)
            selection.add(SelectionImpl.of(pos));
        return selection;
    }

    private static RayTraceResult rayTrace(Fixture fixture, BlockPos pos) {
        double x = pos.getX() + .5;
        double z = pos.getZ() + .5;
        return fixture.scene.getBaseWorldSection().rayTrace(fixture.scene.getWorld(),
            new Vec3d(x, 10, z), new Vec3d(x, -10, z)).getSecond();
    }

    private static int visibleTemporarySections(Fixture fixture) {
        int visible = 0;
        for (net.createmod.ponder.api.element.PonderElement element : fixture.scene.getElements())
            if (element != fixture.scene.getBaseWorldSection() && element.isVisible()) visible++;
        return visible;
    }

    private static final class Fixture {
        final PonderScene scene;
        final StaggeredDisplayWorldSectionInstruction instruction;

        Fixture(PonderScene scene, StaggeredDisplayWorldSectionInstruction instruction) {
            this.scene = scene;
            this.instruction = instruction;
        }
    }
}
