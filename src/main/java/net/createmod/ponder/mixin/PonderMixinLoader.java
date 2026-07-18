package net.createmod.ponder.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.File;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.createmod.ponder.foundation.structure.PonderStructureLoader;
import net.createmod.ponder.script.BuiltinScriptInstaller;
import net.createmod.ponder.script.PonderJsonLoader;
import zone.rong.mixinbooter.IEarlyMixinLoader;

/** Registers Ponder's client hooks after MixinBooter has placed this jar on the launch classpath. */
@IFMLLoadingPlugin.Name("PonderMixinLoader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1100)
@IFMLLoadingPlugin.TransformerExclusions("net.createmod.ponder.mixin.PonderMixinLoader")
public final class PonderMixinLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ponder.json");
    }

    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override
    public void injectData(Map<String, Object> data) {
        Object location = data.get("mcLocation");
        if (!(location instanceof File)) return;
        File gameDirectory = (File) location;
        BuiltinScriptInstaller.installOnce(gameDirectory);
        PonderStructureLoader.setExternalRoot(new File(gameDirectory, "scripts/ponder/structures"));
        PonderJsonLoader.setRoot(new File(gameDirectory, "scripts/ponder/packs"));
    }
    @Override public String getAccessTransformerClass() { return null; }
}
