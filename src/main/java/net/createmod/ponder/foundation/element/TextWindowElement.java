package net.createmod.ponder.foundation.element;

import java.util.List;
import java.util.function.Supplier;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.PonderOverlayElement;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.render.SceneProjection;
import net.createmod.ponder.render.SceneProjection.ProjectedPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public class TextWindowElement extends PonderElementBase implements TextElementBuilder,PonderOverlayElement {
    private final PonderScene scene;
    private final int duration;
    private PonderPalette color=PonderPalette.WHITE;
    private Vec3d target;
    private boolean independent;
    private int independentY;
    private boolean nearTarget;
    private Supplier<String> text=new Supplier<String>(){@Override public String get(){return "";}};
    public TextWindowElement(PonderScene scene,int duration){this.scene=scene;this.duration=Math.max(1,duration);}
    @Override public TextElementBuilder colored(PonderPalette value){if(value!=null)color=value;return this;}
    @Override public TextElementBuilder pointAt(Vec3d value){target=value;return this;}
    @Override public TextElementBuilder independent(int y){independent=true;independentY=y;return this;}
    @Override public TextElementBuilder text(String value){text=scene.registerText(value);return this;}
    @Override public TextElementBuilder text(String value,Object... params){text=scene.registerText(value,params);return this;}
    @Override public TextElementBuilder sharedText(final ResourceLocation key){text=new Supplier<String>(){@Override public String get(){return PonderIndex.getLangAccess().getShared(key);}};return this;}
    @Override public TextElementBuilder sharedText(final ResourceLocation key,final Object... params){text=new Supplier<String>(){@Override public String get(){return PonderIndex.getLangAccess().getShared(key,params);}};return this;}
    @Override public TextElementBuilder sharedText(String key){return sharedText(new ResourceLocation(scene.getNamespace(),key));}
    @Override public TextElementBuilder sharedText(String key,Object... params){return sharedText(new ResourceLocation(scene.getNamespace(),key),params);}
    @Override public TextElementBuilder placeNearTarget(){nearTarget=true;return this;}
    @Override public TextElementBuilder attachKeyFrame(){scene.declareKeyframe(scene.getBuildCursor());return this;}
    @Override public void render(PonderScene scene,int mouseX,int mouseY,float partialTicks){
        FontRenderer font=Minecraft.getMinecraft().fontRenderer;int width=180;
        int x=(Minecraft.getMinecraft().currentScreen==null?0:Minecraft.getMinecraft().currentScreen.width/2)-width/2;
        int y=independent?independentY:18;
        if(target!=null){ProjectedPoint projected=SceneProjection.project(target);if(projected.visible){x=(int)projected.x+(nearTarget?14:-width/2);y=(int)projected.y-(nearTarget?10:48);}}
        List<String> lines=font.listFormattedStringToWidth(text.get(),width-12);int height=lines.size()*10+10;
        Gui.drawRect(x,y,x+width,y+height,0xcc101418);Gui.drawRect(x,y,x+3,y+height,0xff000000|color.getColor());
        int lineY=y+5;for(String line:lines){font.drawString(line,x+8,lineY,0xffeeeeee);lineY+=10;}
    }
    public int getDuration(){return duration;}
}
