package net.createmod.catnip.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

import net.createmod.catnip.render.SuperByteBufferCache.Compartment;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;

public class SuperBufferSemanticsTest {
    @BeforeClass
    public static void bootstrapMinecraft() {
        Bootstrap.register();
    }

    @Test
    public void integerColorIsOpaqueRgbWhileComponentColorKeepsAlpha() throws Exception {
        SuperByteBuffer buffer = buffer();

        buffer.color(0x123456);
        assertEquals(0xff123456, colorOverride(buffer));

        buffer.color(0x12abcdef);
        assertEquals(0xffabcdef, colorOverride(buffer));

        buffer.color(0x12, 0x34, 0x56, 0x78);
        assertEquals(0x78123456, colorOverride(buffer));
    }

    @Test
    public void expiringCompartmentRefreshesOnAccessAndDeletesRemovedMesh() {
        final AtomicLong clock = new AtomicLong();
        final AtomicInteger builds = new AtomicInteger();
        SuperByteBufferCache cache = new SuperByteBufferCache(clock::get);
        Compartment<String> compartment = new Compartment<String>();
        cache.registerCompartment(compartment, 2);

        Callable<SuperByteBuffer> factory = new Callable<SuperByteBuffer>() {
            @Override
            public SuperByteBuffer call() {
                builds.incrementAndGet();
                return buffer();
            }
        };

        SuperByteBuffer first = cache.get(compartment, "key", factory);
        clock.addAndGet(99_000_000L);
        assertSame(first, cache.get(compartment, "key", factory));
        clock.addAndGet(99_000_000L);
        cache.cleanUp();
        assertFalse(first.isDeleted());

        clock.addAndGet(1_000_000L);
        cache.cleanUp();
        assertTrue(first.isDeleted());
        SuperByteBuffer second = cache.get(compartment, "key", factory);
        assertNotSame(first, second);
        assertEquals(2, builds.get());
    }

    @Test
    public void invalidationAndReregistrationDeleteCachedMeshes() {
        SuperByteBufferCache cache = new SuperByteBufferCache(new AtomicLong()::get);
        Compartment<String> compartment = new Compartment<String>();
        cache.registerCompartment(compartment);

        SuperByteBuffer invalidated = cache.get(compartment, "first", () -> buffer());
        cache.invalidate(compartment, "first");
        assertTrue(invalidated.isDeleted());

        SuperByteBuffer replaced = cache.get(compartment, "second", () -> buffer());
        cache.registerCompartment(compartment);
        assertTrue(replaced.isDeleted());
    }

    @Test
    public void failedFactoryResultIsNotCached() {
        SuperByteBufferCache cache = new SuperByteBufferCache(new AtomicLong()::get);
        Compartment<String> compartment = new Compartment<String>();
        cache.registerCompartment(compartment);
        AtomicInteger attempts = new AtomicInteger();

        try {
            cache.get(compartment, "key", () -> {
                attempts.incrementAndGet();
                throw new IllegalStateException("no world");
            });
            fail("Expected failed construction to propagate");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getCause() instanceof IllegalStateException);
        }

        SuperByteBuffer built = cache.get(compartment, "key", () -> {
            attempts.incrementAndGet();
            return buffer();
        });
        assertFalse(built.isEmpty());
        assertEquals(2, attempts.get());
    }

    @Test(expected = IllegalStateException.class)
    public void factoryRejectsMissingWorldInsteadOfReturningEmptyMesh() {
        IBlockState state = new Block(Material.ROCK).getDefaultState();
        new SuperBufferFactory().createForBlock(state, (World) null, (BlockRendererDispatcher) null);
    }

    @Test
    public void factoryUsesEveryForgeSupportedRenderLayer() {
        Block block = new Block(Material.ROCK) {
            @Override
            public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
                return layer == BlockRenderLayer.SOLID || layer == BlockRenderLayer.TRANSLUCENT;
            }
        };

        List<BlockRenderLayer> layers = SuperBufferFactory.renderLayersFor(block.getDefaultState());
        assertEquals(Arrays.asList(BlockRenderLayer.SOLID, BlockRenderLayer.TRANSLUCENT), layers);
    }

    private static SuperByteBuffer buffer() {
        MutableTemplateMesh mesh = new MutableTemplateMesh();
        mesh.add(0, 0, 0, 0xffffffff, 0, 0, 0, 0, 1, 0);
        return new SuperByteBuffer(mesh.toImmutable());
    }

    private static int colorOverride(SuperByteBuffer buffer) throws Exception {
        Field field = SuperByteBuffer.class.getDeclaredField("color");
        field.setAccessible(true);
        return field.getInt(buffer);
    }
}
