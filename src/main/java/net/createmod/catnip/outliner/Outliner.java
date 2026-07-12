package net.createmod.catnip.outliner;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import net.createmod.catnip.outliner.LineOutline.EndChasingLineOutline;
import net.createmod.catnip.outliner.Outline.OutlineParams;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class Outliner {
    private static final Outliner INSTANCE=new Outliner();
    private final Map<Object,OutlineEntry> outlines=new LinkedHashMap<Object,OutlineEntry>();
    private Outliner(){}
    public static Outliner getInstance(){return INSTANCE;}
    public synchronized OutlineParams showOutline(Object slot,Outline outline){outlines.put(slot,new OutlineEntry(outline,1));return outline.getParams();}
    public synchronized OutlineParams showLine(Object slot,Vec3d start,Vec3d end){return refresh(slot,new LineOutline(start,end),1).getParams();}
    public synchronized OutlineParams endChasingLine(Object slot,Vec3d start,Vec3d end,float progress,boolean lockStart){EndChasingLineOutline o=new EndChasingLineOutline(lockStart);o.set(start,end);o.setProgress(progress);return refresh(slot,o,1).getParams();}
    public synchronized OutlineParams showAABB(Object slot,AxisAlignedBB box){return showAABB(slot,box,1);}
    public synchronized OutlineParams showAABB(Object slot,AxisAlignedBB box,int ttl){OutlineEntry e=outlines.get(slot);ChasingAABBOutline o;if(e!=null&&e.outline instanceof ChasingAABBOutline)o=(ChasingAABBOutline)e.outline;else{o=new ChasingAABBOutline(box);outlines.put(slot,new OutlineEntry(o,ttl));}o.setImmediately(box);e=outlines.get(slot);e.ticksRemaining=Math.max(1,ttl);return o.getParams();}
    public synchronized OutlineParams chaseAABB(Object slot,AxisAlignedBB box){OutlineEntry e=outlines.get(slot);if(e==null||!(e.outline instanceof ChasingAABBOutline)){return showAABB(slot,box);}e.ticksRemaining=1;((ChasingAABBOutline)e.outline).target(box);return e.outline.getParams();}
    public synchronized OutlineParams showCluster(Object slot,Iterable<BlockPos> positions){return refresh(slot,new BlockClusterOutline(positions),1).getParams();}
    public synchronized OutlineParams showItem(Object slot,Vec3d pos,ItemStack stack){return refresh(slot,new ItemOutline(pos,stack),1).getParams();}
    public synchronized void keep(Object slot){OutlineEntry e=outlines.get(slot);if(e!=null)e.ticksRemaining=1;}
    public synchronized void remove(Object slot){outlines.remove(slot);}
    public synchronized Optional<OutlineParams> edit(Object slot){OutlineEntry e=outlines.get(slot);if(e==null)return Optional.empty();e.ticksRemaining=1;return Optional.of(e.outline.getParams());}
    public synchronized Map<Object,OutlineEntry> getOutlines(){return Collections.unmodifiableMap(new LinkedHashMap<Object,OutlineEntry>(outlines));}
    public synchronized void tickOutlines(){Iterator<OutlineEntry> it=outlines.values().iterator();while(it.hasNext()){OutlineEntry e=it.next();e.outline.tick();if(--e.ticksRemaining<-OutlineEntry.FADE_TICKS)it.remove();}}
    public synchronized void renderOutlines(Vec3d camera,float partialTicks){for(OutlineEntry e:outlines.values()){float alpha=e.ticksRemaining>=0?1:Math.max(0,(e.ticksRemaining+OutlineEntry.FADE_TICKS)/(float)OutlineEntry.FADE_TICKS);e.outline.getParams().alpha(alpha*alpha*alpha);if(alpha>0)e.outline.render(camera,partialTicks);}}
    public synchronized void clear(){outlines.clear();}
    private Outline refresh(Object slot,Outline replacement,int ttl){OutlineEntry old=outlines.get(slot);if(old==null||old.outline.getClass()!=replacement.getClass()){outlines.put(slot,new OutlineEntry(replacement,ttl));return replacement;}old.outline=replacement;old.ticksRemaining=Math.max(1,ttl);return replacement;}
    public static final class OutlineEntry{public static final int FADE_TICKS=8;private Outline outline;private int ticksRemaining;private OutlineEntry(Outline outline,int ttl){this.outline=outline;this.ticksRemaining=Math.max(1,ttl);}public Outline getOutline(){return outline;}public int getTicksTillRemoval(){return ticksRemaining;}}
}
