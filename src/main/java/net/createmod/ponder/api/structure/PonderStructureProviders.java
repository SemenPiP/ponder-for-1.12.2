package net.createmod.ponder.api.structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import net.minecraft.util.ResourceLocation;

public final class PonderStructureProviders {
    public static final ResourceLocation EXTERNAL_FILE_ID = new ResourceLocation("ponder", "external_file");
    public static final ResourceLocation RESOURCE_PACK_ID = new ResourceLocation("ponder", "resource_pack");
    public static final ResourceLocation JAR_ID = new ResourceLocation("ponder", "jar");
    public static final ResourceLocation DIRECT_ID = new ResourceLocation("ponder", "direct");
    public static final ResourceLocation MISSING_ID = new ResourceLocation("ponder", "missing");

    private static final Map<ResourceLocation, RegisteredProvider> PROVIDERS =
        new LinkedHashMap<ResourceLocation, RegisteredProvider>();
    private static long nextOrder;
    private static boolean servicesDiscovered;

    private PonderStructureProviders() {
    }

    public static synchronized void register(PonderStructureProvider provider) {
        discoverServices();
        registerInternal(provider);
    }

    public static synchronized boolean unregister(ResourceLocation providerId) {
        if (providerId == null)
            return false;
        RegisteredProvider removed = PROVIDERS.remove(providerId);
        if (removed == null)
            return false;
        removed.provider.invalidate();
        return true;
    }

    public static synchronized List<PonderStructureProvider> snapshot() {
        discoverServices();
        List<RegisteredProvider> registrations =
            new ArrayList<RegisteredProvider>(PROVIDERS.values());
        Collections.sort(registrations, new Comparator<RegisteredProvider>() {
            @Override
            public int compare(RegisteredProvider first, RegisteredProvider second) {
                int priority = second.priority < first.priority ? -1
                    : second.priority == first.priority ? 0 : 1;
                if (priority != 0)
                    return priority;
                return first.order < second.order ? -1 : first.order == second.order ? 0 : 1;
            }
        });
        List<PonderStructureProvider> result =
            new ArrayList<PonderStructureProvider>(registrations.size());
        for (RegisteredProvider registration : registrations) {
            if (!registration.id.equals(registration.provider.getId()))
                throw new IllegalStateException("Ponder structure provider changed its ID after registration: "
                    + registration.id + " -> " + registration.provider.getId());
            result.add(registration.provider);
        }
        return Collections.unmodifiableList(result);
    }

    public static void invalidate() {
        RuntimeException failure = null;
        for (PonderStructureProvider provider : snapshot()) {
            try {
                provider.invalidate();
            } catch (RuntimeException exception) {
                if (failure == null)
                    failure = exception;
                else
                    failure.addSuppressed(exception);
            }
        }
        if (failure != null)
            throw failure;
    }

    private static void discoverServices() {
        if (servicesDiscovered)
            return;
        long startingOrder = nextOrder;
        List<ResourceLocation> added = new ArrayList<ResourceLocation>();
        servicesDiscovered = true;
        try {
            for (PonderStructureProvider provider :
                ServiceLoader.load(PonderStructureProvider.class, PonderStructureProvider.class.getClassLoader())) {
                added.add(registerInternal(provider));
            }
        } catch (RuntimeException | ServiceConfigurationError failure) {
            for (ResourceLocation id : added)
                PROVIDERS.remove(id);
            nextOrder = startingOrder;
            servicesDiscovered = false;
            throw failure;
        }
    }

    private static ResourceLocation registerInternal(PonderStructureProvider provider) {
        if (provider == null)
            throw new IllegalArgumentException("Ponder structure provider is required");
        ResourceLocation id = provider.getId();
        if (id == null)
            throw new IllegalArgumentException("Ponder structure provider ID is required");
        if (isReserved(id))
            throw new IllegalArgumentException("Ponder structure provider ID is reserved: " + id);
        if (PROVIDERS.containsKey(id))
            throw new IllegalArgumentException("Duplicate Ponder structure provider ID: " + id);
        PROVIDERS.put(id, new RegisteredProvider(id, provider, provider.getPriority(), nextOrder++));
        return id;
    }

    private static boolean isReserved(ResourceLocation id) {
        return EXTERNAL_FILE_ID.equals(id) || RESOURCE_PACK_ID.equals(id) || JAR_ID.equals(id)
            || DIRECT_ID.equals(id) || MISSING_ID.equals(id);
    }

    private static final class RegisteredProvider {
        final ResourceLocation id;
        final PonderStructureProvider provider;
        final int priority;
        final long order;

        RegisteredProvider(ResourceLocation id, PonderStructureProvider provider, int priority, long order) {
            this.id = id;
            this.provider = provider;
            this.priority = priority;
            this.order = order;
        }
    }
}
