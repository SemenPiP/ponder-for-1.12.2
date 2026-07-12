package net.createmod.catnip.config;

import java.util.Objects;

public final class ConfigPath {
    private final String modId;
    private final ConfigType type;
    private final String category;
    private final String key;
    public ConfigPath(String modId, ConfigType type, String category, String key) {
        this.modId = validateSegment(modId, "mod id");
        this.type = Objects.requireNonNull(type, "type");
        this.category = validatePath(category, "category");
        this.key = validateSegment(key, "key");
    }
    public String getModId(){return modId;} public ConfigType getType(){return type;} public String getCategory(){return category;} public String getKey(){return key;}
    public static ConfigPath parse(String path) {
        if (path == null) throw new IllegalArgumentException("Config path cannot be null");
        int colon=path.indexOf(':'), lastDot=path.lastIndexOf('.');
        if(colon<=0||lastDot<=colon+1||lastDot==path.length()-1)throw new IllegalArgumentException("Expected modid:type.category.key");
        String mod=path.substring(0,colon), remainder=path.substring(colon+1);
        int firstDot=remainder.indexOf('.');
        if(firstDot<=0||firstDot>=remainder.length()-1)throw new IllegalArgumentException("Expected modid:type.category.key");
        ConfigType type=ConfigType.parse(remainder.substring(0,firstDot));
        String configPath=remainder.substring(firstDot+1);
        int split=configPath.lastIndexOf('.');
        if(split<=0||split==configPath.length()-1)throw new IllegalArgumentException("Expected a category and key");
        return new ConfigPath(mod,type,configPath.substring(0,split),configPath.substring(split+1));
    }
    public String toString(){return modId+":"+type.name().toLowerCase(java.util.Locale.ROOT)+"."+category+"."+key;}
    public boolean equals(Object o){if(this==o)return true;if(!(o instanceof ConfigPath))return false;ConfigPath p=(ConfigPath)o;return modId.equals(p.modId)&&type==p.type&&category.equals(p.category)&&key.equals(p.key);}
    public int hashCode(){return Objects.hash(modId,type,category,key);}
    private static String validateSegment(String value,String name){if(value==null||!value.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("Invalid "+name+": "+value);return value;}
    private static String validatePath(String value,String name){if(value==null||!value.matches("[A-Za-z0-9_\\-.]+")||value.startsWith(".")||value.endsWith(".")||value.contains(".."))throw new IllegalArgumentException("Invalid "+name+": "+value);return value;}
}
