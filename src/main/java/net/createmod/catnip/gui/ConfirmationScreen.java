package net.createmod.catnip.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class ConfirmationScreen extends AbstractSimiScreen {
    public enum Response{Confirm,ConfirmDontSave,Cancel}
    private GuiScreen source;private Consumer<Response> action=new Consumer<Response>(){@Override public void accept(Response response){}};private final List<String> text=new ArrayList<String>();private boolean centered,tristate;private int x,y;
    public ConfirmationScreen removeTextLines(int amount){int from=Math.max(0,text.size()-Math.max(0,amount));text.subList(from,text.size()).clear();return this;}public ConfirmationScreen clearText(){text.clear();return this;}public ConfirmationScreen addText(String line){text.add(line==null?"":line);return this;}public ConfirmationScreen withText(String line){return clearText().addText(line);}public ConfirmationScreen at(int x,int y){this.x=Math.max(0,x);this.y=Math.max(0,y);centered=false;return this;}public ConfirmationScreen centered(){centered=true;return this;}
    public ConfirmationScreen withAction(final Consumer<Boolean> callback){action=new Consumer<Response>(){@Override public void accept(Response response){callback.accept(response==Response.Confirm);}};return this;}public ConfirmationScreen withThreeActions(Consumer<Response> callback){action=callback;tristate=true;return this;}
    public void open(GuiScreen source){this.source=source;ScreenOpener.open(this);}
    @Override public void initGui(){buttonList.clear();int boxWidth=Math.min(320,width-24),boxHeight=Math.max(44,text.size()*fontRenderer.FONT_HEIGHT+20);if(centered){x=(width-boxWidth)/2;y=(height-boxHeight)/2;}else{x=Math.min(x,width-boxWidth);y=Math.min(y,height-boxHeight-24);}int count=tristate?3:2,buttonWidth=80,start=x+(boxWidth-(count*buttonWidth+(count-1)*8))/2,by=y+boxHeight+6;addButton(new GuiButton(1,start,by,buttonWidth,20,tristate?"Save":"Confirm"));if(tristate)addButton(new GuiButton(2,start+88,by,buttonWidth,20,"Don't Save"));addButton(new GuiButton(3,start+(count-1)*88,by,buttonWidth,20,"Cancel"));}
    @Override protected void actionPerformed(GuiButton button){accept(button.id==1?Response.Confirm:button.id==2?Response.ConfirmDontSave:Response.Cancel);}
    private void accept(Response response){mc.displayGuiScreen(source);action.accept(response);}
    @Override protected void renderWindowBackground(int mouseX,int mouseY,float partialTicks){if(source!=null)source.drawScreen(-100,-100,partialTicks);Gui.drawRect(0,0,width,height,0x99101010);}
    @Override protected void renderWindow(int mouseX,int mouseY,float partialTicks){int boxWidth=Math.min(320,width-24),boxHeight=Math.max(44,text.size()*fontRenderer.FONT_HEIGHT+20);Gui.drawRect(x,y,x+boxWidth,y+boxHeight,0xee171b20);int lineY=y+10;for(String line:text){fontRenderer.drawString(line,x+10,lineY,0xffeeeeee);lineY+=fontRenderer.FONT_HEIGHT+2;}}
    @Override public boolean doesGuiPauseGame(){return true;}
}
