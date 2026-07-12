package net.createmod.catnip.config;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import net.minecraftforge.common.config.Configuration;

public final class ConfigRegistry {
    private static final Map<String,ConfigBase> CONFIGS=new LinkedHashMap<String,ConfigBase>();
    private ConfigRegistry(){}
    public static synchronized <T extends ConfigBase> T register(String modId,ConfigType type,T config,File file){
        String key=key(modId,type);if(CONFIGS.containsKey(key))throw new IllegalStateException("Config already registered: "+key);
        config.registerAll(new Configuration(file));CONFIGS.put(key,config);return config;
    }
    @Nullable public static synchronized ConfigBase get(String modId,ConfigType type){return CONFIGS.get(key(modId,type));}
    public static synchronized ConfigBase.CValue<?> require(ConfigPath path){ConfigBase config=get(path.getModId(),path.getType());if(config==null)throw new IllegalArgumentException("Unknown config: "+path.getModId()+":"+path.getType());ConfigBase.CValue<?> value=config.find(path.getCategory(),path.getKey());if(value==null)throw new IllegalArgumentException("Unknown config value: "+path);return value;}
    public static synchronized void set(ConfigPath path,String serialized){ConfigBase config=get(path.getModId(),path.getType());if(config==null)throw new IllegalArgumentException("Unknown config: "+path.getModId()+":"+path.getType());ConfigBase.CValue<?> value=config.find(path.getCategory(),path.getKey());if(value==null)throw new IllegalArgumentException("Unknown config value: "+path);value.setSerialized(serialized);config.save();config.onReload();}
    public static synchronized String getSerialized(ConfigPath path){return require(path).serialize();}
    public static synchronized Map<String,ConfigBase> view(){return Collections.unmodifiableMap(new LinkedHashMap<String,ConfigBase>(CONFIGS));}
    public static synchronized Set<String> getModIds(){Set<String> result=new TreeSet<String>();for(String key:CONFIGS.keySet()){int split=key.indexOf(':');if(split>0)result.add(key.substring(0,split));}return Collections.unmodifiableSet(result);}
    private static String key(String modId,ConfigType type){return modId.toLowerCase(java.util.Locale.ROOT)+":"+type.name();}
}
