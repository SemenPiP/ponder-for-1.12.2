package net.createmod.ponder.mmce.structure;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hellfirepvp.modularmachinery.common.CommonProxy;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.common.machine.MachineLoader;
import hellfirepvp.modularmachinery.common.machine.MachineLoader.FileType;
import hellfirepvp.modularmachinery.common.util.BlockArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;

/**
 * Builds side-effect-contained machine snapshots before MMCE commits its formal
 * registry during post-init.
 */
final class MMCEPreviewMachineLoader {
    private static final Object LOCK = new Object();
    private static final Map<ResourceLocation, DynamicMachine> CACHE =
        new LinkedHashMap<ResourceLocation, DynamicMachine>();
    private static File cachedDirectory;
    private static String lastFailureSummary = "";

    private MMCEPreviewMachineLoader() {
    }

    static DynamicMachine find(ResourceLocation machineId) throws IOException {
        if (machineId == null) throw new IllegalArgumentException("MMCE machine id is required");
        synchronized (LOCK) {
            File directory = CommonProxy.dataHolder.getMachineryDirectory();
            if (directory == null)
                throw new IOException("MMCE machinery directory is not initialized");
            directory = directory.getAbsoluteFile();
            if (!directory.equals(cachedDirectory)) {
                CACHE.clear();
                cachedDirectory = directory;
            }
            DynamicMachine cached = CACHE.get(machineId);
            if (hasBlocks(cached)) return cached;
            loadAll(directory);
            DynamicMachine loaded = CACHE.get(machineId);
            if (!hasBlocks(loaded))
                throw new IOException("MMCE machine was not available in the preview parse: "
                    + machineId + lastFailureSummary);
            return loaded;
        }
    }

    static void invalidate() {
        synchronized (LOCK) {
            CACHE.clear();
            cachedDirectory = null;
            lastFailureSummary = "";
        }
    }

    private static void loadAll(File directory) throws IOException {
        if (!directory.isDirectory())
            throw new IOException("MMCE machinery directory does not exist: " + directory);

        Map<FileType, List<File>> discovered = MachineLoader.discoverDirectory(directory);
        List<File> machineFiles = files(discovered, FileType.MACHINE);
        if (machineFiles.isEmpty())
            throw new IOException("MMCE machinery directory contains no machine JSON files: " + directory);

        Map<String, BlockArray.BlockInformation> previousVariables =
            new LinkedHashMap<String, BlockArray.BlockInformation>(MachineLoader.VARIABLE_CONTEXT);
        Map<String, Exception> registrationFailures = Collections.emptyMap();
        Map<String, Exception> loadingFailures = Collections.emptyMap();
        try {
            MachineLoader.VARIABLE_CONTEXT.clear();
            MachineLoader.prepareContext(files(discovered, FileType.VARIABLES));
            clearFailures();

            List<Tuple<DynamicMachine, String>> registered = MachineLoader.registerMachines(machineFiles);
            registrationFailures = copyFailures();
            List<DynamicMachine> loaded = MachineLoader.loadMachines(registered);
            loadingFailures = copyFailures();
            for (DynamicMachine machine : loaded) {
                if (machine != null && machine.getRegistryName() != null && hasBlocks(machine))
                    CACHE.put(machine.getRegistryName(), machine);
            }
        } catch (RuntimeException failure) {
            throw new IOException("MMCE preview deserialization failed: " + failure.getMessage(), failure);
        } finally {
            clearFailures();
            MachineLoader.VARIABLE_CONTEXT.clear();
            MachineLoader.VARIABLE_CONTEXT.putAll(previousVariables);
        }

        if (CACHE.isEmpty()) {
            String detail = failureSummary(registrationFailures, loadingFailures);
            throw new IOException("MMCE preview deserialization produced no usable machines" + detail);
        }
        lastFailureSummary = failureSummary(registrationFailures, loadingFailures);
    }

    private static List<File> files(Map<FileType, List<File>> discovered, FileType type) {
        if (discovered == null) return Collections.emptyList();
        List<File> files = discovered.get(type);
        return files == null ? Collections.emptyList() : new ArrayList<File>(files);
    }

    private static boolean hasBlocks(DynamicMachine machine) {
        return machine != null && machine.getPattern() != null
            && machine.getPattern().getPattern() != null
            && !machine.getPattern().getPattern().isEmpty();
    }

    private static Map<String, Exception> copyFailures() {
        Map<String, Exception> failures = MachineLoader.captureFailedAttempts();
        return failures == null
            ? Collections.<String, Exception>emptyMap()
            : new LinkedHashMap<String, Exception>(failures);
    }

    private static void clearFailures() {
        MachineLoader.captureFailedAttempts();
    }

    private static String failureSummary(Map<String, Exception> registrationFailures,
                                         Map<String, Exception> loadingFailures) {
        List<String> failures = new ArrayList<String>();
        appendFailures(failures, "register", registrationFailures);
        appendFailures(failures, "load", loadingFailures);
        return failures.isEmpty() ? "" : ": " + String.join("; ", failures);
    }

    private static void appendFailures(List<String> output, String phase,
                                       Map<String, Exception> failures) {
        for (Map.Entry<String, Exception> entry : failures.entrySet()) {
            Exception failure = entry.getValue();
            String message = failure == null ? "unknown error" : failure.getMessage();
            output.add(phase + " " + entry.getKey() + " (" + message + ")");
        }
    }
}
