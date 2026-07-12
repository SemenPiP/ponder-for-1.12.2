package net.createmod.catnip.lang;

import java.util.List;

import javax.annotation.Nullable;

import net.createmod.catnip.theme.Color;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class LangBuilder {
    private final String namespace;
    @Nullable private ITextComponent component;
    public LangBuilder(String namespace) {
        if (namespace == null || namespace.isEmpty()) throw new IllegalArgumentException("namespace cannot be empty");
        this.namespace = namespace;
    }
    public LangBuilder space() { return text(" "); }
    public LangBuilder newLine() { return text("\n"); }
    public LangBuilder translate(String langKey, Object... args) {
        return add(new TextComponentTranslation(namespace + "." + langKey, resolveBuilders(args)));
    }
    public LangBuilder text(String literalText) { return add(new TextComponentString(literalText)); }
    public LangBuilder text(TextFormatting format, String literalText) {
        return add(new TextComponentString(literalText).setStyle(new Style().setColor(format)));
    }
    public LangBuilder text(int color, String literalText) {
        return add(new TextComponentString(literalText).setStyle(new Color(color, false).asStyle()));
    }
    public LangBuilder add(LangBuilder other) { return add(other.component().createCopy()); }
    public LangBuilder add(ITextComponent customComponent) {
        if (customComponent == null) throw new NullPointerException("component");
        if (component == null) component = customComponent;
        else component.appendSibling(customComponent);
        return this;
    }
    public LangBuilder style(TextFormatting format) {
        assertComponent(); component.setStyle(component.getStyle().createShallowCopy().setColor(format)); return this;
    }
    public LangBuilder color(int color) {
        assertComponent(); component.setStyle(new Color(color, false).asStyle()); return this;
    }
    public LangBuilder color(Color color) { assertComponent(); component.setStyle(color.asStyle()); return this; }
    public ITextComponent component() { assertComponent(); return component; }
    public String string() { return component().getUnformattedText(); }
    public String json() { return ITextComponent.Serializer.componentToJson(component()); }
    public void sendStatus(EntityPlayer player) { player.sendStatusMessage(component(), true); }
    public void sendChat(EntityPlayer player) { player.sendMessage(component()); }
    public void addTo(List<? super ITextComponent> tooltip) { tooltip.add(component()); }
    public void forGoggles(List<? super ITextComponent> tooltip) { forGoggles(tooltip, 0); }
    public void forGoggles(List<? super ITextComponent> tooltip, int indents) {
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < 4 + Math.max(0, indents); i++) prefix.append(' ');
        tooltip.add(new TextComponentString(prefix.toString()).appendSibling(component().createCopy()));
    }
    private void assertComponent() { if (component == null) throw new IllegalStateException("No components were added to builder"); }
    public static Object[] resolveBuilders(Object[] args) {
        Object[] resolved = args.clone();
        for (int i = 0; i < resolved.length; i++) if (resolved[i] instanceof LangBuilder) resolved[i] = ((LangBuilder) resolved[i]).component();
        return resolved;
    }
}
