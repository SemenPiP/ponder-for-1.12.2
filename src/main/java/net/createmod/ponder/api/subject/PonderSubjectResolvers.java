package net.createmod.ponder.api.subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class PonderSubjectResolvers {
    public static final ResourceLocation ITEM_REGISTRY_RESOLVER_ID =
        new ResourceLocation("ponder", "item_registry");

    private static final List<Registration> RESOLVERS = new ArrayList<Registration>();
    private static final Set<ResourceLocation> RESOLVER_IDS = new HashSet<ResourceLocation>();
    private static long nextRegistrationOrder;

    private static final ItemSubjectResolver ITEM_REGISTRY_RESOLVER = new ItemSubjectResolver() {
        @Override
        public SubjectResolverResult resolve(ItemStack stack) {
            ResourceLocation itemId = Item.REGISTRY.getNameForObject(stack.getItem());
            return itemId == null
                ? SubjectResolverResult.pass()
                : SubjectResolverResult.handled(itemId);
        }
    };

    private static final Comparator<Registration> RESOLVER_ORDER = new Comparator<Registration>() {
        @Override
        public int compare(Registration left, Registration right) {
            int priority = Integer.compare(right.priority, left.priority);
            return priority != 0 ? priority : Long.compare(left.registrationOrder, right.registrationOrder);
        }
    };

    private PonderSubjectResolvers() {
    }

    /**
     * Registers a resolver. Higher priorities run first and equal priorities keep registration order.
     */
    public static void register(ResourceLocation id, ItemSubjectResolver resolver) {
        register(id, 0, resolver);
    }

    /**
     * Registers a resolver. Higher priorities run first and equal priorities keep registration order.
     */
    public static synchronized void register(ResourceLocation id, int priority, ItemSubjectResolver resolver) {
        if (id == null) throw new IllegalArgumentException("Subject resolver id may not be null");
        if (resolver == null) throw new IllegalArgumentException("Subject resolver may not be null");
        if (ITEM_REGISTRY_RESOLVER_ID.equals(id) || !RESOLVER_IDS.add(id))
            throw new IllegalArgumentException("Duplicate Ponder subject resolver id: " + id);
        RESOLVERS.add(new Registration(id, priority, nextRegistrationOrder++, resolver));
        Collections.sort(RESOLVERS, RESOLVER_ORDER);
    }

    public static ResolvedPonderSubject resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ResolvedPonderSubject.pass();

        List<Registration> snapshot;
        synchronized (PonderSubjectResolvers.class) {
            snapshot = new ArrayList<Registration>(RESOLVERS);
        }
        for (Registration registration : snapshot) {
            SubjectResolverResult result = registration.resolver.resolve(stack.copy());
            if (result == null)
                throw new IllegalStateException("Ponder subject resolver " + registration.id + " returned null");
            if (result.isHandled())
                return ResolvedPonderSubject.handled(result.getComponent(), registration.id, false);
        }

        SubjectResolverResult fallback = ITEM_REGISTRY_RESOLVER.resolve(stack);
        return fallback.isHandled()
            ? ResolvedPonderSubject.handled(fallback.getComponent(), ITEM_REGISTRY_RESOLVER_ID, true)
            : ResolvedPonderSubject.pass();
    }

    public static synchronized List<ResourceLocation> getRegisteredResolverIds() {
        List<ResourceLocation> result = new ArrayList<ResourceLocation>(RESOLVERS.size() + 1);
        for (Registration registration : RESOLVERS) result.add(registration.id);
        result.add(ITEM_REGISTRY_RESOLVER_ID);
        return Collections.unmodifiableList(result);
    }

    private static final class Registration {
        final ResourceLocation id;
        final int priority;
        final long registrationOrder;
        final ItemSubjectResolver resolver;

        Registration(ResourceLocation id, int priority, long registrationOrder, ItemSubjectResolver resolver) {
            this.id = id;
            this.priority = priority;
            this.registrationOrder = registrationOrder;
            this.resolver = resolver;
        }
    }
}
