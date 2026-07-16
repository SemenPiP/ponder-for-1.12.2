package net.createmod.ponder.api.subject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class PonderSubjectResolversTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void higherPriorityResolverRunsFirst() {
        PonderSubjectResolvers.register(id("priority_low"), 10, resolverFor(Items.BOOK, "test:low"));
        PonderSubjectResolvers.register(id("priority_high"), 20, resolverFor(Items.BOOK, "test:high"));

        ResolvedPonderSubject result = PonderSubjectResolvers.resolve(new ItemStack(Items.BOOK));

        assertEquals(new ResourceLocation("test", "high"), result.getComponent());
        assertEquals(id("priority_high"), result.getResolverId());
    }

    @Test
    public void equalPriorityResolversKeepRegistrationOrder() {
        PonderSubjectResolvers.register(id("stable_first"), 15, resolverFor(Items.APPLE, "test:first"));
        PonderSubjectResolvers.register(id("stable_second"), 15, resolverFor(Items.APPLE, "test:second"));

        ResolvedPonderSubject result = PonderSubjectResolvers.resolve(new ItemStack(Items.APPLE));

        assertEquals(new ResourceLocation("test", "first"), result.getComponent());
        assertEquals(id("stable_first"), result.getResolverId());
    }

    @Test
    public void passFallsThroughToItemRegistryResolverLast() {
        PonderSubjectResolvers.register(id("always_pass"), Integer.MIN_VALUE, new ItemSubjectResolver() {
            @Override
            public SubjectResolverResult resolve(ItemStack stack) {
                return SubjectResolverResult.pass();
            }
        });

        ResolvedPonderSubject result = PonderSubjectResolvers.resolve(new ItemStack(Items.DIAMOND));
        List<ResourceLocation> ids = PonderSubjectResolvers.getRegisteredResolverIds();

        assertEquals(new ResourceLocation("minecraft", "diamond"), result.getComponent());
        assertEquals(PonderSubjectResolvers.ITEM_REGISTRY_RESOLVER_ID, result.getResolverId());
        assertTrue(result.isDefaultResolver());
        assertEquals(PonderSubjectResolvers.ITEM_REGISTRY_RESOLVER_ID, ids.get(ids.size() - 1));
    }

    @Test
    public void customHandledResultRetainsResolverSource() {
        PonderSubjectResolvers.register(id("custom_source"), 0, resolverFor(Items.PAPER, "test:virtual_component"));

        ResolvedPonderSubject result = PonderSubjectResolvers.resolve(new ItemStack(Items.PAPER));

        assertTrue(result.isHandled());
        assertFalse(result.isDefaultResolver());
        assertEquals(id("custom_source"), result.getResolverId());
        assertEquals(new ResourceLocation("test", "virtual_component"), result.getComponent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateResolverIdsAreRejected() {
        ResourceLocation duplicate = id("duplicate");
        PonderSubjectResolvers.register(duplicate, 0, passResolver());
        PonderSubjectResolvers.register(duplicate, 100, passResolver());
    }

    @Test(expected = IllegalArgumentException.class)
    public void defaultResolverIdIsReserved() {
        PonderSubjectResolvers.register(PonderSubjectResolvers.ITEM_REGISTRY_RESOLVER_ID, 0, passResolver());
    }

    private static ItemSubjectResolver resolverFor(final net.minecraft.item.Item item,
                                                   final String component) {
        return new ItemSubjectResolver() {
            @Override
            public SubjectResolverResult resolve(ItemStack stack) {
                return stack.getItem() == item
                    ? SubjectResolverResult.handled(new ResourceLocation(component))
                    : SubjectResolverResult.pass();
            }
        };
    }

    private static ItemSubjectResolver passResolver() {
        return new ItemSubjectResolver() {
            @Override
            public SubjectResolverResult resolve(ItemStack stack) {
                return SubjectResolverResult.pass();
            }
        };
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
