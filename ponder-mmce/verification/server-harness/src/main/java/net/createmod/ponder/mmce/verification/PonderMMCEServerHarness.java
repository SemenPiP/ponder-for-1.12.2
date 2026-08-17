package net.createmod.ponder.mmce.verification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import hellfirepvp.modularmachinery.common.item.ItemBlueprint;
import hellfirepvp.modularmachinery.common.machine.MachineRegistry;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.structure.PonderStructureProvider;
import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.createmod.ponder.api.subject.PonderSubjectResolvers;
import net.createmod.ponder.api.subject.ResolvedPonderSubject;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.mmce.PonderMMCE;
import net.createmod.ponder.mmce.script.MMCEStructureRef;
import net.createmod.ponder.mmce.script.MMCEStructures;
import net.createmod.ponder.mmce.structure.MMCEStructureProvider;
import net.createmod.ponder.script.ScriptSceneDefinition;
import net.createmod.ponder.script.ScriptSceneRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;

@Mod(
    modid = PonderMMCEServerHarness.MOD_ID,
    name = "Ponder-MMCE Server Harness",
    version = "0.1.0-alpha",
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:ponder_legacy@[1.3.0-alpha.1-mc1.12.2];"
        + "required-after:modularmachinery@[2.3.2,)"
)
public final class PonderMMCEServerHarness {
    public static final String MOD_ID = "ponder_mmce_server_harness";
    private static final String FORCE_INCOMPATIBLE_ABI_PROPERTY =
        "ponder.mmce.verification.forceIncompatibleAbi";
    private static final ResourceLocation STATIC_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_static_demo");
    private static final ResourceLocation DYNAMIC_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_dynamic_demo");
    private static final ResourceLocation UNCONFIGURED_MACHINE =
        new ResourceLocation("modularmachinery", "ponder_mmce_unconfigured");

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        Properties report = new Properties();
        try {
            verifyMachines();
            if (Boolean.getBoolean(FORCE_INCOMPATIBLE_ABI_PROPERTY)) {
                verifyIncompatibleAbiIsolation(report);
            } else if (isFingerprintPhase()) {
                verifyFingerprintIsolation(report);
            } else {
                verifyStaticScene(report);
                verifyDynamicScene(report);
                verifyBlueprints(report);
            }
            report.setProperty("status", "PASS");
            writeReport(report);
            System.out.println("[Ponder-MMCE Fixture] PASS: real MMCE structures and blueprint subjects verified");
        } catch (Throwable failure) {
            report.setProperty("status", "FAIL");
            report.setProperty("error", failure.toString());
            try {
                writeReport(report);
            } catch (Exception suppressed) {
                failure.addSuppressed(suppressed);
            }
            throw new IllegalStateException("Ponder-MMCE server fixture verification failed", failure);
        }
    }

    private static boolean isFingerprintPhase() {
        File config = Loader.instance().getConfigDir();
        return new File(config.getParentFile(), "ponder-mmce-fingerprint-phase.properties").isFile();
    }

    private static void verifyMachines() {
        require(MachineRegistry.getRegistry().getMachine(STATIC_MACHINE) != null,
            "Static smoke machine was not loaded");
        require(MachineRegistry.getRegistry().getMachine(DYNAMIC_MACHINE) != null,
            "Dynamic smoke machine was not loaded");
        require(MachineRegistry.getRegistry().getMachine(UNCONFIGURED_MACHINE) != null,
            "Unconfigured smoke machine was not loaded");
    }

    private static void verifyStaticScene(Properties report) {
        MMCEStructureRef ref = MMCEStructures.machine(STATIC_MACHINE.toString());
        require(ref.sizeX > 0 && ref.sizeY > 0 && ref.sizeZ > 0,
            "Static structure dimensions were not resolved");
        require(ref.controllerX >= 0 && ref.controllerY >= 0 && ref.controllerZ >= 0,
            "Static controller was not normalized");

        PonderScene scene = onlyScene(ref.component, new ResourceLocation("ponder_mmce", "static_demo"));
        Map<String, List<BlockPos>> groups = groups(scene);
        requireGroup(groups, "mmce:all");
        requireGroup(groups, "mmce:controller");
        requireGroup(groups, "mmce:tag/frame");
        requireGroup(groups, "mmce:tag/item_input");
        requireGroup(groups, "mmce:tag/preview_nbt");
        requireNonNegative(groups);

        boolean previewFound = false;
        for (TileEntity tile : scene.getWorld().loadedTileEntityList) {
            NBTTagCompound data = tile.writeToNBT(new NBTTagCompound());
            if ("Ponder MMCE Preview".equals(data.getString("CustomName"))) {
                previewFound = true;
                break;
            }
        }
        require(previewFound, "Static structure did not apply preview NBT");
        report.setProperty("static.scene", scene.getId().toString());
        report.setProperty("static.structure", ref.structure);
        report.setProperty("static.fingerprint", ref.fingerprint);
        report.setProperty("static.size", ref.sizeX + "x" + ref.sizeY + "x" + ref.sizeZ);
    }

    private static void verifyDynamicScene(Properties report) {
        MMCEStructureRef ref = MMCEStructures.dynamic(
            DYNAMIC_MACHINE.toString(), "line", 3, "north", "north");
        PonderScene scene = onlyScene(ref.component, new ResourceLocation("ponder_mmce", "dynamic_demo"));
        Map<String, List<BlockPos>> groups = groups(scene);
        requireGroup(groups, "mmce:all");
        requireGroup(groups, "mmce:controller");
        requireGroup(groups, "mmce:dynamic/line/segment/0/frame");
        requireGroup(groups, "mmce:dynamic/line/segment/1/frame");
        requireGroup(groups, "mmce:dynamic/line/segment/2/frame");
        requireGroup(groups, "mmce:dynamic/line/end/output");
        requireNonNegative(groups);
        report.setProperty("dynamic.scene", scene.getId().toString());
        report.setProperty("dynamic.structure", ref.structure);
        report.setProperty("dynamic.fingerprint", ref.fingerprint);
        report.setProperty("dynamic.size", ref.sizeX + "x" + ref.sizeY + "x" + ref.sizeZ);
    }

    private static void verifyBlueprints(Properties report) {
        verifyBlueprint(STATIC_MACHINE, true);
        verifyBlueprint(DYNAMIC_MACHINE, true);
        verifyBlueprint(UNCONFIGURED_MACHINE, false);
        report.setProperty("blueprint.resolver", PonderMMCE.BLUEPRINT_RESOLVER_ID.toString());
        report.setProperty("blueprint.unconfigured", MMCEStructureRef.componentId(UNCONFIGURED_MACHINE).toString());
    }

    private static void verifyFingerprintIsolation(Properties report) {
        ResourceLocation staticComponent = MMCEStructureRef.componentId(STATIC_MACHINE);
        ResourceLocation dynamicComponent = MMCEStructureRef.componentId(DYNAMIC_MACHINE);
        ResourceLocation staleStructure = null;
        for (ScriptSceneDefinition definition : ScriptSceneRegistry.localSnapshot(true)) {
            if (new ResourceLocation("ponder_mmce", "stale_static").equals(definition.getSceneId())) {
                staleStructure = definition.getStructure();
                break;
            }
        }
        require(staleStructure != null, "Fingerprint fixture did not register the stale scene definition");
        String mismatch = null;
        try {
            MMCEStructureProvider.INSTANCE.find(staleStructure);
        } catch (IOException expected) {
            mismatch = expected.getMessage();
        }
        require(mismatch != null && mismatch.contains("fingerprint mismatch"),
            "Provider did not reject the stale MMCE structure fingerprint");
        PonderMMCE.LOGGER.error("Expected fixture fingerprint mismatch: {}", mismatch);
        require(PonderIndex.getSceneAccess().doScenesExistForId(dynamicComponent),
            "Valid dynamic scene was lost while filtering the stale static scene");
        PonderScene dynamic = onlyScene(dynamicComponent.toString(),
            new ResourceLocation("ponder_mmce", "dynamic_demo"));
        requireGroup(groups(dynamic), "mmce:dynamic/line/end/output");
        verifyBlueprint(STATIC_MACHINE, true);
        verifyBlueprint(DYNAMIC_MACHINE, true);
        report.setProperty("fingerprint.isolation", "PASS");
        report.setProperty("fingerprint.provider_rejection", mismatch);
        report.setProperty("fingerprint.client_filter", "verified-by-client-harness");
        report.setProperty("fingerprint.filtered_component", staticComponent.toString());
        report.setProperty("fingerprint.retained_component", dynamicComponent.toString());
    }

    private static void verifyIncompatibleAbiIsolation(Properties report) {
        for (PonderStructureProvider provider : PonderStructureProviders.snapshot())
            require(!PonderMMCE.PROVIDER_ID.equals(provider.getId()),
                "Ponder-MMCE provider was registered after ABI rejection");
        require(!PonderSubjectResolvers.getRegisteredResolverIds().contains(PonderMMCE.BLUEPRINT_RESOLVER_ID),
            "Ponder-MMCE blueprint resolver was registered after ABI rejection");
        require(!PonderIndex.getSceneAccess().doScenesExistForId(MMCEStructureRef.componentId(STATIC_MACHINE)),
            "Ponder-MMCE scenes were registered after ABI rejection");
        report.setProperty("abi.incompatible", "PASS");
        report.setProperty("abi.provider_disabled", "true");
        report.setProperty("abi.resolver_disabled", "true");
    }

    private static void verifyBlueprint(ResourceLocation machine, boolean expectsScene) {
        ItemStack blueprint = new ItemStack(new ItemBlueprint());
        ItemBlueprint.setAssociatedMachine(blueprint, machine);
        ResolvedPonderSubject resolved = PonderSubjectResolvers.resolve(blueprint);
        require(resolved.isHandled(), "Blueprint was not handled for " + machine);
        require(PonderMMCE.BLUEPRINT_RESOLVER_ID.equals(resolved.getResolverId()),
            "Blueprint used the wrong resolver for " + machine + ": " + resolved.getResolverId());
        require(MMCEStructureRef.componentId(machine).equals(resolved.getComponent()),
            "Blueprint resolved the wrong component for " + machine);
        require(PonderIndex.getSceneAccess().doScenesExistForId(resolved.getComponent()) == expectsScene,
            "Blueprint scene availability was wrong for " + machine);
    }

    private static PonderScene onlyScene(String component, ResourceLocation expectedScene) {
        List<PonderScene> scenes = PonderIndex.getSceneAccess().compile(new ResourceLocation(component));
        require(scenes.size() == 1, "Expected one scene for " + component + ", found " + scenes.size());
        PonderScene scene = scenes.get(0);
        require(expectedScene.equals(scene.getId()),
            "Expected scene " + expectedScene + " for " + component + ", found " + scene.getId());
        return scene;
    }

    private static Map<String, List<BlockPos>> groups(PonderScene scene) {
        require(scene.getWorld() instanceof PonderLevel, "Scene world does not expose named structure groups");
        return ((PonderLevel) scene.getWorld()).getStructureGroups();
    }

    private static void requireGroup(Map<String, List<BlockPos>> groups, String name) {
        List<BlockPos> positions = groups.get(name);
        require(positions != null && !positions.isEmpty(), "Missing or empty structure group " + name);
    }

    private static void requireNonNegative(Map<String, List<BlockPos>> groups) {
        for (Map.Entry<String, List<BlockPos>> group : groups.entrySet())
            for (BlockPos position : group.getValue())
                require(position.getX() >= 0 && position.getY() >= 0 && position.getZ() >= 0,
                    "Structure group " + group.getKey() + " contains negative position " + position);
    }

    private static void writeReport(Properties report) throws Exception {
        File config = Loader.instance().getConfigDir();
        File output = new File(config.getParentFile(), "ponder-mmce-fixture-harness.properties");
        try (OutputStream stream = new FileOutputStream(output)) {
            report.store(stream, "Ponder-MMCE real MMCE fixture verification");
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
