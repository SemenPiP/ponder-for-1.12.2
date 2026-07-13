package net.createmod.ponder.verification;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.MinecartElement;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.PonderWorld;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI.CacheState;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI.CameraState;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI.DepthSnapshot;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI.RenderPassSnapshot;
import net.createmod.ponder.foundation.ui.PonderClientHarnessUI.SectionSnapshot;
import net.createmod.ponder.render.PonderRenderHarnessProbe;
import net.createmod.ponder.render.PonderRenderHarnessProbe.ItemRenderY;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/** Runs a deterministic, development-only visual and state smoke test, then exits Minecraft. */
public final class ClientSmokeController {
    private static final ResourceLocation SUBJECT = new ResourceLocation("minecraft", "crafting_table");
    private static final ResourceLocation[] ADDITIONAL_COMPONENTS = {
        component("chest"), component("furnace"), component("piston"), component("redstone_lamp"),
        component("glass"), component("water_bucket"), component("rail")
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_TIMEOUT_TICKS = 12000;
    private static final int FULL_SCENE_TIMEOUT_TICKS = 1100;

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final File outputDirectory;
    private final File screenshotDirectory;
    private final int timeoutTicks;
    private final List<Step> steps = new ArrayList<Step>();
    private final List<CheckRecord> checks = new ArrayList<CheckRecord>();
    private final List<ScreenshotRecord> screenshots = new ArrayList<ScreenshotRecord>();
    private final long startedAt = System.currentTimeMillis();

    private PonderClientHarnessUI ui;
    private boolean manualPonderLifecycle;
    private boolean armed;
    private boolean terminal;
    private int clientTicks;
    private int renderFrames;
    private int stepIndex;
    private int shutdownAtTick = Integer.MAX_VALUE;
    private int originalGuiScale;
    private boolean originalFullscreen;
    private boolean initialWindowStateCaptured;
    private long fullscreenTransitionFrame;
    private String initialDigest;
    private String keyframeDigest;
    private int pausedAtTick;
    private CameraState cameraBefore;
    private String pendingScreenshot;
    private String completedScreenshot;
    private Throwable screenshotFailure;
    private int revealFloorStartBlocks;
    private int revealFloorMidBlocks;
    private int revealUpperMidBlocks;

    public ClientSmokeController() {
        outputDirectory = resolveOutputDirectory();
        screenshotDirectory = new File(outputDirectory, "screenshots");
        timeoutTicks = positiveIntegerProperty("ponder.clientHarness.timeoutTicks", DEFAULT_TIMEOUT_TICKS);
        prepareOutputDirectory();
        writeReport("STARTING", null);
    }

    public boolean hasFailed() {
        return terminal;
    }

    public void arm(boolean manualLifecycle) {
        if (terminal || armed) return;
        manualPonderLifecycle = manualLifecycle;
        originalGuiScale = minecraft.gameSettings.guiScale;
        originalFullscreen = minecraft.isFullScreen();
        initialWindowStateCaptured = true;
        buildScript();
        armed = true;
        writeReport("RUNNING", null);
    }

    public void failDuringLifecycle(String phase, Throwable throwable) {
        fail("lifecycle." + phase, throwable);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        try {
            clientTicks++;
            if (terminal) {
                if (clientTicks >= shutdownAtTick) minecraft.shutdown();
                return;
            }
            if (!armed) return;
            if (clientTicks > timeoutTicks) {
                throw new IllegalStateException("Client harness exceeded " + timeoutTicks + " ticks");
            }
            if (stepIndex >= steps.size()) {
                completeSuccessfully();
                return;
            }
            Step step = steps.get(stepIndex);
            if (step.tick()) {
                checks.add(new CheckRecord(step.name, "PASS", step.detail));
                stepIndex++;
                writeReport("RUNNING", null);
            }
        } catch (Throwable throwable) {
            String name = stepIndex < steps.size() ? steps.get(stepIndex).name : "client.tick";
            fail(name, throwable);
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || terminal) return;
        renderFrames++;
        if (pendingScreenshot == null) return;
        String name = pendingScreenshot;
        pendingScreenshot = null;
        try {
            captureScreenshot(name);
            completedScreenshot = name;
        } catch (Throwable throwable) {
            screenshotFailure = throwable;
            completedScreenshot = name;
        }
    }

    private void buildScript() {
        addWait("client.ready", 200, new Condition() {
            @Override public boolean get() {
                return Display.isCreated() && minecraft.getFramebuffer() != null
                    && minecraft.displayWidth > 0 && minecraft.displayHeight > 0;
            }
        }, "Minecraft display and framebuffer are ready");

        addAction("ponder.open", new Action() {
            @Override public String run() {
                check(net.createmod.ponder.foundation.PonderIndex.getSceneAccess().doScenesExistForId(SUBJECT),
                    "crafting table storyboard was not registered");
                ui = PonderClientHarnessUI.create(SUBJECT);
                initialDigest = sceneDigest(ui.getActiveScene());
                minecraft.displayGuiScreen(ui);
                check(minecraft.currentScreen == ui, "Ponder UI did not become the active screen");
                return "Opened " + SUBJECT + " with " + ui.getActiveScene().getTotalTicks() + " timeline ticks";
            }
        });
        addWaitFrames("ponder.first_frame", 3, "Ponder UI rendered at least three frames");
        addDepthCheck("render.depth_before_overlay");

        addAction("demo.structure", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                PonderWorld world = scene.getWorld();
                check(world != null, "scene has no virtual world");
                check(world.getOccupiedPositions().size() >= 34, "demo structure is incomplete");
                check(hasBlock(world, Blocks.GLASS), "demo has no glass");
                check(hasBlock(world, Blocks.WATER), "demo has no water");
                check(hasBlock(world, Blocks.CHEST), "demo has no chest");
                check(hasBlock(world, Blocks.TORCH), "demo has no cutout block");
                check(hasBlock(world, Blocks.LEAVES), "demo has no leaves");
                check(hasBlock(world, Blocks.GRASS), "demo has no stable cutout-mipped block");

                EnumSet<BlockRenderLayer> layers = EnumSet.noneOf(BlockRenderLayer.class);
                for (BlockPos pos : world.getOccupiedPositions()) {
                    IBlockState state = world.getBlockState(pos);
                    for (BlockRenderLayer layer : BlockRenderLayer.values())
                        if (state.getBlock().canRenderInLayer(state, layer)) layers.add(layer);
                }
                check(layers.containsAll(EnumSet.allOf(BlockRenderLayer.class)),
                    "demo does not cover all block render layers: " + layers);

                TileEntity tile = world.getTileEntity(new BlockPos(3, 1, 2));
                check(tile instanceof TileEntityChest, "expected chest tile entity at 3,1,2");
                check(TileEntityRendererDispatcher.instance.getRenderer(tile) != null,
                    "chest has no TESR registered");
                check(containsEntity(world, EntityArmorStand.class), "demo armor stand was not loaded");
                int entityElements = 0;
                for (PonderElement element : scene.getElements()) if (element instanceof EntityElement) entityElements++;
                check(entityElements >= 1, "loaded entity has no scene element");
                check(scene.getKeyframeCount() >= 3, "demo has fewer than three keyframes");
                return "blocks=" + world.getOccupiedPositions().size() + ", layers=" + layers
                    + ", keyframes=" + scene.getKeyframes();
            }
        });
        addScreenshot("01-initial.png");

        addWorkbenchRevealCheckpoint("floor_start", 1, "01-reveal-floor-start.png");
        addWorkbenchRevealCheckpoint("floor_mid", 7, "01-reveal-floor-mid.png");
        addWorkbenchRevealCheckpoint("upper_mid", 27, "01-reveal-upper-mid.png");
        addWorkbenchRevealCheckpoint("complete", 38, "01-reveal-complete.png");
        addAction("reveal.reset_for_autoplay", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                ui.seekToTime(0);
                scene.setPaused(false);
                check(scene.getCurrentTick() == 0, "workbench reveal did not reset to tick zero");
                SectionSnapshot sections = ui.harnessSectionSnapshot();
                check(sections.visibleBlocks == 0 && sections.temporarySections == 0,
                    "workbench reveal left sections behind after reset: " + sections);
                return "reset staggered reveal to tick zero";
            }
        });

        addWait("timeline.autoplay", 120, new Condition() {
            @Override public boolean get() { return activeScene().getCurrentTick() >= 45; }
        }, "Scene advanced through the first keyframe");
        addWaitFrames("render.layers.frame", 2, "Layer caches had time to build");
        addDepthCheck("render.layers_depth");
        addAction("render.layers_tesr", new Action() {
            @Override public String run() {
                CacheState state = ui.harnessCacheState();
                check(state.clean > 0, "no section cache was built");
                check(state.layers.containsAll(EnumSet.allOf(BlockRenderLayer.class)),
                    "section cache is missing layers: " + state);
                for (BlockRenderLayer layer : BlockRenderLayer.values())
                    check(state.vertexCount(layer) > 0, "section cache has no vertices for " + layer + ": " + state);
                check(state.cachedTiles >= 1, "section cache did not retain the chest TESR");
                return state.toString();
            }
        });
        addScreenshot("02-four-layers-tesr.png");

        addAction("timeline.keyframe_roundtrip", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                int keyframe = scene.getKeyframeTime(1);
                ui.seekToTime(keyframe);
                check(scene.getCurrentTick() == keyframe, "seek missed keyframe " + keyframe);
                keyframeDigest = sceneDigest(scene);
                ui.seekToTime(Math.min(scene.getTotalTicks(), keyframe + 24));
                ui.seekToTime(keyframe);
                String replayed = sceneDigest(scene);
                check(keyframeDigest.equals(replayed), "complete keyframe state changed after seek roundtrip");
                return "keyframe=" + keyframe + ", digest=" + sha256(keyframeDigest.getBytes(StandardCharsets.UTF_8));
            }
        });
        addAction("timeline.section_entity_particle", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                int target = Math.min(scene.getTotalTicks(), scene.getKeyframeTime(2) + 7);
                ui.seekToTime(target);
                check(containsEntity(scene.getWorld(), EntityArmorStand.class), "armor stand disappeared after seek");
                check(containsEntity(scene.getWorld(), EntityItem.class), "scripted item entity was not created");
                boolean transformed = false;
                int sectionCount = 0;
                for (PonderElement element : scene.getElements()) {
                    if (!(element instanceof WorldSectionElementImpl)) continue;
                    sectionCount++;
                    WorldSectionElementImpl section = (WorldSectionElementImpl) element;
                    if (section.getAnimatedOffset().squareDistanceTo(Vec3d.ZERO) > .01
                        || section.getAnimatedRotation().squareDistanceTo(Vec3d.ZERO) > .01) transformed = true;
                }
                check(sectionCount >= 2, "independent section was not created");
                check(transformed, "independent section did not retain its transform");
                return "tick=" + target + ", sections=" + sectionCount;
            }
        });
        addWait("render.particles", 40, new Condition() {
            @Override public boolean get() { return ui.harnessParticleCount() > 0; }
        }, "Virtual particle manager contains live particles");
        addScreenshot("03-keyframe-section-entity-particles.png");
        addBookRenderYStability(70);

        addAction("controls.pause", new Action() {
            @Override public String run() throws Exception {
                ui.harnessPressKey(Keyboard.KEY_SPACE);
                check(activeScene().isPaused(), "space did not pause scene playback");
                pausedAtTick = activeScene().getCurrentTick();
                return "paused at tick " + pausedAtTick;
            }
        });
        addDelayTicks("controls.pause_holds", 4, new Validation() {
            @Override public String validate() {
                check(activeScene().isPaused(), "pause state was lost");
                check(activeScene().getCurrentTick() == pausedAtTick, "timeline advanced while paused");
                return "timeline remained at tick " + pausedAtTick;
            }
        });
        addScreenshot("04-paused.png");
        addAction("controls.resume", new Action() {
            @Override public String run() throws Exception {
                ui.harnessPressKey(Keyboard.KEY_SPACE);
                check(!activeScene().isPaused(), "space did not resume scene playback");
                return "playback resumed";
            }
        });

        addAction("controls.rotate_zoom", new Action() {
            @Override public String run() throws Exception {
                cameraBefore = ui.harnessCameraState();
                int centerX = ui.width / 2;
                int centerY = ui.height / 2;
                ui.harnessDrag(centerX, centerY, centerX + 52, centerY + 24);
                ui.harnessZoomBy(.72f);
                CameraState after = ui.harnessCameraState();
                check(after.differsFrom(cameraBefore), "camera did not react to drag and zoom");
                check(Math.abs(after.zoom - cameraBefore.zoom) > .001f, "zoom did not change");
                return "before={" + cameraBefore + "}, after={" + after + "}";
            }
        });
        addScreenshot("05-rotated-zoomed.png");

        addAction("controls.identify.enable", new Action() {
            @Override public String run() throws Exception {
                ui.seekToTime(45);
                ui.harnessPressKey(Keyboard.KEY_Q);
                check(ui.harnessIdentifyMode(), "Q did not enable identify mode");
                Mouse.setCursorPosition(minecraft.displayWidth / 2, minecraft.displayHeight / 2);
                return "identify mode enabled at the scene center";
            }
        });
        addWait("controls.identify.hit", 40, new Condition() {
            @Override public boolean get() { return ui.harnessIdentifiedBlock() != null; }
        }, "Identify ray selected a transformed scene block");
        addAction("controls.identify.result", new Action() {
            @Override public String run() {
                check(ui.harnessIdentifyMode(), "identify mode ended unexpectedly");
                BlockPos block = ui.harnessIdentifiedBlock();
                check(block != null, "identify mode produced no block hit");
                return "block=" + block + ", stack=" + ui.harnessIdentifiedStack();
            }
        });
        addScreenshot("06-identify.png");
        addAction("controls.identify.disable", new Action() {
            @Override public String run() throws Exception {
                ui.harnessPressKey(Keyboard.KEY_Q);
                check(!ui.harnessIdentifyMode(), "Q did not disable identify mode");
                return "identify mode disabled";
            }
        });

        addAction("controls.replay", new Action() {
            @Override public String run() throws Exception {
                ui.harnessPressKey(Keyboard.KEY_R);
                check(activeScene().getCurrentTick() == 0, "replay did not return to tick zero");
                check(initialDigest.equals(sceneDigest(activeScene())), "replay did not restore the complete initial state");
                return "initial digest restored: " + sha256(initialDigest.getBytes(StandardCharsets.UTF_8));
            }
        });
        addWait("controls.replay_advances", 40, new Condition() {
            @Override public boolean get() { return activeScene().getCurrentTick() >= 8; }
        }, "Replayed scene resumed deterministic playback");
        addScreenshot("07-replay.png");

        addAction("resources.prepare", new Action() {
            @Override public String run() {
                ui.seekToTime(45);
                return "returned to a fully visible four-layer section";
            }
        });
        addWaitFrames("resources.cache_built", 3, "Section cache rebuilt before reload");
        addAction("resources.reload", new Action() {
            @Override public String run() {
                CacheState before = ui.harnessCacheState();
                check(before.clean > 0, "reload precondition has no clean section cache: " + before);
                minecraft.refreshResources();
                CacheState after = ui.harnessCacheState();
                check(after.sections > 0 && after.dirty == after.sections,
                    "resource reload did not invalidate every section cache: " + after);
                return "before={" + before + "}, after={" + after + "}";
            }
        });
        addWaitFrames("resources.rebuild_frames", 4, "Ponder rendered after resource reload");
        addAction("resources.rebuilt", new Action() {
            @Override public String run() {
                CacheState state = ui.harnessCacheState();
                check(state.clean > 0, "section cache stayed dirty after reload");
                check(state.layers.containsAll(EnumSet.allOf(BlockRenderLayer.class)),
                    "reloaded cache lost render layers: " + state);
                return state.toString();
            }
        });
        addScreenshot("08-resource-reloaded.png");

        addAction("gui_scale.pause", new Action() {
            @Override public String run() throws Exception {
                if (!activeScene().isPaused()) ui.harnessPressKey(Keyboard.KEY_SPACE);
                check(activeScene().isPaused(), "could not freeze scene for GUI scale checks");
                return "scene frozen for layout comparisons";
            }
        });
        for (int scale = 1; scale <= 4; scale++) addGuiScaleSteps(scale);
        addAction("gui_scale.restore", new Action() {
            @Override public String run() {
                minecraft.gameSettings.guiScale = originalGuiScale;
                minecraft.resize(minecraft.displayWidth, minecraft.displayHeight);
                return "restored requested GUI scale " + originalGuiScale;
            }
        });

        addAction("fullscreen.toggle", new Action() {
            @Override public String run() throws Exception {
                check(Display.isCreated(), "display disappeared before fullscreen transition");
                RenderPassSnapshot pass = requireRenderPassSnapshot();
                fullscreenTransitionFrame = pass.frame;
                check(minecraft.isFullScreen() == originalFullscreen,
                    "Minecraft fullscreen state changed before the harness transition");
                minecraft.toggleFullscreen();
                check(minecraft.isFullScreen() != originalFullscreen,
                    "Minecraft did not toggle its fullscreen state");
                return "requested fullscreen=" + minecraft.isFullScreen() + " from=" + originalFullscreen;
            }
        });
        addWait("fullscreen.display_sync", 100, new Condition() {
            @Override public boolean get() {
                return Display.isCreated() && Display.isFullscreen() == minecraft.isFullScreen()
                    && minecraft.displayWidth > 0 && minecraft.displayHeight > 0;
            }
        }, "LWJGL display and Minecraft fullscreen states agree");
        addWaitFrames("fullscreen.changed_frames", 5,
            "Ponder rendered after the fullscreen transition");
        addAction("fullscreen.changed_state", new Action() {
            @Override public String run() {
                RenderPassSnapshot pass = requireRenderPassSnapshot();
                check(pass.frame > fullscreenTransitionFrame, "no Ponder frame was captured after fullscreen toggle");
                check(minecraft.currentScreen == ui, "Ponder UI closed during fullscreen toggle");
                check(ui.width > 0 && ui.height > 0, "fullscreen produced invalid UI dimensions");
                check(ui.harnessControlsFit(), "Ponder controls do not fit after fullscreen toggle");
                DepthSnapshot depth = requireDepthSnapshot();
                return "fullscreen=" + minecraft.isFullScreen() + ", display=" + Display.isFullscreen()
                    + ", ui=" + ui.width + "x" + ui.height + ", depth={" + depth + "}, gl={" + pass + "}";
            }
        });
        addScreenshot("13-fullscreen-toggled.png");
        addAction("fullscreen.restore", new Action() {
            @Override public String run() throws Exception {
                fullscreenTransitionFrame = requireRenderPassSnapshot().frame;
                minecraft.toggleFullscreen();
                check(minecraft.isFullScreen() == originalFullscreen,
                    "Minecraft did not restore its original fullscreen state");
                return "requested original fullscreen=" + originalFullscreen;
            }
        });
        addWait("fullscreen.restore_display_sync", 100, new Condition() {
            @Override public boolean get() {
                return Display.isCreated() && Display.isFullscreen() == originalFullscreen
                    && minecraft.isFullScreen() == originalFullscreen
                    && minecraft.displayWidth > 0 && minecraft.displayHeight > 0;
            }
        }, "LWJGL display returned to its original fullscreen state");
        addWaitFrames("fullscreen.restored_frames", 5,
            "Ponder rendered after restoring the original window mode");
        addAction("fullscreen.restored_state", new Action() {
            @Override public String run() {
                RenderPassSnapshot pass = requireRenderPassSnapshot();
                check(pass.frame > fullscreenTransitionFrame, "no Ponder frame was captured after fullscreen restore");
                check(minecraft.currentScreen == ui, "Ponder UI closed while restoring fullscreen");
                check(ui.harnessControlsFit(), "Ponder controls do not fit after fullscreen restore");
                DepthSnapshot depth = requireDepthSnapshot();
                return "fullscreen=" + minecraft.isFullScreen() + ", ui=" + ui.width + "x" + ui.height
                    + ", depth={" + depth + "}, gl={" + pass + "}";
            }
        });
        addScreenshot("14-fullscreen-restored.png");

        addFullAutoplay("component.crafting_table", SUBJECT);

        int screenshot = 15;
        for (ResourceLocation component : ADDITIONAL_COMPONENTS) {
            addComponentSmoke(component, screenshot++);
        }
        addAction("ponder.close", new Action() {
            @Override public String run() {
                PonderClientHarnessUI closed = ui;
                minecraft.displayGuiScreen(null);
                check(minecraft.currentScreen != closed, "Ponder UI remained active after close");
                check(!(minecraft.currentScreen instanceof net.createmod.ponder.foundation.ui.PonderUI),
                    "another Ponder screen remained active after close");
                return "Ponder exited; currentScreen="
                    + (minecraft.currentScreen == null ? "none" : minecraft.currentScreen.getClass().getName());
            }
        });
        addDelayTicks("ponder.close_stays_closed", 2, new Validation() {
            @Override public String validate() {
                check(!(minecraft.currentScreen instanceof net.createmod.ponder.foundation.ui.PonderUI),
                    "Ponder reopened after the close action");
                return "Ponder remained closed; ordinary-world visual state still requires manual acceptance";
            }
        });
    }

    private void addWorkbenchRevealCheckpoint(final String stage, final int tick,
                                               final String screenshotName) {
        addAction("reveal." + stage + ".seek", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                ui.seekToTime(tick);
                scene.setPaused(true);
                check(scene.getCurrentTick() == tick,
                    "workbench reveal did not seek to " + stage + " tick " + tick);
                return "paused workbench reveal at tick " + tick;
            }
        });
        addWaitFrames("reveal." + stage + ".frames", 3,
            "workbench reveal rendered the " + stage + " checkpoint");
        addAction("reveal." + stage + ".observable", new Action() {
            @Override public String run() {
                SectionSnapshot sections = ui.harnessSectionSnapshot();
                CacheState cache = ui.harnessCacheState();
                RenderPassSnapshot pass = requireRenderPassSnapshot();
                check(cache.clean > 0 && cache.totalVertices() > 0,
                    "workbench reveal has no rendered vertices at " + stage + ": " + cache);
                validateWorkbenchReveal(stage, sections, cache);
                return "sections={" + sections + "}, cache={" + cache + "}, gl={" + pass + "}";
            }
        });
        addScreenshot(screenshotName);
    }

    private void validateWorkbenchReveal(String stage, SectionSnapshot sections, CacheState cache) {
        if ("floor_start".equals(stage)) {
            check(sections.containsVisible(new BlockPos(2, 0, 2)),
                "floor reveal did not begin at the configured center: " + sections);
            check(sections.floorBlocks > 0 && sections.floorBlocks < 25 && sections.upperBlocks == 0,
                "floor-start checkpoint is not a partial floor: " + sections);
            check(sections.temporarySections > 0 && sections.partialSections > 0,
                "floor-start checkpoint has no active temporary fade sections: " + sections);
            revealFloorStartBlocks = sections.floorBlocks;
            requireLayer("crafting_table.floor_start", cache, BlockRenderLayer.SOLID);
            return;
        }
        if ("floor_mid".equals(stage)) {
            check(sections.floorBlocks > revealFloorStartBlocks && sections.floorBlocks < 25
                    && sections.upperBlocks == 0,
                "floor-mid checkpoint did not advance only the floor: " + sections);
            check(sections.temporarySections > 0 && sections.partialSections > 0,
                "floor-mid checkpoint has no active temporary fade sections: " + sections);
            revealFloorMidBlocks = sections.floorBlocks;
            requireLayer("crafting_table.floor_mid", cache, BlockRenderLayer.SOLID);
            return;
        }
        if ("upper_mid".equals(stage)) {
            check(sections.floorBlocks == 25 && sections.upperBlocks > 0 && sections.upperBlocks < 9,
                "upper-mid checkpoint is not a partial upper reveal over a complete floor: " + sections);
            check(sections.temporarySections > 0 && sections.partialSections > 0,
                "upper-mid checkpoint has no active temporary fade sections: " + sections);
            check(revealFloorMidBlocks > revealFloorStartBlocks,
                "floor reveal checkpoints did not progress monotonically");
            revealUpperMidBlocks = sections.upperBlocks;
            return;
        }
        if ("complete".equals(stage)) {
            check(sections.floorBlocks == 25 && sections.upperBlocks == 9 && sections.visibleBlocks == 34,
                "complete checkpoint does not expose the entire workbench structure: " + sections);
            check(sections.temporarySections == 0 && sections.partialSections == 0,
                "complete checkpoint retained temporary reveal sections: " + sections);
            check(revealUpperMidBlocks > 0 && revealUpperMidBlocks < sections.upperBlocks,
                "upper reveal checkpoints did not progress monotonically");
            for (BlockRenderLayer layer : BlockRenderLayer.values())
                requireLayer("crafting_table.complete", cache, layer);
            check(cache.cachedTiles >= 1, "complete checkpoint did not render the chest TESR: " + cache);
            requireDepthSnapshot();
            return;
        }
        throw new IllegalStateException("Unknown workbench reveal stage " + stage);
    }

    private void addComponentSmoke(final ResourceLocation component, int screenshotNumber) {
        final String id = component.getPath();
        addAction("component." + id + ".open", new Action() {
            @Override public String run() {
                check(net.createmod.ponder.foundation.PonderIndex.getSceneAccess().doScenesExistForId(component),
                    component + " storyboard was not registered");
                ui = PonderClientHarnessUI.create(component);
                PonderScene scene = ui.getActiveScene();
                check(scene.getTotalTicks() >= 600 && scene.getTotalTicks() <= 1200,
                    component + " timeline is not 30-60 seconds: " + scene.getTotalTicks());
                check(scene.getTotalTicks() == 640,
                    component + " timeline is not exactly 32 seconds: " + scene.getTotalTicks());
                check(scene.getKeyframeCount() > 0, component + " has no keyframes");
                minecraft.displayGuiScreen(ui);
                check(minecraft.currentScreen == ui, component + " Ponder UI did not open");
                return "opened " + component + " (ticks=" + scene.getTotalTicks()
                    + ", keyframes=" + scene.getKeyframeCount() + ")";
            }
        });
        addWaitFrames("component." + id + ".initial_frame", 2,
            component + " rendered its initial frames");
        addWait("component." + id + ".autoplay", 40, new Condition() {
            @Override public boolean get() {
                PonderScene scene = activeScene();
                return component.equals(scene.getComponent()) && scene.getCurrentTick() >= 8;
            }
        }, component + " timeline advanced automatically");
        addAction("component." + id + ".stable_keyframe", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                List<Integer> keyframes = scene.getKeyframes();
                int tick = Math.min(scene.getTotalTicks(),
                    keyframes.get(keyframes.size() / 2).intValue() + 7);
                ui.seekToTime(tick);
                scene.setPaused(true);
                check(scene.getCurrentTick() == tick, component + " did not seek to keyframe " + tick);
                return "paused seven ticks into the middle keyframe at " + tick;
            }
        });
        addWaitFrames("component." + id + ".stable_frames", 3,
            component + " rendered at a stable keyframe");
        addAction("component." + id + ".render_state", new Action() {
            @Override public String run() {
                DepthSnapshot depth = requireDepthSnapshot();
                RenderPassSnapshot pass = requireRenderPassSnapshot();
                CacheState cache = ui.harnessCacheState();
                check(cache.clean > 0, component + " did not build a section cache: " + cache);
                check(cache.totalVertices() > 0, component + " section caches contain no vertices: " + cache);
                String sceneState = validateComponentState(id, activeScene(), cache);
                return "depth={" + depth + "}, gl={" + pass + "}, cache={" + cache
                    + "}, sceneState={" + sceneState + "}";
            }
        });
        addScreenshot(String.format(Locale.ROOT, "%02d-%s.png", screenshotNumber, id));
        addFullAutoplay("component." + id, component);
    }

    private void addFullAutoplay(final String name, final ResourceLocation component) {
        addAction(name + ".full_autoplay_reset", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                check(component.equals(scene.getComponent()),
                    "wrong component before full autoplay: " + scene.getComponent());
                ui.seekToTime(0);
                scene.setPaused(false);
                check(scene.getCurrentTick() == 0, component + " did not reset to tick zero");
                check(!scene.isPaused(), component + " remained paused after autoplay reset");
                check(!scene.isFinished(), component + " remained finished after autoplay reset");
                return "reset " + component + " to tick zero and resumed playback";
            }
        });
        addWait(name + ".full_autoplay_finished", FULL_SCENE_TIMEOUT_TICKS, new Condition() {
            @Override public boolean get() {
                PonderScene scene = activeScene();
                return component.equals(scene.getComponent()) && scene.isFinished();
            }
        }, component + " reached isFinished through normal updateScreen ticks");
        addAction(name + ".full_autoplay_result", new Action() {
            @Override public String run() {
                PonderScene scene = activeScene();
                check(scene.isFinished(), component + " did not remain finished");
                check(scene.getCurrentTick() >= scene.getTotalTicks(),
                    component + " finished before the end of its timeline");
                check(minecraft.currentScreen == ui, component + " Ponder UI closed during autoplay");
                return "tick=" + scene.getCurrentTick() + "/" + scene.getTotalTicks();
            }
        });
    }

    private void addBookRenderYStability(final int requiredTicks) {
        steps.add(new Step("render.book_y_stability_" + requiredTicks + "_ticks") {
            private int samples;
            private int firstSceneTick = -1;
            private int lastSceneTick = -1;
            private int lastAge = -1;
            private double expectedY;
            private double minimumY = Double.POSITIVE_INFINITY;
            private double maximumY = Double.NEGATIVE_INFINITY;
            private double minimumCompensatedY = Double.POSITIVE_INFINITY;
            private double maximumCompensatedY = Double.NEGATIVE_INFINITY;

            @Override boolean tick() {
                PonderScene scene = activeScene();
                check(SUBJECT.equals(scene.getComponent()), "book stability sample is not on the workbench scene");
                check(!scene.isPaused(), "workbench scene paused during book stability sampling");
                EntityItem book = findItemEntity(scene.getWorld(), Items.BOOK);
                check(book != null, "scripted book entity disappeared during stability sampling");
                check(book.hasNoGravity(), "scripted book regained gravity during stability sampling");
                check(near(book.hoverStart, 0), "scripted book hover phase is not deterministic: " + book.hoverStart);
                check(near(book.motionX, 0) && near(book.motionY, 0) && near(book.motionZ, 0),
                    "scripted book has non-zero motion: " + book.motionX + "," + book.motionY + "," + book.motionZ);
                check(near(book.posX, book.prevPosX) && near(book.posX, book.lastTickPosX)
                        && near(book.posY, book.prevPosY) && near(book.posY, book.lastTickPosY)
                        && near(book.posZ, book.prevPosZ) && near(book.posZ, book.lastTickPosZ),
                    "scripted book position history diverged at tick " + scene.getCurrentTick());

                int sceneTick = scene.getCurrentTick();
                int age = book.getAge();
                if (samples == 0) {
                    firstSceneTick = sceneTick;
                    expectedY = book.posY + 0.1F;
                } else {
                    check(sceneTick == lastSceneTick + 1,
                        "book stability samples were not consecutive scene ticks: " + lastSceneTick + " -> " + sceneTick);
                    check(age == lastAge + 1,
                        "book age did not advance exactly once: " + lastAge + " -> " + age);
                }

                float[] partialTicks = {0, .5f, 1};
                for (float partial : partialTicks) {
                    ItemRenderY renderY = PonderRenderHarnessProbe.sampleItemRenderY(book, partial);
                    check(renderY.shouldBob, "item renderer disabled bobbing; compensation path was not exercised");
                    check(near(renderY.effectiveY, expectedY),
                        "book effective render Y jumped at scene tick " + sceneTick + ", partial=" + partial
                            + ": expected=" + expectedY + ", sample={" + renderY + "}");
                    minimumY = Math.min(minimumY, renderY.effectiveY);
                    maximumY = Math.max(maximumY, renderY.effectiveY);
                    minimumCompensatedY = Math.min(minimumCompensatedY, renderY.compensatedY);
                    maximumCompensatedY = Math.max(maximumCompensatedY, renderY.compensatedY);
                }

                samples++;
                lastSceneTick = sceneTick;
                lastAge = age;
                if (samples < requiredTicks) return false;
                check(lastSceneTick - firstSceneTick >= requiredTicks - 1,
                    "book stability window covered fewer than " + requiredTicks + " scene ticks");
                check(maximumY - minimumY <= 1.0e-6,
                    "book effective render Y range is not stable: " + minimumY + ".." + maximumY);
                check(maximumCompensatedY - minimumCompensatedY > .05,
                    "book compensation did not counter a changing vanilla bob phase");
                detail = "samples=" + samples + ", sceneTicks=" + firstSceneTick + ".." + lastSceneTick
                    + ", effectiveY=" + minimumY + ".." + maximumY + ", compensatedY="
                    + minimumCompensatedY + ".." + maximumCompensatedY;
                return true;
            }
        });
    }

    private String validateComponentState(String id, PonderScene scene, CacheState cache) {
        PonderWorld world = scene.getWorld();
        check(world != null, id + " scene has no virtual world");
        if ("chest".equals(id)) {
            BlockPos pos = new BlockPos(2, 1, 2);
            TileEntity tile = world.getTileEntity(pos);
            check(tile instanceof TileEntityChest, "chest scene lost its chest block entity");
            TileEntityChest chest = (TileEntityChest) tile;
            check(TileEntityRendererDispatcher.instance.getRenderer(chest) != null,
                "chest scene has no TESR for its block entity");
            check(cache.cachedTiles > 0, "chest TESR was not retained by the section cache");
            check(itemIs(chest.getStackInSlot(0), Items.BOOK), "chest slot 0 does not contain the scripted book");
            check(itemIs(chest.getStackInSlot(1), Items.COMPASS),
                "chest slot 1 does not contain the scripted compass");
            NBTTagCompound nbt = chest.writeToNBT(new NBTTagCompound());
            check("Ponder Storage".equals(nbt.getString("CustomName")),
                "chest custom-name NBT was not applied: " + nbt);
            requireLayer(id, cache, BlockRenderLayer.SOLID);
            return "tesr=true, customName=" + nbt.getString("CustomName") + ", slots=book,compass";
        }
        if ("furnace".equals(id)) {
            BlockPos pos = new BlockPos(2, 1, 2);
            IBlockState state = world.getBlockState(pos);
            check(state.getBlock() == Blocks.LIT_FURNACE, "furnace scene did not retain the lit block state");
            TileEntity tile = world.getTileEntity(pos);
            check(tile instanceof TileEntityFurnace, "furnace scene lost its furnace block entity");
            TileEntityFurnace furnace = (TileEntityFurnace) tile;
            check(Block.getBlockFromItem(furnace.getStackInSlot(0).getItem()) == Blocks.IRON_ORE,
                "furnace input slot does not contain iron ore");
            check(itemIs(furnace.getStackInSlot(1), Items.COAL), "furnace fuel slot does not contain coal");
            check(furnace.getField(0) == 160 && furnace.getField(1) == 160,
                "furnace burn fields do not match the scripted state");
            check(furnace.getField(2) == 100 && furnace.getField(3) == 200,
                "furnace cook fields do not match the scripted midpoint");
            requireLayer(id, cache, BlockRenderLayer.SOLID);
            return "lit=true, burn=160/160, cook=100/200";
        }
        if ("piston".equals(id)) {
            IBlockState piston = world.getBlockState(new BlockPos(1, 1, 2));
            check(piston.getBlock() == Blocks.PISTON, "piston scene target is no longer a piston");
            check(piston.getValue(BlockPistonBase.EXTENDED).booleanValue(),
                "piston scene did not retain the explicitly extended state");
            int independentSections = 0;
            boolean movedSection = false;
            for (PonderElement element : scene.getElements()) {
                if (!(element instanceof WorldSectionElementImpl)) continue;
                WorldSectionElementImpl section = (WorldSectionElementImpl) element;
                if (!section.isVisible()) continue;
                independentSections++;
                if (section.getAnimatedOffset().squareDistanceTo(Vec3d.ZERO) > .5) movedSection = true;
            }
            check(independentSections >= 2, "piston scene did not create an independent visible section");
            check(movedSection, "piston independent section did not reach its moved position");
            requireLayer(id, cache, BlockRenderLayer.SOLID);
            return "extended=true, visibleSections=" + independentSections + ", movedSection=true";
        }
        if ("redstone_lamp".equals(id)) {
            IBlockState first = world.getBlockState(new BlockPos(2, 1, 2));
            IBlockState second = world.getBlockState(new BlockPos(3, 1, 2));
            check(first.getBlock() == Blocks.REDSTONE_WIRE && second.getBlock() == Blocks.REDSTONE_WIRE,
                "redstone scene lost its wire blocks");
            check(first.getValue(BlockRedstoneWire.POWER).intValue() == 15
                    && second.getValue(BlockRedstoneWire.POWER).intValue() == 15,
                "redstone wire did not retain scripted power level 15");
            check(world.getBlockState(new BlockPos(4, 1, 2)).getBlock() == Blocks.LIT_REDSTONE_LAMP,
                "redstone lamp did not retain its explicitly lit state");
            requireLayer(id, cache, BlockRenderLayer.SOLID);
            requireLayer(id, cache, BlockRenderLayer.CUTOUT);
            return "wirePower=15, lampLit=true";
        }
        if ("glass".equals(id)) {
            for (BlockRenderLayer layer : BlockRenderLayer.values()) requireLayer(id, cache, layer);
            return "layers=" + cache.layerNames();
        }
        if ("water_bucket".equals(id)) {
            IBlockState source = world.getBlockState(new BlockPos(2, 1, 2));
            IBlockState left = world.getBlockState(new BlockPos(1, 1, 2));
            IBlockState right = world.getBlockState(new BlockPos(3, 1, 2));
            check(source.getBlock() == Blocks.WATER, "water scene did not restore its source block");
            check(left.getBlock() == Blocks.FLOWING_WATER && right.getBlock() == Blocks.FLOWING_WATER,
                "water scene did not restore both flowing blocks");
            check(left.getValue(BlockLiquid.LEVEL).intValue() == 3
                    && right.getValue(BlockLiquid.LEVEL).intValue() == 5,
                "water scene flow levels do not match the scripted 3/5 state");
            requireLayer(id, cache, BlockRenderLayer.TRANSLUCENT);
            return "source=true, flowLevels=3/5, translucentVertices="
                + cache.vertexCount(BlockRenderLayer.TRANSLUCENT);
        }
        if ("rail".equals(id)) {
            MinecartElement cart = null;
            for (PonderElement element : scene.getElements()) {
                if (element instanceof MinecartElement) cart = (MinecartElement) element;
            }
            check(cart != null, "rail scene did not create its minecart element");
            check(cart.isVisible(), "rail minecart element is hidden at the middle keyframe");
            Vec3d position = cart.getPositionOffset();
            Vec3d rotation = cart.getRotation();
            check(position.z >= 3.4, "rail minecart did not reach the curve: " + position);
            check(rotation.y < -.1, "rail minecart did not begin its scripted turn: " + rotation);
            requireLayer(id, cache, BlockRenderLayer.CUTOUT);
            return "minecartVisible=true, position=" + position + ", rotation=" + rotation;
        }
        throw new IllegalStateException("No component-state validator for " + id);
    }

    private static void requireLayer(String id, CacheState cache, BlockRenderLayer layer) {
        check(cache.vertexCount(layer) > 0,
            id + " scene has no cached " + layer + " vertices: " + cache);
    }

    private static boolean itemIs(ItemStack stack, net.minecraft.item.Item item) {
        return stack != null && !stack.isEmpty() && stack.getItem() == item;
    }

    private static EntityItem findItemEntity(PonderWorld world, net.minecraft.item.Item item) {
        if (world == null) return null;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof EntityItem)) continue;
            EntityItem candidate = (EntityItem) entity;
            if (itemIs(candidate.getItem(), item)) return candidate;
        }
        return null;
    }

    private static boolean near(double first, double second) {
        return Math.abs(first - second) <= 1.0e-6;
    }

    private void addDepthCheck(String name) {
        addAction(name, new Action() {
            @Override public String run() {
                DepthSnapshot depth = requireDepthSnapshot();
                return "depth={" + depth + "}, gl={" + requireRenderPassSnapshot() + "}";
            }
        });
    }

    private DepthSnapshot requireDepthSnapshot() {
        RenderPassSnapshot pass = requireRenderPassSnapshot();
        DepthSnapshot depth = ui.harnessDepthSnapshot();
        check(depth != null, "no pre-overlay depth sample was captured");
        check(depth.frame == pass.frame,
            "depth and OpenGL state came from different render frames: depth=" + depth.frame + ", gl=" + pass.frame);
        check(depth.hasBackground(), "depth sample has no cleared background values: " + depth);
        check(depth.hasSceneGeometry(), "depth sample has no 0.9-1.0 scene geometry: " + depth);
        check(depth.unexpectedSamples == 0, "depth sample contains out-of-range geometry: " + depth);
        return depth;
    }

    private RenderPassSnapshot requireRenderPassSnapshot() {
        RenderPassSnapshot pass = ui.harnessRenderPassSnapshot();
        check(pass != null, "no bracketed Ponder render-pass sample was captured");
        check(activeScene().getComponent().equals(pass.component),
            "render-pass sample belongs to " + pass.component + " instead of " + activeScene().getComponent());
        check(pass.hasNoGlErrors(), "OpenGL error around the Ponder 3D pass: " + pass.glErrors());
        String differences = pass.restorationDifferences();
        check(differences.isEmpty(), "Ponder renderer did not restore OpenGL/Minecraft state: " + differences);
        return pass;
    }

    private void addGuiScaleSteps(final int requestedScale) {
        addAction("gui_scale." + requestedScale, new Action() {
            @Override public String run() {
                minecraft.gameSettings.guiScale = requestedScale;
                minecraft.resize(minecraft.displayWidth, minecraft.displayHeight);
                check(minecraft.currentScreen == ui, "Ponder UI closed during GUI scale change");
                check(ui.width > 0 && ui.height > 0, "invalid scaled UI dimensions");
                check(ui.harnessControlsFit(), "Ponder controls overlap or leave the viewport");
                ScaledResolution resolution = new ScaledResolution(minecraft);
                return "requested=" + requestedScale + ", effective=" + resolution.getScaleFactor()
                    + ", ui=" + ui.width + "x" + ui.height;
            }
        });
        addWaitFrames("gui_scale." + requestedScale + ".frame", 2,
            "GUI scale " + requestedScale + " rendered without closing Ponder");
        addScreenshot(String.format(Locale.ROOT, "%02d-gui-scale-%d.png", 8 + requestedScale, requestedScale));
    }

    private void addAction(String name, final Action action) {
        steps.add(new Step(name) {
            @Override boolean tick() throws Exception {
                detail = action.run();
                return true;
            }
        });
    }

    private void addWait(String name, final int maximumTicks, final Condition condition, final String detail) {
        steps.add(new Step(name) {
            private int start = -1;
            @Override boolean tick() {
                if (start < 0) start = clientTicks;
                if (condition.get()) {
                    this.detail = detail;
                    return true;
                }
                if (clientTicks - start >= maximumTicks)
                    throw new IllegalStateException("Timed out after " + maximumTicks + " ticks");
                return false;
            }
        });
    }

    private void addWaitFrames(String name, final int frames, final String detail) {
        steps.add(new Step(name) {
            private int start = -1;
            @Override boolean tick() {
                if (start < 0) start = renderFrames;
                if (renderFrames - start < frames) return false;
                this.detail = detail + " (frames=" + frames + ")";
                return true;
            }
        });
    }

    private void addDelayTicks(String name, final int ticks, final Validation validation) {
        steps.add(new Step(name) {
            private int start = -1;
            @Override boolean tick() {
                if (start < 0) start = clientTicks;
                if (clientTicks - start < ticks) return false;
                detail = validation.validate();
                return true;
            }
        });
    }

    private void addScreenshot(final String fileName) {
        steps.add(new Step("screenshot." + fileName) {
            private boolean requested;
            @Override boolean tick() {
                if (!requested) {
                    check(pendingScreenshot == null, "another screenshot is pending");
                    completedScreenshot = null;
                    screenshotFailure = null;
                    pendingScreenshot = fileName;
                    requested = true;
                    return false;
                }
                if (!fileName.equals(completedScreenshot)) return false;
                if (screenshotFailure != null)
                    throw new IllegalStateException("Screenshot capture failed", screenshotFailure);
                detail = "saved screenshots/" + fileName;
                return true;
            }
        });
    }

    private PonderScene activeScene() {
        check(ui != null && minecraft.currentScreen == ui, "Ponder UI is not active");
        return ui.getActiveScene();
    }

    private void captureScreenshot(String name) throws Exception {
        ScreenShotHelper.saveScreenshot(outputDirectory, name, minecraft.displayWidth, minecraft.displayHeight,
            minecraft.getFramebuffer());
        File file = new File(screenshotDirectory, name).getCanonicalFile();
        check(file.isFile() && file.length() > 1024, "screenshot was not written: " + file);
        BufferedImage image = ImageIO.read(file);
        check(image != null && image.getWidth() > 0 && image.getHeight() > 0, "screenshot is not a readable PNG");
        Set<Integer> colors = new HashSet<Integer>();
        int minBrightness = 255;
        int maxBrightness = 0;
        int stepX = Math.max(1, image.getWidth() / 96);
        int stepY = Math.max(1, image.getHeight() / 64);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y);
                colors.add(rgb & 0x00ffffff);
                int brightness = ((rgb >> 16 & 255) + (rgb >> 8 & 255) + (rgb & 255)) / 3;
                minBrightness = Math.min(minBrightness, brightness);
                maxBrightness = Math.max(maxBrightness, brightness);
            }
        }
        check(colors.size() >= 8 && maxBrightness - minBrightness >= 20,
            "screenshot appears blank (colors=" + colors.size() + ", range=" + (maxBrightness - minBrightness) + ")");
        ScreenshotRecord record = new ScreenshotRecord();
        record.file = "screenshots/" + name;
        record.width = image.getWidth();
        record.height = image.getHeight();
        record.sampledColors = colors.size();
        record.sha256 = sha256(file);
        screenshots.add(record);
    }

    private void completeSuccessfully() {
        terminal = true;
        shutdownAtTick = clientTicks + 3;
        writeReport("PASS", null);
        writeText(new File(outputDirectory, "client-demo-ok.flag"),
            "PASS\ncompletedAt=" + timestamp(System.currentTimeMillis()) + "\n");
        deleteIfExists(new File(outputDirectory, "client-demo-failed.flag"));
    }

    private void fail(String phase, Throwable throwable) {
        if (terminal) return;
        terminal = true;
        checks.add(new CheckRecord(phase, "FAIL", throwable.toString()));
        try {
            if (initialWindowStateCaptured && Display.isCreated()
                && minecraft.isFullScreen() != originalFullscreen) minecraft.toggleFullscreen();
            if (minecraft.gameSettings != null) {
                minecraft.gameSettings.guiScale = originalGuiScale;
                if (Display.isCreated()) minecraft.resize(minecraft.displayWidth, minecraft.displayHeight);
            }
        } catch (Throwable ignored) {
        }
        writeReport("FAIL", throwable);
        writeText(new File(outputDirectory, "client-demo-failed.flag"),
            "FAIL\nphase=" + phase + "\nerror=" + throwable + "\n");
        deleteIfExists(new File(outputDirectory, "client-demo-ok.flag"));
        shutdownAtTick = clientTicks + 3;
    }

    private void writeReport(String status, Throwable failure) {
        try {
            JsonObject report = new JsonObject();
            report.addProperty("schemaVersion", 2);
            report.addProperty("status", status);
            report.addProperty("scope", "automated_ponder_opengl_ui_and_fullscreen_runtime_harness");
            report.addProperty("doesNotReplaceManualVisualAcceptance", true);
            report.addProperty("doesNotReplaceOrdinaryWorldPostCloseVisualAcceptance", true);
            report.addProperty("doesNotReplaceRealMouseAcceptance", true);
            report.addProperty("doesNotReplaceManualFullscreenVisualAcceptance", true);
            report.addProperty("includesProgrammaticFullscreenRoundTrip", true);
            report.addProperty("includesPreOverlayDepthReadback", true);
            report.addProperty("includesBracketedGlStateRestorationChecks", true);
            report.addProperty("includesStaggeredRevealCheckpoints", true);
            report.addProperty("includesSeventyTickItemRenderYCheck", true);
            report.addProperty("includesFullAutoplayForAllEightScenes", true);
            report.addProperty("includesPerSceneTargetContentAssertions", true);
            report.addProperty("guiScaleChecksAreAutomatedLayoutAndFramebufferOnly", true);
            report.addProperty("startedAt", timestamp(startedAt));
            report.addProperty("updatedAt", timestamp(System.currentTimeMillis()));
            report.addProperty("elapsedMillis", System.currentTimeMillis() - startedAt);
            report.addProperty("clientTicks", clientTicks);
            report.addProperty("renderFrames", renderFrames);
            report.addProperty("manualPonderLifecycle", manualPonderLifecycle);
            report.addProperty("minecraft", "1.12.2");
            report.addProperty("forge", ForgeVersion.getVersion());
            report.addProperty("mixinBooter", loadedVersion("mixinbooter"));
            report.addProperty("ponderDiscoveredByFml", Loader.isModLoaded(Ponder.MOD_ID));
            report.addProperty("outputDirectory", outputDirectory.getAbsolutePath());
            if (stepIndex < steps.size()) report.addProperty("currentStep", steps.get(stepIndex).name);

            JsonArray checkArray = new JsonArray();
            for (CheckRecord check : checks) {
                JsonObject value = new JsonObject();
                value.addProperty("name", check.name);
                value.addProperty("status", check.status);
                value.addProperty("detail", check.detail);
                checkArray.add(value);
            }
            report.add("checks", checkArray);

            JsonArray screenshotArray = new JsonArray();
            for (ScreenshotRecord screenshot : screenshots) {
                JsonObject value = new JsonObject();
                value.addProperty("file", screenshot.file);
                value.addProperty("width", screenshot.width);
                value.addProperty("height", screenshot.height);
                value.addProperty("sampledColors", screenshot.sampledColors);
                value.addProperty("sha256", screenshot.sha256);
                screenshotArray.add(value);
            }
            report.add("screenshots", screenshotArray);
            if (failure != null) {
                report.addProperty("failureType", failure.getClass().getName());
                report.addProperty("failureMessage", String.valueOf(failure.getMessage()));
                report.addProperty("stackTrace", stackTrace(failure));
            }
            writeJsonAtomically(new File(outputDirectory, "report.json"), report);
        } catch (Throwable reportFailure) {
            reportFailure.printStackTrace();
        }
    }

    private void prepareOutputDirectory() {
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs())
            throw new IllegalStateException("Could not create client smoke directory: " + outputDirectory);
        if (!screenshotDirectory.isDirectory() && !screenshotDirectory.mkdirs())
            throw new IllegalStateException("Could not create screenshot directory: " + screenshotDirectory);
        deleteIfExists(new File(outputDirectory, "client-demo-ok.flag"));
        deleteIfExists(new File(outputDirectory, "client-demo-failed.flag"));
        deleteIfExists(new File(outputDirectory, "report.json"));
        File[] oldScreenshots = screenshotDirectory.listFiles();
        if (oldScreenshots != null) {
            for (File file : oldScreenshots)
                if (file.isFile() && file.getName().matches("\\d{2}-.+\\.png")) deleteIfExists(file);
        }
    }

    private File resolveOutputDirectory() {
        String configured = System.getProperty("ponder.clientHarness.output", "").trim();
        File result;
        if (!configured.isEmpty()) {
            result = new File(configured);
        } else {
            File runDirectory = minecraft.gameDir.getAbsoluteFile();
            File projectDirectory = runDirectory.getParentFile();
            result = new File(projectDirectory == null ? runDirectory : projectDirectory,
                "verification/client-smoke");
        }
        try {
            return result.getCanonicalFile();
        } catch (IOException exception) {
            return result.getAbsoluteFile();
        }
    }

    private static boolean hasBlock(PonderWorld world, Block block) {
        for (BlockPos pos : world.getOccupiedPositions())
            if (world.getBlockState(pos).getBlock() == block) return true;
        return false;
    }

    private static boolean containsEntity(PonderWorld world, Class<? extends Entity> type) {
        for (Entity entity : world.getEntities()) if (type.isInstance(entity)) return true;
        return false;
    }

    private static String sceneDigest(PonderScene scene) {
        List<String> values = new ArrayList<String>();
        values.add("tick=" + scene.getCurrentTick());
        values.add("finished=" + scene.isFinished());
        values.add("paused=" + scene.isPaused());
        values.add("poi=" + vector(scene.getPointOfInterest()));
        values.add("cursor=" + vector(scene.getCursorPosition()));
        values.add("camera=" + scene.getCameraYaw() + "," + scene.getCameraPitch());
        PonderWorld world = scene.getWorld();
        if (world != null) {
            values.add("worldTime=" + world.getWorldTime() + "," + world.getTotalWorldTime());
            List<BlockPos> positions = new ArrayList<BlockPos>(world.getOccupiedPositions());
            Collections.sort(positions, new Comparator<BlockPos>() {
                @Override public int compare(BlockPos left, BlockPos right) {
                    int y = Integer.compare(left.getY(), right.getY());
                    if (y != 0) return y;
                    int z = Integer.compare(left.getZ(), right.getZ());
                    return z != 0 ? z : Integer.compare(left.getX(), right.getX());
                }
            });
            for (BlockPos pos : positions) {
                IBlockState state = world.getBlockState(pos);
                ResourceLocation id = Block.REGISTRY.getNameForObject(state.getBlock());
                values.add("block=" + pos + ":" + id + ":" + state.getBlock().getMetaFromState(state));
            }
            List<String> tiles = new ArrayList<String>();
            for (TileEntity tile : world.getTileEntities())
                tiles.add(tile.getClass().getName() + ":" + tile.writeToNBT(new NBTTagCompound()));
            Collections.sort(tiles);
            values.addAll(tiles);
            List<String> entities = new ArrayList<String>();
            for (Entity entity : world.getEntities()) {
                NBTTagCompound data = new NBTTagCompound();
                entity.writeToNBT(data);
                ResourceLocation id = EntityList.getKey(entity);
                entities.add("entity=" + id + ":" + data);
            }
            Collections.sort(entities);
            values.addAll(entities);
        }
        int sectionIndex = 0;
        for (PonderElement element : scene.getElements()) {
            if (!(element instanceof WorldSectionElementImpl)) continue;
            WorldSectionElementImpl section = (WorldSectionElementImpl) element;
            values.add("section=" + sectionIndex++ + ":" + section.isVisible() + ":"
                + vector(section.getAnimatedOffset()) + ":" + vector(section.getAnimatedRotation()));
        }
        return join(values, "\n");
    }

    private static String vector(Vec3d value) {
        return Double.doubleToLongBits(value.x) + "," + Double.doubleToLongBits(value.y) + ","
            + Double.doubleToLongBits(value.z);
    }

    private static String loadedVersion(String modId) {
        ModContainer container = Loader.instance().getIndexedModList().get(modId);
        return container == null ? "not-discovered" : container.getVersion();
    }

    private static ResourceLocation component(String path) {
        return new ResourceLocation("minecraft", path);
    }

    private static int positiveIntegerProperty(String key, int fallback) {
        String value = System.getProperty(key, "").trim();
        if (value.isEmpty()) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void writeJsonAtomically(File target, JsonObject json) throws IOException {
        File temporary = new File(target.getParentFile(), target.getName() + ".tmp");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void writeText(File target, String text) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
            writer.write(text);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void deleteIfExists(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        try (FileInputStream input = new FileInputStream(file)) {
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return hex(digest.digest());
    }

    private static String sha256(byte[] bytes) {
        try {
            return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) builder.append(String.format(Locale.ROOT, "%02x", value & 255));
        return builder.toString();
    }

    private static String timestamp(long time) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ROOT).format(new Date(time));
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter text = new StringWriter();
        throwable.printStackTrace(new PrintWriter(text));
        return text.toString();
    }

    private static String join(List<String> values, String separator) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(separator);
            result.append(value);
        }
        return result.toString();
    }

    private abstract static class Step {
        final String name;
        String detail = "";
        Step(String name) { this.name = name; }
        abstract boolean tick() throws Exception;
    }

    private interface Action { String run() throws Exception; }
    private interface Condition { boolean get(); }
    private interface Validation { String validate(); }

    private static final class CheckRecord {
        final String name;
        final String status;
        final String detail;
        CheckRecord(String name, String status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    private static final class ScreenshotRecord {
        String file;
        int width;
        int height;
        int sampledColors;
        String sha256;
    }
}
