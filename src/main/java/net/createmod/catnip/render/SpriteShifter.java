package net.createmod.catnip.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.util.ResourceLocation;

public final class SpriteShifter {
    private static final Map<String,SpriteShiftEntry> CACHE=new HashMap<String,SpriteShiftEntry>();
    private SpriteShifter(){}
    public static synchronized SpriteShiftEntry get(ResourceLocation original,ResourceLocation target){String key=original+"->"+target;SpriteShiftEntry entry=CACHE.get(key);if(entry==null){entry=new SpriteShiftEntry();entry.set(original,target);CACHE.put(key,entry);}return entry;}
    public static synchronized void clear(){CACHE.clear();}
}
