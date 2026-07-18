package net.createmod.ponder.mmce.verification;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.Display;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.mmce.PonderMMCE;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.createmod.ponder.mmce.script.MMCEStructures;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@SuppressWarnings("deprecation")
public final class PonderMMCEClientSmokeController {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation STATIC_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_static_demo");
    private static final ResourceLocation DYNAMIC_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_dynamic_demo");
    private static final ResourceLocation UNCONFIGURED_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_unconfigured");
    private static final ResourceLocation STATIC_COMPONENT = MMCEStructureRef.componentId(STATIC_MACHINE);
    private static final ResourceLocation DYNAMIC_COMPONENT = MMCEStructureRef.componentId(DYNAMIC_MACHINE);
    private static final ResourceLocation UNCONFIGURED_COMPONENT = MMCEStructureRef.componentId(UNCONFIGURED_MACHINE);

    private final Minecraft minecraft = Minecraft.getMinecraft();
    private final File outputDirectory;
    private final File screenshotDirectory;
    private final List<Check> checks = new ArrayList<Check>();
    private final List<Screenshot> screenshots = new ArrayList<Screenshot>();
    private final long startedAt = System.currentTimeMillis();

    private boolean armed;
    private boolean terminal;
    private int ticks;
    private int renderFrames;
    private int phase;
    private int phaseStarted;
    private PonderUI ui;
    private String pendingScreenshot;
    private String completedScreenshot;
    private Throwable screenshotFailure;

    public PonderMMCEClientSmokeController() {
        String configured = System.getProperty("ponder.mmce.clientHarness.output", "").trim();
        outputDirectory = configured.isEmpty()
            ? new File("build/reports/ponder-mmce-client")
            : new File(configured);
        screenshotDirectory = new File(outputDirectory, "screenshots");
        prepareOutput();
        writeReport("STARTING", null);
    }

    public void arm() {
        armed = true;
        writeReport("RUNNING", null);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !armed || terminal) return;
        try {
            ticks++;
            if (ticks > 2400) throw new IllegalStateException("Ponder-MMCE client harness timed out");
            switch (phase) {
                case 0:
                    if (!Display.isCreated() || minecraft.getFramebuffer() == null) return;
                    pass("client.ready", "Display and framebuffer are ready");
                    nextPhase();
                    break;
                case 1:
                    verifyBlueprints();
                    pass("blueprints", "Static, dynamic, and no-scene blueprint subjects resolved");
                    nextPhase();
                    break;
                case 2:
                    verifyFingerprintIsolation();
                    pass("fingerprint.isolation",
                        "A stale MMCE structure fingerprint was rejected without losing valid scenes");
                    nextPhase();
                    break;
                case 3:
                    ui = PonderUI.of(STATIC_COMPONENT);
                    minecraft.displayGuiScreen(ui);
                    require(minecraft.currentScreen == ui, "Static Ponder UI did not open");
                    verifyStaticScene(ui.getActiveScene());
                    pass("static.open", "Static MMCE scene opened with named groups and preview blocks");
                    nextPhase();
                    break;
                case 4:
                    if (renderFrames < 3) return;
                    requestScreenshot("01-static-mmce.png");
                    nextPhase();
                    break;
                case 5:
                    if (!"01-static-mmce.png".equals(completedScreenshot)) return;
                    require(screenshotFailure == null, "Static screenshot failed: " + screenshotFailure);
                    pass("static.screenshot", "Saved screenshots/01-static-mmce.png");
                    completedScreenshot = null;
                    ui = PonderUI.of(DYNAMIC_COMPONENT);
                    minecraft.displayGuiScreen(ui);
                    verifyDynamicScene(ui.getActiveScene());
                    pass("dynamic.open", "Dynamic MMCE scene opened with repeated and terminal groups");
                    renderFrames = 0;
                    nextPhase();
                    break;
                case 6:
                    if (renderFrames < 3) return;
                    requestScreenshot("02-dynamic-mmce.png");
                    nextPhase();
                    break;
                case 7:
                    if (!"02-dynamic-mmce.png".equals(completedScreenshot)) return;
                    require(screenshotFailure == null, "Dynamic screenshot failed: " + screenshotFailure);
                    pass("dynamic.screenshot", "Saved screenshots/02-dynamic-mmce.png");
                    minecraft.displayGuiScreen(null);
                    complete();
                    break;
                default:
                    throw new IllegalStateException("Unknown client harness phase " + phase);
            }
        } catch (Throwable failure) {
            fail(failure);
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
            ScreenShotHelper.saveScreenshot(outputDirectory, name, minecraft.displayWidth,
                minecraft.displayHeight, minecraft.getFramebuffer());
            File file = new File(screenshotDirectory, name);
            require(file.isFile() && file.length() > 1024, "Screenshot was not written: " + file);
            BufferedImage image = ImageIO.read(file);
            require(image != null && image.getWidth() > 0 && image.getHeight() > 0,
                "Screenshot is not a readable PNG");
            int colors = sampledColors(image);
            require(colors >= 8, "Screenshot appears blank: sampled colors=" + colors);
            screenshots.add(new Screenshot(name, image.getWidth(), image.getHeight(), colors, sha256(file)));
        } catch (Throwable failure) {
            screenshotFailure = failure;
        }
        completedScreenshot = name;
    }

    private void verifyBlueprints() {
        verifyBlueprint(STATIC_MACHINE, STATIC_COMPONENT, true);
        verifyBlueprint(DYNAMIC_MACHINE, DYNAMIC_COMPONENT, true);
        verifyBlueprint(UNCONFIGURED_MACHINE, UNCONFIGURED_COMPONENT, false);
    }

    private void verifyBlueprint(ResourceLocation machine, ResourceLocation component, boolean expectsScene) {
        ItemStack blueprint = new ItemStack(new ItemBlueprint());
        ItemBlueprint.setAssociatedMachine(blueprint, machine);
        ResolvedPonderSubject resolved = PonderSubjectResolvers.resolve(blueprint);
        require(resolved.isHandled(), "Blueprint was not handled for " + machine);
        require(PonderMMCE.BLUEPRINT_RESOLVER_ID.equals(resolved.getResolverId()),
            "Blueprint used the wrong resolver for " + machine);
        require(component.equals(resolved.getComponent()), "Blueprint resolved the wrong component for " + machine);
        require(PonderIndex.getSceneAccess().doScenesExistForId(component) == expectsScene,
            "Scene availability was wrong for " + machine);
    }

    private static void verifyStaticScene(PonderScene scene) {
        require(new ResourceLocation("ponder_mmce", "static_demo").equals(scene.getId()),
            "Unexpected static scene " + scene.getId());
        Map<String, List<BlockPos>> groups = groups(scene);
        requireGroup(groups, "mmce:all");
        requireGroup(groups, "mmce:controller");
        requireGroup(groups, "mmce:tag/item_input");
        require(scene.getWorld().getOccupiedPositions().size() >= 6, "Static structure is incomplete");
    }

    private static void verifyDynamicScene(PonderScene scene) {
        require(new ResourceLocation("ponder_mmce", "dynamic_demo").equals(scene.getId()),
            "Unexpected dynamic scene " + scene.getId());
        Map<String, List<BlockPos>> groups = groups(scene);
        requireGroup(groups, "mmce:dynamic/line/segment/0/frame");
        requireGroup(groups, "mmce:dynamic/line/segment/1/frame");
        requireGroup(groups, "mmce:dynamic/line/segment/2/frame");
        requireGroup(groups, "mmce:dynamic/line/end/output");
        require(scene.getWorld().getOccupiedPositions().size() >= 12, "Dynamic structure is incomplete");
    }

    private static void verifyFingerprintIsolation() throws IOException {
        MMCEStructureRef current = MMCEStructures.machine(STATIC_MACHINE.toString());
        String id = current.structure;
        require(id.length() > 64, "MMCE structure id does not contain a fingerprint");
        String staleFingerprint =
            "0000000000000000000000000000000000000000000000000000000000000000";
        if (id.endsWith(staleFingerprint))
            staleFingerprint =
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        ResourceLocation stale = new ResourceLocation(
            id.substring(0, id.length() - 64) + staleFingerprint);
        String rejection = null;
        try {
            new PonderStructureLoader().load(stale);
        } catch (IOException expected) {
            rejection = expected.getMessage();
        }
        require(rejection != null && rejection.contains("fingerprint mismatch"),
            "Stale MMCE structure did not produce a fingerprint mismatch");
        require(PonderIndex.getSceneAccess().doScenesExistForId(STATIC_COMPONENT),
            "Static scene was lost after stale fingerprint rejection");
        require(PonderIndex.getSceneAccess().doScenesExistForId(DYNAMIC_COMPONENT),
            "Dynamic scene was lost after stale fingerprint rejection");
        PonderMMCE.LOGGER.error("Expected client harness fingerprint mismatch: {}", rejection);
    }

    private static Map<String, List<BlockPos>> groups(PonderScene scene) {
        require(scene.getWorld() instanceof PonderLevel, "Scene world has no named structure groups");
        return ((PonderLevel) scene.getWorld()).getStructureGroups();
    }

    private static void requireGroup(Map<String, List<BlockPos>> groups, String name) {
        List<BlockPos> positions = groups.get(name);
        require(positions != null && !positions.isEmpty(), "Missing or empty structure group " + name);
    }

    private void requestScreenshot(String name) {
        require(pendingScreenshot == null, "Another screenshot is pending");
        pendingScreenshot = name;
    }

    private void nextPhase() {
        phase++;
        phaseStarted = ticks;
        writeReport("RUNNING", null);
    }

    private void pass(String name, String detail) {
        checks.add(new Check(name, "PASS", detail));
    }

    private void complete() {
        terminal = true;
        pass("complete", "Automated MMCE scene smoke completed");
        writeReport("PASS", null);
        writeText(new File(outputDirectory, "client-mmce-ok.flag"), "PASS\n");
    }

    private void fail(Throwable failure) {
        if (terminal) return;
        terminal = true;
        checks.add(new Check("phase." + phase, "FAIL", failure.toString()));
        writeReport("FAIL", failure);
        writeText(new File(outputDirectory, "client-mmce-failed.flag"), failure.toString() + "\n");
    }

    private void writeReport(String status, Throwable failure) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("status", status);
        report.addProperty("scope", "automated_ponder_mmce_scene_and_subject_harness");
        report.addProperty("doesNotReplaceManualKeyboardMouseAcceptance", true);
        report.addProperty("startedAt", startedAt);
        report.addProperty("updatedAt", System.currentTimeMillis());
        report.addProperty("ticks", ticks);
        report.addProperty("phase", phase);
        report.addProperty("phaseTicks", ticks - phaseStarted);

        JsonObject versions = new JsonObject();
        versions.addProperty("minecraft", "1.12.2");
        versions.addProperty("forge", ForgeVersion.getVersion());
        for (String modId : new String[] {"ponder_legacy", "ponder_mmce", "modularmachinery",
                "crafttweaker", "mixinbooter"}) {
            ModContainer mod = Loader.instance().getIndexedModList().get(modId);
            versions.addProperty(modId, mod == null ? "missing" : mod.getVersion());
        }
        report.add("versions", versions);

        JsonObject hashes = new JsonObject();
        addModHash(hashes, "ponder_legacy");
        addModHash(hashes, "ponder_mmce");
        report.add("sha256", hashes);

        JsonArray checkData = new JsonArray();
        for (Check check : checks) {
            JsonObject value = new JsonObject();
            value.addProperty("name", check.name);
            value.addProperty("status", check.status);
            value.addProperty("detail", check.detail);
            checkData.add(value);
        }
        report.add("checks", checkData);

        JsonArray screenshotData = new JsonArray();
        for (Screenshot screenshot : screenshots) {
            JsonObject value = new JsonObject();
            value.addProperty("file", "screenshots/" + screenshot.name);
            value.addProperty("width", screenshot.width);
            value.addProperty("height", screenshot.height);
            value.addProperty("sampledColors", screenshot.sampledColors);
            value.addProperty("sha256", screenshot.sha256);
            screenshotData.add(value);
        }
        report.add("screenshots", screenshotData);
        if (failure != null) report.addProperty("failure", failure.toString());
        writeText(new File(outputDirectory, "ponder-mmce-client-report.json"), GSON.toJson(report) + "\n");
    }

    private void addModHash(JsonObject target, String modId) {
        ModContainer mod = Loader.instance().getIndexedModList().get(modId);
        if (mod == null || mod.getSource() == null || !mod.getSource().isFile()) {
            target.addProperty(modId, "unavailable");
            return;
        }
        try {
            target.addProperty(modId, sha256(mod.getSource()));
        } catch (IOException failure) {
            target.addProperty(modId, "error:" + failure.getClass().getSimpleName());
        }
    }

    private void prepareOutput() {
        if (!outputDirectory.isDirectory() && !outputDirectory.mkdirs())
            throw new IllegalStateException("Could not create output directory " + outputDirectory);
        if (!screenshotDirectory.isDirectory() && !screenshotDirectory.mkdirs())
            throw new IllegalStateException("Could not create screenshot directory " + screenshotDirectory);
        delete(new File(outputDirectory, "client-mmce-ok.flag"));
        delete(new File(outputDirectory, "client-mmce-failed.flag"));
    }

    private static int sampledColors(BufferedImage image) {
        Map<Integer, Boolean> colors = new LinkedHashMap<Integer, Boolean>();
        int stepX = Math.max(1, image.getWidth() / 96);
        int stepY = Math.max(1, image.getHeight() / 64);
        for (int y = 0; y < image.getHeight(); y += stepY)
            for (int x = 0; x < image.getWidth(); x += stepX)
                colors.put(image.getRGB(x, y) & 0x00ffffff, Boolean.TRUE);
        return colors.size();
    }

    private static String sha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            try (FileInputStream input = new FileInputStream(file)) {
                int read;
                while ((read = input.read(buffer)) >= 0)
                    if (read > 0) digest.update(buffer, 0, read);
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest())
                result.append(String.format(Locale.ROOT, "%02X", value & 0xff));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void writeText(File file, String value) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs())
                throw new IOException("Could not create " + parent);
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                writer.write(value);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Could not write " + file, failure);
        }
    }

    private static void delete(File file) {
        if (file.exists() && !file.delete())
            throw new IllegalStateException("Could not delete stale file " + file);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    private static final class Check {
        final String name;
        final String status;
        final String detail;

        Check(String name, String status, String detail) {
            this.name = name;
            this.status = status;
            this.detail = detail;
        }
    }

    private static final class Screenshot {
        final String name;
        final int width;
        final int height;
        final int sampledColors;
        final String sha256;

        Screenshot(String name, int width, int height, int sampledColors, String sha256) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.sampledColors = sampledColors;
            this.sha256 = sha256;
        }
    }
}
