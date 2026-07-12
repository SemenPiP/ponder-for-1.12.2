package net.createmod.catnip.config;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Rule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.minecraftforge.common.config.Configuration;

public class ConfigModelTest {
    @BeforeClass public static void initializeForgeConfigHome() throws Exception {
        java.lang.reflect.Field home=net.minecraftforge.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);home.set(null,new File(System.getProperty("java.io.tmpdir")));
    }
    @Rule public final TemporaryFolder temporary=new TemporaryFolder();
    @Test public void valuesValidateAndPersist() throws Exception {
        File file=temporary.newFile("catnip.cfg");TestConfig config=new TestConfig();config.registerAll(new Configuration(file));
        assertEquals(3,(int)config.count.get());config.count.set(7);config.enabled.set(false);
        try{config.count.set(11);fail("Expected range failure");}catch(IllegalArgumentException expected){}
        TestConfig loaded=new TestConfig();loaded.registerAll(new Configuration(file));assertEquals(7,(int)loaded.count.get());assertFalse(loaded.enabled.get());
    }
    @Test public void groupCarriesRealMetadata() {
        TestConfig config=new TestConfig();assertEquals("general",config.general.getName());assertEquals(1,config.general.getDepth());assertEquals("General values",config.general.getComment());
    }
    private static class TestConfig extends ConfigBase {
        final ConfigGroup general=group(1,"general","General values");
        final ConfigInt count=i(3,0,10,"count");
        final ConfigBool enabled=b(true,"enabled");
        public String getName(){return "client";}
    }
}
