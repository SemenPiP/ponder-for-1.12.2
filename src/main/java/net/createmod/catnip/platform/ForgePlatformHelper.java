package net.createmod.catnip.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.createmod.catnip.platform.services.PlatformHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

public class ForgePlatformHelper implements PlatformHelper {
    public Loader getLoader(){return Loader.FORGE;}
    public Env getEnv(){return FMLCommonHandler.instance().getSide().isClient()?Env.CLIENT:Env.SERVER;}
    public boolean isModLoaded(String modId){return net.minecraftforge.fml.common.Loader.isModLoaded(modId);}
    public boolean isDevelopmentEnvironment(){return FMLLaunchHandler.isDeobfuscatedEnvironment();}
    public List<String> getLoadedMods(){
        if(!net.minecraftforge.fml.common.Loader.instance().hasReachedState(LoaderState.CONSTRUCTING))return Collections.emptyList();
        List<String> result=new ArrayList<String>();for(ModContainer mod:net.minecraftforge.fml.common.Loader.instance().getActiveModList())result.add(mod.getModId());return result;
    }
    public String getModDisplayName(String modId){ModContainer mod=net.minecraftforge.fml.common.Loader.instance().getIndexedModList().get(modId);return mod==null?humanize(modId):mod.getName();}
    public void executeOnClientOnly(Supplier<Runnable> action){if(getEnv().isClient())action.get().run();}
    public void executeOnServerOnly(Supplier<Runnable> action){if(getEnv().isServer())action.get().run();}
    private static String humanize(String id){String[] words=id.replace('-','_').split("_");StringBuilder out=new StringBuilder();for(String word:words){if(word.isEmpty())continue;if(out.length()>0)out.append(' ');out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));}return out.toString();}
}
