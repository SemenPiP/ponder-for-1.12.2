package net.createmod.catnip.lang;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public final class FontHelper {
    public static final int MAX_WIDTH_PER_LINE = 200;
    private FontHelper() {}
    public static Style styleFromColor(TextFormatting color) { return new Style().setColor(color); }
    public static Style styleFromColor(int hex) { return new net.createmod.catnip.theme.Color(hex, false).asStyle(); }
    public static List<ITextComponent> cutStringTextComponent(String text, Palette palette) {
        return cutTextComponent(new TextComponentString(text), palette.primary(), palette.highlight(), 0);
    }
    public static List<ITextComponent> cutTextComponent(ITextComponent text, Palette palette) {
        return cutTextComponent(text, palette.primary(), palette.highlight(), 0);
    }
    public static List<ITextComponent> cutStringTextComponent(String text, Style primary, Style highlight) {
        return cutTextComponent(new TextComponentString(text), primary, highlight, 0);
    }
    public static List<ITextComponent> cutTextComponent(ITextComponent text, Style primary, Style highlight) {
        return cutTextComponent(text, primary, highlight, 0);
    }
    public static List<ITextComponent> cutStringTextComponent(String text, Style primary, Style highlight, int indent) {
        return cutTextComponent(new TextComponentString(text), primary, highlight, indent);
    }
    public static List<ITextComponent> cutTextComponent(ITextComponent component, Style primary, Style highlight, int indent) {
        List<String> words = new ArrayList<String>();
        BreakIterator iterator = BreakIterator.getLineInstance(Locale.getDefault());
        String raw = component.getUnformattedText(); iterator.setText(raw);
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) words.add(raw.substring(start, end));
        List<String> lines = new ArrayList<String>(); StringBuilder line = new StringBuilder(); int width = 0;
        for (String word : words) {
            int wordWidth = estimateWidth(word.replace("_", ""));
            if (width > 0 && width + wordWidth > MAX_WIDTH_PER_LINE) { lines.add(line.toString()); line.setLength(0); width = 0; }
            line.append(word); width += wordWidth;
        }
        if (line.length() > 0 || lines.isEmpty()) lines.add(line.toString());
        List<ITextComponent> result = new ArrayList<ITextComponent>(lines.size()); boolean highlighted = false;
        StringBuilder prefix = new StringBuilder(); for (int i = 0; i < Math.max(0, indent); i++) prefix.append(' ');
        for (String source : lines) {
            ITextComponent output = new TextComponentString(prefix.toString()).setStyle(primary.createShallowCopy());
            String[] parts = source.split("_", -1);
            for (String part : parts) { output.appendSibling(new TextComponentString(part).setStyle((highlighted ? highlight : primary).createShallowCopy())); highlighted = !highlighted; }
            highlighted = !highlighted;
            result.add(output);
        }
        return result;
    }
    private static int estimateWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') continue;
            if (c == ' ' || c == '!' || c == '.' || c == ',' || c == ':' || c == ';' || c == 'i' || c == 'l') width += 2;
            else if (c >= 0x2E80) width += 9;
            else width += 6;
        }
        return width;
    }
    public static final class Palette {
        public static final Palette STANDARD_CREATE = new Palette(styleFromColor(0xC9974C), styleFromColor(0xF1DD79));
        public static final Palette BLUE = ofColors(TextFormatting.BLUE, TextFormatting.AQUA);
        public static final Palette GREEN = ofColors(TextFormatting.DARK_GREEN, TextFormatting.GREEN);
        public static final Palette YELLOW = ofColors(TextFormatting.GOLD, TextFormatting.YELLOW);
        public static final Palette RED = ofColors(TextFormatting.DARK_RED, TextFormatting.RED);
        public static final Palette PURPLE = ofColors(TextFormatting.DARK_PURPLE, TextFormatting.LIGHT_PURPLE);
        public static final Palette GRAY = ofColors(TextFormatting.DARK_GRAY, TextFormatting.GRAY);
        public static final Palette ALL_GRAY = ofColors(TextFormatting.GRAY, TextFormatting.GRAY);
        public static final Palette GRAY_AND_BLUE = ofColors(TextFormatting.GRAY, TextFormatting.BLUE);
        public static final Palette GRAY_AND_WHITE = ofColors(TextFormatting.GRAY, TextFormatting.WHITE);
        public static final Palette GRAY_AND_GOLD = ofColors(TextFormatting.GRAY, TextFormatting.GOLD);
        public static final Palette GRAY_AND_RED = ofColors(TextFormatting.GRAY, TextFormatting.RED);
        private final Style primary, highlight;
        public Palette(Style primary, Style highlight) { this.primary = primary; this.highlight = highlight; }
        public Style primary() { return primary; }
        public Style highlight() { return highlight; }
        public static Palette ofColors(TextFormatting primary, TextFormatting highlight) {
            return new Palette(styleFromColor(primary), styleFromColor(highlight));
        }
    }
}
