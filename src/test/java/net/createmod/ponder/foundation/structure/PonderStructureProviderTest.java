package net.createmod.ponder.foundation.structure;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.createmod.ponder.api.structure.PonderStructureProvider;
import net.createmod.ponder.api.structure.PonderStructureProviderResult;
import net.createmod.ponder.api.structure.PonderStructureProviders;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

public class PonderStructureProviderTest {
    private static final ResourceLocation FIRST = new ResourceLocation("test", "first");
    private static final ResourceLocation SECOND = new ResourceLocation("test", "second");
    private static final ResourceLocation THIRD = new ResourceLocation("test", "third");

    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @After
    public void clearProviders() {
        PonderStructureProviders.unregister(FIRST);
        PonderStructureProviders.unregister(SECOND);
        PonderStructureProviders.unregister(THIRD);
        PonderStructureLoader.setExternalRoot(null);
        PonderStructureLoader.setResourceProvider(null);
    }

    @Test
    public void registryRejectsDuplicateAndReservedIdsAndKeepsStablePriorityOrder() {
        PonderStructureProviders.register(provider(FIRST, 5,
            PonderStructureProviderResult.notFound(), null, null));
        PonderStructureProviders.register(provider(SECOND, 10,
            PonderStructureProviderResult.notFound(), null, null));
        PonderStructureProviders.register(provider(THIRD, 10,
            PonderStructureProviderResult.notFound(), null, null));

        List<ResourceLocation> ordered = new ArrayList<ResourceLocation>();
        for (PonderStructureProvider provider : PonderStructureProviders.snapshot())
            if (provider.getId().getNamespace().equals("test"))
                ordered.add(provider.getId());
        assertEquals(Arrays.asList(SECOND, THIRD, FIRST), ordered);

        try {
            PonderStructureProviders.register(provider(FIRST, 100,
                PonderStructureProviderResult.notFound(), null, null));
            fail("Duplicate provider IDs must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Duplicate"));
        }

        try {
            PonderStructureProviders.register(provider(PonderStructureProviders.JAR_ID, 0,
                PonderStructureProviderResult.notFound(), null, null));
            fail("Built-in provider IDs must be reserved");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("reserved"));
        }
    }

    @Test
    public void providersRunBeforeResourceLookupAndOnlyNotFoundContinues() throws Exception {
        byte[] bytes = bundledStructure();
        List<String> calls = new ArrayList<String>();
        Map<String, Collection<BlockPos>> groups =
            new LinkedHashMap<String, Collection<BlockPos>>();
        groups.put("provider_group", Collections.singletonList(new BlockPos(1, 0, 1)));
        PonderStructureProviders.register(provider(FIRST, 20,
            PonderStructureProviderResult.notFound(Collections.singletonList("expected miss")),
            calls, null));
        PonderStructureProviders.register(provider(SECOND, 10,
            PonderStructureProviderResult.found(bytes, "provider-fingerprint", groups,
                Collections.singletonList("provider diagnostic")), calls, null));

        AtomicInteger resourceCalls = new AtomicInteger();
        PonderStructureLoader.setResourceProvider(location -> {
            resourceCalls.incrementAndGet();
            return getClass().getClassLoader().getResourceAsStream(
                "assets/ponder/ponder/demo/basics.nbt");
        });

        PonderStructure structure = new PonderStructureLoader()
            .load(new ResourceLocation("ponder", "demo/basics"));
        assertEquals(Arrays.asList(FIRST.toString(), SECOND.toString()), calls);
        assertEquals(0, resourceCalls.get());
        assertEquals(SECOND, structure.getProviderId());
        assertEquals("provider-fingerprint", structure.getFingerprint());
        assertEquals(Collections.singletonList(new BlockPos(1, 0, 1)),
            structure.getGroup("provider_group"));
        assertTrue(structure.getDiagnostics().contains(FIRST + ": expected miss"));
        assertTrue(structure.getDiagnostics().contains("provider diagnostic"));
    }

    @Test
    public void providerErrorsTerminateWithoutTryingLowerSources() throws Exception {
        AtomicInteger lowerCalls = new AtomicInteger();
        AtomicInteger resourceCalls = new AtomicInteger();
        PonderStructureProviders.register(new PonderStructureProvider() {
            @Override public ResourceLocation getId() { return FIRST; }
            @Override public int getPriority() { return 20; }
            @Override public PonderStructureProviderResult find(ResourceLocation structureId)
                throws IOException {
                throw new IOException("provider exploded");
            }
        });
        PonderStructureProviders.register(provider(SECOND, 10,
            PonderStructureProviderResult.found(bundledStructure(), "lower"),
            null, lowerCalls));
        PonderStructureLoader.setResourceProvider(location -> {
            resourceCalls.incrementAndGet();
            return new java.io.ByteArrayInputStream(bundledStructure());
        });

        try {
            new PonderStructureLoader().load(new ResourceLocation("ponder", "demo/basics"));
            fail("Provider failures must terminate resolution");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains(FIRST.toString()));
            assertTrue(expected.getCause().getMessage().contains("provider exploded"));
        }
        assertEquals(0, lowerCalls.get());
        assertEquals(0, resourceCalls.get());
    }

    @Test
    public void externalFilesStayAheadOfRegisteredProviders() throws Exception {
        File root = temporary.newFolder("external-priority");
        File target = new File(root, "test/order.nbt");
        Files.createDirectories(target.toPath().getParent());
        Files.write(target.toPath(), bundledStructure());
        AtomicInteger providerCalls = new AtomicInteger();
        PonderStructureProviders.register(provider(FIRST, Integer.MAX_VALUE,
            PonderStructureProviderResult.found(bundledStructure(), "provider"),
            null, providerCalls));
        PonderStructureLoader.setExternalRoot(root);

        PonderStructure structure = new PonderStructureLoader()
            .load(new ResourceLocation("test", "order"));
        assertEquals(PonderStructureProviders.EXTERNAL_FILE_ID, structure.getProviderId());
        assertEquals(0, providerCalls.get());
    }

    @Test
    public void cacheInvalidationCallsEveryRegisteredProvider() {
        AtomicInteger firstInvalidations = new AtomicInteger();
        AtomicInteger secondInvalidations = new AtomicInteger();
        PonderStructureProviders.register(provider(FIRST, 0,
            PonderStructureProviderResult.notFound(), null, firstInvalidations));
        PonderStructureProviders.register(provider(SECOND, 0,
            PonderStructureProviderResult.notFound(), null, secondInvalidations));

        PonderStructureLoader.invalidateCaches();
        assertEquals(1, firstInvalidations.get());
        assertEquals(1, secondInvalidations.get());
    }

    @Test
    public void parsedCacheUsesFingerprintAndIsClearedByInvalidation() throws Exception {
        PonderStructureProviders.register(provider(FIRST, 0,
            PonderStructureProviderResult.found(bundledStructure(), "stable-fingerprint"),
            null, null));
        PonderStructureLoader loader = new PonderStructureLoader();
        ResourceLocation id = new ResourceLocation("test", "cached");

        PonderStructure first = loader.load(id);
        PonderStructure second = loader.load(id);
        assertSame(first, second);

        PonderStructureLoader.invalidateCaches();
        PonderStructure third = loader.load(id);
        assertNotSame(first, third);
    }

    @Test
    public void providerResultsDefensivelyCopyPayloadAndMetadata() {
        byte[] bytes = new byte[] {1, 2, 3};
        List<BlockPos> positions = new ArrayList<BlockPos>();
        positions.add(BlockPos.ORIGIN);
        Map<String, Collection<BlockPos>> groups =
            new LinkedHashMap<String, Collection<BlockPos>>();
        groups.put("group", positions);
        List<String> diagnostics = new ArrayList<String>();
        diagnostics.add("diagnostic");

        PonderStructureProviderResult result =
            PonderStructureProviderResult.found(bytes, "fingerprint", groups, diagnostics);
        bytes[0] = 9;
        positions.clear();
        diagnostics.clear();

        assertArrayEquals(new byte[] {1, 2, 3}, result.getNbtBytes());
        assertEquals(Collections.singletonList(BlockPos.ORIGIN), result.getGroups().get("group"));
        assertEquals(Collections.singletonList("diagnostic"), result.getDiagnostics());
        byte[] returned = result.getNbtBytes();
        returned[1] = 9;
        assertArrayEquals(new byte[] {1, 2, 3}, result.getNbtBytes());

        PonderStructureProviderResult missing = PonderStructureProviderResult.notFound();
        assertFalse(missing.isFound());
        assertNull(missing.getNbtBytes());
        assertNull(missing.getFingerprint());
    }

    private static PonderStructureProvider provider(ResourceLocation id, int priority,
                                                     PonderStructureProviderResult result,
                                                     List<String> calls,
                                                     AtomicInteger counter) {
        return new PonderStructureProvider() {
            @Override public ResourceLocation getId() { return id; }
            @Override public int getPriority() { return priority; }
            @Override public PonderStructureProviderResult find(ResourceLocation structureId) {
                if (calls != null) calls.add(id.toString());
                if (counter != null) counter.incrementAndGet();
                return result;
            }
            @Override public void invalidate() {
                if (counter != null) counter.incrementAndGet();
            }
        };
    }

    private byte[] bundledStructure() throws IOException {
        InputStream input = getClass().getClassLoader()
            .getResourceAsStream("assets/ponder/ponder/demo/basics.nbt");
        if (input == null)
            throw new IOException("Bundled basics structure is missing");
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0)
                if (read > 0) output.write(buffer, 0, read);
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
