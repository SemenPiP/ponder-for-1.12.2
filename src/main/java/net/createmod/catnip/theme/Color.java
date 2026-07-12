package net.createmod.catnip.theme;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;
import java.util.zip.CRC32;

import javax.vecmath.Vector3f;

import net.createmod.catnip.data.Couple;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.math.Vec3d;

public class Color {
    public static final Color TRANSPARENT_BLACK = new Color(0, 0, 0, 0).setImmutable();
    public static final Color BLACK = new Color(0, 0, 0).setImmutable();
    public static final Color WHITE = new Color(255, 255, 255).setImmutable();
    public static final Color RED = new Color(255, 0, 0).setImmutable();
    public static final Color GREEN = new Color(0, 255, 0).setImmutable();
    public static final Color PURPLE = new Color(128, 0, 128).setImmutable();
    public static final Color SPRING_GREEN = new Color(0, 255, 187).setImmutable();
    protected boolean mutable = true;
    protected int value;
    public Color(int r, int g, int b) { this(r, g, b, 255); }
    public Color(int r, int g, int b, int a) { value = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255; }
    public Color(float r, float g, float b, float a) { this(round(r), round(g), round(b), round(a)); }
    public Color(int argb) { value = argb; }
    public Color(int rgb, boolean hasAlpha) { value = hasAlpha ? rgb : rgb | 0xFF000000; }
    public Color copy() { return copy(true); }
    public Color copy(boolean mutable) { Color copy = new Color(value); if (!mutable) copy.setImmutable(); return copy; }
    public Color setImmutable() { mutable = false; return this; }
    public int getRed() { return value >>> 16 & 255; }
    public int getGreen() { return value >>> 8 & 255; }
    public int getBlue() { return value & 255; }
    public int getAlpha() { return value >>> 24 & 255; }
    public float getRedAsFloat() { return getRed() / 255f; }
    public float getGreenAsFloat() { return getGreen() / 255f; }
    public float getBlueAsFloat() { return getBlue() / 255f; }
    public float getAlphaAsFloat() { return getAlpha() / 255f; }
    public int getRGB() { return value; }
    public Vec3d asVector() { return new Vec3d(getRedAsFloat(), getGreenAsFloat(), getBlueAsFloat()); }
    public Vector3f asVectorF() { return new Vector3f(getRedAsFloat(), getGreenAsFloat(), getBlueAsFloat()); }
    public Style asStyle() { return new Style().setColor(nearestFormatting()); }
    public Color setRed(int r) { return ensureMutable().setRedUnchecked(r); }
    public Color setGreen(int g) { return ensureMutable().setGreenUnchecked(g); }
    public Color setBlue(int b) { return ensureMutable().setBlueUnchecked(b); }
    public Color setAlpha(int a) { return ensureMutable().setAlphaUnchecked(a); }
    public Color setRed(float r) { return setRed((int) (255 * clamp01(r))); }
    public Color setGreen(float g) { return setGreen((int) (255 * clamp01(g))); }
    public Color setBlue(float b) { return setBlue((int) (255 * clamp01(b))); }
    public Color setAlpha(float a) { return setAlpha((int) (255 * clamp01(a))); }
    public Color scaleAlpha(float factor) { return setAlpha((int) (getAlpha() * clamp01(factor))); }
    public Color scaleAlphaForText(float factor) { return setAlpha(Math.max(5, (int) (getAlpha() * clamp01(factor)))); }
    public Color mixWith(Color other, float weight) {
        float w = clamp01(weight);
        return ensureMutable().setRedUnchecked(lerp(getRed(), other.getRed(), w)).setGreenUnchecked(lerp(getGreen(), other.getGreen(), w))
            .setBlueUnchecked(lerp(getBlue(), other.getBlue(), w)).setAlphaUnchecked(lerp(getAlpha(), other.getAlpha(), w));
    }
    public Color darker() { int alpha = getAlpha(); return ensureMutable().mixWith(BLACK, .25f).setAlphaUnchecked(alpha); }
    public Color brighter() { int alpha = getAlpha(); return ensureMutable().mixWith(WHITE, .25f).setAlphaUnchecked(alpha); }
    public Color setValue(int value) { return ensureMutable().setValueUnchecked(value); }
    public Color modifyValue(UnaryOperator<Integer> function) { int changed = function.apply(value); return changed == value ? this : ensureMutable().setValueUnchecked(changed); }
    public Color ensureMutable() { return mutable ? this : new Color(value); }
    protected Color setRedUnchecked(int r) { value = value & 0xFF00FFFF | (r & 255) << 16; return this; }
    protected Color setGreenUnchecked(int g) { value = value & 0xFFFF00FF | (g & 255) << 8; return this; }
    protected Color setBlueUnchecked(int b) { value = value & 0xFFFFFF00 | b & 255; return this; }
    protected Color setAlphaUnchecked(int a) { value = value & 0x00FFFFFF | (a & 255) << 24; return this; }
    protected Color setValueUnchecked(int value) { this.value = value; return this; }
    public static Color mixColors(Color first, Color second, float weight) { return first.copy().mixWith(second, weight); }
    public static Color mixColors(Couple<Color> colors, float weight) { return mixColors(colors.getFirst(), colors.getSecond(), weight); }
    public static int mixColors(int first, int second, float weight) { return new Color(first).mixWith(new Color(second), weight).getRGB(); }
    public static Color rainbowColor(int step) {
        int local = Math.floorMod(step, 1536), progress = local % 256, phase = local / 256;
        return new Color(colorInPhase(phase + 4, progress), colorInPhase(phase + 2, progress), colorInPhase(phase, progress));
    }
    private static int colorInPhase(int phase, int progress) {
        phase %= 6; if (phase <= 1) return 0; if (phase == 2) return progress; if (phase <= 4) return 255; return 255 - progress;
    }
    public static Color generateFromLong(long value) {
        CRC32 crc = new CRC32(); crc.update(ByteBuffer.allocate(8).putLong(value).array()); return rainbowColor((int) crc.getValue()).mixWith(WHITE, .5f);
    }
    private TextFormatting nearestFormatting() {
        TextFormatting[] values = {TextFormatting.BLACK,TextFormatting.DARK_BLUE,TextFormatting.DARK_GREEN,TextFormatting.DARK_AQUA,
            TextFormatting.DARK_RED,TextFormatting.DARK_PURPLE,TextFormatting.GOLD,TextFormatting.GRAY,TextFormatting.DARK_GRAY,
            TextFormatting.BLUE,TextFormatting.GREEN,TextFormatting.AQUA,TextFormatting.RED,TextFormatting.LIGHT_PURPLE,TextFormatting.YELLOW,TextFormatting.WHITE};
        int[] rgb = {0,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF};
        long best = Long.MAX_VALUE; TextFormatting result = TextFormatting.WHITE;
        for (int i = 0; i < rgb.length; i++) { int r=rgb[i]>>16&255,g=rgb[i]>>8&255,b=rgb[i]&255; long d=sq(getRed()-r)+sq(getGreen()-g)+sq(getBlue()-b); if(d<best){best=d;result=values[i];} }
        return result;
    }
    private static long sq(long value) { return value * value; }
    private static int lerp(int a, int b, float w) { return (int) (a + (b - a) * w); }
    private static int round(float value) { return (int) (.5f + 255 * clamp01(value)); }
    private static float clamp01(float value) { return Math.max(0, Math.min(1, value)); }
}
